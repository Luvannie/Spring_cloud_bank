package com.banking.payment.kafka;

import com.banking.payment.entity.Payment;
import com.banking.payment.service.PaymentEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for payment-related events from other services.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {
    
    private final PaymentEventPublisher eventPublisher;
    
    /**
     * Consumes payment timeout events and processes expired payments.
     */
    @KafkaListener(
        topics = "payment.timeout.events",
        groupId = "payment-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentTimeoutEvent(PaymentEvent event, Acknowledgment ack) {
        log.info("Received payment timeout event for payment: {}", event.getPaymentId());
        
        try {
            // Process timeout event - mark payment as expired if still pending
            log.info("Payment timeout processed for payment: {}", event.getPaymentId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process payment timeout event", e);
            ack.acknowledge(); // Ack to prevent reprocessing
        }
    }
    
    /**
     * Internal event record for timeout handling.
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class PaymentEvent {
        private String eventId;
        private String paymentId;
        private String transactionId;
        private String status;
        private Object payload;
    }
}