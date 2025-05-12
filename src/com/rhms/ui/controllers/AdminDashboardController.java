package com.rhms.ui.controllers;

import com.rhms.userManagement.Administrator;
import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;
import com.rhms.userManagement.User;
import com.rhms.userManagement.UserManager;
import com.rhms.reporting.ReportFormat;
import com.rhms.reporting.ReportGenerator;
import com.rhms.reporting.DownloadHandler;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.scene.control.ButtonBar;
import javafx.stage.Modality;

import java.io.IOException;
import java.io.File;
import java.net.URL;
import java.util.List;

public class AdminDashboardController implements DashboardController {

    @FXML private Label nameLabel;
    @FXML private TextArea outputArea;
    @FXML private VBox contentArea;
    
    // Statistics labels
    @FXML private Label doctorCountLabel;
    @FXML private Label patientCountLabel;
    @FXML private Label adminCountLabel;

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
        
        // Update system statistics
        updateSystemStatistics();
    }
    
    /**
     * Updates the statistics displayed on the dashboard
     */
    private void updateSystemStatistics() {
        if (userManager != null) {
            List<Doctor> doctors = userManager.getAllDoctors();
            List<Patient> patients = userManager.getAllPatients();
            List<Administrator> admins = userManager.getAllAdministrators();
            
            doctorCountLabel.setText(String.valueOf(doctors.size()));
            patientCountLabel.setText(String.valueOf(patients.size()));
            adminCountLabel.setText(String.valueOf(admins.size()));
            
            // Log statistics to output area
            appendToOutput("System Statistics Updated: " +
                    doctors.size() + " doctors, " + 
                    patients.size() + " patients, " + 
                    admins.size() + " administrators");
        } else {
            doctorCountLabel.setText("N/A");
            patientCountLabel.setText("N/A");
            adminCountLabel.setText("N/A");
            appendToOutput("Error: User manager not initialized. Cannot display statistics.");
        }
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

            // Only set owner if the window is available
            if (outputArea.getScene() != null && outputArea.getScene().getWindow() != null) {
                assignStage.initOwner(outputArea.getScene().getWindow());
            }

            // Add event handler to update data when stage is closed
            assignStage.setOnHidden(e -> {
                // Refresh our view of doctor-patient assignments after the dialog is closed
                refreshAssignmentsView();
            });

            assignStage.show();

        } catch (IOException e) {
            showError("Error loading assignment dialog: " + e.getMessage());
        }
    }

    @FXML
    public void handleManageUsers(ActionEvent event) {
        try {
            // Find the ManageUsersDashboard.fxml resource
            URL manageUsersUrl = findResource("com/rhms/ui/views/ManageUsersDashboard.fxml");
            
            if (manageUsersUrl == null) {
                showError("Could not find ManageUsersDashboard.fxml resource");
                return;
            }
            
            // Load the FXML
            FXMLLoader loader = new FXMLLoader(manageUsersUrl);
            Parent manageUsersView = loader.load();
            
            // Get the controller and initialize it with user manager
            ManageUsersDashboardController controller = loader.getController();
            controller.setUserManager(userManager);
            
            // Clear the content area and add the manage users view
            contentArea.getChildren().clear();
            
            // Add the title
            Label titleLabel = new Label("Manage Users");
            titleLabel.getStyleClass().add("section-title");
            titleLabel.setFont(new Font("System Bold", 24));
            
            // Add the title and manage users view to the content area
            contentArea.getChildren().add(titleLabel);
            contentArea.getChildren().add(manageUsersView);

            // Make the manage users view take all available space
            VBox.setVgrow(manageUsersView, Priority.ALWAYS);
            
        } catch (IOException e) {
            showError("Error loading manage users view: " + e.getMessage());
            e.printStackTrace();
        }
        
        // After returning from manage users, update statistics
        updateSystemStatistics();
    }

    /**
     * Refresh the view to show current doctor-patient assignments
     * Called after the assignment window is closed
     */
    private void refreshAssignmentsView() {
        // First ensure assignments are reloaded from the database
        userManager.loadAllAssignmentsFromDatabase();

        // Then update the display
        StringBuilder assignments = new StringBuilder("Doctor-Patient Assignments Updated\n\n");
        assignments.append("Current Doctor-Patient Assignments:\n");

        List<Doctor> doctors = userManager.getAllDoctors();
        int assignmentCount = 0;

        // Go through each doctor and list their assigned patients
        for (Doctor doctor : doctors) {
            List<Patient> assignedPatients = doctor.getAssignedPatients();
            if (!assignedPatients.isEmpty()) {
                assignments.append("\nDr. ").append(doctor.getName())
                        .append(" (").append(doctor.getSpecialization()).append(")");
                assignments.append(" is assigned to:\n");

                for (Patient patient : assignedPatients) {
                    assignments.append("  - ").append(patient.getName())
                            .append(" (ID: ").append(patient.getUserID()).append(")\n");
                    assignmentCount++;
                }
            }
        }

        if (assignmentCount == 0) {
            assignments.append("\nNo current doctor-patient assignments found.");
        } else {
            assignments.append("\nTotal number of assignments: ").append(assignmentCount);
        }

        outputArea.setText(assignments.toString());
        
        // After assignments are updated, refresh the statistics
        updateSystemStatistics();
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

    @FXML
    public void handleDownloadReport(ActionEvent event) {
        // Show a dialog to select a patient
        List<Patient> patients = userManager.getAllPatients();
        if (patients.isEmpty()) {
            showError("No patients available for report generation.");
            return;
        }

        // Create a dialog to select patient and report format
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Download Patient Report");

        ComboBox<Patient> patientComboBox = new ComboBox<>();
        patientComboBox.getItems().addAll(patients);
        patientComboBox.setPromptText("Select Patient");
        patientComboBox.setCellFactory(cb -> new ListCell<Patient>() {
            @Override
            protected void updateItem(Patient item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName() + " (ID: " + item.getUserID() + ")");
            }
        });
        patientComboBox.setButtonCell(new ListCell<Patient>() {
            @Override
            protected void updateItem(Patient item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName() + " (ID: " + item.getUserID() + ")");
            }
        });

        ComboBox<ReportFormat> formatComboBox = new ComboBox<>();
        formatComboBox.getItems().addAll(ReportFormat.values());
        formatComboBox.setValue(ReportFormat.PDF);

        VBox vbox = new VBox(10, new Label("Patient:"), patientComboBox, new Label("Report Format:"), formatComboBox);
        dialog.getDialogPane().setContent(vbox);

        // Add explicit Download and Cancel buttons
        ButtonType downloadButtonType = new ButtonType("Download", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(downloadButtonType, ButtonType.CANCEL);

        // Enable/disable Download button based on selection
        Button downloadButton = (Button) dialog.getDialogPane().lookupButton(downloadButtonType);
        downloadButton.setDisable(true);

        patientComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            downloadButton.setDisable(newVal == null || formatComboBox.getValue() == null);
        });
        formatComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            downloadButton.setDisable(patientComboBox.getValue() == null || newVal == null);
        });

        dialog.setResultConverter(button -> {
            if (button == downloadButtonType) {
                Patient selectedPatient = patientComboBox.getValue();
                ReportFormat format = formatComboBox.getValue();
                if (selectedPatient == null || format == null) {
                    showError("Please select a patient and report format.");
                    return null;
                }

                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setTitle("Select Save Location");
                File directory = directoryChooser.showDialog(nameLabel.getScene().getWindow());

                if (directory != null) {
                    try {
                        ReportGenerator reportGenerator = new ReportGenerator(selectedPatient);
                        File reportFile = reportGenerator.generateCompleteReport(directory.getAbsolutePath(), format);
                        showInfo("Report generated successfully!\nFile: " + reportFile.getAbsolutePath());
                        DownloadHandler.openFile(reportFile);
                    } catch (Exception e) {
                        showError("Error generating report: " + e.getMessage());
                    }
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    @FXML
    private void handleAddAdmin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/rhms/ui/views/AddAdminView.fxml"));
            Parent root = loader.load();

            AddAdminDashboardController controller = loader.getController();
            controller.setUserManager(userManager);

            Stage stage = new Stage();
            stage.setTitle("Register New Administrator");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            
            // Update statistics when admin dialog is closed
            stage.setOnHidden(e -> updateSystemStatistics());
            
            stage.showAndWait();

            appendToOutput("Add Admin dialog opened.");
        } catch (IOException e) {
            appendToOutput("Error opening Add Admin dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleSendEmail(ActionEvent event) {
        try {
            URL sendEmailUrl = findResource("com/rhms/ui/views/SendEmailView.fxml");

            if (sendEmailUrl == null) {
                showError("Could not find SendEmailView.fxml resource");
                return;
            }

            FXMLLoader loader = new FXMLLoader(sendEmailUrl);
            Parent sendEmailView = loader.load();

            // Get controller and initialize it with user manager
            SendEmailDashboardController controller = loader.getController();
            controller.setUserManager(userManager);

            // Create new stage for email dialog
            Stage emailStage = new Stage();
            Scene scene = new Scene(sendEmailView);

            // Load CSS
            URL cssUrl = findResource("com/rhms/ui/resources/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            emailStage.setScene(scene);
            emailStage.setTitle("RHMS - Send Email Notification");
            emailStage.initModality(Modality.APPLICATION_MODAL);

            // Only set owner if the window is available
            if (outputArea.getScene() != null && outputArea.getScene().getWindow() != null) {
                emailStage.initOwner(outputArea.getScene().getWindow());
            }

            emailStage.show();
            
            appendToOutput("Email notification window opened.");

        } catch (IOException e) {
            showError("Error loading email notification dialog: " + e.getMessage());
            e.printStackTrace();
        }
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
            Scene currentScene = nameLabel.getScene();
            Stage stage = (Stage) currentScene.getWindow();
            
            // Get current window dimensions before changing scene
            double width = stage.getWidth();
            double height = stage.getHeight();

            // Setup new scene with same dimensions as current window
            Scene scene = new Scene(loginView, width, height);
            URL cssUrl = findResource("com/rhms/ui/resources/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            stage.setScene(scene);
            stage.setTitle("RHMS - Login");
            
            // Ensure the window stays the same size
            stage.setWidth(width);
            stage.setHeight(height);
            
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

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void appendToOutput(String message) {
        outputArea.appendText(message + "\n");
    }
}
