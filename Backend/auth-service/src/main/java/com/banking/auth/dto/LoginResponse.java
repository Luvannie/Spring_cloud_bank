package com.banking.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for login response containing access and refresh tokens.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private UserResponse user;
}
