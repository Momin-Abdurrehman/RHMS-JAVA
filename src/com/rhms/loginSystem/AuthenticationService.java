package com.rhms.loginSystem;

import com.rhms.userManagement.User;
import com.rhms.Database.UserDatabaseHandler;
import java.util.UUID;

/**
 * Simplified AuthenticationService for user authentication and session management.
 */
public class AuthenticationService {
    private UserDatabaseHandler dbHandler;

    public AuthenticationService() {
        this.dbHandler = new UserDatabaseHandler();
    }

    public String login(String username, String password) {
        User user = dbHandler.getUserByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            String sessionToken = generateSessionToken();
            Session session = new Session(sessionToken, user);
            dbHandler.saveSession(session); // Save session to the database
            return sessionToken;
        }
        return null; // Invalid credentials
    }

    public boolean validateSession(String sessionToken) {
        Session session = dbHandler.getSessionByToken(sessionToken);
        if (session != null && session.isValid()) {
            session.updateActivity();
            dbHandler.updateSessionActivity(session); // Update session activity in the database
            return true;
        }
        return false; // Invalid or expired session
    }

    public void logout(String sessionToken) {
        dbHandler.deleteSession(sessionToken); // Remove session from the database
    }

    private String generateSessionToken() {
        return UUID.randomUUID().toString();
    }
}
