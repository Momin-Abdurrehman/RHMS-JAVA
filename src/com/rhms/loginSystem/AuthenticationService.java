package com.rhms.loginSystem;

import com.rhms.userManagement.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides authentication services for the Remote Healthcare Monitoring System
 * Handles login validation, session management, and security features
 */
public class AuthenticationService {
    // Maps usernames to their credential information
    private Map<String, User> userDatabase;
    
    // Maps active session tokens to user information
    private Map<String, Session> activeSessions;
    
    // Tracks failed login attempts by username
    private Map<String, LoginAttemptInfo> loginAttempts;
    
    // Configuration constants
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int ACCOUNT_LOCKOUT_MINUTES = 15;
    private static final int SESSION_TIMEOUT_MINUTES = 30;

    /**
     * Initializes the authentication service with empty databases
     */
    public AuthenticationService() {
        userDatabase = new HashMap<>();
        activeSessions = new ConcurrentHashMap<>();
        loginAttempts = new HashMap<>();
    }
    
    /**
     * Registers a user in the authentication system
     * @param user User to be registered
     * @return true if registration is successful
     */
    public boolean registerUser(User user) {
        if (userDatabase.containsKey(user.getUsername())) {
            return false; // Username already exists
        }
        
        userDatabase.put(user.getUsername(), user);
        return true;
    }
    
    /**
     * Authenticates a user and creates a session if credentials are valid
     * @param username Username credential
     * @param password Password credential
     * @return Session token if login successful, null otherwise
     */
    public String login(String username, String password) {
        // Check if account is locked due to too many failed attempts
        if (isAccountLocked(username)) {
            System.out.println("Account is locked. Too many failed login attempts.");
            return null;
        }
        
        // Verify user exists and password matches
        User user = userDatabase.get(username);
        if (user != null && verifyPassword(password, user.getPasswordHash())) {
            // Reset failed attempts on successful login
            loginAttempts.remove(username);
            
            // Generate and store session token
            String sessionToken = generateSessionToken();
            activeSessions.put(sessionToken, new Session(sessionToken, user, SESSION_TIMEOUT_MINUTES));
            
            return sessionToken;
        } else {
            // Record failed login attempt
            recordFailedLoginAttempt(username);
            return null;
        }
    }
    
    /**
     * Verifies if a session is valid
     * @param sessionToken The session identifier to validate
     * @return true if session is active and valid
     */
    public boolean validateSession(String sessionToken) {
        Session session = activeSessions.get(sessionToken);
        
        if (session == null) {
            return false;
        }
        
        // Check if session has expired
        if (!session.isValid()) {
            logout(sessionToken); // Automatically expire the session
            return false;
        }
        
        // Update last activity time for the session
        session.updateActivity();
        return true;
    }
    
    /**
     * Gets the user associated with a valid session
     * @param sessionToken Active session identifier
     * @return User object if session is valid, null otherwise
     */
    public User getUserBySession(String sessionToken) {
        if (!validateSession(sessionToken)) {
            return null;
        }
        
        return activeSessions.get(sessionToken).getUser();
    }
    
    /**
     * Ends a user session
     * @param sessionToken The session to terminate
     */
    public void logout(String sessionToken) {
        activeSessions.remove(sessionToken);
    }
    
    /**
     * Changes a user's password
     * @param username User's identifier
     * @param oldPassword Current password for verification
     * @param newPassword New password to set
     * @return true if password change is successful
     */
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        User user = userDatabase.get(username);
        
        if (user != null && verifyPassword(oldPassword, user.getPasswordHash())) {
            String newPasswordHash = hashPassword(newPassword);
            user.setPasswordHash(newPasswordHash);
            return true;
        }
        
        return false;
    }
    
    /**
     * Hashes a password using SHA-256 algorithm
     * @param password Plain text password
     * @return Hashed password string
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple encoding if hashing fails
            System.err.println("Error hashing password: " + e.getMessage());
            return Base64.getEncoder().encodeToString(password.getBytes());
        }
    }
    
    /**
     * Verifies a password against its hash
     * @param password Plain text password to verify
     * @param storedHash Stored password hash to compare against
     * @return true if password matches hash
     */
    private boolean verifyPassword(String password, String storedHash) {
        String inputHash = hashPassword(password);
        return inputHash.equals(storedHash);
    }
    
    /**
     * Generates a unique session token
     * @return Unique session identifier
     */
    private String generateSessionToken() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Records a failed login attempt and handles account lockout
     * @param username The username that failed to login
     */
    private void recordFailedLoginAttempt(String username) {
        LoginAttemptInfo attempts = loginAttempts.getOrDefault(username, 
                                    new LoginAttemptInfo());
        attempts.incrementFailedAttempts();
        
        if (attempts.getFailedAttempts() >= MAX_LOGIN_ATTEMPTS) {
            attempts.lockAccount();
            System.out.println("Account locked due to too many failed attempts: " + username);
        }
        
        loginAttempts.put(username, attempts);
    }
    
    /**
     * Checks if an account is currently locked
     * @param username User account to check
     * @return true if account is locked
     */
    private boolean isAccountLocked(String username) {
        LoginAttemptInfo attempts = loginAttempts.get(username);
        if (attempts == null) {
            return false;
        }
        
        // Check if lockout period has expired
        if (attempts.isLocked() && 
            attempts.getLockTime().plusMinutes(ACCOUNT_LOCKOUT_MINUTES).isBefore(LocalDateTime.now())) {
            loginAttempts.remove(username); // Reset after lockout period
            return false;
        }
        
        return attempts.isLocked();
    }
    
    /**
     * Inner class to track login attempt information
     */
    private static class LoginAttemptInfo {
        private int failedAttempts;
        private boolean locked;
        private LocalDateTime lockTime;
        
        public LoginAttemptInfo() {
            this.failedAttempts = 0;
            this.locked = false;
        }
        
        public void incrementFailedAttempts() {
            failedAttempts++;
        }
        
        public int getFailedAttempts() {
            return failedAttempts;
        }
        
        public void lockAccount() {
            locked = true;
            lockTime = LocalDateTime.now();
        }
        
        public boolean isLocked() {
            return locked;
        }
        
        public LocalDateTime getLockTime() {
            return lockTime;
        }
    }
}
