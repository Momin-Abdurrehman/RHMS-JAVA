package com.rhms.userManagement;

import com.rhms.loginSystem.AuthenticationService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Manages users in the Remote Healthcare Monitoring System
 * Provides functionality for user registration, lookup, and management
 */
public class UserManager {
    private Map<Integer, User> users;
    private List<Doctor> doctors;
    private List<Patient> patients;
    private List<Administrator> administrators;
    
    private AuthenticationService authService;
    private int nextUserId = 1000; // Starting ID for users
    
    /**
     * Initializes the user management system
     */
    public UserManager() {
        this.users = new HashMap<>();
        this.doctors = new ArrayList<>();
        this.patients = new ArrayList<>();
        this.administrators = new ArrayList<>();
        this.authService = new AuthenticationService();
    }
    
    /**
     * Initializes with an existing authentication service
     * @param authService The authentication service to use
     */
    public UserManager(AuthenticationService authService) {
        this();
        this.authService = authService;
    }
    
    /**
     * Registers a new patient in the system
     * @param name Patient's full name
     * @param email Email address
     * @param password Password in plain text (will be hashed)
     * @param phone Phone number
     * @param address Physical address
     * @param emergencyContact Emergency contact information
     * @param healthInsurance Health insurance details
     * @return The created Patient object
     */
    public Patient registerPatient(String name, String email, String password, 
                                  String phone, String address,
                                  String emergencyContact, String healthInsurance) {
        // Generate a unique ID
        int userId = generateUserId();
        
        // Create username and hash the password
        String username = generateUsername(name, userId);
        String passwordHash = hashPassword(password);
        
        // Create the patient
        Patient patient = new Patient(name, email, password, phone, address, userId, 
                                    username, passwordHash, emergencyContact, healthInsurance);
        
        // Register in the collections
        patients.add(patient);
        users.put(userId, patient);
        
        // Register with authentication service
        authService.registerUser(patient);
        
        return patient;
    }
    
    /**
     * Registers a new doctor in the system
     * @param name Doctor's full name
     * @param email Email address
     * @param password Password in plain text (will be hashed)
     * @param phone Phone number
     * @param address Physical address
     * @param specialization Medical specialization
     * @param experienceYears Years of professional experience
     * @return The created Doctor object
     */
    public Doctor registerDoctor(String name, String email, String password,
                               String phone, String address,
                               String specialization, int experienceYears) {
        // Generate a unique ID
        int userId = generateUserId();
        
        // Create username and hash the password
        String username = generateUsername(name, userId);
        String passwordHash = hashPassword(password);
        
        // Create the doctor
        Doctor doctor = new Doctor(name, email, password, phone, address, userId,
                                 username, passwordHash, specialization, experienceYears);
        
        // Register in the collections
        doctors.add(doctor);
        users.put(userId, doctor);
        
        // Register with authentication service
        authService.registerUser(doctor);
        
        return doctor;
    }
    
    /**
     * Registers a new administrator in the system
     * @param name Administrator's full name
     * @param email Email address
     * @param password Password in plain text (will be hashed)
     * @param phone Phone number
     * @param address Physical address
     * @return The created Administrator object
     */
    public Administrator registerAdministrator(String name, String email, String password,
                                             String phone, String address) {
        // Generate a unique ID
        int userId = generateUserId();
        
        // Create username and hash the password
        String username = generateUsername(name, userId);
        String passwordHash = hashPassword(password);
        
        // Create the administrator
        Administrator admin = new Administrator(name, email, password, phone, address, userId,
                                              username, passwordHash);
        
        // Register in the collections
        administrators.add(admin);
        users.put(userId, admin);
        
        // Register with authentication service
        authService.registerUser(admin);
        
        return admin;
    }
    
    /**
     * Find a user by their ID
     * @param userId The user ID to look up
     * @return The User object if found, null otherwise
     */
    public User findUserById(int userId) {
        return users.get(userId);
    }
    
    /**
     * Find a user by their username
     * @param username The username to look up
     * @return The User object if found, null otherwise
     */
    public User findUserByUsername(String username) {
        for (User user : users.values()) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }
    
    /**
     * Find a user by their email address
     * @param email The email to look up
     * @return The User object if found, null otherwise
     */
    public User findUserByEmail(String email) {
        for (User user : users.values()) {
            if (user.getEmail().equals(email)) {
                return user;
            }
        }
        return null;
    }
    
    /**
     * Assign a patient to a doctor
     * @param patientId Patient's ID
     * @param doctorId Doctor's ID
     * @return true if assignment was successful
     */
    public boolean assignPatientToDoctor(int patientId, int doctorId) {
        User userP = findUserById(patientId);
        User userD = findUserById(doctorId);
        
        if (userP instanceof Patient && userD instanceof Doctor) {
            Patient patient = (Patient) userP;
            Doctor doctor = (Doctor) userD;
            doctor.addPatient(patient);
            return true;
        }
        
        return false;
    }
    
    /**
     * Remove a patient from a doctor's care
     * @param patientId Patient's ID
     * @param doctorId Doctor's ID
     * @return true if removal was successful
     */
    public boolean removePatientFromDoctor(int patientId, int doctorId) {
        User userP = findUserById(patientId);
        User userD = findUserById(doctorId);
        
        if (userP instanceof Patient && userD instanceof Doctor) {
            Patient patient = (Patient) userP;
            Doctor doctor = (Doctor) userD;
            doctor.removePatient(patient);
            return true;
        }
        
        return false;
    }
    
    /**
     * Delete a user from the system
     * @param userId ID of the user to delete
     * @return true if user was found and deleted
     */
    public boolean deleteUser(int userId) {
        User user = users.get(userId);
        
        if (user == null) {
            return false;
        }
        
        // Remove from the appropriate list
        if (user instanceof Doctor) {
            doctors.remove(user);
        } else if (user instanceof Patient) {
            patients.remove(user);
        } else if (user instanceof Administrator) {
            administrators.remove(user);
        }
        
        // Remove from main users map
        users.remove(userId);
        
        return true;
    }
    
    /**
     * Get a list of all doctors in the system
     * @return List of doctors
     */
    public List<Doctor> getAllDoctors() {
        return new ArrayList<>(doctors);
    }
    
    /**
     * Get a list of all patients in the system
     * @return List of patients
     */
    public List<Patient> getAllPatients() {
        return new ArrayList<>(patients);
    }
    
    /**
     * Get a list of all administrators in the system
     * @return List of administrators
     */
    public List<Administrator> getAllAdministrators() {
        return new ArrayList<>(administrators);
    }
    
    /**
     * Get all users in the system
     * @return Map of user IDs to User objects
     */
    public Map<Integer, User> getAllUsers() {
        return new HashMap<>(users);
    }
    
    /**
     * Update user information
     * @param userId ID of the user to update
     * @param email New email (null if unchanged)
     * @param phone New phone (null if unchanged)
     * @param address New address (null if unchanged)
     * @return true if user was found and updated
     */
    public boolean updateUserInfo(int userId, String email, String phone, String address) {
        User user = findUserById(userId);
        if (user == null) {
            return false;
        }
        
        if (email != null) {
            user.email = email;
        }
        
        if (phone != null) {
            user.phone = phone;
        }
        
        if (address != null) {
            user.address = address;
        }
        
        return true;
    }
    
    /**
     * Generate a unique user ID
     * @return A new unique user ID
     */
    private synchronized int generateUserId() {
        return nextUserId++;
    }
    
    /**
     * Generate a username based on name and ID
     * @param name User's name
     * @param userId User's ID
     * @return A generated username
     */
    private String generateUsername(String name, int userId) {
        // Create a username from the first part of their name and their ID
        String[] nameParts = name.split("\\s+");
        String firstPart = nameParts[0].toLowerCase();
        
        return firstPart + userId;
    }
    
    /**
     * Hash a password using SHA-256
     * @param password The password to hash
     * @return Hashed password
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple encoding if hashing fails
            System.err.println("Error hashing password: " + e.getMessage());
            return Base64.getEncoder().encodeToString(password.getBytes());
        }
    }
    
    /**
     * Get the authentication service associated with this user manager
     * @return The authentication service
     */
    public AuthenticationService getAuthenticationService() {
        return authService;
    }
    
    /**
     * Print system statistics
     */
    public void printStatistics() {
        System.out.println("===== RHMS User Statistics =====");
        System.out.println("Total users: " + users.size());
        System.out.println("  - Patients: " + patients.size());
        System.out.println("  - Doctors: " + doctors.size());
        System.out.println("  - Administrators: " + administrators.size());
    }
}
