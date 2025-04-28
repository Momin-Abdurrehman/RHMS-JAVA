package com.rhms.notifications;

// Interface for different types of notifications (email, SMS, etc.)
public interface Notifiable {
    // Sends a notification with recipient, subject and message content
    void sendNotification(String recipient, String subject, String message);
}