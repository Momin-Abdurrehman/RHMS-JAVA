package com.rhms.notifications;

// Handles SMS notifications with formatted display
public class SMSNotification implements Notifiable {
    // Simulates sending SMS by displaying formatted message
    @Override
    public void sendNotification(String recipient, String subject, String message) {
        // Format and display SMS notification details
        System.out.println("\n=== SMS Notification ===");
        System.out.println("To: " + recipient);          // Display recipient phone number
        System.out.println("Subject: " + subject);       // Display message subject
        System.out.println("Message: " + message);       // Display message content
        System.out.println("======================\n");   // Visual separator
    }

    // Helper method to validate phone number format
    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && phoneNumber.matches("\\d{10}|\\d{11}");
    }

    // Overloaded method with phone number validation
    public void sendNotification(String phoneNumber, String message) {
        if (!isValidPhoneNumber(phoneNumber)) {
            System.out.println("Invalid phone number format: " + phoneNumber);
            return;
        }
        sendNotification(phoneNumber, "SMS Alert", message);
    }
}