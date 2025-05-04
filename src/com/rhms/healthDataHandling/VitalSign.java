package com.rhms.healthDataHandling;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Represents a single vital sign measurement.
 * Contains heart rate, oxygen level, blood pressure, and temperature.
 */
public class VitalSign {
    private double heartRate;      // Beats per minute
    private double oxygenLevel;    // Percentage
    private double bloodPressure;  // mmHg (systolic)
    private double temperature;    // Celsius
    private Date timestamp;        // When the measurement was taken
    
    // Critical threshold values for vital signs
    private static final double MAX_HEART_RATE = 100.0;    // Maximum normal heart rate in bpm
    private static final double MIN_HEART_RATE = 60.0;     // Minimum normal heart rate in bpm
    private static final double MIN_OXYGEN_LEVEL = 95.0;   // Minimum normal oxygen saturation %
    private static final double MAX_BLOOD_PRESSURE = 120.0; // Maximum normal systolic pressure
    private static final double MIN_BLOOD_PRESSURE = 90.0;  // Minimum normal systolic pressure
    private static final double MAX_TEMPERATURE = 37.2;     // Maximum normal temperature in °C
    private static final double MIN_TEMPERATURE = 36.1;     // Minimum normal temperature in °C

    /**
     * Create a vital sign record with the current timestamp
     */
    public VitalSign(double heartRate, double oxygenLevel, double bloodPressure, double temperature) {
        this.heartRate = heartRate;
        this.oxygenLevel = oxygenLevel;
        this.bloodPressure = bloodPressure;
        this.temperature = temperature;
        this.timestamp = new Date(); // Current timestamp
    }
    
    /**
     * Create a vital sign record with a specific timestamp
     */
    public VitalSign(double heartRate, double oxygenLevel, double bloodPressure, 
                      double temperature, Date timestamp) {
        this.heartRate = heartRate;
        this.oxygenLevel = oxygenLevel;
        this.bloodPressure = bloodPressure;
        this.temperature = temperature;
        this.timestamp = timestamp;
    }
    
    /**
     * Display vital signs in a readable format
     */
    public void displayVitals() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("Timestamp: " + dateFormat.format(timestamp));
        System.out.println("Heart Rate: " + heartRate + " bpm");
        System.out.println("Oxygen Level: " + oxygenLevel + "%");
        System.out.println("Temperature: " + temperature + "°C");
        System.out.println("Blood Pressure: " + bloodPressure + " mmHg");
        
        // Add interpretation of values based on normal ranges
        System.out.println("Status: " + getVitalStatusSummary());
    }
    
    /**
     * Get a summary of the patient's vital status
     * @return String indicating if vitals are normal or concerning
     */
    public String getVitalStatusSummary() {
        boolean allNormal = true;
        StringBuilder concerns = new StringBuilder();
        
        // Check heart rate (normal 60-100 bpm)
        if (heartRate < MIN_HEART_RATE || heartRate > MAX_HEART_RATE) {
            allNormal = false;
            concerns.append("Heart rate ");
            concerns.append(heartRate < MIN_HEART_RATE ? "low" : "high");
            concerns.append(". ");
        }
        
        // Check oxygen level (normal >95%)
        if (oxygenLevel < MIN_OXYGEN_LEVEL) {
            allNormal = false;
            concerns.append("Low oxygen. ");
        }
        
        // Check blood pressure (normal systolic 90-120)
        if (bloodPressure < MIN_BLOOD_PRESSURE || bloodPressure > MAX_BLOOD_PRESSURE) {
            allNormal = false;
            concerns.append("Blood pressure ");
            concerns.append(bloodPressure < MIN_BLOOD_PRESSURE ? "low" : "high");
            concerns.append(". ");
        }
        
        // Check temperature (normal 36.1-37.2°C)
        if (temperature < MIN_TEMPERATURE || temperature > MAX_TEMPERATURE) {
            allNormal = false;
            concerns.append("Temperature ");
            concerns.append(temperature < MIN_TEMPERATURE ? "low" : "high");
            concerns.append(". ");
        }
        
        if (allNormal) {
            return "All vitals within normal range";
        } else {
            return "Concerns: " + concerns.toString();
        }
    }
    
    /**
     * Check if any vital signs are abnormal and should trigger an alert
     * @return true if any vital sign is outside normal range
     */
    public boolean isAbnormal() {
        // Check if any vital is outside normal range
        return (heartRate < MIN_HEART_RATE || heartRate > MAX_HEART_RATE) ||
               (oxygenLevel < MIN_OXYGEN_LEVEL) ||
               (bloodPressure < MIN_BLOOD_PRESSURE || bloodPressure > MAX_BLOOD_PRESSURE) ||
               (temperature < MIN_TEMPERATURE || temperature > MAX_TEMPERATURE);
    }
    
    /**
     * Get detailed information about abnormal vitals for alerts
     * @return String containing information about the abnormal vitals
     */
    public String getAbnormalDetails() {
        if (!isAbnormal()) {
            return "No abnormal vitals detected";
        }
        
        StringBuilder details = new StringBuilder();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        details.append("Abnormal vitals recorded at ").append(dateFormat.format(timestamp)).append(":\n");
        
        if (heartRate < MIN_HEART_RATE || heartRate > MAX_HEART_RATE) {
            details.append("- Heart rate: ").append(heartRate).append(" bpm ")
                   .append(heartRate < MIN_HEART_RATE ? "(Low)" : "(High)")
                   .append(" [Normal range: 60-100 bpm]\n");
        }
        
        if (oxygenLevel < MIN_OXYGEN_LEVEL) {
            details.append("- Oxygen level: ").append(oxygenLevel).append("% (Low)")
                   .append(" [Normal range: >95%]\n");
        }
        
        if (bloodPressure < MIN_BLOOD_PRESSURE || bloodPressure > MAX_BLOOD_PRESSURE) {
            details.append("- Blood pressure: ").append(bloodPressure).append(" mmHg ")
                   .append(bloodPressure < MIN_BLOOD_PRESSURE ? "(Low)" : "(High)")
                   .append(" [Normal range: 90-120 mmHg]\n");
        }
        
        if (temperature < MIN_TEMPERATURE || temperature > MAX_TEMPERATURE) {
            details.append("- Temperature: ").append(temperature).append("°C ")
                   .append(temperature < MIN_TEMPERATURE ? "(Low)" : "(High)")
                   .append(" [Normal range: 36.1-37.2°C]\n");
        }
        
        return details.toString();
    }
    
    /**
     * Check if heart rate is within normal range
     * @return true if heart rate is normal, false otherwise
     */
    public boolean isHeartRateNormal() {
        return heartRate >= MIN_HEART_RATE && heartRate <= MAX_HEART_RATE;
    }
    
    /**
     * Check if oxygen level is within normal range
     * @return true if oxygen level is normal, false otherwise
     */
    public boolean isOxygenLevelNormal() {
        return oxygenLevel >= MIN_OXYGEN_LEVEL;
    }
    
    /**
     * Check if blood pressure is within normal range
     * @return true if blood pressure is normal, false otherwise
     */
    public boolean isBloodPressureNormal() {
        return bloodPressure >= MIN_BLOOD_PRESSURE && bloodPressure <= MAX_BLOOD_PRESSURE;
    }
    
    /**
     * Check if temperature is within normal range
     * @return true if temperature is normal, false otherwise
     */
    public boolean isTemperatureNormal() {
        return temperature >= MIN_TEMPERATURE && temperature <= MAX_TEMPERATURE;
    }
    
    // Getters and setters
    
    public double getHeartRate() {
        return heartRate;
    }
    
    public double getOxygenLevel() {
        return oxygenLevel;
    }
    
    public double getBloodPressure() {
        return bloodPressure;
    }
    
    public double getTemperature() {
        return temperature;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }


}
