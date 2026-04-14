package com.banking.transaction.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published to Kafka when a transaction occurs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEvent {
    
    private String eventId;
    private String eventType;
    private UUID transactionId;
    private UUID sagaId;
    private UUID sourceAccountId;
    private UUID targetAccountId;
    private BigDecimal amount;
    private String status;
    private Instant timestamp;
    private Object payload;
}