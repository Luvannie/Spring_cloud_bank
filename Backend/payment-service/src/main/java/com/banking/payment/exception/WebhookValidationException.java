package com.banking.payment.exception;

import lombok.Getter;

/**
 * Exception for webhook signature validation failures.
 */
@Getter
public class WebhookValidationException extends PaymentException {

    private final String rawSignature;
    private final String calculatedSignature;

    public WebhookValidationException(String message) {
        super("WEBHOOK_VALIDATION_FAILED", message);
        this.rawSignature = null;
        this.calculatedSignature = null;
    }

    public WebhookValidationException(String message, Throwable cause) {
        super("WEBHOOK_VALIDATION_FAILED", message, cause);
        this.rawSignature = null;
        this.calculatedSignature = null;
    }

    public WebhookValidationException(String message, String rawSignature, String calculatedSignature) {
        super("WEBHOOK_VALIDATION_FAILED", message);
        this.rawSignature = rawSignature;
        this.calculatedSignature = calculatedSignature;
    }
}