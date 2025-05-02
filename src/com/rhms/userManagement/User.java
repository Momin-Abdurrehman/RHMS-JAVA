package com.rhms.userManagement;

public class User {
    private String name;
    protected String email;
    private String password;
    protected String phone;
    protected String address;
    private int userID;
    private String username;

    public User(String name, String email, String password, String phone, String address, int userID) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.address = address;
        this.userID = userID;
        this.username = generateDefaultUsername(name, userID);
    }

    public User(String name, String email, String password, String phone, String address, 
                int userID, String username) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.address = address;
        this.userID = userID;
        this.username = username;
    }

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

    @Override
    public String toString() {
        return "User [name=" + name + ", userID=" + userID + ", username=" + username + "]";
    }
}
