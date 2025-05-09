package com.rhms.ui;

import com.rhms.userManagement.UserManager;
import com.rhms.ui.controllers.LoginViewController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class RhmsGuiApp extends Application {

    private static UserManager userManager;

    public static void setUserManager(UserManager manager) {
        userManager = manager;
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // Find login view resource
            URL loginViewUrl = findResource("com/rhms/ui/views/LoginView.fxml");
            
            if (loginViewUrl == null) {
                System.err.println("ERROR: Could not find resource: LoginView.fxml");
                showErrorDialog("Resource not found: LoginView.fxml. Check FXML file location.");
                return;
            }
            
            // Load the FXML file
            FXMLLoader loader = new FXMLLoader(loginViewUrl);
            Parent root = loader.load();
            
            // Get the controller and pass the UserManager
            LoginViewController controller = loader.getController();
            controller.setUserManager(userManager);
            
            // Setup and display the scene
            Scene scene = new Scene(root, 1400, 900);
            
            // Load CSS
            URL cssUrl = findResource("com/rhms/ui/resources/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
            
            primaryStage.setTitle("RHMS - Remote Healthcare Monitoring System");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.centerOnScreen();
            primaryStage.show();
            
        } catch (IOException e) {
            System.err.println("Error loading FXML: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("Failed to load application interface: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("An unexpected error occurred during application start: " + e.getMessage());
            e.printStackTrace();
            showErrorDialog("An unexpected error occurred: " + e.getMessage());
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
            }
        }
        
        return url;
    }

    private void showErrorDialog(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setTitle("Application Error");
        alert.setHeaderText("Error Starting Application");
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
