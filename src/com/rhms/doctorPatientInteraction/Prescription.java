package com.rhms.doctorPatientInteraction;

// Manages medication prescriptions with dosage and schedule information
public class Prescription {
    // Details of the prescribed medication
    private String medicationName;
    private String dosage;        // Amount of medication per dose
    private String schedule;      // Timing/frequency of doses

    // Creates a new prescription with medication details
    public Prescription(String medicationName, String dosage, String schedule) {
        this.medicationName = medicationName;
        this.dosage = dosage;
        this.schedule = schedule;
    }

    // Getters and setters
    public String getMedicationName() {
        return medicationName;
    }

    public String getDosage() {
        return dosage;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    // Displays prescription details in a formatted manner
    public void displayPrescription() {
        System.out.println("Medication: " + medicationName);
        System.out.println("Dosage: " + dosage);
        System.out.println("Schedule: " + schedule);
    }
}
