package com.clientfiles.core;

public enum Role {
    ADMIN("Administrator"),
    CLIENT("Client");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Role fromDb(String value) {
        return "admin".equals(value) ? ADMIN : CLIENT;
    }
}
