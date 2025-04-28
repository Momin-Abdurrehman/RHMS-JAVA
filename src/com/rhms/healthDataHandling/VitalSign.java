package com.rhms.healthDataHandling;

/**
 * Represents a patient's vital signs measurement
 * Stores and validates heart rate, oxygen level, blood pressure, and temperature
 */
public class VitalSign {
    // Normal ranges for vital signs
    private static final double MIN_HEART_RATE = 40.0;   // bpm
    private static final double MAX_HEART_RATE = 120.0;  // bpm
    private static final double MIN_OXYGEN = 90.0;       // percentage
    private static final double MIN_BP = 90.0;           // mmHg
    private static final double MAX_BP = 140.0;          // mmHg
    private static final double MIN_TEMP = 35.0;         // °C
    private static final double MAX_TEMP = 40.0;         // °C

    private double heartRate;      // beats per minute
    private double oxygenLevel;    // percentage
    private double bloodPressure;  // mmHg
    private double temperature;    // °C

    /**
     * Creates a new vital signs record with validation
     * @throws IllegalArgumentException if any vital sign is out of normal range
     */
    public VitalSign(double heartRate, double oxygenLevel, double bloodPressure, double temperature) {
        validateHeartRate(heartRate);
        validateOxygenLevel(oxygenLevel);
        validateBloodPressure(bloodPressure);
        validateTemperature(temperature);

        this.heartRate = heartRate;
        this.oxygenLevel = oxygenLevel;
        this.bloodPressure = bloodPressure;
        this.temperature = temperature;
    }

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

    public void setHeartRate(double heartRate) {
        validateHeartRate(heartRate);
        this.heartRate = heartRate;
    }

    public void setOxygenLevel(double oxygenLevel) {
        validateOxygenLevel(oxygenLevel);
        this.oxygenLevel = oxygenLevel;
    }

    public void setBloodPressure(double bloodPressure) {
        validateBloodPressure(bloodPressure);
        this.bloodPressure = bloodPressure;
    }

    public void setTemperature(double temperature) {
        validateTemperature(temperature);
        this.temperature = temperature;
    }

    // Validation methods
    private void validateHeartRate(double heartRate) {
        if (heartRate < MIN_HEART_RATE || heartRate > MAX_HEART_RATE) {
            throw new IllegalArgumentException(
                String.format("Heart rate must be between %.1f and %.1f bpm", 
                MIN_HEART_RATE, MAX_HEART_RATE));
        }
    }

    private void validateOxygenLevel(double oxygenLevel) {
        if (oxygenLevel < MIN_OXYGEN || oxygenLevel > 100.0) {
            throw new IllegalArgumentException(
                String.format("Oxygen level must be between %.1f and 100.0%%", MIN_OXYGEN));
        }
    }

    private void validateBloodPressure(double bloodPressure) {
        if (bloodPressure < MIN_BP || bloodPressure > MAX_BP) {
            throw new IllegalArgumentException(
                String.format("Blood pressure must be between %.1f and %.1f mmHg", 
                MIN_BP, MAX_BP));
        }
    }

    private void validateTemperature(double temperature) {
        if (temperature < MIN_TEMP || temperature > MAX_TEMP) {
            throw new IllegalArgumentException(
                String.format("Temperature must be between %.1f and %.1f °C", 
                MIN_TEMP, MAX_TEMP));
        }
    }

    public void displayVitals() {
        System.out.println("\n=== Vital Signs ===");
        System.out.println("Heart Rate: " + heartRate + " bpm");
        System.out.println("Oxygen Level: " + oxygenLevel + " %");
        System.out.println("Blood Pressure: " + bloodPressure + " mmHg");
        System.out.println("Temperature: " + temperature + " °C");
        System.out.println("=================");
    }
}
