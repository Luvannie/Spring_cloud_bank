package com.banking.auth.service;

import com.banking.auth.dto.RegisterRequest;
import com.banking.auth.dto.UserResponse;
import com.banking.auth.entity.User;
import com.banking.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for user management operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    
    /**
     * Finds a user by ID.
     */
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }
    
    /**
     * Finds a user by username.
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * Finds a user by email.
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    /**
     * Checks if a username already exists.
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    /**
     * Checks if an email already exists.
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    /**
     * Finds all users with a specific status.
     */
    public List<User> findByStatus(User.UserStatus status) {
        return userRepository.findByStatus(status);
    }
    
    /**
     * Updates the last login timestamp for a user.
     */
    @Transactional
    public void updateLastLogin(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
        });
    }
    
    /**
     * Gets user response DTO from user entity.
     */
    public UserResponse toUserResponse(User user) {
        return UserResponse.from(user);
    }
}
