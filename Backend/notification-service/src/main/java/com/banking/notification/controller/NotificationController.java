package com.banking.notification.controller;

import com.banking.common.dto.ApiResponse;
import com.banking.common.dto.PageResponse;
import com.banking.notification.dto.NotificationRequest;
import com.banking.notification.dto.NotificationResponse;
import com.banking.notification.entity.Notification;
import com.banking.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for notification endpoints.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {
    
    private final NotificationService notificationService;
    
    /**
     * Get current user's notifications with pagination.
     */
    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> getUserNotifications(
            @AuthenticationPrincipal UserDetails user,
            @PageableDefault(size = 20) Pageable pageable) {
        
        UUID userId = extractUserId(user);
        Page<Notification> page = notificationService.getUserNotifications(userId, pageable);
        
        List<NotificationResponse> content = page.getContent().stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
        
        PageResponse<NotificationResponse> pageResponse = PageResponse.<NotificationResponse>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
        
        return ApiResponse.success(pageResponse);
    }
    
    /**
     * Get a specific notification by ID.
     */
    @GetMapping("/{id}")
    public ApiResponse<NotificationResponse> getNotification(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        
        UUID userId = extractUserId(user);
        List<Notification> notifications = notificationService.getUserNotifications(userId);
        
        Notification notification = notifications.stream()
                .filter(n -> n.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        
        return ApiResponse.success(NotificationResponse.from(notification));
    }
    
    /**
     * Mark a notification as read.
     */
    @PutMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        
        notificationService.markAsRead(id);
        return ApiResponse.success(null, "Notification marked as read");
    }
    
    /**
     * Create a new notification.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NotificationResponse> createNotification(
            @Valid @RequestBody NotificationRequest request,
            @AuthenticationPrincipal UserDetails user) {
        
        Notification notification = notificationService.createNotification(request);
        return ApiResponse.success(NotificationResponse.from(notification), "Notification created");
    }
    
    /**
     * Extract user ID from UserDetails.
     */
    private UUID extractUserId(UserDetails user) {
        // In a real implementation, this would extract the UUID from the JWT token
        // For now, we use a placeholder approach
        return UUID.nameUUIDFromBytes(user.getUsername().getBytes());
    }
}
