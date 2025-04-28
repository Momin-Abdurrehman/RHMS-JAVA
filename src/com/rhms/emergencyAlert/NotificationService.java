package com.rhms.emergencyAlert;

import com.rhms.userManagement.Patient;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Handles emergency notifications through multiple channels (email, SMS) and logging
 */
public class NotificationService {
    // Configuration for notification services
    private static final String EMAIL_SERVER = "smtp.hospital.com";
    private static final String SMS_GATEWAY = "sms.hospital.com";

    /**
     * Sends emergency alert through all available channels
     * @param message Alert content to be sent
     * @param patient Patient requiring emergency attention
     */
    public void sendEmergencyAlert(String message, Patient patient) {
        sendEmail(message, patient);
        sendSMS(message, patient);
        logAlert(message);
    }

    /**
     * Sends email notification to patient's registered email
     */
    private void sendEmail(String message, Patient patient) {
        // Simulated email sending via hospital SMTP server
        System.out.println("Sending email alert to: " + patient.getEmail());
        System.out.println("Message: " + message);
    }

    /**
     * Sends SMS notification to patient's registered phone number
     */
    private void sendSMS(String message, Patient patient) {
        // Simulated SMS sending via hospital SMS gateway
        System.out.println("Sending SMS alert to: " + patient.getName());
        System.out.println("Message: " + message);
    }

    /**
     * Records alert details with timestamp for audit purposes
     */
    private void logAlert(String message) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        System.out.println("Alert logged at " + now.format(formatter));
        System.out.println("Alert details: " + message);
    }
}