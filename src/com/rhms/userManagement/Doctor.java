package com.rhms.userManagement;

import com.rhms.appointmentScheduling.Appointment;
import com.rhms.doctorPatientInteraction.Feedback;
import com.rhms.doctorPatientInteraction.Prescription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a Doctor in the healthcare system
 * Extends the base User class with doctor-specific attributes and capabilities
 */
public class Doctor extends User {
    private String specialization;
    private int experienceYears;
    private List<Patient> assignedPatients;
    private List<Appointment> appointments;
    // Map to store feedback by patient ID
    private Map<Integer, List<Feedback>> feedbackByPatient;

    /**
     * Creates a new Doctor with all necessary information
     */
    public Doctor(String name, String email, String password, String phone, String address,
                  int userID, String username, String specialization, int experienceYears) {
        super(name, email, password, phone, address, userID, username);
        this.specialization = specialization;
        this.experienceYears = experienceYears;
        this.assignedPatients = new ArrayList<>();
        this.appointments = new ArrayList<>();
        this.feedbackByPatient = new HashMap<>();
    }

    // Getters
    public String getSpecialization() {
        return specialization;
    }

    public int getExperienceYears() {
        return experienceYears;
    }

    /**
     * Get a list of patients assigned to this doctor
     * @return List of patients assigned to this doctor
     */
    public List<Patient> getAssignedPatients() {
        // Print debug info
        System.out.println("Doctor " + getName() + " (ID: " + getUserID() + 
                     ") currently has " + assignedPatients.size() + " assigned patients");
                     
        // Return a defensive copy of the list
        return new ArrayList<>(assignedPatients);
    }

    public List<Appointment> getAppointments() {
        return new ArrayList<>(appointments);
    }

    // Setters
    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public void setExperienceYears(int experienceYears) {
        this.experienceYears = experienceYears;
    }

    public void setAppointments(List<Appointment> appointments) {
        this.appointments = appointments != null ? appointments : new ArrayList<>();
    }

    /**
     * Add a patient to this doctor's assigned patients
     */
    public void addPatient(Patient patient) {
        if (patient != null && !containsPatient(patient)) {
            assignedPatients.add(patient);
        }
    }
    
    /**
     * Check if the doctor already has this patient assigned
     * Uses getUserID() for comparison to ensure proper equality checking
     */
    private boolean containsPatient(Patient patient) {
        if (patient == null) return false;
        
        for (Patient p : assignedPatients) {
            if (p.getUserID() == patient.getUserID()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remove a patient from this doctor's assigned patients
     */
    public void removePatient(Patient patient) {
        if (patient == null) return;
        
        // Remove using ID comparison to ensure proper removal
        assignedPatients.removeIf(p -> p.getUserID() == patient.getUserID());
    }
    
    /**
     * Clear all patients from this doctor's assigned patients
     * Used when reloading assignments from database
     */
    public void clearPatients() {
        assignedPatients.clear();
    }

    /**
     * Display the doctor's professional summary
     */
    public void displayDoctorInfo() {
        System.out.println("Doctor: " + getName());
        System.out.println("Specialization: " + specialization);
        System.out.println("Experience: " + experienceYears + " years");
        System.out.println("Contact: " + getEmail() + ", " + getPhone());
    }

    /**
     * Provide feedback to a patient
     * @param patient The patient to receive feedback
     * @param message The feedback message
     * @return The created feedback object
     */
    public Feedback provideFeedback(Patient patient, String message) {
        Feedback feedback = new Feedback(this, patient, message, null);
        addFeedbackToHistory(patient, feedback);
        patient.receiveFeedback(feedback);
        return feedback;
    }

    /**
     * Provide feedback with prescription to a patient
     * @param patient The patient to receive feedback
     * @param message The feedback message
     * @param prescription The prescription for the patient
     * @return The created feedback object
     */
    public Feedback provideFeedback(Patient patient, String message, Prescription prescription) {
        Feedback feedback = new Feedback(this, patient, message, prescription);
        addFeedbackToHistory(patient, feedback);
        patient.receiveFeedback(feedback);
        return feedback;
    }

    /**
     * Add feedback to the doctor's internal history, organized by patient ID
     * @param patient The patient who received feedback
     * @param feedback The feedback provided
     */
    private void addFeedbackToHistory(Patient patient, Feedback feedback) {
        if (patient == null || feedback == null) {
            return;
        }

        int patientId = patient.getUserID();
        if (!feedbackByPatient.containsKey(patientId)) {
            feedbackByPatient.put(patientId, new ArrayList<>());
        }
        feedbackByPatient.get(patientId).add(feedback);
    }
    
    /**
     * Get all feedback this doctor has provided to a specific patient
     * @param patient The patient whose feedback to retrieve
     * @return List of feedback given to the specified patient
     */
    public List<Feedback> getFeedbackForPatient(Patient patient) {
        if (patient == null) {
            return new ArrayList<>();
        }
        
        int patientId = patient.getUserID();
        if (!feedbackByPatient.containsKey(patientId)) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(feedbackByPatient.get(patientId));
    }
    
    /**
     * Get all feedback this doctor has provided to all patients
     * @return List of all feedback provided
     */
    public List<Feedback> getAllProvidedFeedback() {
        return feedbackByPatient.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public void viewAllPatients() {
        System.out.println("Patients assigned to Dr. " + getName() + ":");
        for (Patient patient : assignedPatients) {
            System.out.println("- " + patient.getName() + " (ID: " + patient.getUserID() + ")");
        }
    }

    public void viewPatientHistory(Patient patient) {
        if (patient == null) {
            System.out.println("No patient selected.");
            return;
        }
        System.out.println("Patient History for " + patient.getName() + ":");
        List<Appointment> patientAppointments = appointments.stream()
                .filter(appointment -> appointment.getPatient().equals(patient))
                .collect(Collectors.toList());
        if (patientAppointments.isEmpty()) {
            System.out.println("No appointments found for this patient.");
        } else {
            for (Appointment appointment : patientAppointments) {
                System.out.println("- Appointment ID: " + appointment.getAppointmentId() +
                        ", Date: " + appointment.getAppointmentDate() +
                        ", Purpose: " + appointment.getPurpose());
            }
        }
    }
}
