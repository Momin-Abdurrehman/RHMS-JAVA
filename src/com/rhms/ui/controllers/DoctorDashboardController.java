package com.rhms.ui.controllers;

import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;
import com.rhms.userManagement.User;
import com.rhms.userManagement.UserManager;
import com.rhms.appointmentScheduling.Appointment;
import com.rhms.doctorPatientInteraction.VideoCall;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DoctorDashboardController implements DashboardController {

    @FXML private Label nameLabel;
    @FXML private VBox contentArea;

    @FXML private TableView<Patient> patientsTable;
    @FXML private TableColumn<Patient, String> patientNameColumn;
    @FXML private TableColumn<Patient, String> patientContactColumn;
    @FXML private TableColumn<Patient, String> lastCheckupColumn;
    @FXML private TableColumn<Patient, String> vitalStatusColumn;

    @FXML private TableView<Appointment> appointmentsTable;
    @FXML private TableColumn<Appointment, String> dateColumn;
    @FXML private TableColumn<Appointment, String> timeColumn;
    @FXML private TableColumn<Appointment, String> patientColumn;
    @FXML private TableColumn<Appointment, String> statusColumn;

    private Doctor currentDoctor;
    private UserManager userManager;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    @Override
    public void setUser(User user) {
        if (user instanceof Doctor) {
            this.currentDoctor = (Doctor) user;
        } else {
            throw new IllegalArgumentException("User must be a Doctor");
        }
    }

    @Override
    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }

    @Override
    public void initializeDashboard() {
        nameLabel.setText("Dr. " + currentDoctor.getName());

        // Initialize patients table
        patientNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getName()));

        patientContactColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPhone()));

        lastCheckupColumn.setCellValueFactory(cellData -> {
            return new SimpleStringProperty("N/A");
        });

        vitalStatusColumn.setCellValueFactory(cellData -> {
            return new SimpleStringProperty("Normal");
        });

        // Initialize appointments table
        dateColumn.setCellValueFactory(cellData -> {
            return new SimpleStringProperty(dateFormat.format(cellData.getValue().getAppointmentDate()));
        });

        timeColumn.setCellValueFactory(cellData -> {
            return new SimpleStringProperty(timeFormat.format(cellData.getValue().getAppointmentDate()));
        });

        patientColumn.setCellValueFactory(cellData -> {
            return new SimpleStringProperty(cellData.getValue().getPatient().getName());
        });

        statusColumn.setCellValueFactory(cellData -> {
            return new SimpleStringProperty(cellData.getValue().getStatus());
        });

        // Load data
        loadPatients();
        loadAppointments();
    }

    private void loadPatients() {
        List<Patient> patients = currentDoctor.getAssignedPatients();
        ObservableList<Patient> patientData = FXCollections.observableArrayList(patients);
        patientsTable.setItems(patientData);
    }

    private void loadAppointments() {
        List<Appointment> appointments = new ArrayList<>();
        ObservableList<Appointment> appointmentData = FXCollections.observableArrayList(appointments);
        appointmentsTable.setItems(appointmentData);
    }

    @FXML
    public void handlePatients(ActionEvent event) {
        Patient selectedPatient = patientsTable.getSelectionModel().getSelectedItem();
        if (selectedPatient == null) {
            showMessage("Please select a patient first.");
            return;
        }
        showMessage("Patient details view not implemented yet.");
    }

    @FXML
    public void handleAppointments(ActionEvent event) {
        showMessage("Appointments management view not implemented yet.");
    }

    @FXML
    public void handleProvideFeedback(ActionEvent event) {
        Patient selectedPatient = patientsTable.getSelectionModel().getSelectedItem();
        if (selectedPatient == null) {
            showMessage("Please select a patient first.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Provide Feedback");
        dialog.setHeaderText("Feedback for " + selectedPatient.getName());
        dialog.setContentText("Enter your comments:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            showMessage("Feedback provided successfully!");
        }
    }

    @FXML
    public void handleVideoCall(ActionEvent event) {
        Patient selectedPatient = patientsTable.getSelectionModel().getSelectedItem();
        if (selectedPatient == null) {
            showMessage("Please select a patient to call.");
            return;
        }

        String meetingId = VideoCall.generateMeetingId();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Start Video Call");
        alert.setHeaderText("Start a video call with " + selectedPatient.getName());
        alert.setContentText("Meeting ID: " + meetingId + "\n\nClick OK to open the video call in your browser.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            VideoCall.startVideoCall(meetingId);
        }
    }

    @Override
    public void handleLogout() {
        try {
            URL loginUrl = getClass().getResource("/com/rhms/ui/views/LoginView.fxml");

            if (loginUrl == null) {
                showMessage("Could not find login view resource");
                return;
            }

            FXMLLoader loader = new FXMLLoader(loginUrl);
            Parent loginView = loader.load();

            LoginViewController controller = loader.getController();
            controller.setUserManager(userManager);

            Stage stage = (Stage) nameLabel.getScene().getWindow();
            Scene scene = new Scene(loginView);
            scene.getStylesheets().add(getClass().getResource("/com/rhms/ui/resources/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("RHMS - Login");
            stage.show();
        } catch (IOException e) {
            showMessage("Error loading login view: " + e.getMessage());
        }
    }

    private void showMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}