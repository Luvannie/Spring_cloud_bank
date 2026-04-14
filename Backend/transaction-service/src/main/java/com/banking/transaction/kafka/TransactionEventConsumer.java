package com.banking.transaction.kafka;

import com.banking.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Kafka consumer for transaction lifecycle events.
 * Handles transaction completion events from other services.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {
    
    private final TransactionService transactionService;
    
    /**
     * Consume transaction events from the transaction.events topic.
     *
     * @param event the transaction event payload
     */
    @KafkaListener(topics = "transaction.events", groupId = "transaction-service-group")
    public void consume(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        UUID transactionId = (UUID) event.get("transactionId");
        
        log.info("Received transaction event: {} for transaction: {}", eventType, transactionId);
        
        switch (eventType) {
            case "TRANSACTION_COMPLETED" -> handleTransactionCompleted(transactionId);
            case "TRANSACTION_FAILED" -> handleTransactionFailed(event);
            default -> log.warn("Unknown transaction event type: {}", eventType);
        }
    }
    
    private void handleTransactionCompleted(UUID transactionId) {
        transactionService.findById(transactionId)
            .ifPresent(transaction -> {
                if (transaction.getStatus() == com.banking.transaction.entity.Transaction.TransactionStatus.PROCESSING) {
                    transactionService.markCompleted(transactionId);
                    log.info("Marked transaction {} as completed", transactionId);
                }
            });
    }
    
    private void handleTransactionFailed(Map<String, Object> event) {
        UUID transactionId = (UUID) event.get("transactionId");
        String reason = (String) event.getOrDefault("reason", "Unknown error");
        
        transactionService.markFailed(transactionId, reason);
        log.warn("Marked transaction {} as failed: {}", transactionId, reason);
    }
}