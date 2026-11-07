package com.clientfiles.core;

import java.time.LocalDateTime;

public record User(int id, String fullName, String email, String phone, String address,
                   Role role, boolean active, LocalDateTime createdAt) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public User withDetails(String fullName, String email, String phone, String address) {
        return new User(id, fullName, email, phone, address, role, active, createdAt);
    }
}
