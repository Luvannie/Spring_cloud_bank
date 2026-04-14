package com.banking.notification.service;

import com.banking.notification.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * SMS service interface for sending SMS notifications.
 */
public interface SmsService {
    
    /**
     * Send an SMS notification.
     */
    void send(Notification notification);
}
