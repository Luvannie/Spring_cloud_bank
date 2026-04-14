package com.banking.notification.entity;

import com.banking.common.entity.BaseEntity;
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

import java.util.UUID;

/**
 * User notification preference entity for managing channel preferences.
 */
@Entity
@Table(name = "notification_preferences")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference extends BaseEntity {
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private Notification.NotificationType notificationType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Notification.NotificationChannel channel;
    
    @Column(nullable = false)
    private Boolean enabled;
}
