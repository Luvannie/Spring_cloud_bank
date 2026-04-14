package com.banking.notification.repository;

import com.banking.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Notification entity operations.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    
    /**
     * Find all notifications for a user ordered by creation date descending.
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    /**
     * Find notifications for a user with pagination.
     */
    Page<Notification> findByUserId(UUID userId, Pageable pageable);
    
    /**
     * Find all notifications with a specific status.
     */
    List<Notification> findByStatus(Notification.NotificationStatus status);
    
    /**
     * Find notifications with a specific status created before a threshold.
     */
    List<Notification> findByStatusAndCreatedAtBefore(
            Notification.NotificationStatus status, 
            Instant threshold
    );
}
