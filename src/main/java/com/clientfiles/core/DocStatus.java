package com.clientfiles.core;

import java.util.Locale;

public enum DocStatus {
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String label;

    DocStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public String dbValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static DocStatus fromDb(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
