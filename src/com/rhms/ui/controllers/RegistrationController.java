package com.rhms.ui.controllers;

import com.rhms.userManagement.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.File;
import java.net.URL;

public class RegistrationController {
    
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField phoneField;
    @FXML private TextField addressField;
    @FXML private ComboBox<String> userTypeComboBox;
    @FXML private Label messageLabel;
    
    // Doctor-specific fields
    @FXML private TextField specializationField;
    @FXML private TextField experienceYearsField;
    @FXML private VBox doctorFieldsContainer;
    
    private UserManager userManager;
    private AdminDashboardController adminController;
    
    public void initialize() {
        // Initialize user type dropdown
        userTypeComboBox.getItems().addAll("Patient", "Doctor", "Administrator");
        userTypeComboBox.getSelectionModel().selectFirst();
        
        // Show/hide doctor fields based on selection
        userTypeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (doctorFieldsContainer != null) {
                doctorFieldsContainer.setVisible("Doctor".equals(newVal));
                doctorFieldsContainer.setManaged("Doctor".equals(newVal));
            }
        });
    }
    
    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }
    
    public void setAdminController(AdminDashboardController adminController) {
        this.adminController = adminController;
    }
    
    @FXML
    public void handleRegister(ActionEvent event) {
        if (!validateInput()) {
            return;
        }
        
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();
        String userType = userTypeComboBox.getValue();
        
        User newUser = null;
        
        try {
            if ("Patient".equals(userType)) {
                newUser = userManager.registerPatient(name, email, password, phone, address);
            } else if ("Doctor".equals(userType)) {
                String specialization = specializationField.getText().trim();
                int experienceYears = Integer.parseInt(experienceYearsField.getText().trim());
                
                newUser = userManager.registerDoctor(name, email, password, phone, address, 
                                                 specialization, experienceYears);
            } else if ("Administrator".equals(userType)) {
                newUser = userManager.registerAdministrator(name, email, password, phone, address);
            }
            
            if (newUser != null) {
                showMessage("User registered successfully: " + newUser.getUsername(), false);
                clearFields();
                
                // If this was called from admin dashboard, refresh the user list
                if (adminController != null) {
                    adminController.handleViewUsers(null);
                }
            } else {
                showMessage("Failed to register user. Please try again.", true);
            }
        } catch (Exception e) {
            showMessage("Error: " + e.getMessage(), true);
        }
    }
    
    private boolean validateInput() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();
        String userType = userTypeComboBox.getValue();
        
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            showMessage("All fields are required", true);
            return false;
        }
        
        if (!email.contains("@") || !email.contains(".")) {
            showMessage("Please enter a valid email address", true);
            return false;
        }
        
        if (password.length() < 6) {
            showMessage("Password must be at least 6 characters", true);
            return false;
        }
        
        if ("Doctor".equals(userType)) {
            String specialization = specializationField.getText().trim();
            String experienceYears = experienceYearsField.getText().trim();
            
            if (specialization.isEmpty() || experienceYears.isEmpty()) {
                showMessage("Please fill in all doctor-specific fields", true);
                return false;
            }
            
            try {
                int years = Integer.parseInt(experienceYears);
                if (years < 0) {
                    showMessage("Experience years must be a positive number", true);
                    return false;
                }
            } catch (NumberFormatException e) {
                showMessage("Experience years must be a valid number", true);
                return false;
            }
        }
        
        return true;
    }
    
    @FXML
    private void handleCancel(ActionEvent event) {
        // If opened from admin dashboard, just close this window
        if (adminController != null) {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.close();
        } else {
            // Otherwise go back to login
            goBackToLogin(event);
        }
    }
    
    private void goBackToLogin(ActionEvent event) {
        try {
            URL loginUrl = findResource("com/rhms/ui/views/LoginView.fxml");
            
            if (loginUrl == null) {
                showMessage("Could not find login view resource", true);
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(loginUrl);
            Parent loginView = loader.load();
            
            // Pass userManager to login controller
            LoginViewController controller = loader.getController();
            controller.setUserManager(userManager);
            
            // Setup new scene
            Scene scene = new Scene(loginView);
            
            URL cssUrl = findResource("com/rhms/ui/resources/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("RHMS - Login");
            stage.show();
            
        } catch (IOException e) {
            showMessage("Error returning to login screen: " + e.getMessage(), true);
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
                File file = new File("src/" + path);
                if (file.exists()) {
                    url = file.toURI().toURL();
                }
            } catch (Exception e) {
                // Silently handle exception - will return null if file not found
            }
        }
        
        return url;
    }
    
    private void showMessage(String message, boolean isError) {
        messageLabel.setText(message);
        messageLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }
    
    private void clearFields() {
        nameField.clear();
        emailField.clear();
        passwordField.clear();
        phoneField.clear();
        addressField.clear();
        if (specializationField != null) specializationField.clear();
        if (experienceYearsField != null) experienceYearsField.clear();
    }
}
