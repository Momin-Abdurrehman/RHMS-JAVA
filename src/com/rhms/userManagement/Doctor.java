package com.rhms.userManagement;

import java.util.ArrayList;
import com.rhms.doctorPatientInteraction.Feedback;
import com.rhms.doctorPatientInteraction.Prescription;
import com.rhms.Database.UserDatabaseHandler;

public class Doctor extends User {
    private String specialization;
    private int experienceYears;
    private ArrayList<Patient> assignedPatients;

    public Doctor(String name, String email, String password, String phone, String address, int userID, String specialization, int experienceYears) {
        super(name, email, password, phone, address, userID);
        this.specialization = specialization;
        this.experienceYears = experienceYears;
        this.assignedPatients = new ArrayList<>();
    }
    
    // Enhanced constructor with authentication fields
    public Doctor(String name, String email, String password, String phone, String address, 
                  int userID, String username, String specialization, int experienceYears) {
        super(name, email, password, phone, address, userID, username);
        this.specialization = specialization;
        this.experienceYears = experienceYears;
        this.assignedPatients = new ArrayList<>();
    }

    // Verify doctor in the database
    public static Doctor verifyInDatabase(String username, String password) {
        UserDatabaseHandler dbHandler = new UserDatabaseHandler();
        User user = dbHandler.getUserByUsername(username);
        if (user instanceof Doctor && user.getPassword().equals(password)) {
            return (Doctor) user;
        }
        return null;
    }

    public void addPatient(Patient patient) {
        assignedPatients.add(patient);
    }       //add patient method

    public void removePatient(Patient patient) {
        assignedPatients.remove(patient);
    }

    // Enhanced feedback method that uses the Feedback class
    public void provideFeedback(Patient patient, String comments, Prescription prescription) {
        Feedback feedback = new Feedback(this, patient, comments, prescription);
        patient.getMedicalHistory().addConsultation(feedback);
    }
    
    // Simple feedback method (backward compatibility)
    public void provideFeedback(Patient patient, String feedback) {
        patient.getDoctorFeedback().add(feedback);
        // Also add to structured medical history
        provideFeedback(patient, feedback, null);
    }

    public void manageAppointment(String appointmentDetails) {
        System.out.println("Managing appointment: " + appointmentDetails);
    }
    
    // Create a prescription for a patient
    public Prescription createPrescription(String medicationName, String dosage, String schedule) {
        return new Prescription(medicationName, dosage, schedule);
    }
    
    // View all assigned patients
    public void viewAllPatients() {
        if (assignedPatients.isEmpty()) {
            System.out.println("No patients assigned to Dr. " + getName());
            return;
        }
        
        System.out.println("\n=== Dr. " + getName() + "'s Patients ===");
        for (int i = 0; i < assignedPatients.size(); i++) {
            Patient patient = assignedPatients.get(i);
            System.out.println((i+1) + ". " + patient.getName() + " (ID: " + patient.getUserID() + ")");
        }
    }
    
    // View a specific patient's medical history
    public void viewPatientHistory(Patient patient) {
        if (!assignedPatients.contains(patient)) {
            System.out.println("Error: Patient " + patient.getName() + " is not assigned to Dr. " + getName());
            return;
        }
        
        System.out.println("\n=== Medical History for " + patient.getName() + " ===");
        patient.getMedicalHistory().displayMedicalHistory();
    }
    
    // Get assigned patients
    public ArrayList<Patient> getAssignedPatients() {
        return assignedPatients;
    }
    
    // Get specialization
    public String getSpecialization() {
        return specialization;
    }
    
    // Get years of experience
    public int getExperienceYears() {
        return experienceYears;
    }
    
    // Display doctor information
    public void displayDoctorInfo() {
        System.out.println("===== Doctor Information =====");
        System.out.println("Name: Dr. " + getName());
        System.out.println("Specialization: " + specialization);
        System.out.println("Experience: " + experienceYears + " years");
        System.out.println("Contact: " + getPhone());
        System.out.println("Email: " + getEmail());
        System.out.println("Assigned Patients: " + assignedPatients.size());
    }
}
