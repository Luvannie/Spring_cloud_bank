package com.banking.payment.service;

import com.banking.payment.dto.PaymentLinkRequest;
import com.banking.payment.dto.PaymentLinkResponse;
import com.banking.payment.entity.Payment;
import com.banking.payment.exception.PaymentException;
import com.banking.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Main payment service for orchestrating payment operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final PayOSService payOSService;
    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;
    
    /**
     * Creates a payment link for the given request.
     */
    @Transactional
    public PaymentLinkResponse createPaymentLink(PaymentLinkRequest request) {
        log.info("Creating payment link for transaction: {}", request.getTransactionId());
        
        if (request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new PaymentException("INVALID_AMOUNT", "Amount must be positive");
        }
        
        return payOSService.createPaymentLink(request);
    }
    
    /**
     * Gets payment by ID.
     */
    public Payment getPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentException("PAYMENT_NOT_FOUND", "Payment not found: " + paymentId));
    }
    
    /**
     * Gets payment by transaction ID.
     */
    public Payment getPaymentByTransactionId(UUID transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new PaymentException("PAYMENT_NOT_FOUND", "Payment not found for transaction: " + transactionId));
    }
}