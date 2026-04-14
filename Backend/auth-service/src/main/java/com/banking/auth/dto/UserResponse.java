package com.banking.auth.dto;

import com.banking.auth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for user response data (excludes sensitive fields like password).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    
    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String status;
    private Instant lastLoginAt;
    private Instant createdAt;
    
    /**
     * Creates a UserResponse from a User entity.
     */
    public static UserResponse from(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .phone(user.getPhone())
            .status(user.getStatus() != null ? user.getStatus().name() : null)
            .lastLoginAt(user.getLastLoginAt())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
