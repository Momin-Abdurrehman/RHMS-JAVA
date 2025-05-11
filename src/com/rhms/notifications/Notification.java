package com.rhms.notifications;

import java.util.Date;

/**
 * Represents a notification in the system that can be sent to users
 */
public class Notification {
    private int notificationId; // Database ID
    private int recipientId;    // User ID who will receive the notification
    private String title;       // Notification title
    private String message;     // Notification content
    private String type;        // Type of notification (e.g., "appointment", "emergency", etc.)
    private boolean isRead;     // Whether the notification has been read
    private Date createdAt;     // When the notification was created
    private Date readAt;        // When the notification was read (null if unread)
    
    // Constructor for creating a new notification (before saving to database)
    public Notification(int recipientId, String title, String message, String type) {
        this.recipientId = recipientId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = false;
        this.createdAt = new Date();
    }
    
    // Constructor for loading from database
    public Notification(int notificationId, int recipientId, String title, String message, 
                       String type, boolean isRead, Date createdAt, Date readAt) {
        this.notificationId = notificationId;
        this.recipientId = recipientId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = isRead;
        this.createdAt = createdAt;
        this.readAt = readAt;
    }
    
    // Getters and setters
    public int getNotificationId() { return notificationId; }
    public void setNotificationId(int notificationId) { this.notificationId = notificationId; }
    
    public int getRecipientId() { return recipientId; }
    public void setRecipientId(int recipientId) { this.recipientId = recipientId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { 
        this.isRead = read;
        if (read && readAt == null) {
            this.readAt = new Date();
        }
    }
    
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    
    public Date getReadAt() { return readAt; }
    public void setReadAt(Date readAt) { this.readAt = readAt; }
    
    /**
     * Mark notification as read and set the read timestamp
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = new Date();
    }
    
    @Override
    public String toString() {
        return title + " - " + message + " [" + (isRead ? "Read" : "Unread") + "]";
    }
}
