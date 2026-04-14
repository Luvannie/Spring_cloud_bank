package com.banking.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * PayOS configuration properties loaded from application.yml.
 * Prefix: banking.payos
 */
@Configuration
@ConfigurationProperties(prefix = "banking.payos")
@Data
public class PayOSProperties {

    private String baseUrl = "https://api.payos.vn";
    private String clientId;
    private String apiKey;
    private String webhookKey;
    private int connectionTimeout = 10000;
    private int readTimeout = 30000;
    private int maxRetries = 3;
}