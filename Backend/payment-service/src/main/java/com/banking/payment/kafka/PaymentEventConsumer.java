package com.banking.payment.kafka;

import com.banking.payment.entity.Payment;
import com.banking.payment.repository.PaymentRepository;
import com.banking.payment.service.PaymentEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Kafka consumer for payment-related events from other services.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {
    
    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;
    
    /**
     * Consumes payment timeout events and processes expired payments.
     */
    @KafkaListener(
        topics = "payment.timeout.events",
        groupId = "payment-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handlePaymentTimeoutEvent(PaymentEvent event, Acknowledgment ack) {
        log.info("Received payment timeout event for payment: {}", event.getPaymentId());
        
        try {
            UUID paymentId = UUID.fromString(event.getPaymentId());
            Payment payment = paymentRepository.findById(paymentId).orElse(null);
            
            if (payment == null) {
                log.warn("Payment not found for timeout event: {}", event.getPaymentId());
                ack.acknowledge();
                return;
            }
            
            if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
                log.info("Payment {} is not pending (status: {}), skipping timeout",
                    event.getPaymentId(), payment.getStatus());
                ack.acknowledge();
                return;
            }
            
            // Mark payment as expired
            payment.setStatus(Payment.PaymentStatus.EXPIRED);
            paymentRepository.save(payment);
            
            // Publish event to trigger saga compensation
            eventPublisher.publishPaymentExpired(payment);
            
            log.info("Payment {} marked as EXPIRED due to timeout", paymentId);
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