package com.rhms.doctorPatientInteraction;

import java.util.ArrayList;

// Manages patient's medical consultation history and feedback records
public class MedicalHistory {
    // Stores all past consultations and their feedback
    private ArrayList<Feedback> pastConsultations;

    // Initialize empty medical history
    public MedicalHistory() {
        this.pastConsultations = new ArrayList<>();
    }

    // Add new consultation feedback to history
    public void addConsultation(Feedback feedback) {
        pastConsultations.add(feedback);
    }

    // Retrieve all past consultations
    public ArrayList<Feedback> getPastConsultations() {
        return pastConsultations;
    }

    // Display complete medical history with feedback and prescriptions
    public void displayMedicalHistory() {
        if (pastConsultations.isEmpty()) {
            System.out.println("No past consultations available.");
        } else {
            System.out.println("\n=== Medical History ===");
            for (Feedback feedback : pastConsultations) {
                feedback.displayFeedback();
                System.out.println("-----------------------");
            }
        }
    }
}
