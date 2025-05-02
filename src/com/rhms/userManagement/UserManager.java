package com.rhms.userManagement;

import com.rhms.Database.UserDatabaseHandler;
import com.rhms.loginSystem.AuthenticationService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages users in the Remote Healthcare Monitoring System
 * Provides functionality for user registration, lookup, and management
 */
public class UserManager {
    private Map<Integer, User> users;
    private List<Doctor> doctors;
    private List<Patient> patients;
    private List<Administrator> administrators;
    private UserDatabaseHandler dbHandler;
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
        this.dbHandler = new UserDatabaseHandler();
    }

    /**
     * Registers a new patient in the system
     */
    public Patient registerPatient(String name, String email, String password, String phone, String address) {
        if (dbHandler.isEmailExists(email)) {
            System.err.println("Error: Email " + email + " already exists in the database.");
            return null;
        }

        int userId = generateUserId();
        String username = generateUsername(name, userId);

        Patient patient = new Patient(name, email, password, phone, address, userId, username);

        if (!dbHandler.addUser(patient)) {
            System.err.println("Error: Failed to add patient to the database.");
            return null;
        }

        patients.add(patient);
        users.put(userId, patient);
        return patient;
    }

    /**
     * Registers a new doctor in the system
     */
    public Doctor registerDoctor(String name, String email, String password, String phone, String address,
                                  String specialization, int experienceYears) {
        if (dbHandler.isEmailExists(email)) {
            System.err.println("Error: Email " + email + " already exists in the database.");
            return null;
        }

        int userId = generateUserId();
        String username = generateUsername(name, userId);

        Doctor doctor = new Doctor(name, email, password, phone, address, userId, username, specialization, experienceYears);

        if (!dbHandler.addUser(doctor)) {
            System.err.println("Error: Failed to add doctor to the database.");
            return null;
        }

        doctors.add(doctor);
        users.put(userId, doctor);
        return doctor;
    }

    /**
     * Registers a new administrator in the system.
     */
    public Administrator registerAdministrator(String name, String email, String password, String phone, String address) {
        if (isEmailExists(email)) {
            System.err.println("Error: Email " + email + " already exists in the database.");
            return null;
        }

        int userId = generateUserId();
        String username = generateUsername(name, userId);

        Administrator admin = new Administrator(name, email, password, phone, address, userId, username);

        if (!dbHandler.addUser(admin)) {
            System.err.println("Error: Failed to add administrator to the database.");
            return null;
        }

        administrators.add(admin);
        users.put(userId, admin);
        return admin;
    }

    /**
     * Checks if an email already exists in the system.
     * @param email The email to check.
     * @return true if the email exists, false otherwise.
     */
    public boolean isEmailExists(String email) {
        return dbHandler.isEmailExists(email);
    }

    /**
     * Synchronizes users from the database into the UserManager's local collections.
     */
    public void syncUsersFromDatabase() {
        List<User> allUsers = dbHandler.getAllUsers();
        users.clear();
        doctors.clear();
        patients.clear();
        administrators.clear();

        for (User user : allUsers) {
            users.put(user.getUserID(), user);
            if (user instanceof Doctor) {
                doctors.add((Doctor) user);
            } else if (user instanceof Patient) {
                patients.add((Patient) user);
            } else if (user instanceof Administrator) {
                administrators.add((Administrator) user);
            }
        }
        System.out.println("Users synchronized from the database.");
    }

    /**
     * Retrieves all administrators in the system.
     */
    public List<Administrator> getAllAdministrators() {
        return new ArrayList<>(administrators);
    }

    /**
     * Retrieves all doctors in the system.
     */
    public List<Doctor> getAllDoctors() {
        return new ArrayList<>(doctors);
    }

    /**
     * Retrieves all patients in the system.
     */
    public List<Patient> getAllPatients() {
        return new ArrayList<>(patients);
    }

    /**
     * Finds a user by their ID.
     */
    public User findUserById(int userId) {
        return users.get(userId);
    }

    /**
     * Finds a user by their email address.
     * @param email The email to search for.
     * @return The User object if found, null otherwise.
     */
    public User findUserByEmail(String email) {
        List<User> allUsers = dbHandler.getAllUsers(); // Fetch all users from the database
        for (User user : allUsers) {
            if (user.getEmail().equalsIgnoreCase(email)) {
                return user;
            }
        }
        return null; // Return null if no user is found with the given email
    }

    /**
     * Generates a unique user ID.
     */
    private int generateUserId() {
        return nextUserId++;
    }

    /**
     * Generates a unique username based on the user's name and ID.
     */
    private String generateUsername(String name, int userId) {
        String[] nameParts = name.split("\\s+");
        String baseUsername = nameParts[0].toLowerCase() + userId;
        String username = baseUsername;

        int counter = 1;
        while (dbHandler.isUsernameExists(username)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }
}
