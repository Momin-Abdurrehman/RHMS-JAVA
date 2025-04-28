package com.rhms.userManagement;

/**
 * Base class for all users in the Remote Healthcare Monitoring System
 * Provides common user attributes and authentication methods
 */
public class User {
    private String name;
    protected String email;
    private String password;
    protected String phone;
    protected String address;
    private int userID;
    private String username;
    private String passwordHash;
    
    /**
     * Legacy constructor for backward compatibility
     */
    public User(String name, String email, String password, String phone, String address, int userID) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.address = address;
        this.userID = userID;
        this.username = generateDefaultUsername(name, userID);
        this.passwordHash = password; // Insecure - for compatibility only
    }
    
    /**
     * Enhanced constructor with authentication fields
     */
    public User(String name, String email, String password, String phone, String address, 
               int userID, String username, String passwordHash) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.address = address;
        this.userID = userID;
        this.username = username;
        this.passwordHash = passwordHash;
    }
    
    /**
     * Generate a default username from name and ID
     */
    private String generateDefaultUsername(String name, int userID) {
        String[] nameParts = name.split("\\s+");
        String firstPart = nameParts[0].toLowerCase();
        return firstPart + userID;
    }
    
    // Getters and setters
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public int getUserID() {
        return userID;
    }
    
    public void setUserID(int userID) {
        this.userID = userID;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPasswordHash() {
        return passwordHash;
    }
    
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    
    @Override
    public String toString() {
        return "User [name=" + name + ", userID=" + userID + ", username=" + username + "]";
    }
}
