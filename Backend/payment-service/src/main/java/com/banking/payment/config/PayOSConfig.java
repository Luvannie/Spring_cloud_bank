package com.banking.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * PayOS web client configuration for API communication.
 */
@Configuration
public class PayOSConfig {
    
    private final PayOSProperties payOSProperties;
    
    public PayOSConfig(PayOSProperties payOSProperties) {
        this.payOSProperties = payOSProperties;
    }
    
    @Bean
    public WebClient payOSWebClient(WebClient.Builder builder) {
        return builder
            .baseUrl(payOSProperties.getBaseUrl())
            .defaultHeader("x-client-id", payOSProperties.getClientId())
            .defaultHeader("x-api-key", payOSProperties.getApiKey())
            .build();
    }
}