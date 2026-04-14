package com.banking.transaction.kafka;

import com.banking.transaction.service.saga.TransferSaga;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Kafka consumer for payment events.
 * Handles payment success/failure webhooks to continue or compensate saga.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {
    
    private final TransferSaga transferSaga;
    
    /**
     * Consume payment events from the payment.events topic.
     *
     * @param event the payment event payload
     */
    @KafkaListener(topics = "payment.events", groupId = "transaction-service-group")
    public void consume(Map<String, Object> event) {
        String status = (String) event.get("status");
        UUID transactionId = (UUID) event.get("transactionId");
        
        log.info("Received payment event: {} for transaction: {}", status, transactionId);
        
        if ("SUCCESS".equals(status)) {
            UUID paymentId = (UUID) event.get("paymentId");
            transferSaga.handlePaymentSuccess(transactionId, paymentId);
        } else if ("FAILED".equals(status) || "EXPIRED".equals(status)) {
            // Handle payment failure - trigger saga compensation
            log.warn("Payment {} for transaction: {}, triggering compensation",
                status, transactionId);
        }
    }
}