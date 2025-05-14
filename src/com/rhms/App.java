package com.rhms;

import com.rhms.userManagement.*;
import com.rhms.ui.RhmsGuiApp;
import javafx.application.Application;

public class App {
    private static final UserManager userManager = new UserManager();

    public static void main(String[] args) {
        System.out.println("Starting RHMS Application...");
        
        initializeSystem();

        
        // Pass the userManager to the GUI app
        RhmsGuiApp.setUserManager(userManager);
        
        // Launch the JavaFX application
        System.out.println("Launching JavaFX application...");
        Application.launch(RhmsGuiApp.class, args);
    }

    private static void initializeSystem() {
        System.out.println("Initializing system...");
        userManager.syncUsersFromDatabase();
        createInitialAdminIfNeeded();
        System.out.println("System initialized successfully.");
    }

    private static void createInitialAdminIfNeeded() {
        if (userManager.getAllAdministrators().isEmpty()) {
            User existingAdmin = userManager.findUserByEmail("admin@rhms.com");
            if (existingAdmin == null) {
                System.out.println("No default administrator found. Creating one...");
                Administrator admin = userManager.registerAdministrator(
                    "System Admin", "admin@rhms.com", "admin123", 
                    "123-456-7890", "RHMS Headquarters"
                );
                if (admin != null) {
                    System.out.println("Default administrator account created. Username: " + 
                                     admin.getUsername());
                }
            }
        }
    }
    

    

}
