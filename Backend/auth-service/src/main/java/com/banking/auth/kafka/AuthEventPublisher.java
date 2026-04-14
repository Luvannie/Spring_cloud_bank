package com.banking.auth.kafka;

import com.banking.auth.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Kafka event publisher for authentication events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthEventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String TOPIC = "auth.events";
    
    /**
     * Publishes a user login event.
     */
    public void publishLoginEvent(User user) {
        Map<String, Object> event = Map.of(
            "eventType", "USER_LOGIN",
            "userId", user.getId(),
            "username", user.getUsername(),
            "timestamp", Instant.now()
        );
        
        kafkaTemplate.send(TOPIC, user.getId().toString(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish login event for user: {}", user.getId(), ex);
                } else {
                    log.debug("Login event published for user: {}", user.getId());
                }
            });
    }
    
    /**
     * Publishes a user registration event.
     */
    public void publishUserRegisteredEvent(User user) {
        Map<String, Object> event = Map.of(
            "eventType", "USER_REGISTERED",
            "userId", user.getId(),
            "username", user.getUsername(),
            "email", user.getEmail(),
            "timestamp", Instant.now()
        );
        
        kafkaTemplate.send(TOPIC, user.getId().toString(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish registration event for user: {}", user.getId(), ex);
                } else {
                    log.debug("Registration event published for user: {}", user.getId());
                }
            });
    }
}
