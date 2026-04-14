package com.banking.payment.controller;

import com.banking.payment.dto.WebhookPayload;
import com.banking.payment.service.PayOSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dedicated controller for PayOS webhook handling.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {
    
    private final PayOSService payOSService;
    
    /**
     * Receives and processes PayOS webhook payloads.
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody WebhookPayload payload) {
        log.info("Received PayOS webhook for order: {}", payload.getOrderCode());
        try {
            payOSService.processWebhook(payload);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Webhook processing failed for order: {}", payload.getOrderCode(), e);
            return ResponseEntity.badRequest().body("FAIL");
        }
    }
}