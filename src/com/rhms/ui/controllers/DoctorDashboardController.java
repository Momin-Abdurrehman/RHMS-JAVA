package com.rhms.ui.controllers;

import com.rhms.Database.AppointmentDatabaseHandler;
import com.rhms.appointmentScheduling.AppointmentManager;
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

    private AppointmentManager appointmentManager;
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

        // Create appointment manager with database handler
        AppointmentDatabaseHandler dbHandler = new AppointmentDatabaseHandler(userManager);
        this.appointmentManager = new AppointmentManager(dbHandler);
        
        // If doctor is already set, load their assignments
        if (currentDoctor != null) {
            loadAssignmentsForDoctor();
        }
    }

    @Override
    public void initializeDashboard() {
        nameLabel.setText("Dr. " + currentDoctor.getName());

        // Explicitly load assignments for this doctor
        loadAssignmentsForDoctor();
        
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
    
    /**
     * Load doctor-patient assignments from database for the current doctor
     */
    private void loadAssignmentsForDoctor() {
        if (userManager == null || currentDoctor == null) {
            System.out.println("Cannot load assignments: UserManager or Doctor is null");
            return;
        }
        
        System.out.println("Loading assignments for doctor: " + currentDoctor.getName() + " (ID: " + currentDoctor.getUserID() + ")");
        userManager.loadAssignmentsForDoctor(currentDoctor);
    }

    private void loadPatients() {
        List<Patient> patients = currentDoctor.getAssignedPatients();
        System.out.println("Doctor " + currentDoctor.getName() + " has " + patients.size() + " assigned patients");
        
        // Debug: print all assigned patients
        for (Patient patient : patients) {
            System.out.println("  - Patient: " + patient.getName() + " (ID: " + patient.getUserID() + ")");
        }
        
        ObservableList<Patient> patientData = FXCollections.observableArrayList(patients);
        patientsTable.setItems(patientData);
        
        // If no patients, set a placeholder message
        if (patients.isEmpty()) {
            patientsTable.setPlaceholder(new Label("No patients assigned to you. Please contact an administrator."));
        }
    }

    private void loadAppointments() {
        try {
            List<Appointment> appointments;

            if (appointmentManager != null) {
                // Load from database
                appointments = appointmentManager.loadDoctorAppointments(currentDoctor);
            } else {
                // Fallback to empty list
                appointments = new ArrayList<>();
            }

            ObservableList<Appointment> appointmentData = FXCollections.observableArrayList(appointments);
            appointmentsTable.setItems(appointmentData);
        } catch (AppointmentManager.AppointmentException e) {
            showMessage("Error loading appointments: " + e.getMessage());
            appointmentsTable.setItems(FXCollections.observableArrayList());
        }
    }

    /**
     * Update the status of an appointment
     */
    private boolean updateAppointmentStatus(Appointment appointment, String newStatus) {
        try {
            if (appointmentManager != null) {
                return appointmentManager.updateAppointmentStatus(appointment, newStatus);
            } else {
                appointment.setStatus(newStatus);
                return true;
            }
        } catch (AppointmentManager.AppointmentException e) {
            showMessage("Error updating appointment: " + e.getMessage());
            return false;
        }
    }

    @FXML
    public void handlePatients(ActionEvent event) {
        Patient selectedPatient = patientsTable.getSelectionModel().getSelectedItem();
        if (selectedPatient == null) {
            showMessage("Please select a patient first.");
            return;
        }
        
        try {
            // Load the patient details view
            URL patientDetailsUrl = getClass().getResource("/com/rhms/ui/views/DoctorPatientDetailsView.fxml");
            if (patientDetailsUrl == null) {
                showMessage("Could not find patient details view resource");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(patientDetailsUrl);
            Parent patientDetailsView = loader.load();
            
            // Initialize the controller with the selected patient and current doctor
            DoctorPatientDetailsController controller = loader.getController();
            controller.initializeData(currentDoctor, selectedPatient, userManager);
            
            // Show the view in a new window
            Stage stage = new Stage();
            stage.setTitle("Patient Details - " + selectedPatient.getName());
            stage.setScene(new Scene(patientDetailsView));
            stage.show();
        } catch (IOException e) {
            showMessage("Error loading patient details view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleAppointments(ActionEvent event) {
        try {
            // Load the appointments management view
            URL appointmentsUrl = getClass().getResource("/com/rhms/ui/views/DoctorAppointmentsView.fxml");
            if (appointmentsUrl == null) {
                showMessage("Could not find appointments view resource");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(appointmentsUrl);
            Parent appointmentsView = loader.load();
            
            // Initialize the controller with the current doctor
            DoctorAppointmentsController controller = loader.getController();
            controller.initializeData(currentDoctor, userManager, appointmentManager);
            
            // Show the view in a new window
            Stage stage = new Stage();
            stage.setTitle("Appointment Management - Dr. " + currentDoctor.getName());
            Scene scene = new Scene(appointmentsView);
            
            // Apply CSS if available
            URL cssUrl = getClass().getResource("/com/rhms/ui/resources/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
            
            stage.setScene(scene);
            
            // Add listener to refresh main dashboard appointments when appointments view is closed
            stage.setOnHidden(e -> loadAppointments());
            
            stage.show();
        } catch (IOException e) {
            showMessage("Error loading appointments view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleProvideFeedback(ActionEvent event) {
        Patient selectedPatient = patientsTable.getSelectionModel().getSelectedItem();
        if (selectedPatient == null) {
            showMessage("Please select a patient first.");
            return;
        }

        try {
            // Load the feedback form view
            URL feedbackFormUrl = getClass().getResource("/com/rhms/ui/views/DoctorFeedbackView.fxml");
            if (feedbackFormUrl == null) {
                showMessage("Could not find feedback form resource");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(feedbackFormUrl);
            Parent feedbackFormView = loader.load();
            
            // Initialize the controller with the selected patient and current doctor
            DoctorFeedbackController controller = loader.getController();
            controller.initializeData(currentDoctor, selectedPatient);
            
            // Show the view in a new window
            Stage stage = new Stage();
            stage.setTitle("Provide Feedback - " + selectedPatient.getName());
            stage.setScene(new Scene(feedbackFormView));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL); // Make it modal
            
            // Add CSS styles if available
            URL cssUrl = getClass().getResource("/com/rhms/ui/resources/styles.css");
            if (cssUrl != null) {
                stage.getScene().getStylesheets().add(cssUrl.toExternalForm());
            }
            
            stage.showAndWait();
            
            // After closing the feedback form, refresh data if needed
            loadPatients(); // This will refresh the patient list to show any updates
        } catch (IOException e) {
            showMessage("Error loading feedback form: " + e.getMessage());
            e.printStackTrace();
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
