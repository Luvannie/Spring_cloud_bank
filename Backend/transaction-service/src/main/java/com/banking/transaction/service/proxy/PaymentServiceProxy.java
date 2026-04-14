package com.banking.transaction.service.proxy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka-based proxy for Payment Service operations.
 * Sends commands to payment.service via Kafka messaging.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceProxy {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    /**
     * Create a payment link for the transfer.
     *
     * @param transactionId the transaction ID
     * @param amount the payment amount
     * @param description the payment description
     * @return the created payment ID
     */
    public UUID createPaymentLink(UUID transactionId, BigDecimal amount, String description) {
        // In production, this would call the payment service synchronously
        // For now, return a placeholder UUID
        UUID paymentId = UUID.randomUUID();
        log.info("Created payment link: {} for transaction: {}", paymentId, transactionId);
        return paymentId;
    }
    
    /**
     * Cancel a payment (compensation action).
     *
     * @param paymentId the payment ID to cancel
     */
    public void cancelPayment(UUID paymentId) {
        Map<String, Object> event = Map.of(
            "action", "CANCEL_PAYMENT",
            "paymentId", paymentId,
            "timestamp", Instant.now()
        );
        kafkaTemplate.send("payment.commands", paymentId.toString(), event);
        log.info("Sent CANCEL_PAYMENT command for payment: {}", paymentId);
    }
}