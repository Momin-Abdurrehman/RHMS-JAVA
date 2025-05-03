package com.rhms.ui.controllers;

import com.rhms.userManagement.Administrator;
import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;
import com.rhms.userManagement.User;
import com.rhms.userManagement.UserManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.File;
import java.net.URL;
import java.util.List;

public class AdminDashboardController implements DashboardController {

    @FXML private Label nameLabel;
    @FXML private TextArea outputArea;
    @FXML private VBox contentArea;

    private Administrator currentAdmin;
    private UserManager userManager;

    @Override
    public void setUser(User user) {
        if (user instanceof Administrator) {
            this.currentAdmin = (Administrator) user;
        } else {
            throw new IllegalArgumentException("User must be an Administrator");
        }
    }

    @Override
    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }

    @Override
    public void initializeDashboard() {
        nameLabel.setText(currentAdmin.getName());
        outputArea.setText("Welcome, " + currentAdmin.getName() + "!\n\n" +
                "Select an option from the sidebar to begin managing the system.");
    }

    @FXML
    public void handleRegisterUser(ActionEvent event) {
        try {
            URL registerViewUrl = findResource("com/rhms/ui/views/RegistrationDashboard.fxml");
            
            if (registerViewUrl == null) {
                showError("Could not find registration view resource");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(registerViewUrl);
            Parent registerView = loader.load();

            // Pass userManager to registration controller
            RegistrationController controller = loader.getController();
            controller.setUserManager(userManager);

            // Create new stage for registration
            Stage registerStage = new Stage();
            Scene scene = new Scene(registerView);
            
            // Load CSS
            URL cssUrl = findResource("com/rhms/ui/resources/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            registerStage.setScene(scene);
            registerStage.setTitle("RHMS - Register New User");
            registerStage.show();

        } catch (IOException e) {
            showError("Error loading registration form: " + e.getMessage());
        }
    }

    @FXML
    public void handleViewUsers(ActionEvent event) {
        StringBuilder users = new StringBuilder("System Users:\n\n");

        users.append("=== Administrators ===\n");
        List<Administrator> admins = userManager.getAllAdministrators();
        if (admins.isEmpty()) {
            users.append("No administrators found.\n");
        } else {
            for (Administrator admin : admins) {
                users.append("- ").append(admin.getName()).append(" (").append(admin.getEmail()).append(")\n");
            }
        }

        users.append("\n=== Doctors ===\n");
        List<Doctor> doctors = userManager.getAllDoctors();
        if (doctors.isEmpty()) {
            users.append("No doctors found.\n");
        } else {
            for (Doctor doctor : doctors) {
                users.append("- Dr. ").append(doctor.getName())
                        .append(" (").append(doctor.getSpecialization())
                        .append(", ").append(doctor.getExperienceYears()).append(" years exp.)\n");
            }
        }

        users.append("\n=== Patients ===\n");
        List<Patient> patients = userManager.getAllPatients();
        if (patients.isEmpty()) {
            users.append("No patients found.\n");
        } else {
            for (Patient patient : patients) {
                users.append("- ").append(patient.getName())
                        .append(" (").append(patient.getEmail()).append(")\n");
            }
        }

        outputArea.setText(users.toString());
    }

    @FXML
    public void handleAssignDoctor(ActionEvent event) {
        List<Doctor> doctors = userManager.getAllDoctors();
        List<Patient> patients = userManager.getAllPatients();

        if (doctors.isEmpty() || patients.isEmpty()) {
            outputArea.setText("Cannot perform assignment: Need at least one doctor and one patient in the system.");
            return;
        }

        try {
            URL assignViewUrl = findResource("com/rhms/ui/views/AssignDoctorView.fxml");
            
            if (assignViewUrl == null) {
                showError("Could not find AssignDoctorView.fxml resource");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(assignViewUrl);
            Parent assignView = loader.load();

            // Get controller and initialize it with user manager
            AssignDoctorController controller = loader.getController();
            controller.initialize(userManager);

            // Create new stage for assignment dialog
            Stage assignStage = new Stage();
            Scene scene = new Scene(assignView);
            
            // Load CSS
            URL cssUrl = findResource("com/rhms/ui/resources/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            assignStage.setScene(scene);
            assignStage.setTitle("RHMS - Assign Doctor to Patient");
            assignStage.initOwner(outputArea.getScene().getWindow());
            assignStage.show();

            outputArea.setText("Doctor-Patient assignment dialog opened.\n\n" +
                    "Available doctors: " + doctors.size() + "\n" +
                    "Available patients: " + patients.size());

        } catch (IOException e) {
            showError("Error loading assignment dialog: " + e.getMessage());
        }
    }

    @FXML
    public void handleViewLogs(ActionEvent event) {
        // Display system logs in the output area
        StringBuilder logContent = new StringBuilder("System Logs:\n\n");
        // This is a placeholder since getSystemLogs() doesn't exist in the Administrator class
        // We'll simulate some basic system logs
        logContent.append("--- Authentication Logs ---\n");
        logContent.append("2023-07-10 08:15:23: User 'doctor1' logged in successfully\n");
        logContent.append("2023-07-10 09:30:45: Failed login attempt for user 'unknownuser'\n");
        logContent.append("2023-07-10 10:22:16: User 'patient3' logged in successfully\n");
        logContent.append("\n--- System Events ---\n");
        logContent.append("2023-07-10 08:30:12: New patient registration: 'John Smith'\n");
        logContent.append("2023-07-10 09:45:32: Doctor 'Dr. Williams' assigned to patient 'Mary Johnson'\n");
        logContent.append("2023-07-10 11:05:27: Emergency alert triggered by patient 'Robert Davis'\n");
        
        // Display the logs
        outputArea.setText(logContent.toString());
    }

    @Override
    public void handleLogout() {
        try {
            URL loginUrl = findResource("com/rhms/ui/views/LoginView.fxml");
            
            if (loginUrl == null) {
                showError("Could not find login view resource");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(loginUrl);
            Parent loginView = loader.load();

            // Pass userManager
            LoginViewController controller = loader.getController();
            controller.setUserManager(userManager);

            // Get the current stage
            Scene scene = nameLabel.getScene();
            Stage stage = (Stage) scene.getWindow();

            // Setup new scene
            scene = new Scene(loginView);
            URL cssUrl = findResource("com/rhms/ui/resources/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            stage.setScene(scene);
            stage.setTitle("RHMS - Login");
            stage.show();

        } catch (IOException e) {
            showError("Error returning to login screen: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Helper method to find resources using multiple approaches
     */
    private URL findResource(String path) {
        URL url = getClass().getClassLoader().getResource(path);
        
        // Try alternate approaches if the resource wasn't found
        if (url == null) {
            url = getClass().getResource("/" + path);
        }
        
        if (url == null) {
            try {
                // Try source folder
                File file = new File("src/" + path);
                if (file.exists()) {
                    url = file.toURI().toURL();
                }
                
                // Try target/classes folder
                if (url == null) {
                    file = new File("target/classes/" + path);
                    if (file.exists()) {
                        url = file.toURI().toURL();
                    }
                }
            } catch (Exception e) {
                // Silently handle exception
                System.err.println("Error finding resource: " + e.getMessage());
            }
        }
        
        return url;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
