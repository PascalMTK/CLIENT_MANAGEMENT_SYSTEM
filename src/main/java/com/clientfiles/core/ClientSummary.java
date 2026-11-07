package com.clientfiles.core;

import java.time.LocalDateTime;

public record ClientSummary(int id, String fullName, String email, String phone, String address,
                            boolean active, LocalDateTime createdAt,
                            int documents, int pending, LocalDateTime lastSubmission) {
}
