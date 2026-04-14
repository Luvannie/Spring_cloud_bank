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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for payment operations.
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
            @Valid @RequestBody PaymentLinkRequest request) {
        log.info("Creating payment link for transaction: {}", request.getTransactionId());
        PaymentLinkResponse response = paymentService.createPaymentLink(request);
        return ApiResponse.success(response, "Payment link created");
    }
    
    /**
     * Gets payment by ID.
     */
    @GetMapping("/{id}")
    public ApiResponse<Payment> getPayment(@PathVariable UUID id) {
        return ApiResponse.success(paymentService.getPayment(id));
    }
    
    /**
     * Gets payment by transaction ID.
     */
    @GetMapping("/transaction/{transactionId}")
    public ApiResponse<Payment> getPaymentByTransactionId(@PathVariable UUID transactionId) {
        return ApiResponse.success(paymentService.getPaymentByTransactionId(transactionId));
    }
}