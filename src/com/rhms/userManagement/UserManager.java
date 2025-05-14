package com.rhms.userManagement;

import com.rhms.Database.AppointmentDatabaseHandler;
import com.rhms.Database.DoctorPatientAssignmentHandler;
import com.rhms.Database.UserDatabaseHandler;
import com.rhms.appointmentScheduling.Appointment;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages users in the Remote Healthcare Monitoring System
 * Provides functionality for user registration, lookup, and management
 */
public class UserManager {
    private static final Logger LOGGER = Logger.getLogger(UserManager.class.getName());
    private Map<Integer, User> users;
    private List<Doctor> doctors;
    private List<Patient> patients;
    private List<Administrator> administrators;
    public UserDatabaseHandler dbHandler;
    private AppointmentDatabaseHandler appointmentDbHandler;
    private DoctorPatientAssignmentHandler assignmentHandler;
    /**
     * Initializes the user management system
     */
    public UserManager() {
        this.users = new HashMap<>();
        this.doctors = new ArrayList<>();
        this.patients = new ArrayList<>();
        this.administrators = new ArrayList<>();
        this.dbHandler = new UserDatabaseHandler();
        this.appointmentDbHandler = new AppointmentDatabaseHandler(this);
        assignmentHandler = new DoctorPatientAssignmentHandler();
        loadUsers();
        loadAllAssignmentsFromDatabase();
    }
    /**
     * Loads all doctor-patient assignments from the database
     */
    public void loadAllAssignmentsFromDatabase() {
        try {
            System.out.println("Starting to load all doctor-patient assignments from database...");

            // Clear existing assignments for a clean reload
            for (Doctor doctor : doctors) {
                doctor.clearPatients();
                System.out.println("Cleared patients for doctor: " + doctor.getName() + " (ID: " + doctor.getUserID() + ")");
            }

            for (Patient patient : patients) {
                patient.clearAssignedDoctors();
                System.out.println("Cleared doctors for patient: " + patient.getName() + " (ID: " + patient.getUserID() + ")");
            }

            // Get all assignments directly
            List<DoctorPatientAssignmentHandler.DoctorPatientAssignment> allAssignments =
                    assignmentHandler.getAllAssignments(dbHandler);

            System.out.println("Retrieved " + allAssignments.size() + " assignments from database");

            // Process all assignments at once
            int successfulAssignments = 0;
            for (DoctorPatientAssignmentHandler.DoctorPatientAssignment assignment : allAssignments) {
                // Get the doctor and patient IDs from the database objects
                int doctorId = assignment.getDoctor().getUserID();
                int patientId = assignment.getPatient().getUserID();

                // Find the matching objects in our in-memory collections
                Doctor doctor = null;
                for (Doctor d : doctors) {
                    if (d.getUserID() == doctorId) {
                        doctor = d;
                        break;
                    }
                }

                Patient patient = null;
                for (Patient p : patients) {
                    if (p.getUserID() == patientId) {
                        patient = p;
                        break;
                    }
                }

                // Only proceed if we found both objects in memory
                if (doctor != null && patient != null) {
                    // Update the in-memory relationship on both sides
                    doctor.addPatient(patient);
                    patient.addAssignedDoctor(doctor);
                    successfulAssignments++;

                    System.out.println("Assigned: Doctor " + doctor.getName() + " (ID: " + doctor.getUserID() +
                            ") to Patient " + patient.getName() + " (ID: " + patient.getUserID() + ")");
                } else {
                    System.out.println("Warning: Could not find doctor ID " + doctorId +
                            " or patient ID " + patientId + " in memory. Assignment not created.");
                }
            }

            // Verify loaded assignments
            int assignmentCount = 0;
            for (Doctor doctor : doctors) {
                assignmentCount += doctor.getAssignedPatients().size();
            }

            System.out.println("Assignment loading complete. Successfully processed " + successfulAssignments +
                    " out of " + allAssignments.size() + " assignments.");
            System.out.println("Total active assignments in memory: " + assignmentCount);

            if (assignmentCount != successfulAssignments) {
                System.out.println("Warning: Discrepancy between processed assignments and counted assignments.");
            }

        } catch (SQLException e) {
            System.err.println("Error loading doctor-patient assignments: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * Registers a new patient in the system
     */
    public Patient registerPatient(String name, String email, String password, String phone, String address, String emergencyContact) {
        if (dbHandler.isEmailExists(email)) {
            System.err.println("Error: Email " + email + " already exists in the database.");
            return null;
        }

        String username = generateUsername(name);

        Patient patient = new Patient(name, email, password, phone, address, 0, username); // userId will be set after DB insert
        patient.setEmergencyContact(emergencyContact);

        boolean dbSuccess = false;
        try {
            dbSuccess = dbHandler.addUser(patient);
            if (!dbSuccess) {
                System.err.println("Error: Failed to add patient to the database.");
                return null;
            }
            // Fetch the user with the generated ID from the database
            Patient dbPatient = dbHandler.getPatientByEmail(email);
            if (dbPatient != null) {
                patient.setUserID(dbPatient.getUserID());
            }
            patients.add(patient);
            users.put(patient.getUserID(), patient);
            return patient;
        } catch (Exception e) {
            System.err.println("Error: Exception during patient registration: " + e.getMessage());
            // Rollback: remove user from DB if added
            if (dbSuccess) {
                dbHandler.deleteUserByEmail(email);
            }
            return null;
        }
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

        String username = generateUsername(name);

        Doctor doctor = new Doctor(name, email, password, phone, address, 0, username, specialization, experienceYears); // userId will be set after DB insert

        boolean dbSuccess = false;
        try {
            dbSuccess = dbHandler.addUser(doctor);
            if (!dbSuccess) {
                System.err.println("Error: Failed to add doctor to the database.");
                return null;
            }
            // Fetch the user with the generated ID from the database
            Doctor dbDoctor = dbHandler.getDoctorByEmail(email);
            if (dbDoctor != null) {
                doctor.setUserID(dbDoctor.getUserID());
            }
            doctors.add(doctor);
            users.put(doctor.getUserID(), doctor);
            return doctor;
        } catch (Exception e) {
            System.err.println("Error: Exception during doctor registration: " + e.getMessage());
            if (dbSuccess) {
                dbHandler.deleteUserByEmail(email);
            }
            return null;
        }
    }

    /**
     * Registers a new administrator in the system.
     */
    public Administrator registerAdministrator(String name, String email, String password, String phone, String address) {
        if (isEmailExists(email)) {
            System.err.println("Error: Email " + email + " already exists in the database.");
            return null;
        }

        String username = generateUsername(name);

        Administrator admin = new Administrator(name, email, password, phone, address, 0, username); // userId will be set after DB insert

        boolean dbSuccess = false;
        try {
            dbSuccess = dbHandler.addUser(admin);
            if (!dbSuccess) {
                System.err.println("Error: Failed to add administrator to the database.");
                return null;
            }
            // Fetch the user with the generated ID from the database
            Administrator dbAdmin = dbHandler.getAdminByEmail(email);
            if (dbAdmin != null) {
                admin.setUserID(dbAdmin.getUserID());
            }
            administrators.add(admin);
            users.put(admin.getUserID(), admin);
            return admin;
        } catch (Exception e) {
            System.err.println("Error: Exception during admin registration: " + e.getMessage());
            if (dbSuccess) {
                dbHandler.deleteUserByEmail(email);
            }
            return null;
        }
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

    public Patient getPatientById(int patientId) {
        for (User user : users.values()) { // Correct iteration
            if (user instanceof Patient && user.getUserID() == patientId) {
                return (Patient) user;
            }
        }
        return null;
    }

    public Doctor getDoctorById(int doctorId) {
        for (User user : users.values()) { // Correct iteration
            if (user instanceof Doctor && user.getUserID() == doctorId) {
                return (Doctor) user;
            }
        }
        return null;
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
     * Generates a unique username based on the user's name.
     */
    private String generateUsername(String name) {
        String[] nameParts = name.split("\\s+");
        String baseUsername = nameParts[0].toLowerCase();
        String username = baseUsername;

        int counter = 1;
        while (dbHandler.isUsernameExists(username)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }

    /**
     * Load appointments for a specific patient from the database
     * @param patient The patient for whom to load appointments
     */
    public void loadAppointmentsForPatient(Patient patient) {
        if (patient == null) {
            LOGGER.log(Level.WARNING, "Cannot load appointments for null patient");
            return;
        }

        try {
            List<Appointment> appointments = appointmentDbHandler.loadAppointmentsForPatient(patient.getUserID());
            patient.setAppointments(appointments);
            LOGGER.log(Level.INFO, "Loaded {0} appointments for patient {1}",
                    new Object[]{appointments.size(), patient.getName()});
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading appointments for patient " + patient.getUserID(), e);
        }
    }

    /**
     * Get the appointment database handler
     * @return The appointment database handler
     */
    public AppointmentDatabaseHandler getAppointmentDbHandler() {
        return appointmentDbHandler;
    }

    /**
     * Assign a doctor to a patient and save to database
     */
    public boolean assignDoctorToPatient(Doctor doctor, Patient patient) {
        try {
            // Save to database first
            boolean success = assignmentHandler.assignDoctorToPatient(doctor, patient);

            if (success) {
                // Update in-memory objects
                doctor.addPatient(patient);
                patient.addAssignedDoctor(doctor);
                return true;
            }
            return false;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error assigning doctor to patient", e);
            return false;
        }
    }

    /**
     * Remove a doctor-patient assignment
     */
    public boolean removeDoctorFromPatient(Doctor doctor, Patient patient) {
        try {
            // Remove from database first
            boolean success = assignmentHandler.removeDoctorFromPatient(doctor, patient);

            if (success) {
                // Update in-memory objects
                doctor.removePatient(patient);
                patient.removeAssignedDoctor(doctor);
                return true;
            }
            return false;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error removing doctor from patient", e);
            return false;
        }
    }

    /**
     * Load doctor-patient assignments when loading users
     */
    private void loadDoctorPatientAssignments() {
        try {
            for (Doctor doctor : doctors) {
                List<Patient> patients = assignmentHandler.getPatientsForDoctor(doctor.getUserID(), dbHandler);
                for (Patient patient : patients) {
                    doctor.addPatient(patient);

                    // Find corresponding patient in our in-memory list and update it
                    for (Patient p : patients) {
                        if (p.getUserID() == patient.getUserID()) {
                            p.addAssignedDoctor(doctor);
                            break;
                        }
                    }
                }
            }

            for (Patient patient : patients) {
                List<Doctor> doctors = assignmentHandler.getAssignedDoctorsForPatient(patient.getUserID(), dbHandler);
                for (Doctor doctor : doctors) {
                    patient.addAssignedDoctor(doctor);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading doctor-patient assignments", e);
        }
    }

    // Update loadUsers() to call loadDoctorPatientAssignments()
    private void loadUsers() {
        List<User> allUsers = dbHandler.getAllUsers();

        administrators = new ArrayList<>();
        doctors = new ArrayList<>();
        patients = new ArrayList<>();

        for (User user : allUsers) {
            if (user instanceof Administrator) {
                administrators.add((Administrator) user);
            } else if (user instanceof Doctor) {
                doctors.add((Doctor) user);
            } else if (user instanceof Patient) {
                patients.add((Patient) user);
            }
        }

        // Load assignments after users are loaded
        loadDoctorPatientAssignments();
    }

    /**
     * Load assignments specifically for one doctor
     * This is used when initializing the doctor dashboard
     */
    public void loadAssignmentsForDoctor(Doctor doctor) {
        try {
            if (doctor == null) {
                LOGGER.log(Level.WARNING, "Cannot load assignments for null doctor");
                return;
            }

            System.out.println("Loading patient assignments specifically for doctor: " + doctor.getName() + " (ID: " + doctor.getUserID() + ")");

            // Clear existing patient assignments for this doctor
            doctor.clearPatients();

            // Get patients assigned to this doctor from the database
            List<Patient> assignedPatients = assignmentHandler.getPatientsForDoctor(doctor.getUserID(), dbHandler);
            System.out.println("Database returned " + assignedPatients.size() + " assigned patients for doctor ID " + doctor.getUserID());

            // For each returned patient, find the matching in-memory patient instance
            for (Patient dbPatient : assignedPatients) {
                // Find the patient in our in-memory collection
                Patient memoryPatient = getPatientById(dbPatient.getUserID());

                if (memoryPatient != null) {
                    // Add the bi-directional relationship
                    doctor.addPatient(memoryPatient);
                    memoryPatient.addAssignedDoctor(doctor);
                    System.out.println("Added doctor-patient assignment: Dr. " + doctor.getName() +
                            " - Patient " + memoryPatient.getName());
                } else {
                    System.out.println("Warning: Could not find patient ID " + dbPatient.getUserID() +
                            " in memory. Assignment not created.");
                }
            }

            System.out.println("Doctor now has " + doctor.getAssignedPatients().size() +
                    " patients in memory after loading");

        } catch (SQLException e) {
            System.err.println("Error loading assignments for doctor: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error loading doctor assignments: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load assignments specifically for one patient
     * This is used when initializing the patient dashboard
     * @param patient The patient whose doctor assignments should be loaded
     */
    public void loadAssignmentsForPatient(Patient patient) {
        try {
            if (patient == null) {
                LOGGER.log(Level.WARNING, "Cannot load assignments for null patient");
                return;
            }

            System.out.println("Loading doctor assignments specifically for patient: " + patient.getName() + " (ID: " + patient.getUserID() + ")");

            // Clear existing doctor assignments for this patient
            patient.clearAssignedDoctors();

            // Get doctors assigned to this patient from the database
            List<Doctor> assignedDoctors = assignmentHandler.getAssignedDoctorsForPatient(patient.getUserID(), dbHandler);
            System.out.println("Database returned " + assignedDoctors.size() + " assigned doctors for patient ID " + patient.getUserID());

            // For each returned doctor, find the matching in-memory doctor instance
            for (Doctor dbDoctor : assignedDoctors) {
                // Find the doctor in our in-memory collection
                Doctor memoryDoctor = getDoctorById(dbDoctor.getUserID());

                if (memoryDoctor != null) {
                    // Add the bi-directional relationship
                    patient.addAssignedDoctor(memoryDoctor);
                    memoryDoctor.addPatient(patient);
                    System.out.println("Added doctor-patient assignment: Dr. " + memoryDoctor.getName() +
                            " - Patient " + patient.getName());
                } else {
                    System.out.println("Warning: Could not find doctor ID " + dbDoctor.getUserID() +
                            " in memory. Assignment not created.");
                }
            }

            System.out.println("Patient now has " + patient.getAssignedDoctors().size() +
                    " doctors in memory after loading");

        } catch (SQLException e) {
            System.err.println("Error loading assignments for patient: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error loading patient assignments: " + e.getMessage());
            e.printStackTrace();
        }
    }



}
