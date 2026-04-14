package com.banking.auth.controller;

import com.banking.auth.dto.LoginRequest;
import com.banking.auth.dto.LoginResponse;
import com.banking.auth.dto.RefreshTokenRequest;
import com.banking.auth.dto.RegisterRequest;
import com.banking.auth.dto.UserResponse;
import com.banking.auth.entity.User;
import com.banking.auth.repository.UserRepository;
import com.banking.auth.service.AuthService;
import com.banking.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication endpoints.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthService authService;
    private final UserRepository userRepository;
    
    /**
     * Authenticates a user and returns tokens.
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }
    
    /**
     * Registers a new user.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }
    
    /**
     * Refreshes access token using a valid refresh token.
     */
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refreshToken(request));
    }
    
    /**
     * Logs out the current user by revoking all their sessions.
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        authService.logout(token);
        return ApiResponse.success(null);
    }
    
    /**
     * Gets the current authenticated user's information.
     */
    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ApiResponse.error("Not authenticated");
        }
        
        User user = userRepository.findByUsername(userDetails.getUsername())
            .orElse(null);
        
        return ApiResponse.success(UserResponse.from(user));
    }
}
