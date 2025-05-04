package com.rhms.userManagement;

import java.util.ArrayList;

import com.rhms.appointmentScheduling.AppointmentManager;
import com.rhms.doctorPatientInteraction.Feedback;
import com.rhms.emergencyAlert.PanicButton;
import com.rhms.doctorPatientInteraction.MedicalHistory;
import com.rhms.appointmentScheduling.Appointment;
import com.rhms.healthDataHandling.VitalsDatabase;
import com.rhms.healthDataHandling.VitalSign;
import com.rhms.healthDataHandling.CSVVitalsUploader;
import com.rhms.healthDataHandling.VitalsUploadReport;
import com.rhms.Database.UserDatabaseHandler;
import java.io.IOException;
import java.util.List;

public class Patient extends User {
    private ArrayList<String> medicalRecords;
    private ArrayList<String> doctorFeedback;
    private ArrayList<Appointment> appointments;
    private PanicButton panicButton;
    private MedicalHistory medicalHistory;
    private VitalsDatabase vitalsDatabase;
    private ArrayList<Doctor> assignedDoctors; // Track assigned doctors

    public Patient(String name, String email, String password, String phone, String address, int userID) {
        super(name, email, password, phone, address, userID);
        this.medicalRecords = new ArrayList<>();
        this.doctorFeedback = new ArrayList<>();
        this.appointments = new ArrayList<>();
        this.panicButton = new PanicButton(this);
        this.medicalHistory = new MedicalHistory();
        this.vitalsDatabase = new VitalsDatabase(this);
        this.assignedDoctors = new ArrayList<>();
    }
    
    // Enhanced constructor with authentication fields
    public Patient(String name, String email, String password, String phone, String address, 
                  int userID, String username) {
        super(name, email, password, phone, address, userID, username);
        this.medicalRecords = new ArrayList<>();
        this.doctorFeedback = new ArrayList<>();
        this.appointments = new ArrayList<>();
        this.panicButton = new PanicButton(this);
        this.medicalHistory = new MedicalHistory();
        this.vitalsDatabase = new VitalsDatabase(this);
        this.assignedDoctors = new ArrayList<>();
    }

    /**
     * Verify patient in the database.
     */
    public static Patient verifyInDatabase(String username, String password) {
        UserDatabaseHandler dbHandler = new UserDatabaseHandler();
        User user = dbHandler.getUserByUsername(username);
        if (user instanceof Patient && user.getPassword().equals(password)) {
            return (Patient) user;
        }
        return null;
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
    
    /**
     * Set the appointments list for this patient
     * @param appointments The list of appointments to set
     */
    public void setAppointments(List<Appointment> appointments) {
        if (appointments != null) {
            this.appointments = new ArrayList<>(appointments);
        } else {
            this.appointments = new ArrayList<>();
        }
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
    
    // Display patient information
    public void displayPatientInfo() {
        System.out.println("===== Patient Information =====");
        System.out.println("Name: " + getName());
        System.out.println("ID: " + getUserID());
        System.out.println("Contact: " + getPhone());
        System.out.println("Email: " + getEmail());
        System.out.println("Address: " + getAddress());
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
    
    /**
     * Add a doctor to this patient's list of assigned doctors
     * @param doctor The doctor to assign to this patient
     */
    public void addAssignedDoctor(Doctor doctor) {
        if (!assignedDoctors.contains(doctor)) {
            assignedDoctors.add(doctor);
            // Also add the patient to the doctor's list of assigned patients
            if (!doctor.getAssignedPatients().contains(this)) {
                doctor.addPatient(this);
            }
        }
    }

    
    /**
     * Remove a doctor from this patient's list of assigned doctors
     * @param doctor The doctor to remove from this patient
     */
    public void removeAssignedDoctor(Doctor doctor) {
        if (assignedDoctors.contains(doctor)) {
            assignedDoctors.remove(doctor);
            // Also remove the patient from the doctor's list of assigned patients
            if (doctor.getAssignedPatients().contains(this)) {
                doctor.removePatient(this);
            }
        }
    }
    
    /**
     * Get all doctors assigned to this patient
     * @return ArrayList of assigned doctors
     */
    public ArrayList<Doctor> getAssignedDoctors() {
        return assignedDoctors;
    }
    
    /**
     * Check if this patient has at least one assigned doctor
     * @return true if the patient has at least one assigned doctor
     */
    public boolean hasAssignedDoctor() {
        return !assignedDoctors.isEmpty();
    }

    /**
     * Schedule an appointment for this patient using the appointment manager
     *
     * @param appointment The appointment to schedule
     * @param appointmentManager The appointment manager to use
     * @return The scheduled appointment with ID set from database
     */
    public Appointment scheduleAppointment(Appointment appointment, AppointmentManager appointmentManager) {
        try {
            return appointmentManager.scheduleAppointment(appointment);
        } catch (AppointmentManager.AppointmentException e) {
            System.err.println("Error scheduling appointment: " + e.getMessage());
            return null;
        }
    }

    /**
     * Add appointment to patient's list (used when loading from database)
     */
    public void addAppointment(Appointment appointment) {
        if (!appointments.contains(appointment)) {
            appointments.add(appointment);
        }
    }



    /**
     * Cancel an appointment for this patient
     *
     * @param appointment The appointment to cancel
     * @param appointmentManager The appointment manager to use
     * @return true if cancelled successfully, false otherwise
     */
    public boolean cancelAppointment(Appointment appointment, AppointmentManager appointmentManager) {
        try {
            return appointmentManager.updateAppointmentStatus(appointment, "Cancelled");
        } catch (AppointmentManager.AppointmentException e) {
            System.err.println("Error cancelling appointment: " + e.getMessage());
            return false;
        }
    }


    public List<Feedback> getAllFeedback() {
        List<Feedback> allFeedback = new ArrayList<>();
        for (Doctor doctor : assignedDoctors) {
            allFeedback.addAll(doctor.getFeedbackForPatient(this));
        }
        return allFeedback;
    }

    public void receiveFeedback(Feedback feedback) {
        if (feedback != null) {
            doctorFeedback.add(feedback.getMessage());
        }
    }

    public List<VitalSign> getVitalSigns() {
        return vitalsDatabase.getAllVitalSigns();
    }
}
