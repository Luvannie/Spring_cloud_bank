package com.banking.account.entity;

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
import java.util.UUID;

/**
 * Statement entity representing an account ledger entry/transaction record.
 * Extends AuditableEntity for id, version, and audit timestamps.
 */
@Entity
@Table(name = "statements")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Statement extends AuditableEntity {
    
    @Column(name = "account_id", nullable = false)
    private UUID accountId;
    
    @Column(name = "transaction_id")
    private UUID transactionId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "statement_type", nullable = false)
    private StatementType statementType;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Column(name = "balance_before", nullable = false)
    private BigDecimal balanceBefore;
    
    @Column(name = "balance_after", nullable = false)
    private BigDecimal balanceAfter;
    
    private String description;
    
    /**
     * Statement type enumeration for different transaction kinds.
     */
    public enum StatementType {
        CREDIT,
        DEBIT,
        RESERVE,
        RELEASE
    }
}
