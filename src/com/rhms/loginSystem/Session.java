package com.rhms.loginSystem;

import com.rhms.userManagement.User;
import java.time.LocalDateTime;

public class Session {
    private String sessionToken;
    private User user;
    private LocalDateTime creationTime;
    private LocalDateTime lastActivityTime;
    private static final int TIMEOUT_MINUTES = 30;

    public Session(String sessionToken, User user) {
        this.sessionToken = sessionToken;
        this.user = user;
        this.creationTime = LocalDateTime.now();
        this.lastActivityTime = this.creationTime;
    }

    // Overloaded constructor to handle creationTime and lastActivityTime
    public Session(String sessionToken, User user, LocalDateTime creationTime, LocalDateTime lastActivityTime) {
        this.sessionToken = sessionToken;
        this.user = user;
        this.creationTime = creationTime;
        this.lastActivityTime = lastActivityTime;
    }

    public boolean isValid() {
        return LocalDateTime.now().isBefore(lastActivityTime.plusMinutes(TIMEOUT_MINUTES));
    }

    public void updateActivity() {
        this.lastActivityTime = LocalDateTime.now();
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public User getUser() {
        return user;
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public LocalDateTime getLastActivityTime() {
        return lastActivityTime;
    }
}
