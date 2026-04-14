package com.banking.common.exception;

/**
 * Enum with common error codes for banking operations.
 */
public enum ErrorCode {
    
    // Validation errors
    VALIDATION_ERROR("VALIDATION_ERROR", "Validation failed"),
    INVALID_REQUEST("INVALID_REQUEST", "Invalid request"),
    INVALID_ARGUMENT("INVALID_ARGUMENT", "Invalid argument"),
    
    // Resource errors
    NOT_FOUND("NOT_FOUND", "Resource not found"),
    ALREADY_EXISTS("ALREADY_EXISTS", "Resource already exists"),
    
    // Authentication/Authorization errors
    UNAUTHORIZED("UNAUTHORIZED", "Unauthorized"),
    FORBIDDEN("FORBIDDEN", "Access forbidden"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "Invalid credentials"),
    TOKEN_EXPIRED("TOKEN_EXPIRED", "Token expired"),
    TOKEN_INVALID("TOKEN_INVALID", "Token invalid"),
    
    // Business logic errors
    INSUFFICIENT_BALANCE("INSUFFICIENT_BALANCE", "Insufficient balance"),
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", "Account is locked"),
    ACCOUNT_INACTIVE("ACCOUNT_INACTIVE", "Account is inactive"),
    TRANSACTION_FAILED("TRANSACTION_FAILED", "Transaction failed"),
    DUPLICATE_TRANSACTION("DUPLICATE_TRANSACTION", "Duplicate transaction"),
    
    // External service errors
    EXTERNAL_SERVICE_ERROR("EXTERNAL_SERVICE_ERROR", "External service error"),
    PAYMENT_SERVICE_UNAVAILABLE("PAYMENT_SERVICE_UNAVAILABLE", "Payment service unavailable"),
    
    // System errors
    INTERNAL_ERROR("INTERNAL_ERROR", "Internal server error"),
    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", "Service temporarily unavailable"),
    DATABASE_ERROR("DATABASE_ERROR", "Database error"),
    KAFKA_ERROR("KAFKA_ERROR", "Kafka error");
    
    private final String code;
    private final String defaultMessage;
    
    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDefaultMessage() {
        return defaultMessage;
    }
}
