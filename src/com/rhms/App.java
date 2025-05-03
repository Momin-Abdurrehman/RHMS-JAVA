package com.rhms;

import com.rhms.userManagement.*;
import com.rhms.ui.RhmsGuiApp;
import javafx.application.Application;

public class App {
    private static final UserManager userManager = new UserManager();

    public static void main(String[] args) {
        System.out.println("Starting RHMS Application...");
        
        initializeSystem();
        createDemoUsersIfNeeded();
        
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
    
    private static void createDemoUsersIfNeeded() {
        // Create a demo doctor if none exists
        if (userManager.getAllDoctors().isEmpty()) {
            User existingDoctor = userManager.findUserByEmail("doctor@rhms.com");
            if (existingDoctor == null) {
                System.out.println("Creating demo doctor account...");
                Doctor doctor = userManager.registerDoctor(
                    "John Smith", "doctor@rhms.com", "doctor123", 
                    "555-123-4567", "123 Medical Center Dr",
                    "Cardiology", 10
                );
                if (doctor != null) {
                    System.out.println("Demo doctor account created. Username: " + doctor.getUsername());
                }
            }
        }
        
        // Create a demo patient if none exists
        if (userManager.getAllPatients().isEmpty()) {
            User existingPatient = userManager.findUserByEmail("patient@rhms.com");
            if (existingPatient == null) {
                System.out.println("Creating demo patient account...");
                Patient patient = userManager.registerPatient(
                    "Mary Johnson", "patient@rhms.com", "patient123", 
                    "555-987-6543", "456 Health St"
                );
                if (patient != null) {
                    System.out.println("Demo patient account created. Username: " + patient.getUsername());
                    
                    // Assign the demo doctor to this patient if possible
                    Doctor demoDoctor = findDoctorByEmail("doctor@rhms.com");
                    if (demoDoctor != null) {
                        patient.addAssignedDoctor(demoDoctor);
                        System.out.println("Demo doctor assigned to demo patient");
                    }
                }
            }
        }
        
        // Make sure changes are saved
        userManager.syncUsersFromDatabase();
    }
    
    private static Doctor findDoctorByEmail(String email) {
        User user = userManager.findUserByEmail(email);
        return (user instanceof Doctor) ? (Doctor)user : null;
    }
}
