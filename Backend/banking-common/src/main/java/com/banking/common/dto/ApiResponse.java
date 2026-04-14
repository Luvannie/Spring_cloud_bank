package com.banking.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standard API response wrapper for success and error responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private boolean success;
    private String message;
    private T data;
    private Instant timestamp;
    
    /**
     * Creates a successful response with data.
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .message("Success")
            .data(data)
            .timestamp(Instant.now())
            .build();
    }
    
    /**
     * Creates a successful response with custom message.
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
            .success(true)
            .message(message)
            .data(data)
            .timestamp(Instant.now())
            .build();
    }
    
    /**
     * Creates an error response with message only.
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .message(message)
            .timestamp(Instant.now())
            .build();
    }
    
    /**
     * Creates an error response with error code and message.
     */
    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .message(message)
            .timestamp(Instant.now())
            .build();
    }
}
