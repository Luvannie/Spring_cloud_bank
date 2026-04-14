package com.banking.notification.service;

import com.banking.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Stub implementation of EmailService that logs instead of sending.
 * Actual implementation would integrate with an email provider (e.g., SendGrid, SES).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {
    
    @Override
    public void send(Notification notification) {
        log.info("Sending EMAIL notification to: {} | Subject: {}",
                notification.getRecipient(),
                notification.getSubject());
        
        // Stub implementation - actual integration would:
        // 1. Connect to email provider (SendGrid, AWS SES, etc.)
        // 2. Format email with subject and body
        // 3. Send via provider's API
        // 4. Handle delivery receipts and bounces
        
        log.debug("EMAIL notification sent successfully for notification ID: {}",
                notification.getId());
    }
}
