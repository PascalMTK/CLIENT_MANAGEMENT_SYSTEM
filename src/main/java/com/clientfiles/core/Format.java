package com.clientfiles.core;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public final class Format {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH);

    private Format() {
    }

    public static String date(LocalDate date) {
        return date == null ? "—" : DATE.format(date);
    }

    public static String date(LocalDateTime date) {
        return date == null ? "—" : DATE.format(date);
    }

    public static String dateTime(LocalDateTime date) {
        return date == null ? "—" : DATE_TIME.format(date);
    }

    public static String size(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return Math.round(bytes / 1024.0) + " KB";
        }
        return String.format(Locale.ENGLISH, "%.1f MB", bytes / (1024.0 * 1024));
    }

    public static String daysUntil(LocalDate date, LocalDate today) {
        long days = ChronoUnit.DAYS.between(today, date);
        if (days == 0) {
            return "today";
        }
        if (days == 1) {
            return "tomorrow";
        }
        return days < 0 ? plural(-days, "day") + " ago" : "in " + plural(days, "day");
    }

    public static String plural(long count, String noun) {
        return count + " " + noun + (count == 1 ? "" : "s");
    }

    public static String initials(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        if (parts[0].isEmpty()) {
            return "?";
        }
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase(Locale.ROOT);
    }
}
