package com.banking.transaction.service;

import com.banking.transaction.service.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Publisher for transaction events to Kafka.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEventPublisher {
    
    private static final String TOPIC = "transaction-events";
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    /**
     * Publish a transaction event to Kafka.
     *
     * @param event the transaction event to publish
     */
    public void publishEvent(TransactionEvent event) {
        log.info("Publishing transaction event: {} to topic: {}", 
            event.getEventType(), TOPIC);
        kafkaTemplate.send(TOPIC, event.getTransactionId().toString(), event);
    }
}