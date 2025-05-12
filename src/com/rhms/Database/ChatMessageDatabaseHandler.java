package com.rhms.Database;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles database operations for chat messages between users
 */
public class ChatMessageDatabaseHandler {
    private static final Logger LOGGER = Logger.getLogger(ChatMessageDatabaseHandler.class.getName());
    private Connection connection;

    /**
     * Initializes the handler with a database connection
     */
    public ChatMessageDatabaseHandler() {
        this.connection = DatabaseConnection.getConnection();
    }

    /**
     * Saves a new chat message to the database
     *
     * @param senderId ID of the message sender
     * @param receiverId ID of the message receiver
     * @param messageText Content of the message
     * @return ID of the saved message or -1 if operation failed
     */
    public int saveMessage(int senderId, int receiverId, String messageText) {
        if (connection == null) {
            LOGGER.log(Level.SEVERE, "Database connection is null. Cannot save message.");
            return -1;
        }

        String sql = "INSERT INTO chat_messages (sender_id, receiver_id, message_text) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, senderId);
            stmt.setInt(2, receiverId);
            stmt.setString(3, messageText);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                LOGGER.log(Level.WARNING, "Failed to save chat message, no rows affected.");
                return -1;
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    LOGGER.log(Level.WARNING, "Failed to get message ID for saved message.");
                    return -1;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving chat message: " + e.getMessage(), e);
            return -1;
        }
    }

    /**
     * Gets all messages between two users
     *
     * @param user1Id ID of the first user
     * @param user2Id ID of the second user
     * @return List of message maps containing message details
     */
    public List<ChatMessage> getMessagesBetweenUsers(int user1Id, int user2Id) {
        if (connection == null) {
            LOGGER.log(Level.SEVERE, "Database connection is null. Cannot fetch messages.");
            return new ArrayList<>();
        }

        List<ChatMessage> messages = new ArrayList<>();

        String sql = "SELECT * FROM chat_messages WHERE (sender_id = ? AND receiver_id = ?) OR " +
                "(sender_id = ? AND receiver_id = ?) ORDER BY sent_at ASC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, user1Id);
            stmt.setInt(2, user2Id);
            stmt.setInt(3, user2Id);
            stmt.setInt(4, user1Id);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ChatMessage message = new ChatMessage(
                            rs.getInt("message_id"),
                            rs.getInt("sender_id"),
                            rs.getInt("receiver_id"),
                            rs.getString("message_text"),
                            rs.getTimestamp("sent_at").toLocalDateTime(),
                            rs.getBoolean("is_read")
                    );
                    messages.add(message);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching chat messages: " + e.getMessage(), e);
        }

        return messages;
    }

    /**
     * Marks messages as read
     *
     * @param receiverId User ID of the receiver
     * @param senderId User ID of the sender
     * @return Number of messages marked as read
     */
    public int markMessagesAsRead(int receiverId, int senderId) {
        if (connection == null) {
            LOGGER.log(Level.SEVERE, "Database connection is null. Cannot mark messages as read.");
            return 0;
        }

        String sql = "UPDATE chat_messages SET is_read = 1 WHERE receiver_id = ? AND sender_id = ? AND is_read = 0";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, receiverId);
            stmt.setInt(2, senderId);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error marking messages as read: " + e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Gets count of unread messages for a user
     *
     * @param userId ID of the user
     * @return Number of unread messages
     */
    public int getUnreadMessageCount(int userId) {
        if (connection == null) {
            LOGGER.log(Level.SEVERE, "Database connection is null. Cannot get unread message count.");
            return 0;
        }

        String sql = "SELECT COUNT(*) FROM chat_messages WHERE receiver_id = ? AND is_read = 0";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting unread messages: " + e.getMessage(), e);
        }

        return 0;
    }

    /**
     * Gets list of users with whom the given user has conversations
     *
     * @param userId ID of the user
     * @return List of user IDs with conversation history
     */
    public List<Integer> getUserConversations(int userId) {
        if (connection == null) {
            LOGGER.log(Level.SEVERE, "Database connection is null. Cannot get user conversations.");
            return new ArrayList<>();
        }

        List<Integer> conversations = new ArrayList<>();

        String sql = "SELECT DISTINCT sender_id AS other_user_id FROM chat_messages WHERE receiver_id = ? " +
                "UNION " +
                "SELECT DISTINCT receiver_id AS other_user_id FROM chat_messages WHERE sender_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int otherUserId = rs.getInt("other_user_id");
                    if (otherUserId != userId) {  // Avoid adding the user's own ID
                        conversations.add(otherUserId);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching user conversations: " + e.getMessage(), e);
        }

        return conversations;
    }

    /**
     * Class representing a chat message from the database
     */
    public static class ChatMessage {
        private int messageId;
        private int senderId;
        private int receiverId;
        private String messageText;
        private LocalDateTime sentAt;
        private boolean isRead;

        public ChatMessage(int messageId, int senderId, int receiverId, String messageText,
                           LocalDateTime sentAt, boolean isRead) {
            this.messageId = messageId;
            this.senderId = senderId;
            this.receiverId = receiverId;
            this.messageText = messageText;
            this.sentAt = sentAt;
            this.isRead = isRead;
        }

        // Getters
        public int getMessageId() { return messageId; }
        public int getSenderId() { return senderId; }
        public int getReceiverId() { return receiverId; }
        public String getMessageText() { return messageText; }
        public LocalDateTime getSentAt() { return sentAt; }
        public boolean isRead() { return isRead; }
    }
}