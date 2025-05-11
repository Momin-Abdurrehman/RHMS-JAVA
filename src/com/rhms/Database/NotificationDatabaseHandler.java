package com.rhms.Database;

import com.rhms.notifications.Notification;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles database operations for notifications
 */
public class NotificationDatabaseHandler {
    private static final Logger LOGGER = Logger.getLogger(NotificationDatabaseHandler.class.getName());
    private Connection connection;
    
    public NotificationDatabaseHandler() {
        this.connection = DatabaseConnection.getConnection();
    }
    
    /**
     * Save a notification to the database
     * 
     * @param notification The notification to save
     * @return The saved notification with ID from database
     * @throws SQLException If a database error occurs
     */
    public Notification saveNotification(Notification notification) throws SQLException {
        String sql = "INSERT INTO notifications (recipient_id, title, message, type, is_read, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
                     
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, notification.getRecipientId());
            stmt.setString(2, notification.getTitle());
            stmt.setString(3, notification.getMessage());
            stmt.setString(4, notification.getType());
            stmt.setBoolean(5, notification.isRead());
            stmt.setTimestamp(6, new Timestamp(notification.getCreatedAt().getTime()));
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected == 0) {
                throw new SQLException("Creating notification failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int notificationId = generatedKeys.getInt(1);
                    notification.setNotificationId(notificationId);
                    LOGGER.log(Level.INFO, "Saved notification with ID: {0}", notificationId);
                    return notification;
                } else {
                    throw new SQLException("Creating notification failed, no ID obtained.");
                }
            }
        }
    }
    
    /**
     * Get all notifications for a user
     * 
     * @param userId The ID of the user
     * @return List of notifications for the user
     * @throws SQLException If a database error occurs
     */
    public List<Notification> getNotificationsForUser(int userId) throws SQLException {
        String sql = "SELECT * FROM notifications WHERE recipient_id = ? ORDER BY created_at DESC";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            
            ResultSet rs = stmt.executeQuery();
            List<Notification> notifications = new ArrayList<>();
            
            while (rs.next()) {
                Notification notification = mapResultSetToNotification(rs);
                notifications.add(notification);
            }
            
            LOGGER.log(Level.INFO, "Retrieved {0} notifications for user {1}", 
                      new Object[]{notifications.size(), userId});
            return notifications;
        }
    }
    
    /**
     * Get unread notifications for a user
     * 
     * @param userId The ID of the user
     * @return List of unread notifications for the user
     * @throws SQLException If a database error occurs
     */
    public List<Notification> getUnreadNotificationsForUser(int userId) throws SQLException {
        String sql = "SELECT * FROM notifications WHERE recipient_id = ? AND is_read = false ORDER BY created_at DESC";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            
            ResultSet rs = stmt.executeQuery();
            List<Notification> notifications = new ArrayList<>();
            
            while (rs.next()) {
                Notification notification = mapResultSetToNotification(rs);
                notifications.add(notification);
            }
            
            LOGGER.log(Level.INFO, "Retrieved {0} unread notifications for user {1}", 
                      new Object[]{notifications.size(), userId});
            return notifications;
        }
    }
    
    /**
     * Mark a notification as read in the database
     * 
     * @param notificationId The ID of the notification
     * @return true if successful, false otherwise
     * @throws SQLException If a database error occurs
     */
    public boolean markNotificationAsRead(int notificationId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = true, read_at = CURRENT_TIMESTAMP WHERE notification_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, notificationId);
            
            int rowsAffected = stmt.executeUpdate();
            boolean success = rowsAffected > 0;
            
            if (success) {
                LOGGER.log(Level.INFO, "Marked notification {0} as read", notificationId);
            } else {
                LOGGER.log(Level.WARNING, "Failed to mark notification {0} as read - not found", notificationId);
            }
            
            return success;
        }
    }
    
    /**
     * Delete a notification from the database
     * 
     * @param notificationId The ID of the notification to delete
     * @return true if successful, false otherwise
     * @throws SQLException If a database error occurs
     */
    public boolean deleteNotification(int notificationId) throws SQLException {
        String sql = "DELETE FROM notifications WHERE notification_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, notificationId);
            
            int rowsAffected = stmt.executeUpdate();
            boolean success = rowsAffected > 0;
            
            if (success) {
                LOGGER.log(Level.INFO, "Deleted notification {0}", notificationId);
            } else {
                LOGGER.log(Level.WARNING, "Failed to delete notification {0} - not found", notificationId);
            }
            
            return success;
        }
    }
    
    /**
     * Delete all notifications for a user
     * 
     * @param userId The ID of the user
     * @return Number of notifications deleted
     * @throws SQLException If a database error occurs
     */
    public int deleteAllNotificationsForUser(int userId) throws SQLException {
        String sql = "DELETE FROM notifications WHERE recipient_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            
            int rowsAffected = stmt.executeUpdate();
            LOGGER.log(Level.INFO, "Deleted {0} notifications for user {1}", 
                      new Object[]{rowsAffected, userId});
            
            return rowsAffected;
        }
    }
    
    /**
     * Helper method to map a database row to a Notification object
     */
    private Notification mapResultSetToNotification(ResultSet rs) throws SQLException {
        int notificationId = rs.getInt("notification_id");
        int recipientId = rs.getInt("recipient_id");
        String title = rs.getString("title");
        String message = rs.getString("message");
        String type = rs.getString("type");
        boolean isRead = rs.getBoolean("is_read");
        Date createdAt = new Date(rs.getTimestamp("created_at").getTime());
        
        Timestamp readAtTimestamp = rs.getTimestamp("read_at");
        Date readAt = readAtTimestamp != null ? new Date(readAtTimestamp.getTime()) : null;
        
        return new Notification(notificationId, recipientId, title, message, type, isRead, createdAt, readAt);
    }
    
    /**
     * Check if the notifications table exists, and create it if it doesn't
     * This can be called during application startup to ensure the table exists
     */
    public void ensureNotificationsTableExists() {
        try {
            // Check if table exists
            DatabaseMetaData meta = connection.getMetaData();
            ResultSet tables = meta.getTables(null, null, "notifications", null);
            
            if (!tables.next()) {
                // Table doesn't exist, create it
                String createTableSQL = 
                    "CREATE TABLE notifications (" +
                    "notification_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "recipient_id INT NOT NULL, " +
                    "title VARCHAR(255) NOT NULL, " +
                    "message TEXT NOT NULL, " +
                    "type VARCHAR(50) NOT NULL, " +
                    "is_read BOOLEAN DEFAULT false, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "read_at TIMESTAMP NULL, " +
                    "FOREIGN KEY (recipient_id) REFERENCES users(user_id) ON DELETE CASCADE" +
                    ")";
                
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute(createTableSQL);
                    LOGGER.log(Level.INFO, "Created notifications table");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error ensuring notifications table exists", e);
        }
    }
}
