package com.banking.payment.exception;

import com.banking.common.exception.BankingException;

import java.util.Map;

/**
 * Custom exception for payment-related errors.
 */
public class PaymentException extends BankingException {
    
    public PaymentException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    public PaymentException(String errorCode, String message, Map<String, String> details) {
        super(errorCode, message, details);
    }
    
    public PaymentException(String errorCode, String message, Throwable cause) {
        super(errorCode, message);
        initCause(cause);
    }
}