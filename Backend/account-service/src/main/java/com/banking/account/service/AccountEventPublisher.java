package com.banking.account.service;

import com.banking.account.entity.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka event publisher for account events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountEventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String TOPIC = "account.events";
    
    /**
     * Publishes account created event.
     */
    public void publishAccountCreated(Account account) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "ACCOUNT_CREATED");
        event.put("accountId", account.getId());
        event.put("userId", account.getUserId());
        event.put("accountNumber", account.getAccountNumber());
        event.put("accountType", account.getAccountType().name());
        event.put("timestamp", Instant.now());
        event.put("payload", Map.of(
            "balance", account.getBalance(),
            "currency", account.getCurrency()
        ));
        
        sendEvent(event, account.getId().toString());
    }
    
    /**
     * Publishes account status changed event.
     */
    public void publishAccountStatusChanged(Account account) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "ACCOUNT_STATUS_CHANGED");
        event.put("accountId", account.getId());
        event.put("userId", account.getUserId());
        event.put("status", account.getStatus().name());
        event.put("timestamp", Instant.now());
        
        sendEvent(event, account.getId().toString());
    }
    
    private void sendEvent(Map<String, Object> event, String key) {
        CompletableFuture<SendResult<String, Object>> future = 
            kafkaTemplate.send(TOPIC, key, event);
        
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event: {} - {}", event.get("eventType"), ex.getMessage());
            } else {
                log.debug("Published event: {} to partition: {}", 
                    event.get("eventType"), result.getRecordMetadata().partition());
            }
        });
    }
}