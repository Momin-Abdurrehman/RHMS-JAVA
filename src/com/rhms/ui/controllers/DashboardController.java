package com.rhms.ui.controllers;

import com.rhms.userManagement.User;
import com.rhms.userManagement.UserManager;

/**
 * Interface for all dashboard controllers to implement.
 * Provides standard methods that all dashboard controllers should have.
 */
public interface DashboardController {
    
    /**
     * Sets the current user for the dashboard
     * @param user The user who logged in
     */
    void setUser(User user);
    
    /**
     * Sets the UserManager instance for database operations
     * @param userManager The application's UserManager
     */
    void setUserManager(UserManager userManager);
    
    /**
     * Initializes the dashboard with user-specific data
     */
    void initializeDashboard();
    
    /**
     * Handles logout action
     */
    void handleLogout();
}
