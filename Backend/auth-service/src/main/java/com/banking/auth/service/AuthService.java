package com.banking.auth.service;

import com.banking.auth.dto.LoginRequest;
import com.banking.auth.dto.LoginResponse;
import com.banking.auth.dto.RefreshTokenRequest;
import com.banking.auth.dto.RegisterRequest;
import com.banking.auth.dto.UserResponse;
import com.banking.auth.entity.Session;
import com.banking.auth.entity.User;
import com.banking.auth.kafka.AuthEventPublisher;
import com.banking.auth.repository.SessionRepository;
import com.banking.auth.repository.UserRepository;
import com.banking.common.exception.BankingException;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Main authentication service handling login, registration, logout, and token refresh.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthEventPublisher eventPublisher;
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String TOKEN_BLACKLIST_PREFIX = "blacklist:";
    private static final long ACCESS_TOKEN_EXPIRY_SECONDS = 900L; // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRY_DAYS = 7L;
    
    @Value("${security.max-login-attempts:5}")
    private int maxLoginAttempts;
    
    @Value("${security.lockout-duration-minutes:15}")
    private int lockoutDurationMinutes;
    
    /**
     * Authenticates a user and returns login response with tokens.
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());
        
        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new BankingException("INVALID_CREDENTIALS", "Invalid username or password"));
        
        // Check if account is locked
        if (user.getLockedUntil() != null && Instant.now().isBefore(user.getLockedUntil())) {
            throw new BankingException("ACCOUNT_LOCKED", "Account is temporarily locked");
        }
        
        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new BankingException("INVALID_CREDENTIALS", "Invalid username or password");
        }
        
        // Reset failed login attempts and unlock account
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        
        // Reset status from LOCKED to ACTIVE if account was locked
        if (user.getStatus() == User.UserStatus.LOCKED) {
            user.setStatus(User.UserStatus.ACTIVE);
        }
        
        userRepository.save(user);
        
        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        // Create session
        createSession(user, request, refreshToken);
        
        // Publish login event
        eventPublisher.publishLoginEvent(user);
        
        log.info("User logged in successfully: {}", request.getUsername());
        
        return LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(ACCESS_TOKEN_EXPIRY_SECONDS)
            .user(UserResponse.from(user))
            .build();
    }
    
    /**
     * Refreshes access token using a valid refresh token.
     * Implements refresh token rotation - old token is invalidated after use.
     */
    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        
        if (!jwtService.validateToken(refreshToken)) {
            throw new BankingException("INVALID_TOKEN", "Invalid refresh token");
        }
        
        Claims claims = jwtService.getClaims(refreshToken);
        String tokenType = claims.get("type", String.class);
        
        if (!"refresh".equals(tokenType)) {
            throw new BankingException("INVALID_TOKEN", "Not a refresh token");
        }
        
        UUID userId = UUID.fromString(claims.getSubject());
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BankingException("USER_NOT_FOUND", "User not found"));
        
        // Rotate refresh token - invalidate old one and create new session
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);
        
        // Rotate: revoke old session and create new one
        rotateRefreshToken(userId, refreshToken, newRefreshToken);
        
        return LoginResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .tokenType("Bearer")
            .expiresIn(ACCESS_TOKEN_EXPIRY_SECONDS)
            .user(UserResponse.from(user))
            .build();
    }
    
    /**
     * Rotates refresh token by revoking the old session and creating a new one.
     * This prevents token reuse attacks.
     */
    private void rotateRefreshToken(UUID userId, String oldRefreshToken, String newRefreshToken) {
        Session session = sessionRepository.findByRefreshToken(oldRefreshToken)
            .orElseThrow(() -> new BankingException("INVALID_TOKEN", "Invalid refresh token"));
        
        // Verify it belongs to this user
        if (!session.getUserId().equals(userId)) {
            throw new BankingException("INVALID_TOKEN", "Token does not belong to user");
        }
        
        // Verify not already revoked
        if (session.getRevoked()) {
            throw new BankingException("INVALID_TOKEN", "Refresh token already revoked");
        }
        
        // Invalidate old session
        session.setRevoked(true);
        sessionRepository.save(session);
        
        // Create new session with new refresh token
        Session newSession = Session.builder()
            .userId(userId)
            .sessionId(UUID.randomUUID().toString())
            .refreshToken(newRefreshToken)
            .expiresAt(Instant.now().plus(Duration.ofDays(REFRESH_TOKEN_EXPIRY_DAYS)))
            .createdAt(Instant.now())
            .revoked(false)
            .build();
        
        sessionRepository.save(newSession);
        log.debug("Refresh token rotated for user: {}", userId);
    }
    
    /**
     * Logs out a user by revoking all their sessions and blacklisting the access token.
     */
    @Transactional
    public void logout(String accessToken) {
        Claims claims = jwtService.getClaims(accessToken);
        UUID userId = UUID.fromString(claims.getSubject());
        
        // Add token to blacklist with TTL = remaining token lifetime
        addTokenToBlacklist(accessToken, claims);
        
        // Revoke all user sessions
        sessionRepository.revokeAllUserSessions(userId);
        log.info("User logged out: {}", userId);
    }
    
    /**
     * Adds a token to the Redis blacklist with appropriate TTL.
     */
    private void addTokenToBlacklist(String token, Claims claims) {
        Date expiration = claims.getExpiration();
        long ttlSeconds = (expiration.getTime() - System.currentTimeMillis()) / 1000;
        
        if (ttlSeconds > 0) {
            String blacklistKey = TOKEN_BLACKLIST_PREFIX + token;
            redisTemplate.opsForValue().set(blacklistKey, "revoked", Duration.ofSeconds(ttlSeconds));
            log.debug("Token added to blacklist with TTL: {} seconds", ttlSeconds);
        }
    }
    
    /**
     * Checks if a token has been revoked (blacklisted).
     */
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + token));
    }
    
    /**
     * Registers a new user.
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        log.info("Registration attempt for username: {}", request.getUsername());
        
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BankingException("USERNAME_EXISTS", "Username already taken");
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BankingException("EMAIL_EXISTS", "Email already registered");
        }
        
        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phone(request.getPhone())
            .status(User.UserStatus.ACTIVE)
            .failedLoginAttempts(0)
            .build();
        
        User saved = userRepository.save(user);
        
        eventPublisher.publishUserRegisteredEvent(saved);
        
        log.info("User registered successfully: {}", request.getUsername());
        
        return UserResponse.from(saved);
    }
    
    /**
     * Handles failed login attempt by incrementing counter and locking account if threshold reached.
     */
    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() + 1 : 1;
        user.setFailedLoginAttempts(attempts);
        
        // Lock account after max failed attempts for configured duration
        if (attempts >= maxLoginAttempts) {
            user.setLockedUntil(Instant.now().plus(Duration.ofMinutes(lockoutDurationMinutes)));
            user.setStatus(User.UserStatus.LOCKED);
            log.warn("Account locked due to {} failed login attempts: {}", attempts, user.getUsername());
        }
        
        userRepository.save(user);
    }
    
    /**
     * Creates a new session for a user after successful login.
     */
    private void createSession(User user, LoginRequest request, String refreshToken) {
        Session session = Session.builder()
            .userId(user.getId())
            .sessionId(UUID.randomUUID().toString())
            .refreshToken(refreshToken)
            .expiresAt(Instant.now().plus(Duration.ofDays(REFRESH_TOKEN_EXPIRY_DAYS)))
            .createdAt(Instant.now())
            .ipAddress(request.getDeviceId())  // Would be actual IP in production
            .revoked(false)
            .build();
        
        sessionRepository.save(session);
    }
    
    /**
     * Updates an existing session with a new refresh token.
     */
    private void updateSession(UUID userId, String newRefreshToken) {
        sessionRepository.findByUserIdAndRevokedFalse(userId.toString())
            .ifPresent(session -> {
                session.setRefreshToken(newRefreshToken);
                sessionRepository.save(session);
            });
    }
}
