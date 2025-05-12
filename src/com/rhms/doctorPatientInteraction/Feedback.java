package com.rhms.doctorPatientInteraction;

import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;

import java.util.Date;

// Manages feedback and prescriptions between doctors and patients
public class Feedback {
    private int feedbackId; // DB feedback_id
    private int prescriptionId; // DB prescription_id
    private Doctor doctor;
    private Patient patient;
    private String comments;
    private Prescription prescription;
    private Date timestamp;

    // Creates new feedback with compulsory prescription
    public Feedback(Doctor doctor, Patient patient, String comments, Prescription prescription) {
        if (prescription == null) throw new IllegalArgumentException("Prescription is required for feedback.");
        this.doctor = doctor;
        this.patient = patient;
        this.comments = comments;
        this.prescription = prescription;
        this.timestamp = new Date(); // Set current timestamp
    }

    // Creates new feedback with specified timestamp
    public Feedback(Doctor doctor, Patient patient, String comments, Prescription prescription, Date timestamp) {
        if (prescription == null) throw new IllegalArgumentException("Prescription is required for feedback.");
        this.doctor = doctor;
        this.patient = patient;
        this.comments = comments;
        this.prescription = prescription;
        this.timestamp = timestamp != null ? timestamp : new Date();
    }

    // Setters for DB IDs
    public void setFeedbackId(int feedbackId) { this.feedbackId = feedbackId; }
    public int getFeedbackId() { return feedbackId; }
    public void setPrescriptionId(int prescriptionId) { this.prescriptionId = prescriptionId; }
    public int getPrescriptionId() { return prescriptionId; }

    // Getters and setters
    public Doctor getDoctor() {
        return doctor;
    }

    public Patient getPatient() {
        return patient;
    }

    public String getMessage() {
        return comments;
    }

    public Prescription getPrescription() {
        return prescription;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public void setPrescription(Prescription prescription) {
        this.prescription = prescription;
    }
    
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Check if this feedback has an associated prescription
     * @return true if a prescription is attached to this feedback
     */
    public boolean hasPrescription() {
        return prescription != null;
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
