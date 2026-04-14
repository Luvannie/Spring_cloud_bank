package com.banking.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PayOS API response for order creation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayOSCreateOrderResponse {
    
    private String orderCode;
    private String checkoutUrl;
    private String qrCodeUrl;
}