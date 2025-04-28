package com.rhms.healthDataHandling;

import com.rhms.userManagement.Patient;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages and stores vital sign records for a specific patient
 * Provides functionality to add, retrieve, and display vital sign history
 */
public class VitalsDatabase {
    // Collection to store patient's vital sign records
    private ArrayList<VitalSign> vitals;
    // Patient associated with these vital records
    private Patient patient;

    /**
     * Creates a new vitals database for a specific patient
     * @param patient The patient whose vitals will be tracked
     */
    public VitalsDatabase(Patient patient) {
        this.patient = patient;
        this.vitals = new ArrayList<>();
    }

    /**
     * Adds a new vital sign record to the patient's history
     * @param vitalSign The vital sign measurements to be recorded
     */
    public void addVitalRecord(VitalSign vitalSign) {
        vitals.add(vitalSign);
        System.out.println("Vital signs recorded successfully for " + patient.getName());
    }
    
    /**
     * Adds multiple vital sign records in a batch operation
     * @param vitalSigns List of vital sign measurements to be recorded
     */
    public void addVitalRecords(List<VitalSign> vitalSigns) {
        if (vitalSigns == null || vitalSigns.isEmpty()) {
            return;
        }
        
        vitals.addAll(vitalSigns);
        System.out.println(vitalSigns.size() + " vital records added successfully for " + patient.getName());
    }

    /**
     * Retrieves all vital sign records for the patient
     * @return ArrayList containing all recorded vital signs
     */
    public ArrayList<VitalSign> getVitals() {
        return vitals;
    }

    /**
     * Displays complete vital sign history for the patient
     * Shows a message if no vitals are recorded
     */
    public void displayAllVitals() {
        System.out.println("Vital History for Patient: " + patient.getName());
        if (vitals.isEmpty()) {
            System.out.println("No vitals recorded.");
        } else {
            for (VitalSign vital : vitals) {
                vital.displayVitals();
                System.out.println("----------------------");
            }
        }
    }

    /**
     * Gets the patient associated with this vitals database
     * @return Patient object
     */
    public Patient getPatient() {
        return patient;
    }
}
