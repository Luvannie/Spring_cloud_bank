package com.banking.auth.service;

import com.banking.auth.entity.User;
import com.banking.auth.repository.RoleRepository;
import com.banking.auth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Service for JWT token operations including generation, validation, and parsing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    
    private static final int MIN_SECRET_LENGTH = 32;
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;
    
    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;
    
    @PostConstruct
    public void validateJwtSecret() {
        if (jwtSecret == null || jwtSecret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                String.format("JWT secret must be at least %d characters for HS256. Current length: %d",
                    MIN_SECRET_LENGTH, jwtSecret != null ? jwtSecret.length() : 0));
        }
        log.info("JWT secret validation passed");
    }
    
    /**
     * Generates an access token for the given user.
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = Map.of(
            "sub", user.getId().toString(),
            "username", user.getUsername(),
            "email", user.getEmail(),
            "roles", getUserRoles(user.getId())
        );
        
        return Jwts.builder()
            .claims(claims)
            .subject(user.getId().toString())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
            .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
            .compact();
    }
    
    /**
     * Generates a refresh token for the given user.
     */
    public String generateRefreshToken(User user) {
        return Jwts.builder()
            .claims(Map.of("sub", user.getId().toString(), "type", "refresh"))
            .subject(user.getId().toString())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
            .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
            .compact();
    }
    
    /**
     * Validates a JWT token.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Extracts claims from a JWT token.
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
    
    /**
     * Extracts user ID from a JWT token.
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = getClaims(token);
        return UUID.fromString(claims.getSubject());
    }
    
    /**
     * Extracts roles from JWT token claims.
     */
    @SuppressWarnings("unchecked")
    public List<SimpleGrantedAuthority> getRoles(String token) {
        Claims claims = getClaims(token);
        List<String> roles = claims.get("roles", List.class);
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority(role))
            .toList();
    }
    
    /**
     * Retrieves user roles from the database.
     */
    private List<String> getUserRoles(UUID userId) {
        return roleRepository.findByUserId(userId)
            .stream()
            .map(role -> "ROLE_" + role.getName().toUpperCase())
            .toList();
    }
}
