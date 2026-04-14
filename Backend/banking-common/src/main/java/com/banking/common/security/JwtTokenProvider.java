package com.banking.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT token generation and validation using jjwt library.
 */
@Component
@Slf4j
public class JwtTokenProvider {
    
    @Value("${jwt.secret:ThisIsASecretKeyForJWTTokenGenerationThatShouldBeAtLeast256BitsLong}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;
    
    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration;
    
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * Extracts username from JWT token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * Extracts expiration date from JWT token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * Extracts a specific claim from JWT token.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
    
    /**
     * Checks if token is expired.
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    /**
     * Generates access token for user.
     */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username, jwtExpiration);
    }
    
    /**
     * Generates access token with custom claims.
     */
    public String generateToken(String username, Map<String, Object> additionalClaims) {
        return createToken(additionalClaims, username, jwtExpiration);
    }
    
    /**
     * Generates refresh token for user.
     */
    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username, refreshExpiration);
    }
    
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        return Jwts.builder()
            .claims(claims)
            .subject(subject)
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey())
            .compact();
    }
    
    /**
     * Validates JWT token.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("JWT token is expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("JWT token is unsupported: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }
    
    /**
     * Validates token for specific user.
     */
    public boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
}
