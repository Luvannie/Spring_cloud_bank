package com.banking.common.exception;

import java.util.Map;

/**
 * Base runtime exception with error code for banking operations.
 */
public class BankingException extends RuntimeException {
    
    private final String errorCode;
    private final Map<String, String> details;
    
    public BankingException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = Map.of();
    }
    
    public BankingException(String errorCode, String message, Map<String, String> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public Map<String, String> getDetails() {
        return details;
    }
}
