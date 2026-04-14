package com.banking.notification.dto;

import com.banking.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Kafka event DTO for notification events.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {
    
    private String eventId;
    
    private String eventType;
    
    private java.util.UUID userId;
    
    private Notification.NotificationType type;
    
    private List<Notification.NotificationChannel> channels;
    
    private String recipient;
    
    private String subject;
    
    private String body;
    
    private Map<String, Object> data;
    
    private Instant timestamp;
}
