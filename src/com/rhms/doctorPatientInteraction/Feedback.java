package com.rhms.doctorPatientInteraction;

import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;

// Manages feedback and prescriptions between doctors and patients
public class Feedback {
    private Doctor doctor;
    private Patient patient;
    private String comments;
    private Prescription prescription;

    // Creates new feedback with optional prescription
    public Feedback(Doctor doctor, Patient patient, String comments, Prescription prescription) {
        this.doctor = doctor;
        this.patient = patient;
        this.comments = comments;
        this.prescription = prescription;
    }

    // Getters and setters
    public Doctor getDoctor() {
        return doctor;
    }

    public Patient getPatient() {
        return patient;
    }

    public String getComments() {
        return comments;
    }

    public Prescription getPrescription() {
        return prescription;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public void setPrescription(Prescription prescription) {
        this.prescription = prescription;
    }

    // Displays feedback details including prescription if available
    public void displayFeedback() {
        System.out.println("Doctor: " + doctor.getName());
        System.out.println("Patient: " + patient.getName());
        System.out.println("Comments: " + comments);
        if (prescription != null) {
            prescription.displayPrescription();
        }
    }
}
