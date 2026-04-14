package com.banking.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Error response structure with code, message, and details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    private String code;
    private String message;
    private Map<String, String> details;
    private Instant timestamp;
    
    /**
     * Creates an ErrorResponse with code and message.
     */
    public static ErrorResponse of(String code, String message) {
        return ErrorResponse.builder()
            .code(code)
            .message(message)
            .timestamp(Instant.now())
            .build();
    }
    
    /**
     * Creates an ErrorResponse with code, message, and details.
     */
    public static ErrorResponse of(String code, String message, Map<String, String> details) {
        return ErrorResponse.builder()
            .code(code)
            .message(message)
            .details(details)
            .timestamp(Instant.now())
            .build();
    }
}
