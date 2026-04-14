package com.banking.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PayOS API request for order creation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayOSCreateOrderRequest {
    
    private String orderCode;
    private Integer amount;
    private String description;
    private String buyerEmail;
    private String buyerPhone;
    private String buyerName;
}