package com.rhms.loginSystem;

import com.rhms.userManagement.User;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Represents an authenticated user session in the system
 * Tracks session creation time, activity and expiration
 */
public class Session {
    // Session identification and user information
    private String sessionToken;
    private User user;
    
    // Session timing information
    private LocalDateTime creationTime;
    private LocalDateTime lastActivityTime;
    private int timeoutMinutes = 30; // Default timeout in minutes
    
    /**
     * Creates a new user session with the provided token and user
     * @param sessionToken Unique identifier for this session
     * @param user The authenticated user this session belongs to
     */
    public Session(String sessionToken, User user) {
        this.sessionToken = sessionToken;
        this.user = user;
        this.creationTime = LocalDateTime.now();
        this.lastActivityTime = this.creationTime;
    }
    
    /**
     * Creates a new session with custom timeout duration
     * @param sessionToken Unique identifier for this session
     * @param user The authenticated user this session belongs to
     * @param timeoutMinutes Number of minutes of inactivity before session expires
     */
    public Session(String sessionToken, User user, int timeoutMinutes) {
        this(sessionToken, user);
        this.timeoutMinutes = timeoutMinutes;
    }
    
    /**
     * Checks if the session is still valid or has expired
     * @return true if session is still active and valid
     */
    public boolean isValid() {
        LocalDateTime expirationTime = lastActivityTime.plusMinutes(timeoutMinutes);
        return LocalDateTime.now().isBefore(expirationTime);
    }
    
    /**
     * Updates the last activity timestamp to extend session duration
     */
    public void updateActivity() {
        this.lastActivityTime = LocalDateTime.now();
    }
    
    /**
     * Gets the minutes remaining until session expires
     * @return Minutes until expiration (negative if already expired)
     */
    public long getMinutesRemaining() {
        LocalDateTime expirationTime = lastActivityTime.plusMinutes(timeoutMinutes);
        return ChronoUnit.MINUTES.between(LocalDateTime.now(), expirationTime);
    }
    
    /**
     * Get session duration since creation
     * @return Minutes elapsed since session was created
     */
    public long getSessionDuration() {
        return ChronoUnit.MINUTES.between(creationTime, LocalDateTime.now());
    }
    
    // Getters and setters
    
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
    
    public int getTimeoutMinutes() {
        return timeoutMinutes;
    }
    
    public void setTimeoutMinutes(int timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }
    
    @Override
    public String toString() {
        return "Session [token=" + sessionToken + ", user=" + user.getUsername() 
               + ", created=" + creationTime 
               + ", expires in=" + getMinutesRemaining() + " minutes]";
    }
}
