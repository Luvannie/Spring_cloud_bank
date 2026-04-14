package com.banking.common.util;

import java.util.UUID;

/**
 * UUID generation utility.
 */
public final class IdGenerator {
    
    private IdGenerator() {
        // Utility class
    }
    
    /**
     * Generates a random UUID string.
     */
    public static String generateId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Generates a random UUID.
     */
    public static UUID generateUUID() {
        return UUID.randomUUID();
    }
    
    /**
     * Generates a UUID from string.
     */
    public static UUID fromString(String id) {
        return UUID.fromString(id);
    }
}
