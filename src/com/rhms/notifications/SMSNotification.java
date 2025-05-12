package com.rhms.notifications;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles SMS notifications with placeholder functionality
 * In a real application, this would integrate with an SMS API
 */
public class SMSNotification implements Notifiable {
    private static final Logger LOGGER = Logger.getLogger(SMSNotification.class.getName());
    
    // Placeholder for SMS notification
    @Override
    public boolean sendNotification(String recipient, String subject, String message) {
        try {
            // Input validation
            if (recipient == null || recipient.trim().isEmpty()) {
                LOGGER.log(Level.WARNING, "Cannot send SMS: Empty recipient phone number");
                return false;
            }
            
            if (message == null || message.trim().isEmpty()) {
                LOGGER.log(Level.WARNING, "Cannot send SMS: Empty message");
                return false;
            }
            
            // PLACEHOLDER: In a real app, this would be functional.
            LOGGER.log(Level.INFO, "SMS would be sent to: {0}", recipient);
            LOGGER.log(Level.INFO, "SMS content: {0}", message);
            
            // For development/testing purposes:
            System.out.println("=== SMS Notification (Simulated) ===");
            System.out.println("To: " + recipient);
            System.out.println("Message: " + message);
            System.out.println("============================");
            
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error sending SMS notification: {0}", e.getMessage());
            return false;
        }
    }
}
