package com.banking.notification.service;

import com.banking.notification.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Email service interface for sending email notifications.
 */
public interface EmailService {
    
    /**
     * Send an email notification.
     */
    void send(Notification notification);
}
