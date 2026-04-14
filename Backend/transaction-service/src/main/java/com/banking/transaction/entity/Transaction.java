package com.banking.transaction.entity;

import com.banking.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Transaction entity representing financial transactions in the banking system.
 */
@Entity
@Table(name = "transactions")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends AuditableEntity {
    
    @Column(name = "saga_id", nullable = false)
    private UUID sagaId;
    
    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;
    
    @Column(name = "target_account_id")
    private UUID targetAccountId;
    
    @Column(name = "target_account_number")
    private String targetAccountNumber;
    
    @Column(name = "target_bank_code")
    private String targetBankCode;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Column(nullable = false)
    private String currency;
    
    @Column
    private BigDecimal fee;
    
    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;
    
    private String description;
    
    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;
    
    @Column(name = "reference_number", nullable = false, unique = true)
    private String referenceNumber;
    
    @Column(name = "payos_payment_id")
    private UUID payosPaymentId;
    
    @Column(name = "processed_at")
    private Instant processedAt;
    
    /**
     * Transaction status enum representing the lifecycle states of a transaction.
     */
    public enum TransactionStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
    
    /**
     * Transaction type enum representing different kinds of financial transactions.
     */
    public enum TransactionType {
        TRANSFER,
        DEPOSIT,
        WITHDRAWAL,
        PAYMENT,
        REFUND
    }
}
