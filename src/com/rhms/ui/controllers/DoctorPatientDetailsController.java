package com.rhms.ui.controllers;

import com.rhms.doctorPatientInteraction.Feedback;
import com.rhms.healthDataHandling.VitalSign;
import com.rhms.healthDataHandling.VitalsDatabase;
import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;

import com.rhms.userManagement.UserManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the Doctor Patient Details view that displays detailed patient information
 * and medical history to the doctor.
 */
public class DoctorPatientDetailsController {
    private static final Logger LOGGER = Logger.getLogger(DoctorPatientDetailsController.class.getName());

    // Patient information section
    @FXML private Label patientNameLabel;
    @FXML private Label patientIdLabel;
    @FXML private Label emailLabel;
    @FXML private Label phoneLabel;
    @FXML private TextArea addressTextArea;

    // Vitals tab
    @FXML private TableView<VitalSign> vitalsTable;
    @FXML private TableColumn<VitalSign, String> dateColumn;
    @FXML private TableColumn<VitalSign, String> bpColumn;
    @FXML private TableColumn<VitalSign, String> heartRateColumn;
    @FXML private TableColumn<VitalSign, String> tempColumn;
 

    // Medical history tab
    @FXML private ListView<String> medicalHistoryList;

    // Previous feedback tab
    @FXML private ListView<String> feedbackList;

    // Buttons
    @FXML private Button provideFeedbackButton;
    @FXML private Button closeButton;

    private Doctor currentDoctor;
    private Patient currentPatient;
    private UserManager vitalsDatabase;

    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    /**
     * Initialize the controller with doctor and patient data
     */
    public void initializeData(Doctor doctor, Patient patient, UserManager vitalsDb) {
        this.currentDoctor = doctor;
        this.currentPatient = patient;
        this.vitalsDatabase = vitalsDb;

        // Initialize UI components
        setupPatientInfo();
        setupVitalsTable();
        loadMedicalHistory();
        loadPreviousFeedback();

        LOGGER.log(Level.INFO, "DoctorPatientDetailsController initialized for patient: {0}", patient.getName());
    }

    /**
     * Set up the patient information section
     */
    private void setupPatientInfo() {
        patientNameLabel.setText(currentPatient.getName());
        patientIdLabel.setText("ID: " + currentPatient.getUserID());
        emailLabel.setText(currentPatient.getEmail());
        phoneLabel.setText(currentPatient.getPhone());
        addressTextArea.setText(currentPatient.getAddress());
    }

    /**
     * Set up the vitals table columns
     */
    private void setupVitalsTable() {
        // Configure table columns
        dateColumn.setCellValueFactory(cellData -> {
            VitalSign vital = cellData.getValue();
            return new SimpleStringProperty(dateTimeFormat.format(vital.getTimestamp()));
        });

        bpColumn.setCellValueFactory(cellData -> {
            VitalSign vital = cellData.getValue();
            return new SimpleStringProperty(formatBloodPressure(vital.getBloodPressure()));
        });

        heartRateColumn.setCellValueFactory(cellData -> {
            VitalSign vital = cellData.getValue();
            return new SimpleStringProperty(vital.getHeartRate() + " bpm");
        });

        tempColumn.setCellValueFactory(cellData -> {
            VitalSign vital = cellData.getValue();
            return new SimpleStringProperty(vital.getTemperature() + " °C");
        });

      

        // Load patient's vitals data
        loadVitalsData();
    }

    /**
     * Format blood pressure for display
     */
    private String formatBloodPressure(double bloodPressure) {
        // Assuming blood pressure is stored as a single value
        // In a real application, you might have systolic/diastolic separately
        return String.format("%.0f/%.0f mmHg",
                bloodPressure,
                bloodPressure * 0.65); // Simplified approximation for example
    }

    /**
     * Load vitals data from the vitals database
     */
    private void loadVitalsData() {
        if (vitalsDatabase != null) {
            List<VitalSign> allVitals = vitalsDatabase.getSortedVitals(false); // newest first
            vitalsTable.setItems(FXCollections.observableArrayList(allVitals));
        } else {
            LOGGER.log(Level.WARNING, "Vitals database not available for patient: {0}", currentPatient.getName());
            vitalsTable.setPlaceholder(new Label("No vitals data available"));
        }
    }

    /**
     * Load medical history for the patient
     * Note: In a real app, this would come from a database
     */
    private void loadMedicalHistory() {
        // Placeholder for medical history - in a real application, this would be retrieved from a database
        medicalHistoryList.setItems(FXCollections.observableArrayList(
                "2023-10-15: Annual checkup - Overall health good",
                "2023-06-22: Treated for mild seasonal allergies",
                "2022-12-05: Influenza vaccination administered",
                "2022-08-17: Physical therapy for lower back pain"
        ));
    }

    /**
     * Load previous feedback for the patient
     */
    private void loadPreviousFeedback() {
        try {
            // Retrieve all feedback for this patient
            List<Feedback> feedbackHistory = new ArrayList<>();
            
            // Get feedback provided by the current doctor
            List<Feedback> doctorFeedback = currentDoctor.getFeedbackForPatient(currentPatient);
            if (doctorFeedback != null) {
                feedbackHistory.addAll(doctorFeedback);
            }
            
            // Get feedback provided by all doctors to this patient
            List<Feedback> allFeedback = currentPatient.getAllFeedback();
            if (allFeedback != null) {
                // Add feedback from other doctors that isn't already in our list
                for (Feedback feedback : allFeedback) {
                    if (!feedbackHistory.contains(feedback) && feedback.getDoctor() != null) {
                        feedbackHistory.add(feedback);
                    }
                }
            }
            
            // Sort feedback by timestamp (most recent first)
            feedbackHistory.sort(Comparator.comparing(Feedback::getTimestamp).reversed());
            
            if (feedbackHistory.isEmpty()) {
                feedbackList.setItems(FXCollections.observableArrayList("No previous feedback available"));
                return;
            }
            
            // Convert feedback objects to formatted strings for display
            List<String> displayItems = new ArrayList<>();
            for (Feedback feedback : feedbackHistory) {
                StringBuilder feedbackText = new StringBuilder();
                String doctorName = feedback.getDoctor() != null ? feedback.getDoctor().getName() : "Unknown Doctor";
                
                feedbackText.append(dateTimeFormat.format(feedback.getTimestamp()))
                          .append(" - Dr. ").append(doctorName).append("\n")
                          .append(feedback.getMessage());
                
                // Add prescription info if available
                if (feedback.hasPrescription()) {
                    feedbackText.append("\n\nPrescription: ")
                              .append(feedback.getPrescription().getMedicationName())
                              .append(" - ").append(feedback.getPrescription().getDosage())
                              .append(" - ").append(feedback.getPrescription().getSchedule());
                }
                
                displayItems.add(feedbackText.toString());
            }
            
            feedbackList.setItems(FXCollections.observableArrayList(displayItems));
            
            // Set up custom cell factory for better display
            feedbackList.setCellFactory(listView -> new ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (empty || item == null) {
                        setText(null);
                        setStyle("-fx-background-color: transparent;");
                    } else {
                        setText(item);
                        setWrapText(true);
                        setPrefWidth(0); // Enable text wrapping
                        
                        // Add some styling to make feedback easier to read
                        if (!item.equals("No previous feedback available")) {
                            setStyle("-fx-background-color: #f8f9fa; -fx-padding: 5;");
                        }
                    }
                }
            });
            
            LOGGER.log(Level.INFO, "Loaded {0} feedback items for patient {1}", 
                     new Object[]{feedbackHistory.size(), currentPatient.getName()});
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading feedback for patient " + currentPatient.getName(), e);
            feedbackList.setItems(FXCollections.observableArrayList(
                    "Error loading feedback: " + e.getMessage()));
        }
    }

    /**
     * Handle providing feedback to the patient
     */
    @FXML
    private void handleProvideFeedback() {
        try {
            // Load the feedback view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/rhms/ui/views/DoctorFeedbackView.fxml"));
            javafx.scene.Parent feedbackView = loader.load();

            // Get the controller and initialize it with the current doctor and patient
            DoctorFeedbackController controller = loader.getController();
            controller.initializeData(currentDoctor, currentPatient);

            // Create and show the feedback dialog
            Stage stage = new Stage();
            stage.setTitle("Provide Feedback - " + currentPatient.getName());
            stage.setScene(new Scene(feedbackView));
            stage.initModality(Modality.APPLICATION_MODAL);

            // Apply CSS if available
            Scene scene = stage.getScene();
            scene.getStylesheets().add(getClass().getResource("/com/rhms/ui/resources/styles.css").toExternalForm());

            stage.showAndWait();

            // After dialog closes, refresh the feedback list in case new feedback was added
            loadPreviousFeedback();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading feedback form", e);
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Failed to open feedback form: " + e.getMessage());
        }
    }

    /**
     * Handle closing the window
     */
    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Show an alert dialog
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
