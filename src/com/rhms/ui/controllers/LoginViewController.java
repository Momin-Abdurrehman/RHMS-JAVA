package com.rhms.ui.controllers;

import com.rhms.userManagement.*;
import com.rhms.Database.UserDatabaseHandler;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginViewController {

    private static final Logger LOGGER = Logger.getLogger(LoginViewController.class.getName());

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Label messageLabel;

    private UserManager userManager;
    private UserDatabaseHandler dbHandler;
    private User currentUser;

    public void initialize() {
        try {
            // Initialize user manager if not already set
            if (userManager == null) {
                userManager = new UserManager();
                LOGGER.info("UserManager initialized in LoginViewController");
            }

            dbHandler = new UserDatabaseHandler();
            LOGGER.info("UserDatabaseHandler initialized in LoginViewController");

            // Add enter key support for login
            passwordField.setOnAction(event -> handleLogin(event));
            loginButton.setOnAction(event -> handleLogin(event));

            LOGGER.info("LoginViewController initialization complete");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during LoginViewController initialization", e);
            showErrorMessage("Initialization error: " + e.getMessage());
        }
    }

    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
        LOGGER.info("UserManager set externally in LoginViewController");
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        try {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();

            LOGGER.info("Login attempt with username: " + username);

            // Basic validation
            if (username.isEmpty() || password.isEmpty()) {
                showErrorMessage("Username and password cannot be empty");
                return;
            }

            // Attempt login
            User user = dbHandler.getUserByUsername(username);
            if (user == null) {
                showErrorMessage("Invalid username or password");
                passwordField.clear();
                LOGGER.warning("Login failed: User not found for username: " + username);
                return;
            }

            if (!user.getPassword().equals(password)) {
                showErrorMessage("Invalid username or password");
                passwordField.clear();
                LOGGER.warning("Login failed: Incorrect password for username: " + username);
                return;
            }

            // Login successful
            currentUser = user;
            messageLabel.setText("Login successful!");
            LOGGER.info("Login successful for user: " + username);

            // Navigate to appropriate dashboard based on user type
            String viewName = getDashboardViewName(user);
            LOGGER.info("Loading dashboard: " + viewName + " for user type: " + user.getClass().getSimpleName());

            try {
                // Load dashboard FXML with absolute path first
                String fxmlPath = "/com/rhms/ui/views/" + viewName;
                URL dashboardUrl = getClass().getResource(fxmlPath);

                if (dashboardUrl == null) {
                    LOGGER.warning("Could not find dashboard at: " + fxmlPath);
                    // Try alternate path formats
                    dashboardUrl = findResource("com/rhms/ui/views/" + viewName);
                }

                if (dashboardUrl == null) {
                    showErrorMessage("Could not find dashboard view: " + viewName);
                    LOGGER.severe("Dashboard view not found: " + viewName);
                    return;
                }

                LOGGER.info("Dashboard resource found at: " + dashboardUrl);
                FXMLLoader loader = new FXMLLoader(dashboardUrl);
                Parent dashboardView = loader.load();

                // Pass user to the dashboard controller
                Object controller = loader.getController();
                if (controller instanceof DashboardController) {
                    LOGGER.info("Setting up controller: " + controller.getClass().getName());
                    DashboardController dashboardController = (DashboardController) controller;
                    dashboardController.setUser(user);
                    dashboardController.setUserManager(userManager);
                    dashboardController.initializeDashboard();
                } else {
                    LOGGER.warning("Controller does not implement DashboardController: " +
                                 (controller != null ? controller.getClass().getName() : "null"));
                }

                // Setup new scene
                Scene scene = new Scene(dashboardView);

                // Load CSS
                URL cssUrl = findResource("com/rhms/ui/resources/styles.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                    LOGGER.info("CSS loaded successfully");
                } else {
                    LOGGER.warning("CSS not found");
                }

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(scene);
                stage.setTitle("RHMS - " + getUserTypeString(user) + " Dashboard");
                stage.show();
                LOGGER.info("Dashboard displayed successfully");

            } catch (IOException e) {
                showErrorMessage("Error loading dashboard: " + e.getMessage());
                LOGGER.log(Level.SEVERE, "Error loading dashboard", e);
                e.printStackTrace();
            }
        } catch (Exception e) {
            showErrorMessage("Unexpected error: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Unexpected error in handleLogin", e);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        try {
            LOGGER.info("Handling register button click");
            
            // Try multiple approaches to find the registration view
            URL regUrl = null;
            
            // Approach 1: Direct class resource
            regUrl = getClass().getResource("/com/rhms/ui/views/RegistrationDashboard.fxml");
            LOGGER.info("Approach 1 result: " + (regUrl != null ? "found" : "not found"));
            
            // Approach 2: Using findResource helper
            if (regUrl == null) {
                regUrl = findResource("com/rhms/ui/views/RegistrationDashboard.fxml");
                LOGGER.info("Approach 2 result: " + (regUrl != null ? "found" : "not found"));
            }
            
            // Approach 3: Try direct file access
            if (regUrl == null) {
                File file = new File("src/com/rhms/ui/views/RegistrationDashboard.fxml");
                if (file.exists()) {
                    regUrl = file.toURI().toURL();
                    LOGGER.info("Approach 3 result: found at " + file.getAbsolutePath());
                } else {
                    LOGGER.warning("File does not exist at: " + file.getAbsolutePath());
                }
            }
            
            // Check one more location
            if (regUrl == null) {
                File file = new File("/Users/apple/Desktop/RHMS-JAVA/src/com/rhms/ui/views/RegistrationDashboard.fxml");
                if (file.exists()) {
                    regUrl = file.toURI().toURL();
                    LOGGER.info("Approach 4 result: found at " + file.getAbsolutePath());
                } else {
                    LOGGER.warning("File does not exist at: " + file.getAbsolutePath());
                }
            }
            
            if (regUrl == null) {
                showErrorMessage("Could not find registration view resource. Please make sure the file exists.");
                LOGGER.severe("Registration view resource not found after multiple attempts");
                return;
            }
            
            LOGGER.info("Loading registration view from: " + regUrl);
            FXMLLoader loader = new FXMLLoader(regUrl);
            Parent regView = loader.load();
            
            // Pass UserManager to registration controller
            RegistrationController regController = loader.getController();
            regController.setUserManager(userManager);
            
            // Setup new scene
            Scene scene = new Scene(regView);
            
            URL cssUrl = findResource("com/rhms/ui/resources/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("RHMS - Registration");
            stage.show();
            
            LOGGER.info("Navigation to registration screen successful");
        } catch (IOException e) {
            showErrorMessage("Error opening registration form: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error navigating to registration screen", e);
            e.printStackTrace();
        }
    }

    private void loadDashboardForUser(User user) {
        try {
            String fxmlFile;

            // Determine which dashboard to load based on user role
            if (user instanceof Patient) {
                fxmlFile = "PatientDashboard.fxml";
            } else {
                fxmlFile = "StaffDashboard.fxml";
            }
            
            // Load the resource bundle for internationalization
            ResourceBundle bundle = ResourceBundle.getBundle("com.rhms.ui.resources.UIResources",
                                                           Locale.getDefault());

            // Create the FXML loader with the resource bundle
            URL url = findResource("com/rhms/ui/views/" + fxmlFile);
            if (url == null) {
                showErrorMessage("Error loading dashboard: Could not find " + fxmlFile);
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(url, bundle);
            Parent dashboardView = loader.load();
            
            // Get the controller and initialize it with user data
            DashboardController controller = loader.getController();
            controller.setUser(user);
            controller.setUserManager(userManager);
            controller.initializeDashboard();

            // Set up the scene
            Scene scene = dashboardView.getScene();
            if (scene == null) {
                scene = new Scene(dashboardView);
            }
            
            // Apply CSS if available
            URL cssUrl = findResource("com/rhms/ui/resources/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
            
            // Show the dashboard in the current stage
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("RHMS - Dashboard");
            stage.show();
            
        } catch (IOException e) {
            showErrorMessage("Error loading dashboard: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showErrorMessage("Error initializing dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Helper method to find resources using multiple approaches
     */
    private URL findResource(String path) {
        LOGGER.info("Attempting to find resource: " + path);

        // Try various class loaders and approaches
        URL url = getClass().getClassLoader().getResource(path);
        if (url != null) {
            LOGGER.info("Found resource using getClassLoader(): " + url);
            return url;
        }

        url = getClass().getResource("/" + path);
        if (url != null) {
            LOGGER.info("Found resource using getResource with leading slash: " + url);
            return url;
        }

        url = getClass().getResource(path);
        if (url != null) {
            LOGGER.info("Found resource using getResource without leading slash: " + url);
            return url;
        }
        
        try {
            File file = new File("src/" + path);
            if (file.exists()) {
                url = file.toURI().toURL();
                LOGGER.info("Found resource as file: " + url);
                return url;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error checking file existence", e);
        }
        
        LOGGER.warning("Resource not found after all attempts: " + path);
        return null;
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
        LOGGER.warning("Error message displayed: " + message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

