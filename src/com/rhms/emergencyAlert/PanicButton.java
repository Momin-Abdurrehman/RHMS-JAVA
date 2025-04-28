package com.rhms.emergencyAlert;

import com.rhms.userManagement.Patient;
import java.time.LocalDateTime;

/**
 * Manages emergency panic button functionality for patients
 * Allows patients to trigger manual emergency alerts
 */
public class PanicButton {
    // Services and state management
    private NotificationService notificationService;
    private Patient patient;
    private boolean isActive;
    private LocalDateTime lastTriggered;

    /**
     * Creates a new panic button for a specific patient
     * @param patient The patient associated with this panic button
     */
    public PanicButton(Patient patient) {
        this.patient = patient;
        this.notificationService = new NotificationService();
        this.isActive = true;
        this.lastTriggered = null;
    }

    /**
     * Triggers an emergency alert with a specified reason
     * @param reason Description of the emergency situation
     */
    public void triggerAlert(String reason) {
        if (!isActive) {
            System.out.println("Panic button is currently disabled.");
            return;
        }

        lastTriggered = LocalDateTime.now();
        String alertMessage = String.format(
            "EMERGENCY: Manual alert triggered by patient %s\n" +
            "Reason: %s\n" +
            "Time: %s\n" +
            "Location: %s",
            patient.getName(),
            reason,
            lastTriggered.toString(),
            patient.getAddress()
        );

        notificationService.sendEmergencyAlert(alertMessage, patient);
    }

    /**
     * Enables the panic button if currently disabled
     */
    public void enable() {
        if (isActive) {
            System.out.println("Panic button is already enabled for patient: " + patient.getName());
            return;
        }
        isActive = true;
        System.out.println("Panic button enabled for patient: " + patient.getName());
    }

    /**
     * Disables the panic button if currently enabled
     */
    public void disable() {
        if (!isActive) {
            System.out.println("Panic button is already disabled for patient: " + patient.getName());
            return;
        }
        isActive = false;
        System.out.println("Panic button disabled for patient: " + patient.getName());
    }

    /**
     * Returns the timestamp of the last emergency alert
     * @return LocalDateTime of the last trigger, null if never triggered
     */
    public LocalDateTime getLastTriggered() {
        return lastTriggered;
    }

    /**
     * Checks if the panic button is currently active
     * @return true if enabled, false if disabled
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Gets the current status as a readable string
     * @return "enabled" or "disabled"
     */
    public String getStatus() {
        return isActive ? "enabled" : "disabled";
    }
}