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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class AssignDoctorController {

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

    private UserManager userManager;
    private ObservableList<Doctor> doctors;
    private ObservableList<Patient> patients;
    private ObservableList<DoctorPatientAssignment> assignments;

    /**
     * Initialize the controller with user manager
     */
    public void initialize(UserManager userManager) {
        this.userManager = userManager;
        
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
        
        // Load patients
        List<Patient> allPatients = userManager.getAllPatients();
        patients = FXCollections.observableArrayList(allPatients);
        patientTable.setItems(patients);
        
        // Load existing assignments
        loadExistingAssignments();
    }
    
    /**
     * Load existing doctor-patient assignments
     */
    private void loadExistingAssignments() {
        assignments = FXCollections.observableArrayList();
        
        // Collect all doctor-patient relationships
        for (Doctor doctor : doctors) {
            for (Patient patient : doctor.getAssignedPatients()) {
                assignments.add(new DoctorPatientAssignment(doctor, patient));
            }
        }
        
        assignmentTable.setItems(assignments);
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
            if (assignment.getDoctor().equals(selectedDoctor) && 
                assignment.getPatient().equals(selectedPatient)) {
                alreadyAssigned = true;
                break;
            }
        }
        
        if (alreadyAssigned) {
            messageArea.setText("Dr. " + selectedDoctor.getName() + " is already assigned to " + 
                selectedPatient.getName() + ".");
            return;
        }
        
        // Create the assignment
        selectedPatient.addAssignedDoctor(selectedDoctor);
        
        // Refresh data
        refreshData();
        
        messageArea.setText("Successfully assigned Dr. " + selectedDoctor.getName() + 
            " to patient " + selectedPatient.getName() + ".");
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
        
        // Remove the assignment
        Doctor doctor = selectedAssignment.getDoctor();
        Patient patient = selectedAssignment.getPatient();
        patient.removeAssignedDoctor(doctor);
        
        // Refresh data
        refreshData();
        
        messageArea.setText("Successfully removed assignment: Dr. " + doctor.getName() + 
            " from patient " + patient.getName() + ".");
    }
    
    /**
     * Refresh all data in tables
     */
    private void refreshData() {
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
        Stage stage = (Stage) assignButton.getScene().getWindow();
        stage.close();
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
    }
}
