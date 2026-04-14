package com.banking.notification.service;

import com.banking.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Stub implementation of SmsService that logs instead of sending.
 * Actual implementation would integrate with an SMS provider (e.g., Twilio, Vonage).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmsServiceImpl implements SmsService {
    
    @Override
    public void send(Notification notification) {
        log.info("Sending SMS notification to: {} | Body: {}",
                notification.getRecipient(),
                truncateBody(notification.getBody()));
        
        // Stub implementation - actual integration would:
        // 1. Connect to SMS provider (Twilio, Vonage, etc.)
        // 2. Format SMS message
        // 3. Send via provider's API
        // 4. Handle delivery receipts
        
        log.debug("SMS notification sent successfully for notification ID: {}",
                notification.getId());
    }
    
    private String truncateBody(String body) {
        if (body == null) {
            return null;
        }
        return body.length() > 50 ? body.substring(0, 50) + "..." : body;
    }
}
