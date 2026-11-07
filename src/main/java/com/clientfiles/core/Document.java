package com.clientfiles.core;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Document(int id, String name, String category, String type, long size,
                       LocalDateTime submittedAt, LocalDate expires, String supervisor,
                       DocStatus status, String reviewNote, LocalDateTime reviewedAt,
                       int clientId, String clientName, String clientMail, String reviewerName) {

    public static final int EXPIRY_WARNING_DAYS = 30;

    public boolean isExpired(LocalDate today) {
        return expires != null && expires.isBefore(today);
    }

    public boolean isExpiringSoon(LocalDate today) {
        return expires != null && !isExpired(today) && !expires.isAfter(today.plusDays(EXPIRY_WARNING_DAYS));
    }

    public boolean canBeDeletedByOwner() {
        return status != DocStatus.APPROVED;
    }
}
