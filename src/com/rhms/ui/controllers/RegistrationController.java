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
import java.util.logging.Level;
import java.util.logging.Logger;

public class RegistrationController {
    private static final Logger LOGGER = Logger.getLogger(RegistrationController.class.getName());
    
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
        if (userManager == null) {
            LOGGER.warning("Attempted to set null UserManager");
            return;
        }
        this.userManager = userManager;
    }
    
    public void setAdminController(AdminDashboardController adminController) {
        this.adminController = adminController;
    }
    
    @FXML
    public void handleRegister(ActionEvent event) {
        // Verify UserManager is set
        if (userManager == null) {
            showMessage("System error: UserManager not initialized", true);
            LOGGER.severe("Cannot register user: UserManager is null");
            return;
        }
        
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
            // Check if email already exists
            if (userManager.isEmailExists(email)) {
                showMessage("Email already registered in the system. Please use a different email.", true);
                return;
            }
            
            if ("Patient".equals(userType)) {
                newUser = userManager.registerPatient(name, email, password, phone, address);
            } else if ("Doctor".equals(userType)) {
                String specialization = specializationField.getText().trim();
                
                // Parse experience years with error handling
                int experienceYears;
                try {
                    experienceYears = Integer.parseInt(experienceYearsField.getText().trim());
                } catch (NumberFormatException e) {
                    showMessage("Experience years must be a valid number", true);
                    return;
                }
                
                newUser = userManager.registerDoctor(name, email, password, phone, address, 
                                                 specialization, experienceYears);
            } else if ("Administrator".equals(userType)) {
                newUser = userManager.registerAdministrator(name, email, password, phone, address);
            }
            
            if (newUser != null) {
                LOGGER.info("Successfully registered " + userType + ": " + email);
                showMessage(userType + " registered successfully: " + newUser.getUsername(), false);
                clearFields();
                
                // If this was called from admin dashboard, refresh the user management view
                if (adminController != null) {
                    adminController.handleManageUsers(null);
                }
            } else {
                LOGGER.warning("Failed to register " + userType + ": " + email);
                showMessage("Failed to register user. Please try again.", true);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during user registration", e);
            showMessage("Registration error: " + e.getMessage(), true);
        }
    }
    
    private boolean validateInput() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();
        String userType = userTypeComboBox.getValue();
        
        // Check for empty fields
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            showMessage("All fields are required", true);
            return false;
        }
        
        // Basic email validation
        if (!email.contains("@") || !email.contains(".") || email.length() < 5) {
            showMessage("Please enter a valid email address", true);
            return false;
        }
        
        // Password strength check
        if (password.length() < 6) {
            showMessage("Password must be at least 6 characters", true);
            return false;
        }
        
        // Phone number basic validation
        if (!phone.matches("\\d{10}") && !phone.matches("\\d{3}[-\\s]\\d{3}[-\\s]\\d{4}")) {
            showMessage("Please enter a valid 10-digit phone number", true);
            return false;
        }
        
        // Validate doctor-specific fields
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
                if (years > 80) {  // Reasonable upper limit
                    showMessage("Experience years value seems too high", true);
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
        try {
            // If opened from admin dashboard, just close this window
            if (adminController != null) {
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.close();
            } else {
                // Otherwise go back to login
                goBackToLogin(event);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling cancel operation", e);
            showMessage("Error closing form: " + e.getMessage(), true);
        }
    }
    
    private void goBackToLogin(ActionEvent event) {
        try {
            URL loginUrl = findResource("com/rhms/ui/views/LoginView.fxml");
            
            if (loginUrl == null) {
                showMessage("Could not find login view resource", true);
                LOGGER.severe("Login view resource not found");
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
            LOGGER.log(Level.SEVERE, "Error navigating to login screen", e);
            showMessage("Error returning to login screen: " + e.getMessage(), true);
        }
    }
    
    /**
     * Helper method to find resources using multiple approaches
     */
    private URL findResource(String path) {
        try {
            URL url = getClass().getClassLoader().getResource(path);
            
            // Try alternate approaches if the resource wasn't found
            if (url == null) {
                url = getClass().getResource("/" + path);
            }
            
            if (url == null) {
                File file = new File("src/" + path);
                if (file.exists()) {
                    url = file.toURI().toURL();
                }
            }
            
            return url;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding resource: " + path, e);
            return null;
        }
    }
    
    private void showMessage(String message, boolean isError) {
        messageLabel.setText(message);
        messageLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
        
        if (isError) {
            messageLabel.getStyleClass().add("error-message");
            // Log error messages
            LOGGER.warning("Registration error: " + message);
        } else {
            messageLabel.getStyleClass().remove("error-message");
            // Log success messages
            LOGGER.info("Registration message: " + message);
        }
    }
    
    private void clearFields() {
        try {
            nameField.clear();
            emailField.clear();
            passwordField.clear();
            phoneField.clear();
            addressField.clear();
            if (specializationField != null) specializationField.clear();
            if (experienceYearsField != null) experienceYearsField.clear();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error clearing form fields", e);
        }
    }
}
