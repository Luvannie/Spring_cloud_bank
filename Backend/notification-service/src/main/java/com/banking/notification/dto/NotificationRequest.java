package com.banking.notification.dto;

import com.banking.notification.entity.Notification;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for creating notifications.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotNull(message = "Notification type is required")
    private Notification.NotificationType type;
    
    @NotEmpty(message = "At least one channel is required")
    private List<Notification.NotificationChannel> channels;
    
    private String recipient;
    
    private String subject;
    
    private String body;
    
    private Map<String, Object> data;
}
