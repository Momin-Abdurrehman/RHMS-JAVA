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

import java.util.ArrayList;
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

    @FXML private TextArea messageArea;
    @FXML private Button assignButton;
    @FXML private Button removeButton;
    @FXML private Button closeButton;

    private UserManager userManager;
    private ObservableList<Doctor> doctors;
    private ObservableList<Patient> patients;
    private ObservableList<DoctorPatientAssignment> assignments;

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

        // Load data
        loadDoctorsAndPatients();

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

        // Create the assignment through UserManager to ensure database is updated
        boolean success = userManager.assignDoctorToPatient(selectedDoctor, selectedPatient);
        
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

        // Remove the assignment through UserManager to ensure database is updated
        Doctor doctor = selectedAssignment.getDoctor();
        Patient patient = selectedAssignment.getPatient();
        
        boolean success = userManager.removeDoctorFromPatient(doctor, patient);
        
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

        // Reload assignments
        loadExistingAssignments();
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
}
