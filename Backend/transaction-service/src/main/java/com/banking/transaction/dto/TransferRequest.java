package com.banking.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for initiating money transfers between accounts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequest {
    
    @NotNull(message = "Source account ID is required")
    private UUID sourceAccountId;
    
    private UUID targetAccountId;  // For internal transfers
    
    private String targetAccountNumber;  // For external transfers
    
    private String targetBankCode;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1000", message = "Minimum transfer amount is 1000 VND")
    private BigDecimal amount;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    private String idempotencyKey;
}