package com.rhms.ui.controllers;

import com.rhms.userManagement.Administrator;
import com.rhms.userManagement.UserManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class AddAdminDashboardController {
    
    @FXML
    private TextField nameField;
    
    @FXML
    private TextField emailField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private PasswordField confirmPasswordField;
    
    @FXML
    private TextField phoneField;
    
    @FXML
    private TextField addressField;
    
    @FXML
    private Label messageLabel;
    
    private UserManager userManager;
    
    public void initialize() {
        this.userManager = new UserManager();
    }
    
    @FXML
    private void handleRegisterAdmin(ActionEvent event) {
        // Clear previous messages
        messageLabel.setText("");
        
        // Validate form fields
        if (!validateFields()) {
            return;
        }
        
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();
        
        // Check if email already exists
        if (userManager.isEmailExists(email)) {
            showError("Email address already exists. Please use a different email.");
            return;
        }
        
        // Register the administrator
        Administrator admin = userManager.registerAdministrator(name, email, password, phone, address);
        
        if (admin != null) {
            showSuccess("Administrator registered successfully with ID: " + admin.getUserID());
            clearFields();
        } else {
            showError("Failed to register administrator. Please try again.");
        }
    }
    
    @FXML
    private void handleCancel(ActionEvent event) {
        // Close the window
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
    
    private boolean validateFields() {
        // Check for empty fields
        if (nameField.getText().trim().isEmpty()) {
            showError("Name cannot be empty");
            return false;
        }
        
        if (emailField.getText().trim().isEmpty()) {
            showError("Email cannot be empty");
            return false;
        }
        
        if (!isValidEmail(emailField.getText().trim())) {
            showError("Invalid email format");
            return false;
        }
        
        if (passwordField.getText().isEmpty()) {
            showError("Password cannot be empty");
            return false;
        }
        
        if (passwordField.getText().length() < 6) {
            showError("Password must be at least 6 characters long");
            return false;
        }
        
        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            showError("Passwords do not match");
            return false;
        }
        
        if (phoneField.getText().trim().isEmpty()) {
            showError("Phone number cannot be empty");
            return false;
        }
        
        if (addressField.getText().trim().isEmpty()) {
            showError("Address cannot be empty");
            return false;
        }
        
        return true;
    }
    
    private boolean isValidEmail(String email) {
        // Simple email validation using regex
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }
    
    private void showError(String message) {
        messageLabel.setTextFill(Color.RED);
        messageLabel.setText(message);
    }
    
    private void showSuccess(String message) {
        messageLabel.setTextFill(Color.GREEN);
        messageLabel.setText(message);
    }
    
    private void clearFields() {
        nameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        phoneField.clear();
        addressField.clear();
    }
    
    // Method to set the UserManager
    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }
}
