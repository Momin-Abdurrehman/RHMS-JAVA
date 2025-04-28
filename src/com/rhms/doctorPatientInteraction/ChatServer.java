package com.rhms.doctorPatientInteraction;

import com.rhms.userManagement.*;
import java.util.*;

// Central server that manages all chat messages between doctors and patients
public class ChatServer {
    // Stores chat messages using a unique chat ID for each conversation pair
    private Map<String, List<String>> chatHistory;
    
    // Initializes a new chat server with empty message history
    public ChatServer() {
        this.chatHistory = new HashMap<>();
    }
    
    // Stores a new message in the chat history between sender and receiver
    public void sendMessage(String sender, String receiver, String message) {
        String chatId = getChatId(sender, receiver);
        // Creates new chat history if none exists, then adds timestamped message
        chatHistory.computeIfAbsent(chatId, k -> new ArrayList<>())
                  .add(String.format("[%s] %s: %s", 
                        new Date(), sender, message));
    }
    
    // Retrieves all messages exchanged between two users
    public List<String> getChatHistory(String user1, String user2) {
        // Returns empty list if no messages exist between users
        return chatHistory.getOrDefault(getChatId(user1, user2), 
                                      new ArrayList<>());
    }
    
    // Creates unique identifier for chat between two users, order-independent
    private String getChatId(String user1, String user2) {
        // Ensures same ID regardless of parameter order
        return user1.compareTo(user2) < 0 
               ? user1 + "_" + user2 
               : user2 + "_" + user1;
    }
}