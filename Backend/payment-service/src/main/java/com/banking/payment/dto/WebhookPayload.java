package com.banking.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for PayOS webhook payload validation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookPayload {
    
    private String orderCode;
    private String amount;
    private String status;
    private String transactionId;
    private String signature;
    private Instant paidAt;
}