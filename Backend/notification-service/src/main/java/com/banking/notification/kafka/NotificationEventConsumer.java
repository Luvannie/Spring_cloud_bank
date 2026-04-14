package com.banking.notification.kafka;

import com.banking.notification.dto.NotificationEvent;
import com.banking.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for notification events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {
    
    private final NotificationService notificationService;
    
    /**
     * Consume notification events from the notify topic.
     */
    @KafkaListener(topics = "notify", groupId = "notification-service-group")
    public void consume(NotificationEvent event) {
        log.info("Received notification event: {}", event.getEventId());
        try {
            notificationService.processNotificationEvent(event);
        } catch (Exception e) {
            log.error("Error processing notification event: {}", event.getEventId(), e);
        }
    }
}
