package org.example.domain;

import java.time.LocalDateTime;

public class UserActivity {
    private final String text;
    private final LocalDateTime createdAt;

    public UserActivity(String text, LocalDateTime createdAt) {
        this.text = text;
        this.createdAt = createdAt;
    }

    public String getText() {
        return text;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
