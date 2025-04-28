package com.rhms.emergencyAlert;

import com.rhms.healthDataHandling.VitalSign;
import com.rhms.userManagement.Patient;

/**
 * Monitors patient vital signs and triggers emergency alerts when critical thresholds are exceeded
 */
public class EmergencyAlert {
    // Critical threshold values for vital signs
    private static final double MAX_HEART_RATE = 120.0;    // Maximum safe heart rate in bpm
    private static final double MIN_HEART_RATE = 40.0;     // Minimum safe heart rate in bpm
    private static final double MIN_OXYGEN_LEVEL = 90.0;   // Minimum safe oxygen saturation %
    private static final double MAX_BLOOD_PRESSURE = 140.0; // Maximum safe systolic pressure
    private static final double MAX_TEMPERATURE = 39.0;     // Maximum safe temperature in °C

    // Service for sending emergency notifications
    private NotificationService notificationService;

    /**
     * Initializes emergency alert system with notification service
     */
    public EmergencyAlert() {
        this.notificationService = new NotificationService();
    }

    /**
     * Checks patient vitals against critical thresholds and sends alerts if needed
     * @param patient The patient whose vitals are being monitored
     * @param vitals Current vital sign readings for the patient
     */
    public void checkVitals(Patient patient, VitalSign vitals) {
        StringBuilder alertMessage = new StringBuilder();
        boolean isEmergency = false;

        // Check heart rate (bradycardia and tachycardia)
        if (vitals.getHeartRate() > MAX_HEART_RATE || vitals.getHeartRate() < MIN_HEART_RATE) {
            alertMessage.append("CRITICAL: Abnormal heart rate detected: ").append(vitals.getHeartRate()).append(" bpm\n");
            isEmergency = true;
        }

        // Check oxygen saturation (hypoxemia)
        if (vitals.getOxygenLevel() < MIN_OXYGEN_LEVEL) {
            alertMessage.append("CRITICAL: Low oxygen level detected: ").append(vitals.getOxygenLevel()).append("%\n");
            isEmergency = true;
        }

        // Check blood pressure (hypertension)
        if (vitals.getBloodPressure() > MAX_BLOOD_PRESSURE) {
            alertMessage.append("CRITICAL: High blood pressure detected: ").append(vitals.getBloodPressure()).append(" mmHg\n");
            isEmergency = true;
        }

        // Check temperature (hyperthermia)
        if (vitals.getTemperature() > MAX_TEMPERATURE) {
            alertMessage.append("CRITICAL: High temperature detected: ").append(vitals.getTemperature()).append("°C\n");
            isEmergency = true;
        }

        // Send emergency alert if any vital sign is critical
        if (isEmergency) {
            String alert = String.format("Emergency Alert for patient %s:\n%s", 
                patient.getName(), alertMessage.toString());
            notificationService.sendEmergencyAlert(alert, patient);
        }
    }
}