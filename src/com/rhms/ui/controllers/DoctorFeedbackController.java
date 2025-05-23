package com.rhms.ui.controllers;

import com.rhms.doctorPatientInteraction.Feedback;
import com.rhms.doctorPatientInteraction.Prescription;
import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;

import com.rhms.userManagement.UserManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the doctor feedback view where doctors can provide feedback and prescriptions to patients
 */
public class DoctorFeedbackController {
    private static final Logger LOGGER = Logger.getLogger(DoctorFeedbackController.class.getName());

    @FXML private Label patientNameLabel;
    @FXML private TextArea feedbackTextArea;
    @FXML private CheckBox includePrescriptionCheckBox;
    @FXML private VBox prescriptionContainer;

    @FXML private TextField medicationNameField;
    @FXML private TextField dosageField;
    @FXML private ComboBox<String> frequencyComboBox;
    @FXML private TextField durationField;
    @FXML private TextArea instructionsTextArea;

    @FXML private TableView<PrescriptionItem> prescriptionsTable;
    @FXML private TableColumn<PrescriptionItem, String> medicationColumn;
    @FXML private TableColumn<PrescriptionItem, String> dosageColumn;
    @FXML private TableColumn<PrescriptionItem, String> frequencyColumn;
    @FXML private TableColumn<PrescriptionItem, String> durationColumn;

    @FXML private Button addPrescriptionButton;
    @FXML private Button removePrescriptionButton;
    @FXML private Button submitButton;
    @FXML private Button cancelButton;

    private Doctor currentDoctor;
    private Patient currentPatient;
    private List<PrescriptionItem> prescriptionItems = new ArrayList<>();

    private Feedback submittedFeedback;

    public Feedback getSubmittedFeedback() {
        return submittedFeedback;
    }

    /**
     * Initialize the controller with doctor and patient data
     */
    public void initializeData(Doctor doctor, Patient patient) {
        this.currentDoctor = doctor;
        this.currentPatient = patient;

        // Update UI with patient information
        patientNameLabel.setText(patient.getName());

        // Set up the prescription section initially hidden
        prescriptionContainer.setVisible(false);
        prescriptionContainer.setManaged(false);

        // Set up frequency combo box options
        frequencyComboBox.getItems().addAll("Once daily", "Twice daily", "Three times daily",
                "Every 4 hours", "Every 6 hours", "Every 8 hours",
                "As needed", "Before meals", "After meals");

        // Set up table columns
        setupTableColumns();

        // Listener for the prescription checkbox
        includePrescriptionCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            prescriptionContainer.setVisible(newVal);
            prescriptionContainer.setManaged(newVal);
        });

        LOGGER.log(Level.INFO, "DoctorFeedbackController initialized for patient: {0}", patient.getName());
    }

    /**
     * Set up the table columns for the prescriptions table
     */
    private void setupTableColumns() {
        medicationColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMedicationName()));

        dosageColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDosage()));

        frequencyColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFrequency()));

        durationColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDuration()));

        prescriptionsTable.setItems(javafx.collections.FXCollections.observableArrayList(prescriptionItems));
    }

    /**
     * Handle adding a prescription to the list
     */
    @FXML
    void handleAddPrescription(ActionEvent event) {
        String medication = medicationNameField.getText().trim();
        String dosage = dosageField.getText().trim();
        String frequency = frequencyComboBox.getValue();
        String duration = durationField.getText().trim();
        String instructions = instructionsTextArea.getText().trim();

        if (medication.isEmpty() || dosage.isEmpty() || frequency == null) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input",
                    "Please provide medication name, dosage, and frequency.");
            return;
        }

        PrescriptionItem item = new PrescriptionItem(medication, dosage, frequency, duration, instructions);
        prescriptionItems.add(item);
        prescriptionsTable.setItems(javafx.collections.FXCollections.observableArrayList(prescriptionItems));

        // Clear fields for next entry
        medicationNameField.clear();
        dosageField.clear();
        frequencyComboBox.getSelectionModel().clearSelection();
        durationField.clear();
        instructionsTextArea.clear();
    }

    /**
     * Handle removing a prescription from the list
     */
    @FXML
    void handleRemovePrescription(ActionEvent event) {
        PrescriptionItem selectedItem = prescriptionsTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            prescriptionItems.remove(selectedItem);
            prescriptionsTable.setItems(javafx.collections.FXCollections.observableArrayList(prescriptionItems));
        } else {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a prescription to remove.");
        }
    }

    /**
     * Handle submitting the feedback and prescriptions
     */
    @FXML
    void handleSubmit(ActionEvent event) {
        String feedbackText = feedbackTextArea.getText().trim();

        if (feedbackText.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Missing Feedback", "Please provide feedback text.");
            return;
        }

        try {
            // Prescription is compulsory
            if (!includePrescriptionCheckBox.isSelected() || prescriptionItems.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Prescription Required", "At least one prescription is required.");
                return;
            }

            // Only one prescription allowed per feedback (per your DB schema)
            PrescriptionItem item = prescriptionItems.get(0);
            Prescription prescription = new Prescription(
                    item.getMedicationName(),
                    item.getDosage(),
                    item.getFrequency(),
                    item.getDuration(),
                    item.getInstructions()
            );

            submittedFeedback = new Feedback(currentDoctor, currentPatient, feedbackText, prescription);

            // --- Store feedback and prescription in the database immediately ---
            // Find UserManager from parent window's user data if possible
            UserManager userManager = null;
            Stage stage = (Stage) submitButton.getScene().getWindow();

            if (stage.getUserData() instanceof UserManager) {
                userManager = (UserManager) stage.getUserData();
            }

            boolean dbSuccess = false;
            String dbErrorMsg = null;
            if (userManager != null && userManager.dbHandler != null) {
                dbSuccess = userManager.dbHandler.addDoctorFeedback(submittedFeedback);
                // If failed, try to get error from dbHandler (if implemented)
                if (!dbSuccess) {
                    dbErrorMsg = "Failed to save feedback and prescription to database. Please check database connection and required fields.";
                }
            } else {
                dbErrorMsg = "UserManager or database handler is not available. Please contact admin.";
                submittedFeedback = null;
            }

            if (dbSuccess) {
                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "Feedback and prescription successfully provided to " + currentPatient.getName());
                closeWindow();
            } else {
                showAlert(Alert.AlertType.ERROR, "Database Error",
                        dbErrorMsg != null ? dbErrorMsg : "Unknown database error.");
                submittedFeedback = null;
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error submitting feedback", e);
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Failed to submit feedback: " + e.getMessage());
            submittedFeedback = null;
        }
    }

    /**
     * Handle cancelling the feedback
     */
    @FXML
    void handleCancel(ActionEvent event) {
        // Confirm with user before discarding
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Cancellation");
        alert.setHeaderText("Cancel Feedback");
        alert.setContentText("Are you sure you want to cancel? Any entered data will be lost.");

        if (alert.showAndWait().get() == ButtonType.OK) {
            closeWindow();
        }
    }

    /**
     * Close the feedback window
     */
    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
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



    /**
     * Inner class to represent a prescription item in the table
     */
    public static class PrescriptionItem {
        private String medicationName;
        private String dosage;
        private String frequency;
        private String duration;
        private String instructions;

        public PrescriptionItem(String medicationName, String dosage, String frequency,
                                String duration, String instructions) {
            this.medicationName = medicationName;
            this.dosage = dosage;
            this.frequency = frequency;
            this.duration = duration;
            this.instructions = instructions;
        }

        public String getMedicationName() { return medicationName; }
        public String getDosage() { return dosage; }
        public String getFrequency() { return frequency; }
        public String getDuration() { return duration; }
        public String getInstructions() { return instructions; }
    }
}

