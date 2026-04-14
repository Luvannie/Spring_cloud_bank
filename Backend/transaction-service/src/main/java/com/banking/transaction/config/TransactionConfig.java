package com.banking.transaction.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topic configuration for transaction service.
 */
@Configuration
public class TransactionConfig {
    
    /**
     * Create topic for transaction commands.
     */
    @Bean
    public NewTopic transactionCommandsTopic() {
        return TopicBuilder.name("transaction.commands")
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    /**
     * Create topic for transaction events.
     */
    @Bean
    public NewTopic transactionEventsTopic() {
        return TopicBuilder.name("transaction.events")
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    /**
     * Create topic for payment events.
     */
    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name("payment.events")
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    /**
     * Create topic for account commands.
     */
    @Bean
    public NewTopic accountCommandsTopic() {
        return TopicBuilder.name("account.commands")
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    /**
     * Create topic for payment commands.
     */
    @Bean
    public NewTopic paymentCommandsTopic() {
        return TopicBuilder.name("payment.commands")
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    /**
     * Create topic for notifications.
     */
    @Bean
    public NewTopic notifyTopic() {
        return TopicBuilder.name("notify")
            .partitions(3)
            .replicas(1)
            .build();
    }
}