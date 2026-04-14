package com.banking.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Instant/Date formatting utilities.
 */
public final class DateUtils {
    
    private static final DateTimeFormatter ISO_INSTANT = DateTimeFormatter.ISO_INSTANT;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId UTC = ZoneId.of("UTC");
    
    private DateUtils() {
        // Utility class
    }
    
    /**
     * Gets current Instant.
     */
    public static Instant now() {
        return Instant.now();
    }
    
    /**
     * Parses ISO instant string to Instant.
     */
    public static Instant parse(String instantString) {
        return Instant.parse(instantString);
    }
    
    /**
     * Formats Instant to ISO string.
     */
    public static String format(Instant instant) {
        return instant.toString();
    }
    
    /**
     * Formats Instant to date string (yyyy-MM-dd).
     */
    public static String formatDate(Instant instant) {
        return DATE_FORMAT.format(instant.atZone(UTC));
    }
    
    /**
     * Formats Instant to datetime string (yyyy-MM-dd HH:mm:ss).
     */
    public static String formatDateTime(Instant instant) {
        return DATETIME_FORMAT.format(instant.atZone(UTC));
    }
    
    /**
     * Converts LocalDate to Instant at start of day in UTC.
     */
    public static Instant toInstantStart(LocalDate date) {
        return date.atStartOfDay(UTC).toInstant();
    }
    
    /**
     * Converts Instant to LocalDate in UTC.
     */
    public static LocalDate toLocalDate(Instant instant) {
        return instant.atZone(UTC).toLocalDate();
    }
    
    /**
     * Converts Instant to ZonedDateTime in UTC.
     */
    public static ZonedDateTime toZonedDateTime(Instant instant) {
        return instant.atZone(UTC);
    }
    
    /**
     * Adds days to instant.
     */
    public static Instant plusDays(Instant instant, long days) {
        return instant.plus(days, ChronoUnit.DAYS);
    }
    
    /**
     * Adds hours to instant.
     */
    public static Instant plusHours(Instant instant, long hours) {
        return instant.plus(hours, ChronoUnit.HOURS);
    }
    
    /**
     * Adds minutes to instant.
     */
    public static Instant plusMinutes(Instant instant, long minutes) {
        return instant.plus(minutes, ChronoUnit.MINUTES);
    }
    
    /**
     * Calculates seconds between two instants.
     */
    public static long getSecondsBetween(Instant start, Instant end) {
        return ChronoUnit.SECONDS.between(start, end);
    }
    
    /**
     * Calculates milliseconds between two instants.
     */
    public static long getMillisBetween(Instant start, Instant end) {
        return ChronoUnit.MILLIS.between(start, end);
    }
    
    /**
     * Checks if instant is before another.
     */
    public static boolean isBefore(Instant a, Instant b) {
        return a.isBefore(b);
    }
    
    /**
     * Checks if instant is after another.
     */
    public static boolean isAfter(Instant a, Instant b) {
        return a.isAfter(b);
    }
}
