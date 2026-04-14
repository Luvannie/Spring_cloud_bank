package com.banking.payment.service;

import com.banking.payment.entity.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes payment events to Kafka for downstream consumers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String TOPIC = "payment.events";
    
    /**
     * Publishes a payment event to Kafka.
     */
    public void publishPaymentEvent(Payment payment, Payment.PaymentStatus status) {
        Map<String, Object> event = Map.of(
            "eventId", UUID.randomUUID().toString(),
            "paymentId", payment.getId(),
            "transactionId", payment.getTransactionId(),
            "status", status,
            "payosOrderCode", payment.getPayosOrderCode(),
            "amount", payment.getAmount(),
            "timestamp", Instant.now()
        );
        
        kafkaTemplate.send(TOPIC, payment.getTransactionId().toString(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish payment event: {}", payment.getId(), ex);
                } else {
                    log.debug("Payment event published: {} with status {}", payment.getId(), status);
                }
            });
    }
    
    /**
     * Publishes a payment expired event to trigger saga compensation.
     */
    public void publishPaymentExpired(Payment payment) {
        Map<String, Object> event = Map.of(
            "eventId", UUID.randomUUID().toString(),
            "paymentId", payment.getId(),
            "transactionId", payment.getTransactionId(),
            "status", "EXPIRED",
            "previousStatus", payment.getStatus(),
            "timestamp", Instant.now()
        );
        
        kafkaTemplate.send(TOPIC, payment.getTransactionId().toString(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish payment expired event: {}", payment.getId(), ex);
                } else {
                    log.info("Payment expired event published: {}", payment.getId());
                }
            });
    }
}