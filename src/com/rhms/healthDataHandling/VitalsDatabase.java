package com.rhms.healthDataHandling;

import com.rhms.userManagement.Patient;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;

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
     * Retrieves all vital sign records for the patient (legacy method)
     * @return ArrayList containing all recorded vital signs
     */
    public ArrayList<VitalSign> getVitals() {
        return vitals;
    }
    
    /**
     * Retrieves all vital sign records for the patient
     * Method name matches PatientDashboardController expectations
     * @return List containing all recorded vital signs
     */
    public List<VitalSign> getAllVitals() {
        return vitals;
    }
    
    /**
     * Retrieves the most recent vital sign record if available
     * @return The most recent VitalSign, or null if no records exist
     */
    public VitalSign getLatestVitalSigns() {
        if (vitals.isEmpty()) {
            return null;
        }
        return vitals.get(vitals.size() - 1); // Return the last element
    }
    
    /**
     * Retrieves vital signs recorded between two dates (inclusive)
     * @param start The start date for the range
     * @param end The end date for the range
     * @return List of vital signs within the date range
     */
    public List<VitalSign> getVitalsInDateRange(Date start, Date end) {
        if (vitals.isEmpty()) {
            return new ArrayList<>();
        }
        
        return vitals.stream()
            .filter(v -> (v.getTimestamp().after(start) || v.getTimestamp().equals(start)) && 
                         (v.getTimestamp().before(end) || v.getTimestamp().equals(end)))
            .collect(Collectors.toList());
    }
    
    /**
     * Retrieves vital signs from the last specified number of days
     * @param days Number of days to look back
     * @return List of vital signs from the specified period
     */
    public List<VitalSign> getVitalsFromLastDays(int days) {
        if (vitals.isEmpty()) {
            return new ArrayList<>();
        }
        
        long cutoffTime = System.currentTimeMillis() - ((long)days * 24 * 60 * 60 * 1000);
        Date cutoffDate = new Date(cutoffTime);
        
        return vitals.stream()
            .filter(v -> v.getTimestamp().after(cutoffDate))
            .collect(Collectors.toList());
    }
    
    /**
     * Get all abnormal vital sign readings
     * @return List of vital signs that have abnormal readings
     */
    public List<VitalSign> getAbnormalVitals() {
        return vitals.stream()
            .filter(VitalSign::isAbnormal)
            .collect(Collectors.toList());
    }
    
    /**
     * Get vital sign readings sorted by timestamp
     * @param ascending true for oldest first, false for newest first
     * @return Sorted list of vital signs
     */
    public List<VitalSign> getSortedVitals(boolean ascending) {
        Comparator<VitalSign> comparator = Comparator.comparing(VitalSign::getTimestamp);
        if (!ascending) {
            comparator = comparator.reversed();
        }
        
        return vitals.stream()
            .sorted(comparator)
            .collect(Collectors.toList());
    }
    
    /**
     * Calculates the average heart rate over all recorded vitals
     * @return Average heart rate or 0 if no records
     */
    public double getAverageHeartRate() {
        if (vitals.isEmpty()) {
            return 0;
        }
        
        double sum = vitals.stream()
            .mapToDouble(VitalSign::getHeartRate)
            .sum();
            
        return sum / vitals.size();
    }
    
    /**
     * Calculates the average blood pressure over all recorded vitals
     * @return Average blood pressure or 0 if no records
     */
    public double getAverageBloodPressure() {
        if (vitals.isEmpty()) {
            return 0;
        }
        
        double sum = vitals.stream()
            .mapToDouble(VitalSign::getBloodPressure)
            .sum();
            
        return sum / vitals.size();
    }
    
    /**
     * Calculates the average oxygen level over all recorded vitals
     * @return Average oxygen level or 0 if no records
     */
    public double getAverageOxygenLevel() {
        if (vitals.isEmpty()) {
            return 0;
        }
        
        double sum = vitals.stream()
            .mapToDouble(VitalSign::getOxygenLevel)
            .sum();
            
        return sum / vitals.size();
    }
    
    /**
     * Calculates the average temperature over all recorded vitals
     * @return Average temperature or 0 if no records
     */
    public double getAverageTemperature() {
        if (vitals.isEmpty()) {
            return 0;
        }
        
        double sum = vitals.stream()
            .mapToDouble(VitalSign::getTemperature)
            .sum();
            
        return sum / vitals.size();
    }
    
    /**
     * Gets the trend of heart rate compared to the average
     * @return "increasing", "decreasing", or "stable"
     */
    public String getHeartRateTrend() {
        if (vitals.size() < 3) {
            return "insufficient data";
        }
        
        List<VitalSign> recentVitals = getSortedVitals(false).subList(0, Math.min(3, vitals.size()));
        double recentAvg = recentVitals.stream()
            .mapToDouble(VitalSign::getHeartRate)
            .average()
            .orElse(0);
            
        double overallAvg = getAverageHeartRate();
        
        double difference = recentAvg - overallAvg;
        if (Math.abs(difference) < 2) {
            return "stable";
        } else {
            return difference > 0 ? "increasing" : "decreasing";
        }
    }
    
    /**
     * Gets the trend of blood pressure compared to the average
     * @return "increasing", "decreasing", or "stable"
     */
    public String getBloodPressureTrend() {
        if (vitals.size() < 3) {
            return "insufficient data";
        }
        
        List<VitalSign> recentVitals = getSortedVitals(false).subList(0, Math.min(3, vitals.size()));
        double recentAvg = recentVitals.stream()
            .mapToDouble(VitalSign::getBloodPressure)
            .average()
            .orElse(0);
            
        double overallAvg = getAverageBloodPressure();
        
        double difference = recentAvg - overallAvg;
        if (Math.abs(difference) < 2) {
            return "stable";
        } else {
            return difference > 0 ? "increasing" : "decreasing";
        }
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
    
    /**
     * Gets the total number of vital sign records
     * @return count of vital sign records
     */
    public int getVitalsCount() {
        return vitals.size();
    }
    
    /**
     * Check if the patient has any vital sign records
     * @return true if there are vitals recorded, false otherwise
     */
    public boolean hasVitalsData() {
        return !vitals.isEmpty();
    }

    public List<VitalSign> getAllVitalSigns() {
        return new ArrayList<>(vitals);
    }
}
