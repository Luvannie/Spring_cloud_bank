package com.banking.account.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for reserving balance for a transaction.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReserveBalanceRequest {
    
    @NotNull
    private UUID transactionId;
    
    @NotNull
    private BigDecimal amount;
}