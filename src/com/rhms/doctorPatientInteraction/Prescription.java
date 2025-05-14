package com.rhms.doctorPatientInteraction;

// Manages medication prescriptions with dosage and schedule information
public class Prescription {
    private int prescriptionId; // DB prescription_id
    // Details of the prescribed medication
    private String medicationName;
    private String dosage;        // Amount of medication per dose
    private String schedule;      // Timing/frequency of doses
    private String duration;      // Add duration field
    private String instructions;  // Additional instructions for the patient

    // Creates a new prescription with medication details
    public Prescription(String medicationName, String dosage, String schedule, String duration, String instructions) {
        this.medicationName = medicationName;
        this.dosage = dosage;
        this.schedule = schedule;
        this.duration = duration;
        this.instructions = instructions;
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

    // Setters and getters for prescriptionId and duration
    public void setPrescriptionId(int prescriptionId) { this.prescriptionId = prescriptionId; }
    public int getPrescriptionId() { return prescriptionId; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    // Displays prescription details in a formatted manner
    public void displayPrescription() {
        System.out.println("Medication: " + medicationName);
        System.out.println("Dosage: " + dosage);
        System.out.println("Schedule: " + schedule);
    }

    public String getMedicationInfo() {
        return "Medication: " + medicationName + "\nDosage: " + dosage + "\nSchedule: " + schedule;
    }

    public String getInstructions() {
        return "Instructions: " + instructions;
    }
}
