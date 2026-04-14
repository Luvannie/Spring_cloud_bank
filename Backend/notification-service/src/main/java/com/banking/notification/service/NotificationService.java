package com.banking.notification.service;

import com.banking.notification.dto.NotificationEvent;
import com.banking.notification.dto.NotificationRequest;
import com.banking.notification.entity.Notification;
import com.banking.notification.entity.NotificationPreference;
import com.banking.notification.repository.NotificationPreferenceRepository;
import com.banking.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Main service for processing and sending notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    
    /**
     * Process a notification event from Kafka.
     */
    @Transactional
    public void processNotificationEvent(NotificationEvent event) {
        log.info("Processing notification event: {} for user: {}",
                event.getEventType(), event.getUserId());
        
        for (Notification.NotificationChannel channel : event.getChannels()) {
            if (!isChannelEnabled(event.getUserId(), event.getType(), channel)) {
                log.debug("Channel {} disabled for user {} and type {}",
                        channel, event.getUserId(), event.getType());
                continue;
            }
            
            Notification notification = Notification.builder()
                    .userId(event.getUserId())
                    .type(event.getType())
                    .channel(channel)
                    .recipient(event.getRecipient())
                    .subject(event.getSubject())
                    .body(event.getBody())
                    .status(Notification.NotificationStatus.PENDING)
                    .build();
            
            notificationRepository.save(notification);
            sendNotification(notification);
        }
    }
    
    /**
     * Create and send a notification from request.
     */
    @Transactional
    public Notification createNotification(NotificationRequest request) {
        log.info("Creating notification for user: {} via channels: {}",
                request.getUserId(), request.getChannels());
        
        Notification.NotificationChannel channel = request.getChannels().get(0);
        
        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .channel(channel)
                .recipient(request.getRecipient())
                .subject(request.getSubject())
                .body(request.getBody())
                .status(Notification.NotificationStatus.PENDING)
                .build();
        
        notification = notificationRepository.save(notification);
        sendNotification(notification);
        
        return notification;
    }
    
    /**
     * Get paginated notifications for a user.
     */
    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserId(userId, pageable);
    }
    
    /**
     * Get all notifications for a user.
     */
    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Mark a notification as read.
     */
    @Transactional
    public void markAsRead(UUID notificationId) {
        notificationRepository.findById(notificationId)
                .ifPresent(notification -> {
                    notification.setStatus(Notification.NotificationStatus.READ);
                    notification.setReadAt(Instant.now());
                    notificationRepository.save(notification);
                    log.debug("Notification {} marked as read", notificationId);
                });
    }
    
    /**
     * Check if a channel is enabled for a user and notification type.
     */
    private boolean isChannelEnabled(UUID userId, Notification.NotificationType type,
                                     Notification.NotificationChannel channel) {
        return preferenceRepository
                .findByUserIdAndNotificationTypeAndChannel(userId, type, channel)
                .map(NotificationPreference::getEnabled)
                .orElse(true); // Default to enabled if no preference set
    }
    
    /**
     * Send notification via appropriate channel.
     */
    private void sendNotification(Notification notification) {
        try {
            switch (notification.getChannel()) {
                case EMAIL -> emailService.send(notification);
                case SMS -> smsService.send(notification);
                case PUSH, IN_APP -> {
                    log.info("Push/In-app notification processed for ID: {}",
                            notification.getId());
                }
            }
            notification.setStatus(Notification.NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
            log.info("Notification sent successfully via {} channel",
                    notification.getChannel());
        } catch (Exception e) {
            log.error("Failed to send notification: {}", notification.getId(), e);
            notification.setStatus(Notification.NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
        }
        notificationRepository.save(notification);
    }
}
