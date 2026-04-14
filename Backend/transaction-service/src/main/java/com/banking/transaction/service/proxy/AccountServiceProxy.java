package com.banking.transaction.service.proxy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka-based proxy for Account Service operations.
 * Sends commands to account.service via Kafka messaging.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceProxy {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    /**
     * Reserve balance for a transaction.
     *
     * @param accountId the account ID
     * @param transactionId the transaction ID
     * @param amount the amount to reserve
     */
    public void reserveBalance(UUID accountId, UUID transactionId, BigDecimal amount) {
        Map<String, Object> event = Map.of(
            "action", "RESERVE_BALANCE",
            "accountId", accountId,
            "transactionId", transactionId,
            "amount", amount,
            "timestamp", Instant.now()
        );
        kafkaTemplate.send("account.commands", accountId.toString(), event);
        log.info("Sent RESERVE_BALANCE command for account: {}, amount: {}", accountId, amount);
    }
    
    /**
     * Commit the balance reservation after successful transfer.
     *
     * @param accountId the account ID
     * @param transactionId the transaction ID
     */
    public void commitReservation(UUID accountId, UUID transactionId) {
        Map<String, Object> event = Map.of(
            "action", "COMMIT_RESERVATION",
            "accountId", accountId,
            "transactionId", transactionId,
            "timestamp", Instant.now()
        );
        kafkaTemplate.send("account.commands", accountId.toString(), event);
        log.info("Sent COMMIT_RESERVATION command for account: {}", accountId);
    }
    
    /**
     * Rollback the balance reservation (compensation action).
     *
     * @param accountId the account ID
     * @param transactionId the transaction ID
     */
    public void rollbackReservation(UUID accountId, UUID transactionId) {
        Map<String, Object> event = Map.of(
            "action", "ROLLBACK_RESERVATION",
            "accountId", accountId,
            "transactionId", transactionId,
            "timestamp", Instant.now()
        );
        kafkaTemplate.send("account.commands", accountId.toString(), event);
        log.info("Sent ROLLBACK_RESERVATION command for account: {}", accountId);
    }
}