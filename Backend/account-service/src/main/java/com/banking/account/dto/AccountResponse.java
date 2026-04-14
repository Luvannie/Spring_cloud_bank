package com.banking.account.dto;

import com.banking.account.entity.Account;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for account information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {
    
    private UUID id;
    private String accountNumber;
    private Account.AccountType accountType;
    private BigDecimal balance;
    private BigDecimal availableBalance;
    private BigDecimal reservedBalance;
    private Account.AccountStatus status;
    private String currency;
    private Instant createdAt;
    
    /**
     * Creates AccountResponse from Account entity.
     */
    public static AccountResponse from(Account account) {
        return AccountResponse.builder()
            .id(account.getId())
            .accountNumber(account.getAccountNumber())
            .accountType(account.getAccountType())
            .balance(account.getBalance())
            .availableBalance(account.getAvailableBalance())
            .reservedBalance(account.getReservedBalance())
            .status(account.getStatus())
            .currency(account.getCurrency())
            .createdAt(account.getCreatedAt())
            .build();
    }
}