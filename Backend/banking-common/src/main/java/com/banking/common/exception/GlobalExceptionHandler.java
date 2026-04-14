package com.banking.common.exception;

import com.banking.common.dto.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST controllers.
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BankingException.class)
    public ResponseEntity<ErrorResponse> handleBankingException(BankingException ex) {
        log.error("Banking exception: {} - {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
            .badRequest()
            .body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage(), ex.getDetails()));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                e -> e.getDefaultMessage() != null ? e.getDefaultMessage() : "Invalid value"
            ));
        log.error("Validation exception: {}", errors);
        return ResponseEntity
            .badRequest()
            .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR.getCode(), "Validation failed", errors));
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Illegal argument exception: {}", ex.getMessage());
        return ResponseEntity
            .badRequest()
            .body(ErrorResponse.of(ErrorCode.INVALID_ARGUMENT.getCode(), ex.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected exception: ", ex);
        return ResponseEntity
            .internalServerError()
            .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR.getCode(), "An unexpected error occurred"));
    }
}
