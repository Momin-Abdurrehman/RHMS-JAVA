package com.rhms.emergencyAlert;

import com.rhms.healthDataHandling.VitalSign;
import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;
import com.rhms.notifications.EmailNotification;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Monitors patient vital signs and triggers emergency alerts when critical thresholds are exceeded
 * Also handles doctor notifications for abnormal vital signs
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
    private EmailNotification emailNotifier;
    
    // Track recent alerts to prevent duplicates
    private Map<Integer, LocalDateTime> recentAlerts;
    private static final int ALERT_COOLDOWN_MINUTES = 15;

    /**
     * Initializes emergency alert system with notification service
     */
    public EmergencyAlert() {
        this.notificationService = new NotificationService();
        this.emailNotifier = new EmailNotification();
        this.recentAlerts = new HashMap<>();
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
    
    /**
     * Send alert to the patient's assigned doctor(s) about abnormal vital signs
     * @param patient The patient with abnormal vital signs
     * @param vitalSign The abnormal vital sign record
     */
    public void alertDoctorOfAbnormalVitals(Patient patient, VitalSign vitalSign) {
        ArrayList<Doctor> assignedDoctors = patient.getAssignedDoctors();
        
        if (assignedDoctors.isEmpty()) {
            System.out.println("Warning: Patient " + patient.getName() + " has abnormal vitals, but no assigned doctor.");
            return;
        }
        
        // Check if we've sent an alert for this patient recently
        if (isOnCooldown(patient.getUserID())) {
            System.out.println("Alert for patient " + patient.getName() + " is on cooldown, skipping notification.");
            return;
        }
        
        // Send one alert to each assigned doctor
        for (Doctor doctor : assignedDoctors) {
            sendAbnormalVitalsAlert(doctor, patient, vitalSign);
        }
        
        // Record this alert to prevent duplicates
        recentAlerts.put(patient.getUserID(), LocalDateTime.now());
    }
    
    /**
     * Check if an alert for this patient is currently on cooldown
     * @param patientId The patient's user ID
     * @return True if we should skip sending another alert
     */
    private boolean isOnCooldown(int patientId) {
        LocalDateTime lastAlert = recentAlerts.get(patientId);
        if (lastAlert == null) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        return lastAlert.plusMinutes(ALERT_COOLDOWN_MINUTES).isAfter(now);
    }
    
    /**
     * Send email alert to a specific doctor about a patient's abnormal vitals
     * @param doctor The doctor to notify
     * @param patient The patient with abnormal vitals
     * @param vitalSign The abnormal vital sign record
     */
    private void sendAbnormalVitalsAlert(Doctor doctor, Patient patient, VitalSign vitalSign) {
        String subject = "MEDICAL ALERT: Abnormal Vital Signs for Patient " + patient.getName();
        
        StringBuilder messageBody = new StringBuilder();
        messageBody.append("Dear Dr. ").append(doctor.getName()).append(",\n\n");
        messageBody.append("This is an automated alert from the Remote Healthcare Monitoring System.\n\n");
        messageBody.append("Your patient, ").append(patient.getName()).append(" (ID: ").append(patient.getUserID()).append("), ");
        messageBody.append("has recorded abnormal vital signs that require your attention.\n\n");
        
        // Add detailed vital sign information
        messageBody.append(vitalSign.getAbnormalDetails()).append("\n");
        
        // Add patient contact information
        messageBody.append("Patient contact information:\n");
        messageBody.append("- Phone: ").append(patient.getPhone()).append("\n");
        messageBody.append("- Email: ").append(patient.getEmail()).append("\n");
        messageBody.append("- Address: ").append(patient.getAddress()).append("\n\n");
        
        messageBody.append("Please review these results and take appropriate action.\n\n");
        messageBody.append("This is an automated message from RHMS. Please do not reply to this email.");
        
        // Send the email alert to the doctor
        emailNotifier.sendNotification(doctor.getEmail(), subject, messageBody.toString());
        
        // Log the alert
        logAlert(doctor, patient, vitalSign);
    }
    
    /**
     * Log the alert for record-keeping
     * @param doctor The doctor who was notified
     * @param patient The patient with abnormal vitals
     * @param vitalSign The abnormal vital sign record
     */
    private void logAlert(Doctor doctor, Patient patient, VitalSign vitalSign) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        System.out.println("[" + now.format(formatter) + "] ALERT: Doctor " + 
                          doctor.getName() + " notified about abnormal vitals for patient " +
                          patient.getName() + " - " + vitalSign.getVitalStatusSummary());
    }
}
