package com.banking.transaction.service.proxy;

import com.banking.transaction.dto.TransferRequest;
import com.banking.transaction.dto.TransferResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Kafka-based proxy for Notification Service operations.
 * Sends notification commands via Kafka messaging.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceProxy {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    /**
     * Send transfer confirmation notification.
     *
     * @param request the transfer request
     * @param response the transfer response
     */
    public void sendTransferConfirmation(TransferRequest request, TransferResponse response) {
        Map<String, Object> event = Map.of(
            "eventType", "TRANSFER_CONFIRMATION",
            "sourceAccountId", request.getSourceAccountId(),
            "targetAccountId", request.getTargetAccountId(),
            "amount", request.getAmount(),
            "referenceNumber", response.getReferenceNumber(),
            "timestamp", Instant.now()
        );
        kafkaTemplate.send("notify", request.getSourceAccountId().toString(), event);
        log.info("Sent TRANSFER_CONFIRMATION notification for reference: {}", 
            response.getReferenceNumber());
    }
    
    /**
     * Send compensation notification (when saga fails).
     *
     * @param request the original transfer request
     * @param errorMessage the error message
     */
    public void sendCompensationNotification(TransferRequest request, String errorMessage) {
        Map<String, Object> event = Map.of(
            "eventType", "TRANSFER_COMPENSATION",
            "sourceAccountId", request.getSourceAccountId(),
            "targetAccountId", request.getTargetAccountId(),
            "amount", request.getAmount(),
            "errorMessage", errorMessage,
            "timestamp", Instant.now()
        );
        kafkaTemplate.send("notify", request.getSourceAccountId().toString(), event);
        log.info("Sent TRANSFER_COMPENSATION notification");
    }
}