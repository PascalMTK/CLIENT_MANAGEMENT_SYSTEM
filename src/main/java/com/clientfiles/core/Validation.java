package com.clientfiles.core;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Pattern;

/** Field rules. Each method returns the message to display, or null when the value is valid. */
public final class Validation {
    public static final int MIN_PASSWORD = 8;
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9][0-9 ().-]{5,28}$");

    private Validation() {
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static String fullName(String value) {
        String name = value.trim();
        if (name.isEmpty()) {
            return "Enter your full name.";
        }
        return name.length() > 100 ? "Use 100 characters or fewer." : null;
    }

    public static String email(String value) {
        String email = value.trim();
        if (email.isEmpty()) {
            return "Enter your email address.";
        }
        if (email.length() > 255 || !EMAIL.matcher(email).matches()) {
            return "Enter a valid email address, like name@example.com.";
        }
        return null;
    }

    public static String phone(String value) {
        String phone = value.trim();
        return phone.isEmpty() || PHONE.matcher(phone).matches()
                ? null : "Enter a valid phone number, like +243 812 345 678.";
    }

    public static String address(String value) {
        return value.trim().length() > 255 ? "Use 255 characters or fewer." : null;
    }

    public static String newPassword(char[] value) {
        return value.length < MIN_PASSWORD ? "Use at least " + MIN_PASSWORD + " characters." : null;
    }

    public static String supervisor(String value) {
        return value.trim().length() > 100 ? "Use 100 characters or fewer." : null;
    }

    /** Accepts an empty value; an expiry date is optional but must lie in the future. */
    public static String expiryDate(String value, LocalDate today) {
        if (value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim()).isAfter(today) ? null : "Choose a date in the future.";
        } catch (DateTimeParseException e) {
            return "Use the format YYYY-MM-DD.";
        }
    }
}
