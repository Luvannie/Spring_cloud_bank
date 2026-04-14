package com.banking.auth.entity;

import com.banking.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Session entity for user session management.
 */
@Entity
@Table(name = "sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session extends BaseEntity {
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "session_id", nullable = false, unique = true)
    private String sessionId;
    
    @Column(name = "refresh_token")
    private String refreshToken;
    
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    private String ipAddress;
    
    private String userAgent;
    
    private Boolean revoked;
}
