package com.banking.notification.entity;

import com.banking.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Notification entity for storing user notifications.
 */
@Entity
@Table(name = "notifications")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends AuditableEntity {
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;
    
    private String recipient;
    
    private String subject;
    
    @Column(columnDefinition = "TEXT")
    private String body;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;
    
    @Column(name = "sent_at")
    private Instant sentAt;
    
    @Column(name = "read_at")
    private Instant readAt;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    /**
     * Notification type enum.
     */
    public enum NotificationType {
        TRANSFER,
        PAYMENT,
        LOGIN,
        SECURITY,
        MARKETING
    }
    
    /**
     * Notification channel enum.
     */
    public enum NotificationChannel {
        EMAIL,
        SMS,
        PUSH,
        IN_APP
    }
    
    /**
     * Notification status enum.
     */
    public enum NotificationStatus {
        PENDING,
        SENT,
        FAILED,
        READ
    }
}
