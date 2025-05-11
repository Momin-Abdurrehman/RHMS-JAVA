package com.rhms.notifications;

import com.rhms.Database.NotificationDatabaseHandler;
import com.rhms.userManagement.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for managing and sending notifications to users
 */
public class NotificationService {
    private static final Logger LOGGER = Logger.getLogger(NotificationService.class.getName());
    private NotificationDatabaseHandler dbHandler;
    
    // In-memory cache of notifications for quick access
    private Map<Integer, List<Notification>> userNotificationsCache;
    
    public NotificationService() {
        this.dbHandler = new NotificationDatabaseHandler();
        this.userNotificationsCache = new HashMap<>();
    }
    
    /**
     * Send a notification to a user
     * 
     * @param notification The notification to send
     * @return true if the notification was successfully saved and sent, false otherwise
     */
    public boolean sendNotification(Notification notification) {
        if (notification == null) {
            LOGGER.log(Level.WARNING, "Cannot send null notification");
            return false;
        }
        
        try {
            // Save notification to database
            Notification savedNotification = dbHandler.saveNotification(notification);
            
            if (savedNotification != null) {
                // Update in-memory cache
                addToUserCache(savedNotification);
                
                LOGGER.log(Level.INFO, "Notification sent successfully: {0}", savedNotification.getTitle());
                return true;
            } else {
                LOGGER.log(Level.WARNING, "Failed to save notification to database");
                return false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error sending notification", e);
            return false;
        }
    }
    
    /**
     * Add notification to user's in-memory cache
     */
    private void addToUserCache(Notification notification) {
        int userId = notification.getRecipientId();
        
        // Get or create user's notification list
        List<Notification> userNotifications = userNotificationsCache.getOrDefault(userId, new ArrayList<>());
        
        // Add notification to list
        userNotifications.add(notification);
        
        // Update cache
        userNotificationsCache.put(userId, userNotifications);
    }
    
    /**
     * Get all notifications for a user
     * 
     * @param userId The ID of the user
     * @return List of all notifications for the user
     */
    public List<Notification> getNotificationsForUser(int userId) {
        try {
            // Load from database and refresh cache
            List<Notification> notifications = dbHandler.getNotificationsForUser(userId);
            
            // Update cache with latest from database
            userNotificationsCache.put(userId, notifications);
            
            return notifications;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching notifications for user " + userId, e);
            
            // Return cached notifications if available, otherwise empty list
            return userNotificationsCache.getOrDefault(userId, new ArrayList<>());
        }
    }
    
    /**
     * Get unread notifications for a user
     * 
     * @param userId The ID of the user
     * @return List of unread notifications for the user
     */
    public List<Notification> getUnreadNotificationsForUser(int userId) {
        List<Notification> allNotifications = getNotificationsForUser(userId);
        List<Notification> unreadNotifications = new ArrayList<>();
        
        for (Notification notification : allNotifications) {
            if (!notification.isRead()) {
                unreadNotifications.add(notification);
            }
        }
        
        return unreadNotifications;
    }
    
    /**
     * Mark a notification as read
     * 
     * @param notificationId The ID of the notification to mark as read
     * @return true if successful, false otherwise
     */
    public boolean markNotificationAsRead(int notificationId) {
        try {
            boolean success = dbHandler.markNotificationAsRead(notificationId);
            
            if (success) {
                // Update cache
                updateNotificationInCache(notificationId, true);
            }
            
            return success;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error marking notification " + notificationId + " as read", e);
            return false;
        }
    }
    
    /**
     * Update a notification's read status in the cache
     */
    private void updateNotificationInCache(int notificationId, boolean isRead) {
        // Iterate through all users in cache
        for (Map.Entry<Integer, List<Notification>> entry : userNotificationsCache.entrySet()) {
            List<Notification> userNotifications = entry.getValue();
            
            // Find and update the notification
            for (Notification notification : userNotifications) {
                if (notification.getNotificationId() == notificationId) {
                    notification.setRead(isRead);
                    break;
                }
            }
        }
    }
}
