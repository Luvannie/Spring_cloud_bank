package com.banking.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for transfer operations containing transaction and saga information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferResponse {
    
    private UUID transactionId;
    private UUID sagaId;
    private String referenceNumber;
    private TransferStatus status;
    private BigDecimal amount;
    private String message;
    
    /**
     * Transfer status enum representing the lifecycle states of a transfer.
     */
    public enum TransferStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}