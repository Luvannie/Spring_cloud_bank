package com.banking.common.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j configuration for circuit breakers, retries, and time limiters.
 * 
 * This configuration provides default registries that can be customized via
 * application.yml using resilience4j.* properties.
 */
@Configuration
@Slf4j
public class ResilienceConfig {
    
    /**
     * Creates CircuitBreakerRegistry with default configuration.
     * Customizable via resilience4j.circuitbreaker.configs.default in application.yml.
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
        
        return CircuitBreakerRegistry.of(defaultConfig);
    }
    
    /**
     * Creates RetryRegistry with default configuration.
     * Customizable via resilience4j.retry.configs.default in application.yml.
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig defaultConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(2))
                .build();
        
        return RetryRegistry.of(defaultConfig);
    }
    
    /**
     * Creates TimeLimiterRegistry with default configuration.
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterConfig defaultConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5))
                .build();
        
        return TimeLimiterRegistry.of(defaultConfig);
    }
    
    /**
     * Gets or creates a circuit breaker from the registry.
     */
    @Bean
    public CircuitBreakerConfig defaultCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
    }
    
    /**
     * Gets or creates a retry configuration from the registry.
     */
    @Bean
    public RetryConfig defaultRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(2))
                .build();
    }
}