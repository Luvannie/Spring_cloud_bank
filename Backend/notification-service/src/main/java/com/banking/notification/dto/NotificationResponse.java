package com.banking.notification.dto;

import com.banking.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for notification data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    
    private UUID id;
    
    private UUID userId;
    
    private Notification.NotificationType type;
    
    private Notification.NotificationChannel channel;
    
    private String recipient;
    
    private String subject;
    
    private String body;
    
    private Notification.NotificationStatus status;
    
    private Instant sentAt;
    
    private Instant readAt;
    
    private Instant createdAt;
    
    /**
     * Creates a NotificationResponse from a Notification entity.
     */
    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .channel(notification.getChannel())
                .recipient(notification.getRecipient())
                .subject(notification.getSubject())
                .body(notification.getBody())
                .status(notification.getStatus())
                .sentAt(notification.getSentAt())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
