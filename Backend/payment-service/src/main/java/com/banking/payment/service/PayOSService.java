package com.banking.payment.service;

import com.banking.payment.config.PayOSProperties;
import com.banking.payment.dto.PayOSCreateOrderRequest;
import com.banking.payment.dto.PayOSCreateOrderResponse;
import com.banking.payment.dto.PaymentLinkRequest;
import com.banking.payment.dto.PaymentLinkResponse;
import com.banking.payment.dto.WebhookPayload;
import com.banking.payment.entity.Payment;
import com.banking.payment.exception.PaymentException;
import com.banking.payment.exception.WebhookValidationException;
import com.banking.payment.repository.PaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Service for PayOS API integration with circuit breaker and retry patterns.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayOSService {
    
    private final WebClient payOSWebClient;
    private final PayOSProperties payOSProperties;
    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String WEBHOOK_CACHE_PREFIX = "payos:webhook:";
    private static final String PAYOS_API_PATH = "/v3/payment-requests";
    
    /**
     * Creates a payment link via PayOS API with circuit breaker and retry.
     */
    @CircuitBreaker(name = "payosApi", fallbackMethod = "createPaymentLinkFallback")
    @Retry(name = "payosApi")
    public PaymentLinkResponse createPaymentLink(PaymentLinkRequest request) {
        log.info("Creating PayOS payment link for transaction: {}", request.getTransactionId());
        
        PayOSCreateOrderRequest payOSRequest = PayOSCreateOrderRequest.builder()
            .orderCode(String.valueOf(System.currentTimeMillis()))
            .amount(request.getAmount().intValue())
            .description(request.getDescription())
            .buyerEmail(request.getBuyerEmail())
            .buyerPhone(request.getBuyerPhone())
            .buyerName(request.getBuyerName())
            .build();
        
        PayOSCreateOrderResponse payOSResponse = callPayOSApi(payOSRequest);
        
        Payment payment = Payment.builder()
            .transactionId(request.getTransactionId())
            .payosOrderCode(payOSResponse.getOrderCode())
            .amount(request.getAmount())
            .status(Payment.PaymentStatus.PENDING)
            .qrCodeUrl(payOSResponse.getQrCodeUrl())
            .paymentUrl(payOSResponse.getCheckoutUrl())
            .expiresAt(Instant.now().plus(Duration.ofMinutes(15)))
            .build();
        
        paymentRepository.save(payment);
        
        return PaymentLinkResponse.builder()
            .paymentId(payment.getId())
            .checkoutUrl(payOSResponse.getCheckoutUrl())
            .qrCodeUrl(payOSResponse.getQrCodeUrl())
            .expiresAt(payment.getExpiresAt())
            .build();
    }
    
    /**
     * Processes PayOS webhook with idempotency check and signature validation.
     */
    @Transactional
    public void processWebhook(WebhookPayload payload) {
        String webhookKey = WEBHOOK_CACHE_PREFIX + payload.getOrderCode();
        
        // Idempotency check
        if (Boolean.TRUE.equals(redisTemplate.hasKey(webhookKey))) {
            log.info("Webhook already processed for order: {}", payload.getOrderCode());
            return;
        }
        
        // Validate signature
        validateWebhookSignature(payload);
        
        Payment payment = paymentRepository.findByPayosOrderCode(payload.getOrderCode())
            .orElseThrow(() -> new WebhookValidationException("Payment not found: " + payload.getOrderCode()));
        
        Payment.PaymentStatus newStatus = mapPayOSStatus(payload.getStatus());
        payment.setStatus(newStatus);
        payment.setPaidAt(payload.getPaidAt());
        
        if (newStatus == Payment.PaymentStatus.SUCCESS) {
            payment.setPayosTransactionId(payload.getTransactionId());
        }
        
        paymentRepository.save(payment);
        
        // Cache webhook for idempotency
        try {
            redisTemplate.opsForValue().set(webhookKey, objectMapper.writeValueAsString(payload), Duration.ofHours(24));
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache webhook payload", e);
        }
        
        // Publish event
        eventPublisher.publishPaymentEvent(payment, newStatus);
        
        log.info("Webhook processed for order: {}, status: {}", payload.getOrderCode(), newStatus);
    }
    
    /**
     * Calls PayOS API to create an order.
     */
    private PayOSCreateOrderResponse callPayOSApi(PayOSCreateOrderRequest request) {
        return payOSWebClient.post()
            .uri(PAYOS_API_PATH)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(PayOSCreateOrderResponse.class)
            .block();
    }
    
    /**
     * Validates webhook signature using HMAC-SHA256.
     */
    private void validateWebhookSignature(WebhookPayload payload) {
        try {
            String data = String.format("%s|%s|%s", payload.getOrderCode(), payload.getAmount(), payload.getStatus());
            
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                payOSProperties.getWebhookKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = Base64.getEncoder().encodeToString(hash);
            
            if (!calculatedSignature.equals(payload.getSignature())) {
                throw new WebhookValidationException("Invalid webhook signature");
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new WebhookValidationException("Signature validation failed", e);
        }
    }
    
    /**
     * Maps PayOS status string to PaymentStatus enum.
     */
    private Payment.PaymentStatus mapPayOSStatus(String payosStatus) {
        return switch (payosStatus.toUpperCase()) {
            case "PAID" -> Payment.PaymentStatus.SUCCESS;
            case "CANCELLED" -> Payment.PaymentStatus.CANCELLED;
            case "EXPIRED" -> Payment.PaymentStatus.EXPIRED;
            default -> Payment.PaymentStatus.PENDING;
        };
    }
    
    /**
     * Fallback method when PayOS API is unavailable.
     */
    private PaymentLinkResponse createPaymentLinkFallback(PaymentLinkRequest request, Exception ex) {
        log.error("PayOS API fallback triggered for transaction: {}", request.getTransactionId(), ex);
        throw new PaymentException("PAYOS_UNAVAILABLE", "Payment service temporarily unavailable");
    }
}