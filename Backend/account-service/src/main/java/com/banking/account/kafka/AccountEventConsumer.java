package com.banking.account.kafka;

import com.banking.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka consumer for account-related events from other services.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountEventConsumer {
    
    private final AccountService accountService;
    
    /**
     * Consumes account events from the account.events topic.
     */
    @KafkaListener(topics = "account.events", groupId = "account-service-group")
    public void consume(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        log.info("Received account event: {}", eventType);
        
        switch (eventType) {
            case "ACCOUNT_CREATED" -> handleAccountCreated(event);
            case "ACCOUNT_FROZEN" -> handleAccountFrozen(event);
            case "ACCOUNT_STATUS_CHANGED" -> handleAccountStatusChanged(event);
            default -> log.warn("Unknown event type: {}", eventType);
        }
    }
    
    private void handleAccountCreated(Map<String, Object> event) {
        // Handle cross-service account creation events
        log.debug("Handling account created event: {}", event);
    }
    
    private void handleAccountFrozen(Map<String, Object> event) {
        // Handle account frozen events
        log.debug("Handling account frozen event: {}", event);
    }
    
    private void handleAccountStatusChanged(Map<String, Object> event) {
        // Handle account status change events
        log.debug("Handling account status changed event: {}", event);
    }
}