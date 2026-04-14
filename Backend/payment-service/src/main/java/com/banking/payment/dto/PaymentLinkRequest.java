package com.banking.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for creating a payment link via PayOS.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentLinkRequest {
    
    @NotNull
    private UUID transactionId;
    
    @NotNull
    private BigDecimal amount;
    
    private String description;
    private String buyerEmail;
    private String buyerPhone;
    private String buyerName;
}