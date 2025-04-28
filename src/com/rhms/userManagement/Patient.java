package com.rhms.userManagement;

import java.util.ArrayList;
import com.rhms.emergencyAlert.PanicButton;
import com.rhms.doctorPatientInteraction.MedicalHistory;
import com.rhms.appointmentScheduling.Appointment;
import com.rhms.healthDataHandling.VitalsDatabase;
import com.rhms.healthDataHandling.VitalSign;
import com.rhms.healthDataHandling.CSVVitalsUploader;
import com.rhms.healthDataHandling.VitalsUploadReport;
import java.io.IOException;

public class Patient extends User {
    private ArrayList<String> medicalRecords;
    private ArrayList<String> doctorFeedback;
    private ArrayList<Appointment> appointments;
    private PanicButton panicButton;
    private MedicalHistory medicalHistory;
    private String emergencyContact;
    private String healthInsuranceInfo;
    private VitalsDatabase vitalsDatabase;

    public Patient(String name, String email, String password, String phone, String address, int userID) {
        super(name, email, password, phone, address, userID);
        this.medicalRecords = new ArrayList<>();
        this.doctorFeedback = new ArrayList<>();
        this.appointments = new ArrayList<>();
        this.panicButton = new PanicButton(this);
        this.medicalHistory = new MedicalHistory();
        this.vitalsDatabase = new VitalsDatabase(this);
    }
    
    // Enhanced constructor with authentication fields
    public Patient(String name, String email, String password, String phone, String address, 
                  int userID, String username, String passwordHash) {
        super(name, email, password, phone, address, userID, username, passwordHash);
        this.medicalRecords = new ArrayList<>();
        this.doctorFeedback = new ArrayList<>();
        this.appointments = new ArrayList<>();
        this.panicButton = new PanicButton(this);
        this.medicalHistory = new MedicalHistory();
        this.vitalsDatabase = new VitalsDatabase(this);
    }
    
    // Constructor with emergency contact and insurance info
    public Patient(String name, String email, String password, String phone, String address, 
                  int userID, String username, String passwordHash,
                  String emergencyContact, String healthInsuranceInfo) {
        this(name, email, password, phone, address, userID, username, passwordHash);
        this.emergencyContact = emergencyContact;
        this.healthInsuranceInfo = healthInsuranceInfo;
    }

    public void uploadMedicalRecord(String record) {        
        medicalRecords.add(record);
    }

    public ArrayList<String> getDoctorFeedback() {
        return doctorFeedback;
    }

    public void viewDoctorFeedback() {
        if (doctorFeedback.isEmpty()) {
            System.out.println("No feedback available.");
            return;
        }
        for (String feedback : doctorFeedback) {
            System.out.println(feedback);
        }
    }

    /**
     * Schedules an appointment for the patient
     * @param appointment The appointment object to add to the patient's schedule
     */
    public void scheduleAppointment(Appointment appointment) {
        appointments.add(appointment);
    }
    
    /**
     * Schedules an appointment for the patient (backward compatibility method)
     * @param appointmentDescription A text description of the appointment
     */
    public void scheduleAppointment(String appointmentDescription) {
        // Create a placeholder appointment with the description
        Appointment tempAppointment = new Appointment(
            new java.util.Date(),  // Current date as placeholder
            null,                  // No doctor assigned yet
            this,                  // This patient
            appointmentDescription // Use the description as the status
        );
        appointments.add(tempAppointment);
    }
    
    public ArrayList<Appointment> getAppointments() {
        return appointments;
    }

    public void triggerPanicButton(String reason) {
        panicButton.triggerAlert(reason);
    }

    public void enablePanicButton() {
        panicButton.enable();
    }

    public void disablePanicButton() {
        panicButton.disable();
    }

    public PanicButton getPanicButton() {
        return panicButton;
    }
    
    // Medical history integration
    public MedicalHistory getMedicalHistory() {
        return medicalHistory;
    }
    
    // Emergency contact getters and setters
    public String getEmergencyContact() {
        return emergencyContact;
    }
    
    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }
    
    // Health insurance getters and setters
    public String getHealthInsuranceInfo() {
        return healthInsuranceInfo;
    }
    
    public void setHealthInsuranceInfo(String healthInsuranceInfo) {
        this.healthInsuranceInfo = healthInsuranceInfo;
    }
    
    // Display patient information
    public void displayPatientInfo() {
        System.out.println("===== Patient Information =====");
        System.out.println("Name: " + getName());
        System.out.println("ID: " + getUserID());
        System.out.println("Contact: " + getPhone());
        System.out.println("Email: " + getEmail());
        System.out.println("Address: " + getAddress());
        if (emergencyContact != null && !emergencyContact.isEmpty()) {
            System.out.println("Emergency Contact: " + emergencyContact);
        }
        if (healthInsuranceInfo != null && !healthInsuranceInfo.isEmpty()) {
            System.out.println("Health Insurance: " + healthInsuranceInfo);
        }
    }

    /**
     * Upload vital signs from a CSV file
     * @param filePath Path to the CSV file
     * @return Number of records successfully uploaded, -1 if error occurs
     */
    public int uploadVitalsFromCSV(String filePath) {
        try {
            return CSVVitalsUploader.uploadVitalsFromCSV(this, filePath);
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * Upload vital signs from a CSV file with detailed reporting
     * @param filePath Path to the CSV file
     * @return Detailed report of the upload process
     * @throws IOException If file cannot be read
     */
    public VitalsUploadReport uploadVitalsFromCSVWithReport(String filePath) throws IOException {
        return CSVVitalsUploader.uploadVitalsFromCSVWithReport(this, filePath);
    }
    
    /**
     * Adds a single vital sign record to patient's history
     * @param vitalSign The vital sign measurements to record
     */
    public void addVitalSign(VitalSign vitalSign) {
        this.vitalsDatabase.addVitalRecord(vitalSign);
    }
    
    /**
     * Get patient's vitals database
     * @return The VitalsDatabase for this patient
     */
    public VitalsDatabase getVitalsDatabase() {
        return vitalsDatabase;
    }
}
