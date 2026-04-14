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
 * Main account entity representing a user's bank account.
 * Extends AuditableEntity for automatic audit fields (createdAt, updatedAt).
 */
@Entity
@Table(name = "accounts")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account extends AuditableEntity {
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;
    
    @Column(nullable = false)
    private BigDecimal balance;
    
    @Column(name = "reserved_balance", nullable = false)
    private BigDecimal reservedBalance;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;
    
    @Column(nullable = false)
    private String currency;
    
    @Column(name = "daily_transfer_limit")
    private BigDecimal dailyTransferLimit;
    
    @Column(name = "daily_transfer_used")
    private BigDecimal dailyTransferUsed;
    
    /**
     * Account type enumeration.
     */
    public enum AccountType {
        CHECKING,
        SAVINGS,
        BUSINESS
    }
    
    /**
     * Account status enumeration.
     */
    public enum AccountStatus {
        ACTIVE,
        FROZEN,
        CLOSED
    }
    
    /**
     * Calculates available balance by subtracting reserved balance from total balance.
     *
     * @return available balance for withdrawals/transfers
     */
    public BigDecimal getAvailableBalance() {
        return balance.subtract(reservedBalance);
    }
}
