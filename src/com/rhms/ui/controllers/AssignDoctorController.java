package com.rhms.ui.controllers;

import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;
import com.rhms.userManagement.UserManager;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AssignDoctorController {
    private static final Logger LOGGER = Logger.getLogger(AssignDoctorController.class.getName());

    @FXML private TableView<Doctor> doctorTable;
    @FXML private TableColumn<Doctor, Integer> doctorIdColumn;
    @FXML private TableColumn<Doctor, String> doctorNameColumn;
    @FXML private TableColumn<Doctor, String> doctorSpecialtyColumn;

    @FXML private TableView<Patient> patientTable;
    @FXML private TableColumn<Patient, Integer> patientIdColumn;
    @FXML private TableColumn<Patient, String> patientNameColumn;
    @FXML private TableColumn<Patient, String> patientAssignedColumn;

    @FXML private TableView<DoctorPatientAssignment> assignmentTable;
    @FXML private TableColumn<DoctorPatientAssignment, String> assignmentDoctorColumn;
    @FXML private TableColumn<DoctorPatientAssignment, String> assignmentPatientColumn;

    // Fields for doctor requests
    @FXML private TableView<DoctorRequest> requestsTable;
    @FXML private TableColumn<DoctorRequest, String> requestPatientColumn;
    @FXML private TableColumn<DoctorRequest, String> requestTypeColumn;
    @FXML private TableColumn<DoctorRequest, String> requestSpecializationColumn;
    @FXML private TableColumn<DoctorRequest, String> requestDateColumn;
    @FXML private TableColumn<DoctorRequest, String> requestStatusColumn;
    @FXML private Button refreshRequestsButton;

    @FXML private TextArea messageArea;
    @FXML private Button assignButton;
    @FXML private Button removeButton;
    @FXML private Button closeButton;

    private UserManager userManager;
    private ObservableList<Doctor> doctors;
    private ObservableList<Patient> patients;
    private ObservableList<DoctorPatientAssignment> assignments;
    private ObservableList<DoctorRequest> doctorRequests = FXCollections.observableArrayList();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    /**
     * Initialize the controller with user manager
     */
    public void initialize(UserManager userManager) {
        this.userManager = userManager;

        // Force reload of all assignments from database
        userManager.loadAllAssignmentsFromDatabase();
        LOGGER.log(Level.INFO, "Forced reload of all assignments in AssignDoctorController initialization");

        // Initialize table columns for doctors
        doctorIdColumn.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getUserID()).asObject());
        doctorNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getName()));
        doctorSpecialtyColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getSpecialization()));

        // Initialize table columns for patients
        patientIdColumn.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getUserID()).asObject());
        patientNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getName()));
        patientAssignedColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().hasAssignedDoctor() ? "Yes" : "No"));

        // Initialize table columns for assignments
        assignmentDoctorColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDoctor().getName()));
        assignmentPatientColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPatient().getName()));

        // Initialize table columns for doctor requests - updated for new schema
        requestPatientColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPatientName()));
        requestTypeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getRequestType()));
        requestSpecializationColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDoctorSpecialization()));
        requestDateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatDate(cellData.getValue().getRequestDate())));
        requestStatusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatus()));

        // Load data
        loadDoctorsAndPatients();
        loadDoctorRequests();

        // Add selection listeners
        doctorTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> updateButtonState());
        patientTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> updateButtonState());
        assignmentTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> updateButtonState());

        // Initial button state
        updateButtonState();

        // Set initial message
        messageArea.setText("Select a doctor and patient to create an assignment.");
    }

    /**
     * Format date for display
     */
    private String formatDate(Date date) {
        if (date == null) return "N/A";
        return dateFormat.format(date);
    }

    /**
     * Load all doctors and patients from user manager
     */
    private void loadDoctorsAndPatients() {
        // Load doctors
        List<Doctor> allDoctors = userManager.getAllDoctors();
        doctors = FXCollections.observableArrayList(allDoctors);
        doctorTable.setItems(doctors);
        LOGGER.log(Level.INFO, "Loaded {0} doctors", allDoctors.size());

        // Load patients
        List<Patient> allPatients = userManager.getAllPatients();
        patients = FXCollections.observableArrayList(allPatients);
        patientTable.setItems(patients);
        LOGGER.log(Level.INFO, "Loaded {0} patients", allPatients.size());

        // Load existing assignments
        loadExistingAssignments();
    }

    /**
     * Load existing doctor-patient assignments
     */
    private void loadExistingAssignments() {
        assignments = FXCollections.observableArrayList();
        int assignmentCount = 0;

        // Collect all doctor-patient relationships
        for (Doctor doctor : doctors) {
            List<Patient> assignedPatients = doctor.getAssignedPatients();
            LOGGER.log(Level.INFO, "Doctor {0} (ID: {1}) has {2} assigned patients", 
                       new Object[]{doctor.getName(), doctor.getUserID(), assignedPatients.size()});
            
            for (Patient patient : assignedPatients) {
                assignments.add(new DoctorPatientAssignment(doctor, patient));
                assignmentCount++;
                LOGGER.log(Level.FINE, "Added assignment: Doctor {0} - Patient {1}", 
                          new Object[]{doctor.getName(), patient.getName()});
            }
        }

        assignmentTable.setItems(assignments);
        LOGGER.log(Level.INFO, "Loaded {0} assignments into the assignment table", assignmentCount);
        
        // Debug output to messageArea
        messageArea.appendText("\nLoaded " + assignmentCount + " existing assignments.");
        if (assignmentCount == 0) {
            messageArea.appendText("\nNo assignments found. If this is unexpected, there may be a database issue.");
        }
    }

    /**
     * Load doctor requests from database - updated for new schema
     */
    private void loadDoctorRequests() {
        doctorRequests.clear();
        
        try {
            Connection conn = com.rhms.Database.DatabaseConnection.getConnection();
            String query = "SELECT r.request_id, r.user_id, u.name AS patient_name, r.request_type, " +
                           "r.doctor_specialization, r.additional_details, r.request_date, r.status " +
                           "FROM Doctor_Requests r " +
                           "JOIN Users u ON r.user_id = u.user_id " +
                           "ORDER BY r.request_date DESC";
            
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    int requestId = rs.getInt("request_id");
                    int userId = rs.getInt("user_id");
                    String patientName = rs.getString("patient_name");
                    String requestType = rs.getString("request_type");
                    String specialization = rs.getString("doctor_specialization");
                    String additionalDetails = rs.getString("additional_details");
                    Date requestDate = rs.getTimestamp("request_date");
                    String status = rs.getString("status");
                    
                    DoctorRequest request = new DoctorRequest(
                            requestId, userId, patientName, requestType, specialization,
                            additionalDetails, requestDate, status);
                    doctorRequests.add(request);
                }
            }
            
            requestsTable.setItems(doctorRequests);
            messageArea.appendText("\nLoaded " + doctorRequests.size() + " doctor requests.");
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading doctor requests", e);
            messageArea.appendText("\nError loading doctor requests: " + e.getMessage());
        }
    }

    /**
     * Update button state based on selections
     */
    private void updateButtonState() {
        Doctor selectedDoctor = doctorTable.getSelectionModel().getSelectedItem();
        Patient selectedPatient = patientTable.getSelectionModel().getSelectedItem();
        DoctorPatientAssignment selectedAssignment = assignmentTable.getSelectionModel().getSelectedItem();

        // Assign button enabled if both doctor and patient are selected
        assignButton.setDisable(selectedDoctor == null || selectedPatient == null);

        // Remove button enabled if an assignment is selected
        removeButton.setDisable(selectedAssignment == null);
    }

    /**
     * Handle refreshing the doctor requests table
     */
    @FXML
    private void handleRefreshRequests() {
        loadDoctorRequests();
        messageArea.setText("Doctor requests refreshed.");
    }

    /**
     * Handle the assign button click
     */
    @FXML
    private void handleAssign() {
        Doctor selectedDoctor = doctorTable.getSelectionModel().getSelectedItem();
        Patient selectedPatient = patientTable.getSelectionModel().getSelectedItem();

        if (selectedDoctor == null || selectedPatient == null) {
            messageArea.setText("Please select both a doctor and a patient.");
            return;
        }

        // Check if assignment already exists
        boolean alreadyAssigned = false;
        for (DoctorPatientAssignment assignment : assignments) {
            if (assignment.getDoctor().getUserID() == selectedDoctor.getUserID() &&
                    assignment.getPatient().getUserID() == selectedPatient.getUserID()) {
                alreadyAssigned = true;
                break;
            }
        }

        if (alreadyAssigned) {
            messageArea.setText("Dr. " + selectedDoctor.getName() + " is already assigned to " +
                    selectedPatient.getName() + ".");
            return;
        }

        // Assign doctor to patient in the database and update in-memory assignments
        boolean success = false;
        try {
            // Assign in DB (ensure UserManager uses DoctorPatientAssignmentHandler)
            success = userManager.assignDoctorToPatient(selectedDoctor, selectedPatient);
        } catch (Exception e) {
            messageArea.setText("Database error during assignment: " + e.getMessage());
            return;
        }

        if (success) {
            // Refresh data
            refreshData();
            messageArea.setText("Successfully assigned Dr. " + selectedDoctor.getName() +
                    " to patient " + selectedPatient.getName() + ".");
        } else {
            messageArea.setText("Failed to assign Dr. " + selectedDoctor.getName() +
                    " to patient " + selectedPatient.getName() + ". See log for details.");
        }
    }

    /**
     * Handle the remove button click
     */
    @FXML
    private void handleRemove() {
        DoctorPatientAssignment selectedAssignment = assignmentTable.getSelectionModel().getSelectedItem();

        if (selectedAssignment == null) {
            messageArea.setText("Please select an assignment to remove.");
            return;
        }

        Doctor doctor = selectedAssignment.getDoctor();
        Patient patient = selectedAssignment.getPatient();

        boolean success = false;
        try {
            // Remove assignment from DB (ensure UserManager uses DoctorPatientAssignmentHandler)
            success = userManager.removeDoctorFromPatient(doctor, patient);
        } catch (Exception e) {
            messageArea.setText("Database error during removal: " + e.getMessage());
            return;
        }

        if (success) {
            // Refresh data
            refreshData();
            messageArea.setText("Successfully removed assignment: Dr. " + doctor.getName() +
                    " from patient " + patient.getName() + ".");
        } else {
            messageArea.setText("Failed to remove assignment: Dr. " + doctor.getName() +
                    " from patient " + patient.getName() + ". See log for details.");
        }
    }

    /**
     * Refresh all data in tables
     */
    private void refreshData() {
        // Force reload from database
        userManager.loadAllAssignmentsFromDatabase();
        
        // Refresh the patients table to update "Has Doctor" column
        patientTable.refresh();

        // Reload assignments and requests
        loadExistingAssignments();
        loadDoctorRequests();
    }

    /**
     * Handle the close button click
     */
    @FXML
    private void handleClose() {
        // Close the stage
        if (closeButton != null && closeButton.getScene() != null) {
            Stage stage = (Stage) closeButton.getScene().getWindow();
            stage.close();
        } else {
            LOGGER.log(Level.WARNING, "Could not close window: closeButton or its scene is null");
        }
    }

    /**
     * Inner class to represent a doctor-patient assignment
     */
    public static class DoctorPatientAssignment {
        private Doctor doctor;
        private Patient patient;

        public DoctorPatientAssignment(Doctor doctor, Patient patient) {
            this.doctor = doctor;
            this.patient = patient;
        }

        public Doctor getDoctor() {
            return doctor;
        }

        public Patient getPatient() {
            return patient;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            DoctorPatientAssignment that = (DoctorPatientAssignment) obj;
            return doctor.getUserID() == that.doctor.getUserID() && 
                   patient.getUserID() == that.patient.getUserID();
        }
        
        @Override
        public int hashCode() {
            return 31 * doctor.getUserID() + patient.getUserID();
        }
    }
    
    /**
     * Inner class to represent a doctor request - updated for new schema
     */
    public static class DoctorRequest {
        private final int requestId;
        private final int userId;
        private final String patientName;
        private final String requestType;
        private final String doctorSpecialization;
        private final String additionalDetails;
        private final Date requestDate;
        private final String status;
        
        public DoctorRequest(int requestId, int userId, String patientName, 
                            String requestType, String doctorSpecialization, 
                            String additionalDetails, Date requestDate, String status) {
            this.requestId = requestId;
            this.userId = userId;
            this.patientName = patientName;
            this.requestType = requestType;
            this.doctorSpecialization = doctorSpecialization;
            this.additionalDetails = additionalDetails;
            this.requestDate = requestDate;
            this.status = status;
        }
        
        public int getRequestId() { return requestId; }
        public int getUserId() { return userId; }
        public String getPatientName() { return patientName; }
        public String getRequestType() { return requestType; }
        public String getDoctorSpecialization() { return doctorSpecialization; }
        public String getAdditionalDetails() { return additionalDetails; }
        public Date getRequestDate() { return requestDate; }
        public String getStatus() { return status; }
    }
}

