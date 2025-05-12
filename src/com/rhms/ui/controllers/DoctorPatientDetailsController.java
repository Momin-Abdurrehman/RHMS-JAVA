package com.rhms.ui.controllers;

import com.rhms.Database.VitalSignDatabaseHandler;
import com.rhms.doctorPatientInteraction.Feedback;
import com.rhms.doctorPatientInteraction.Prescription;
import com.rhms.healthDataHandling.VitalSign;
import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;
import com.rhms.userManagement.UserManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
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

    // Previous feedback tab
    @FXML private ListView<String> feedbackList;

    // Buttons
    @FXML private Button provideFeedbackButton;
    @FXML private Button closeButton;

    private Doctor currentDoctor;
    private Patient currentPatient;
    private UserManager userManager;

    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    /**
     * Initialize the controller with doctor and patient data
     */
    public void initializeData(Doctor doctor, Patient patient, UserManager userManager) {
        this.currentDoctor = doctor;
        this.currentPatient = patient;
        this.userManager = userManager;

        // Initialize UI components
        setupPatientInfo();
        setupVitalsTable();
        loadVitalsData();
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
     * Load vitals data directly from the database
     */
    private void loadVitalsData() {
        List<VitalSign> vitals = new ArrayList<>();
        
        try {
            // Create a new VitalSignDatabaseHandler to access the database
            VitalSignDatabaseHandler vitalHandler = new VitalSignDatabaseHandler();
            
            // Fetch vitals for this patient directly from the database
            vitals = vitalHandler.getVitalSignsForPatient(currentPatient.getUserID());
            
            LOGGER.log(Level.INFO, "Loaded {0} vital records from database for patient ID {1}", 
                      new Object[]{vitals.size(), currentPatient.getUserID()});
            
            // Sort by date (newest first)
            vitals.sort(Comparator.comparing(VitalSign::getTimestamp).reversed());
            
            // Update the UI
            vitalsTable.setItems(FXCollections.observableArrayList(vitals));
            
            // Also update the Patient object's vital history
            currentPatient.getVitalsDatabase().addVitalRecords(vitals);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading vital signs: {0}", e.getMessage());
            vitalsTable.setPlaceholder(new Label("Error loading vital signs data"));
        }
        
        // Set a placeholder message if no vitals are available
        if (vitals == null || vitals.isEmpty()) {
            vitalsTable.setPlaceholder(new Label("No vital signs data available for this patient"));
        }
    }

    /**
     * Load previous feedback and prescriptions directly from the database
     */
    private void loadPreviousFeedback() {
        List<String> displayItems = new ArrayList<>();
        
        try {
            if (userManager != null && userManager.dbHandler != null && userManager.dbHandler.connection != null) {
                Connection conn = userManager.dbHandler.connection;
                
                // SQL to fetch feedback with prescriptions and doctor information
                String sql = "SELECT f.feedback_id, f.doctor_id, f.comments, f.timestamp, " +
                             "u.name as doctor_name, d.specialization, " +
                             "p.prescription_id, p.medication_name, p.dosage, p.schedule, p.duration, p.instructions " +
                             "FROM feedback_by_doctor f " +
                             "JOIN Doctors d ON f.doctor_id = d.doctor_id " +
                             "JOIN Users u ON d.user_id = u.user_id " +
                             "LEFT JOIN prescription p ON f.feedback_id = p.feedback_id " +
                             "WHERE f.patient_id = ? " +
                             "ORDER BY f.timestamp DESC";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, currentPatient.getUserID());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.isBeforeFirst()) { // No records found
                            displayItems.add("No previous feedback available");
                        } else {
                            while (rs.next()) {
                                String doctorName = rs.getString("doctor_name");
                                if (doctorName == null) doctorName = "Unknown Doctor";
                                
                                String specialization = rs.getString("specialization");
                                if (specialization != null && !specialization.isEmpty()) {
                                    doctorName += " (" + specialization + ")";
                                }
                                
                                Date feedbackDate = rs.getTimestamp("timestamp");
                                String comments = rs.getString("comments");
                                
                                StringBuilder feedbackText = new StringBuilder();
                                feedbackText.append(dateTimeFormat.format(feedbackDate))
                                        .append(" - Dr. ").append(doctorName).append("\n")
                                        .append(comments);
                                
                                // Add prescription info if available
                                String medicationName = rs.getString("medication_name");
                                if (medicationName != null && !medicationName.isEmpty()) {
                                    feedbackText.append("\n\n💊 Prescription: ")
                                            .append(medicationName);
                                    
                                    String dosage = rs.getString("dosage");
                                    if (dosage != null && !dosage.isEmpty()) {
                                        feedbackText.append("\n• Dosage: ").append(dosage);
                                    }
                                    
                                    String schedule = rs.getString("schedule"); 
                                    if (schedule != null && !schedule.isEmpty()) {
                                        feedbackText.append("\n• Schedule: ").append(schedule);
                                    }
                                    
                                    String duration = rs.getString("duration");
                                    if (duration != null && !duration.isEmpty()) {
                                        feedbackText.append("\n• Duration: ").append(duration);
                                    }
                                    
                                    String instructions = rs.getString("instructions");
                                    if (instructions != null && !instructions.isEmpty()) {
                                        feedbackText.append("\n• Instructions: ").append(instructions);
                                    }
                                }
                                
                                displayItems.add(feedbackText.toString());
                            }
                        }
                    }
                }
                
                LOGGER.log(Level.INFO, "Loaded {0} feedback items from database for patient ID {1}",
                        new Object[]{displayItems.size(), currentPatient.getUserID()});
                
            } else {
                // Fallback to using in-memory feedback data
                LOGGER.log(Level.WARNING, "Database connection not available, falling back to in-memory feedback data");
                
                // Get feedback provided by all doctors to this patient
                List<Feedback> allFeedback = currentPatient.getAllFeedback();
                
                if (allFeedback == null || allFeedback.isEmpty()) {
                    displayItems.add("No previous feedback available");
                } else {
                    // Sort feedback by timestamp (most recent first)
                    allFeedback.sort(Comparator.comparing(Feedback::getTimestamp).reversed());
                    
                    for (Feedback feedback : allFeedback) {
                        StringBuilder feedbackText = new StringBuilder();
                        String doctorName = feedback.getDoctor() != null ? 
                                feedback.getDoctor().getName() : "Unknown Doctor";
                        
                        feedbackText.append(dateTimeFormat.format(feedback.getTimestamp()))
                                .append(" - Dr. ").append(doctorName).append("\n")
                                .append(feedback.getMessage());
                        
                        // Add prescription info if available
                        if (feedback.hasPrescription()) {
                            Prescription prescription = feedback.getPrescription();
                            feedbackText.append("\n\n💊 Prescription: ")
                                    .append(prescription.getMedicationName());
                                    
                            if (prescription.getDosage() != null && !prescription.getDosage().isEmpty()) {
                                feedbackText.append("\n• Dosage: ").append(prescription.getDosage());
                            }
                            
                            if (prescription.getSchedule() != null && !prescription.getSchedule().isEmpty()) {
                                feedbackText.append("\n• Schedule: ").append(prescription.getSchedule());
                            }
                            
                            if (prescription.getDuration() != null && !prescription.getDuration().isEmpty()) {
                                feedbackText.append("\n• Duration: ").append(prescription.getDuration());
                            }
                            
                            if (prescription.getInstructions() != null && !prescription.getInstructions().isEmpty()) {
                                feedbackText.append("\n• Instructions: ").append(prescription.getInstructions());
                            }
                        }
                        
                        displayItems.add(feedbackText.toString());
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while loading feedback: {0}", e.getMessage());
            displayItems.add("Error loading feedback: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading feedback: {0}", e.getMessage());
            displayItems.add("Error loading feedback: " + e.getMessage());
        }

        feedbackList.setItems(FXCollections.observableArrayList(displayItems));

        // Setup custom cell factory for better display
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
                    if (!item.equals("No previous feedback available") && 
                        !item.startsWith("Error loading")) {
                        setStyle("-fx-background-color: #f8f9fa; -fx-padding: 5;");
                    } else if (item.startsWith("Error loading")) {
                        setStyle("-fx-background-color: #ffebee; -fx-padding: 5;"); // Light red for errors
                    }
                }
            }
        });
    }

    /**
     * Handle providing feedback to the patient
     */
    @FXML
    private void handleProvideFeedback() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/rhms/ui/views/DoctorFeedbackView.fxml"));
            javafx.scene.Parent feedbackView = loader.load();

            DoctorFeedbackController controller = loader.getController();
            controller.initializeData(currentDoctor, currentPatient);

            // Pass UserManager to feedback window for DB access
            Stage stage = new Stage();
            stage.setTitle("Provide Feedback - " + currentPatient.getName());
            stage.setScene(new Scene(feedbackView));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setUserData(userManager); // Pass UserManager

            // Apply CSS if available
            Scene scene = stage.getScene();
            scene.getStylesheets().add(getClass().getResource("/com/rhms/ui/resources/styles.css").toExternalForm());

            // Show dialog and wait for feedback submission
            stage.showAndWait();

            // After dialog closes, refresh the feedback list to show the new entry
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
