package com.banking.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for account balance information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceResponse {
    
    private UUID accountId;
    private BigDecimal balance;
    private BigDecimal reservedBalance;
    private BigDecimal availableBalance;
    private String currency;
}