package com.rhms.doctorPatientInteraction;

import com.rhms.userManagement.*;

// Handles real-time chat functionality between doctors and patients in the system
public class ChatClient {
    // Reference to the user (doctor/patient) who owns this chat client
    private User user;
    // Connection to the central chat server that manages all messages
    private ChatServer chatServer;
    
    // Creates a new chat client for a specific user with server connection
    public ChatClient(User user, ChatServer chatServer) {
        this.user = user;
        this.chatServer = chatServer;
    }
    
    // Routes a message from current user to specified recipient through server
    public void sendMessage(String receiverName, String message) {
        chatServer.sendMessage(user.getName(), receiverName, message);
    }
    
    // Shows all messages exchanged between current user and specified other user
    public void displayChat(String otherUser) {
        System.out.println("\nChat history with " + otherUser + ":");
        // Retrieves and displays each message from the chat history
        for (String message : chatServer.getChatHistory(user.getName(), otherUser)) {
            System.out.println(message);
        }
    }
}