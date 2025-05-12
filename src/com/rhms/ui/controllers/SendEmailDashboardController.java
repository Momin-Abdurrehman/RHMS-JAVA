package com.rhms.ui.controllers;

import com.rhms.notifications.EmailNotification;
import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;
import com.rhms.userManagement.User;
import com.rhms.userManagement.UserManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class SendEmailDashboardController implements Initializable {

    @FXML private ComboBox<String> recipientTypeComboBox;
    @FXML private ComboBox<User> recipientComboBox;
    @FXML private TextField subjectField;
    @FXML private TextArea messageArea;
    @FXML private Label statusLabel;

    private UserManager userManager;
    private EmailNotification emailNotification;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize email notification service
        emailNotification = new EmailNotification();
        
        // Set up recipient type combo box
        ObservableList<String> recipientTypes = FXCollections.observableArrayList("All Doctors", "All Patients", "Specific Doctor", "Specific Patient");
        recipientTypeComboBox.setItems(recipientTypes);
        
        // Set up the change listener for recipient type
        recipientTypeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                updateRecipientComboBox(newValue);
            }
        });
        
        // Set up display format for recipient combo box
        recipientComboBox.setConverter(new StringConverter<User>() {
            @Override
            public String toString(User user) {
                if (user == null) return "";
                String userType = user instanceof Doctor ? "Doctor" : "Patient";
                return user.getName() + " (" + userType + ", ID: " + user.getUserID() + ", Email: " + user.getEmail() + ")";
            }

            @Override
            public User fromString(String string) {
                return null; // Not needed for this use case
            }
        });
    }
    
    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
        recipientTypeComboBox.getSelectionModel().selectFirst();
    }

    private void updateRecipientComboBox(String recipientType) {
        if (userManager == null) return;
        
        ObservableList<User> recipients = FXCollections.observableArrayList();
        
        switch (recipientType) {
            case "All Doctors":
                recipientComboBox.setDisable(true);
                break;
            case "All Patients":
                recipientComboBox.setDisable(true);
                break;
            case "Specific Doctor":
                List<Doctor> doctors = userManager.getAllDoctors();
                recipients.addAll(doctors);
                recipientComboBox.setItems(recipients);
                recipientComboBox.setDisable(false);
                break;
            case "Specific Patient":
                List<Patient> patients = userManager.getAllPatients();
                recipients.addAll(patients);
                recipientComboBox.setItems(recipients);
                recipientComboBox.setDisable(false);
                break;
        }
        
        if (!recipientComboBox.isDisable() && !recipients.isEmpty()) {
            recipientComboBox.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void handleSendEmail(ActionEvent event) {
        // Validate inputs
        if (recipientTypeComboBox.getValue() == null) {
            showStatus("Please select a recipient type", true);
            return;
        }
        
        String subject = subjectField.getText().trim();
        if (subject.isEmpty()) {
            showStatus("Subject cannot be empty", true);
            return;
        }
        
        String message = messageArea.getText().trim();
        if (message.isEmpty()) {
            showStatus("Message cannot be empty", true);
            return;
        }
        
        // Disable the send button and show progress
        Button sendButton = (Button) event.getSource();
        sendButton.setDisable(true);
        showStatus("Sending email...", false);
        
        Task<Boolean> sendTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                boolean success = false;
                String recipientType = recipientTypeComboBox.getValue();
                
                switch (recipientType) {
                    case "All Doctors":
                        success = sendToAllDoctors(subject, message);
                        break;
                    case "All Patients":
                        success = sendToAllPatients(subject, message);
                        break;
                    case "Specific Doctor":
                    case "Specific Patient":
                        User selectedUser = recipientComboBox.getValue();
                        if (selectedUser != null) {
                            success = emailNotification.sendNotification(selectedUser.getEmail(), subject, message);
                        } else {
                            success = false;
                        }
                        break;
                }
                
                return success;
            }
            
            @Override
            protected void succeeded() {
                Boolean success = getValue();
                if (success) {
                    showStatus("Email sent successfully!", false);
                    // Close the window after a delay
                    new Thread(() -> {
                        try {
                            Thread.sleep(1500);
                            Platform.runLater(() -> closeWindow());
                        } catch (InterruptedException e) {
                            // Ignore interruption
                        }
                    }).start();
                } else {
                    showStatus("Failed to send email. Please try again.", true);
                    sendButton.setDisable(false);
                }
            }
        };
        
        new Thread(sendTask).start();
    }
    
    private boolean sendToAllDoctors(String subject, String message) {
        List<Doctor> doctors = userManager.getAllDoctors();
        boolean allSuccess = true;
        
        for (Doctor doctor : doctors) {
            boolean sent = emailNotification.sendNotification(doctor.getEmail(), subject, message);
            if (!sent) allSuccess = false;
        }
        
        return allSuccess;
    }
    
    private boolean sendToAllPatients(String subject, String message) {
        List<Patient> patients = userManager.getAllPatients();
        boolean allSuccess = true;
        
        for (Patient patient : patients) {
            boolean sent = emailNotification.sendNotification(patient.getEmail(), subject, message);
            if (!sent) allSuccess = false;
        }
        
        return allSuccess;
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeWindow();
    }
    
    private void closeWindow() {
        Stage stage = (Stage) messageArea.getScene().getWindow();
        stage.close();
    }
    
    private void showStatus(String message, boolean isError) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            statusLabel.setTextFill(isError ? Color.RED : Color.GREEN);
        });
    }
}
