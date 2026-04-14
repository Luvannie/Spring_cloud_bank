package com.banking.account.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for account service.
 */
@Configuration
@ConfigurationProperties(prefix = "account")
@Data
public class AccountConfig {
    
    /**
     * Daily transfer limit for accounts.
     */
    private String dailyTransferLimit = "100000000";
    
    /**
     * Reservation expiration time in minutes.
     */
    private int reservationExpirationMinutes = 30;
    
    /**
     * Maximum number of accounts per user.
     */
    private int maxAccountsPerUser = 10;
}