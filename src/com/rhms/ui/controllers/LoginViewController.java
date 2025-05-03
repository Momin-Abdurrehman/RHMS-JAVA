package com.rhms.ui.controllers;

import com.rhms.userManagement.*;
import com.rhms.Database.UserDatabaseHandler;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.io.File;

public class LoginViewController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Label messageLabel;

    private UserManager userManager;
    private UserDatabaseHandler dbHandler;
    private User currentUser;

    public void initialize() {
        dbHandler = new UserDatabaseHandler();
        
        // Add enter key support for login
        passwordField.setOnAction(event -> handleLogin(event));
    }

    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        
        // Basic validation
        if (username.isEmpty() || password.isEmpty()) {
            showErrorMessage("Username and password cannot be empty");
            return;
        }
        
        // Attempt login
        User user = dbHandler.getUserByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            currentUser = user;
            messageLabel.setText("");
            
            // Navigate to appropriate dashboard based on user type
            try {
                String viewName = getDashboardViewName(user);
                
                // Load dashboard FXML
                URL dashboardUrl = findResource("com/rhms/ui/views/" + viewName);
                
                if (dashboardUrl == null) {
                    showErrorMessage("Could not find dashboard view: " + viewName);
                    return;
                }
                
                FXMLLoader loader = new FXMLLoader(dashboardUrl);
                Parent dashboardView = loader.load();
                
                // Pass user to the dashboard controller
                Object controller = loader.getController();
                if (controller instanceof DashboardController) {
                    ((DashboardController) controller).setUser(user);
                    ((DashboardController) controller).setUserManager(userManager);
                    ((DashboardController) controller).initializeDashboard();
                }
                
                // Setup new scene
                Scene scene = new Scene(dashboardView);
                
                // Load CSS
                URL cssUrl = findResource("com/rhms/ui/resources/styles.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                }
                
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(scene);
                stage.setTitle("RHMS - " + getUserTypeString(user) + " Dashboard");
                stage.show();
                
            } catch (IOException e) {
                showErrorMessage("Error loading dashboard: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            showErrorMessage("Invalid username or password");
            passwordField.clear();
        }
    }
    
    @FXML
    private void handleRegister(ActionEvent event) {
        try {
            // Load registration view
            URL registrationUrl = findResource("com/rhms/ui/views/RegistrationDashboard.fxml");
            
            if (registrationUrl == null) {
                showErrorMessage("Could not find registration view");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(registrationUrl);
            Parent registrationView = loader.load();
            
            // Pass userManager to registration controller
            RegistrationController controller = loader.getController();
            controller.setUserManager(userManager);
            
            // Setup new scene
            Scene scene = new Scene(registrationView);
            
            // Load CSS
            URL cssUrl = findResource("com/rhms/ui/resources/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("RHMS - Registration");
            stage.show();
            
        } catch (IOException e) {
            showErrorMessage("Error loading registration page: " + e.getMessage());
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
    
    private String getDashboardViewName(User user) {
        if (user instanceof Patient) {
            return "PatientDashboard.fxml";
        } else if (user instanceof Doctor) {
            return "DoctorDashboard.fxml";
        } else if (user instanceof Administrator) {
            return "AdminDashboard.fxml";
        } else {
            return "DefaultDashboard.fxml";
        }
    }
    
    private String getUserTypeString(User user) {
        if (user instanceof Patient) {
            return "Patient";
        } else if (user instanceof Doctor) {
            return "Doctor";
        } else if (user instanceof Administrator) {
            return "Administrator";
        } else {
            return "User";
        }
    }
    
    private void showErrorMessage(String message) {
        messageLabel.setText(message);
    }
}
