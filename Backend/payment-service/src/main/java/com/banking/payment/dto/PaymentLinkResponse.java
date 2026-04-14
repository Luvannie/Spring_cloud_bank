package com.banking.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for payment link creation containing checkout URL and QR code.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentLinkResponse {
    
    private UUID paymentId;
    private String checkoutUrl;
    private String qrCodeUrl;
    private Instant expiresAt;
}