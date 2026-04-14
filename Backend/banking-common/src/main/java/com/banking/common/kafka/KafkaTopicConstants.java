package com.banking.common.kafka;

/**
 * Constants for Kafka topic names.
 */
public final class KafkaTopicConstants {
    
    private KafkaTopicConstants() {
        // Utility class
    }
    
    // Account topics
    public static final String ACCOUNT_EVENTS = "account.events";
    public static final String ACCOUNT_CREATED = "account.created";
    public static final String ACCOUNT_UPDATED = "account.updated";
    public static final String ACCOUNT_DELETED = "account.deleted";
    
    // Transaction topics
    public static final String TRANSACTION_EVENTS = "transaction.events";
    public static final String TRANSACTION_CREATED = "transaction.created";
    public static final String TRANSACTION_COMPLETED = "transaction.completed";
    public static final String TRANSACTION_FAILED = "transaction.failed";
    
    // Payment topics
    public static final String PAYMENT_EVENTS = "payment.events";
    public static final String PAYMENT_INITIATED = "payment.initiated";
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED = "payment.failed";
    
    // Notification topics
    public static final String NOTIFICATION_EVENTS = "notification.events";
    public static final String EMAIL_NOTIFICATIONS = "notification.email";
    public static final String SMS_NOTIFICATIONS = "notification.sms";
    
    // Saga orchestration topics
    public static final String SAGA_TRANSACTIONS = "saga.transactions";
    public static final String SAGA_COMMANDS = "saga.commands";
    public static final String SAGA_REPLIES = "saga.replies";
}
