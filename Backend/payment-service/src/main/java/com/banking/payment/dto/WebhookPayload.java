package com.banking.payment.dto;

import jakarta.validation.constraints.NotBlank;
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

    @NotBlank
    private String orderCode;

    @NotBlank
    private String amount;

    @NotBlank
    private String status;
    private String transactionId;

    @NotBlank
    private String signature;
    private Instant paidAt;
}
