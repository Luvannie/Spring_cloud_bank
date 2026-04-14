package com.banking.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

/**
 * Utility to get current user from SecurityContext.
 */
@Slf4j
public final class SecurityUtils {
    
    private SecurityUtils() {
        // Utility class
    }
    
    /**
     * Gets current authenticated username.
     */
    public static Optional<String> getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return Optional.of(userDetails.getUsername());
        } else if (principal instanceof String s) {
            return Optional.of(s);
        }
        
        return Optional.empty();
    }
    
    /**
     * Gets current authenticated username or throws exception.
     */
    public static String requireCurrentUsername() {
        return getCurrentUsername().orElseThrow(() -> 
            new IllegalStateException("No authenticated user found"));
    }
    
    /**
     * Gets current authenticated UserDetails.
     */
    public static Optional<UserDetails> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return Optional.of(userDetails);
        }
        
        return Optional.empty();
    }
    
    /**
     * Checks if current user is authenticated.
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() 
            && !"anonymousUser".equals(authentication.getPrincipal());
    }
}
