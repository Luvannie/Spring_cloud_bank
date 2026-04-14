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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
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
    
    private static final long ACCESS_TOKEN_EXPIRY_SECONDS = 900L; // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRY_DAYS = 7L;
    
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
        
        // Reset failed login attempts
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
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
        
        // Generate new tokens
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);
        
        // Update session with new refresh token
        updateSession(user.getId(), newRefreshToken);
        
        return LoginResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .tokenType("Bearer")
            .expiresIn(ACCESS_TOKEN_EXPIRY_SECONDS)
            .user(UserResponse.from(user))
            .build();
    }
    
    /**
     * Logs out a user by revoking all their sessions.
     */
    @Transactional
    public void logout(String accessToken) {
        Claims claims = jwtService.getClaims(accessToken);
        UUID userId = UUID.fromString(claims.getSubject());
        
        sessionRepository.revokeAllUserSessions(userId);
        log.info("User logged out: {}", userId);
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
        
        // Lock account after 5 failed attempts for 15 minutes
        if (attempts >= 5) {
            user.setLockedUntil(Instant.now().plus(Duration.ofMinutes(15)));
            log.warn("Account locked due to failed login attempts: {}", user.getUsername());
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
