package com.banking.notification.repository;

import com.banking.notification.entity.Notification;
import com.banking.notification.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for NotificationPreference entity operations.
 */
@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {
    
    /**
     * Find all notification preferences for a user.
     */
    List<NotificationPreference> findByUserId(UUID userId);
    
    /**
     * Find a specific notification preference by user, type, and channel.
     */
    Optional<NotificationPreference> findByUserIdAndNotificationTypeAndChannel(
            UUID userId,
            Notification.NotificationType type,
            Notification.NotificationChannel channel
    );
}
