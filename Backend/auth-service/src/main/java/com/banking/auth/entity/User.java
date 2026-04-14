package com.banking.auth.entity;

import com.banking.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * User entity for authentication and authorization.
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends AuditableEntity {
    
    @Column(nullable = false, unique = true)
    private String username;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    @Column(nullable = false)
    private String firstName;
    
    private String lastName;
    
    @Column(nullable = false)
    private String phone;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;
    
    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts;
    
    @Column(name = "locked_until")
    private Instant lockedUntil;
    
    @Column(name = "last_login_at")
    private Instant lastLoginAt;
    
    public enum UserStatus {
        ACTIVE,
        INACTIVE,
        LOCKED,
        SUSPENDED
    }
}
