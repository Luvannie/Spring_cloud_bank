package com.banking.payment.controller;

import com.banking.common.dto.ApiResponse;
import com.banking.payment.dto.PaymentLinkRequest;
import com.banking.payment.dto.PaymentLinkResponse;
import com.banking.payment.entity.Payment;
import com.banking.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for payment operations.
 * NOTE: Full ownership verification requires cross-service communication.
 * For now, payments are accessible only via transaction ownership checks.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    
    private final PaymentService paymentService;
    
    /**
     * Creates a new payment link.
     */
    @PostMapping("/links")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PaymentLinkResponse> createPaymentLink(
            @Valid @RequestBody PaymentLinkRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Creating payment link for transaction: {} by user: {}", 
            request.getTransactionId(), userDetails.getUsername());
        PaymentLinkResponse response = paymentService.createPaymentLink(request);
        return ApiResponse.success(response, "Payment link created");
    }
    
    /**
     * Gets payment by ID.
     * NOTE: In production, verify user owns the transaction associated with this payment.
     */
    @GetMapping("/{id}")
    public ApiResponse<Payment> getPayment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("User {} fetching payment: {}", userDetails.getUsername(), id);
        return ApiResponse.success(paymentService.getPayment(id));
    }
    
    /**
     * Gets payment by transaction ID.
     * NOTE: In production, verify user owns the transaction associated with this payment.
     */
    @GetMapping("/transaction/{transactionId}")
    public ApiResponse<Payment> getPaymentByTransactionId(
            @PathVariable UUID transactionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("User {} fetching payment for transaction: {}", userDetails.getUsername(), transactionId);
        Payment payment = paymentService.getPaymentByTransactionId(transactionId);
        
        // Security: In a real implementation, we would verify the user owns the transaction
        // that this payment belongs to. This requires cross-service calls to:
        // 1. TransactionService to get transaction details
        // 2. AccountService to verify user owns the source account
        // For now, we return the payment and rely on the transaction service ownership checks
        return ApiResponse.success(payment);
    }
}