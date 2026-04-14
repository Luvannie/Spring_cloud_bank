package com.banking.transaction.dto;

import com.banking.transaction.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for transaction details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    
    private UUID id;
    private UUID sagaId;
    private UUID sourceAccountId;
    private UUID targetAccountId;
    private String targetAccountNumber;
    private String targetBankCode;
    private BigDecimal amount;
    private String currency;
    private BigDecimal fee;
    private BigDecimal totalAmount;
    private Transaction.TransactionStatus status;
    private Transaction.TransactionType type;
    private String description;
    private String referenceNumber;
    private String idempotencyKey;
    private Instant createdAt;
    private Instant processedAt;
    
    /**
     * Creates a TransactionResponse from a Transaction entity.
     *
     * @param transaction the transaction entity
     * @return the response DTO
     */
    public static TransactionResponse from(Transaction transaction) {
        return TransactionResponse.builder()
            .id(transaction.getId())
            .sagaId(transaction.getSagaId())
            .sourceAccountId(transaction.getSourceAccountId())
            .targetAccountId(transaction.getTargetAccountId())
            .targetAccountNumber(transaction.getTargetAccountNumber())
            .targetBankCode(transaction.getTargetBankCode())
            .amount(transaction.getAmount())
            .currency(transaction.getCurrency())
            .fee(transaction.getFee())
            .totalAmount(transaction.getTotalAmount())
            .status(transaction.getStatus())
            .type(transaction.getType())
            .description(transaction.getDescription())
            .referenceNumber(transaction.getReferenceNumber())
            .idempotencyKey(transaction.getIdempotencyKey())
            .createdAt(transaction.getCreatedAt())
            .processedAt(transaction.getProcessedAt())
            .build();
    }
}