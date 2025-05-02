package com.rhms;

import com.rhms.userManagement.*;
import com.rhms.appointmentScheduling.*;
import com.rhms.doctorPatientInteraction.*;
import com.rhms.emergencyAlert.*;
import com.rhms.notifications.*;
import com.rhms.Database.*;
import com.rhms.healthDataHandling.*;

import java.io.IOException;
import java.util.*;

public class App {
    private static final Scanner scanner = new Scanner(System.in);
    private static User currentUser = null;
    private static final UserManager userManager = new UserManager();
    private static final AppointmentManager appointmentManager = new AppointmentManager();
    private static final ChatServer chatServer = new ChatServer();

    public static void main(String[] args) {
        initializeSystem();
        mainMenu();
    }

    /**
     * Initialize the system by syncing users from the database.
     */
    private static void initializeSystem() {
        System.out.println("Initializing system...");
        userManager.syncUsersFromDatabase();
        createInitialAdminIfNeeded();
        System.out.println("System initialized successfully.");
    }

    /**
     * Main menu for login, registration, and exit.
     */
    private static void mainMenu() {
        while (true) {
            System.out.println("\n===== Main Menu =====");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("0. Exit");
            System.out.print("Choose an option: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    login();
                    break;
                case 2:
                    register();
                    break;
                case 0:
                    System.out.println("Exiting system. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid choice! Please try again.");
                    break;
            }
        }
    }

    /**
     * Login functionality.
     */
    private static void login() {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        UserDatabaseHandler dbHandler = new UserDatabaseHandler();
        User user = dbHandler.getUserByUsername(username);

        if (user != null && user.getPassword().equals(password)) {
            currentUser = user;
            System.out.println("Login successful. Welcome, " + currentUser.getName() + "!");
            userMenu(); // Navigate directly to the user-specific menu
        } else {
            System.out.println("Login failed. Invalid username or password.");
        }
    }

    /**
     * Registration functionality.
     */
    private static void register() {
        System.out.println("\n===== Registration =====");
        System.out.println("1. Register as Patient");
        System.out.println("2. Register as Doctor");
        System.out.println("3. Register as Administrator");
        System.out.println("0. Back");
        System.out.print("Choose an option: ");

        int choice = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        if (choice == 0) return;

        System.out.print("Enter name: ");
        String name = scanner.nextLine();

        System.out.print("Enter email: ");
        String email = scanner.nextLine();

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        System.out.print("Enter phone: ");
        String phone = scanner.nextLine();

        System.out.print("Enter address: ");
        String address = scanner.nextLine();

        switch (choice) {
            case 1:
                registerPatient(name, email, password, phone, address);
                break;
            case 2:
                registerDoctor(name, email, password, phone, address);
                break;
            case 3:
                registerAdministrator(name, email, password, phone, address);
                break;
            default:
                System.out.println("Invalid choice! Please try again.");
                break;
        }

        // Synchronize users from the database after registration
        userManager.syncUsersFromDatabase();
    }

    /**
     * Register a new patient.
     */
    private static void registerPatient(String name, String email, String password, String phone, String address) {
        // Removed emergency contact and insurance info prompts as they were trimmed previously

        Patient patient = userManager.registerPatient(name, email, password, phone, address);
        if (patient != null) {
            System.out.println("Patient registered successfully! Your username is: " + patient.getUsername());
        } else {
            System.out.println("Patient registration failed."); // Error message already printed by UserManager
        }
    }

    /**
     * Register a new doctor.
     */
    private static void registerDoctor(String name, String email, String password, String phone, String address) {
        System.out.print("Enter specialization: ");
        String specialization = scanner.nextLine();

        System.out.print("Enter years of experience: ");
        int experienceYears = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        Doctor doctor = userManager.registerDoctor(name, email, password, phone, address, specialization, experienceYears);
        if (doctor != null) {
            System.out.println("Doctor registered successfully! Your username is: " + doctor.getUsername());
        } else {
            System.out.println("Doctor registration failed."); // Error message already printed by UserManager
        }
    }

    /**
     * Register a new administrator.
     */
    private static void registerAdministrator(String name, String email, String password, String phone, String address) {
        // Removed the check for currentUser instanceof Administrator to allow initial admin creation via registration menu if needed
        // if (!(currentUser instanceof Administrator)) {
        //     System.out.println("Error: Only administrators can register new administrators.");
        //     return;
        // }

        Administrator admin = userManager.registerAdministrator(name, email, password, phone, address);
        if (admin != null) {
            System.out.println("Administrator registered successfully! Your username is: " + admin.getUsername());
        } else {
            System.out.println("Administrator registration failed."); // Error message already printed by UserManager
        }
    }

    /**
     * Menu for logged-in users based on their type.
     */
    private static void userMenu() {
        if (currentUser instanceof Patient) {
            showPatientMenu();
        } else if (currentUser instanceof Doctor) {
            showDoctorMenu();
        } else if (currentUser instanceof Administrator) {
            showAdminMenu();
        } else {
            System.out.println("Error: Unknown user type.");
        }
    }

    /**
     * Menu for patients.
     */
    private static void showPatientMenu() {
        while (true) {
            System.out.println("\n===== Patient Menu =====");
            System.out.println("1. Schedule Appointment");
            System.out.println("2. View Feedback");
            System.out.println("3. Trigger Emergency Alert");
            System.out.println("4. Enable/Disable Panic Button");
            System.out.println("5. Join Video Consultation");
            System.out.println("6. Upload Vital Signs (CSV)");
            System.out.println("7. View Vital Signs History");
            System.out.println("0. Logout");
            System.out.print("Choose an option: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    scheduleAppointment();
                    break;
                case 2:
                    viewFeedback();
                    break;
                case 3:
                    triggerEmergencyAlert();
                    break;
                case 4:
                    togglePanicButton();
                    break;
                case 5:
                    joinVideoConsultation();
                    break;
                case 6:
                    uploadVitalSigns();
                    break;
                case 7:
                    viewVitalSignsHistory();
                    break;
                case 0:
                    logout();
                    return;
                default:
                    System.out.println("Invalid choice! Please try again.");
                    break;
            }
        }
    }

    /**
     * Menu for doctors.
     */
    private static void showDoctorMenu() {
        while (true) {
            System.out.println("\n===== Doctor Menu =====");
            System.out.println("1. Approve Appointment");
            System.out.println("2. View Patient History");
            System.out.println("3. Start Video Consultation");
            System.out.println("4. Open Chat");
            System.out.println("0. Logout");
            System.out.print("Choose an option: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    approveAppointment();
                    break;
                case 2:
                    viewPatientHistory();
                    break;
                case 3:
                    startVideoConsultation();
                    break;
                case 4:
                    openChat();
                    break;
                case 0:
                    logout();
                    return;
                default:
                    System.out.println("Invalid choice! Please try again.");
                    break;
            }
        }
    }

    /**
     * Menu for administrators.
     */
    private static void showAdminMenu() {
        while (true) {
            System.out.println("\n===== Admin Menu =====");
            System.out.println("1. Register Patient");
            System.out.println("2. Register Doctor");
            System.out.println("3. Register Administrator");
            System.out.println("4. View System Logs");
            System.out.println("5. Assign Doctor to Patient");
            System.out.println("0. Logout");
            System.out.print("Choose an option: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    register();
                    break;
                case 2:
                    register();
                    break;
                case 3:
                    register();
                    break;
                case 4:
                    viewSystemLogs();
                    break;
                case 5:
                    assignDoctorToPatient();
                    break;
                case 0:
                    logout();
                    return;
                default:
                    System.out.println("Invalid choice! Please try again.");
                    break;
            }
        }
    }

    /**
     * Assign a doctor to a patient
     */
    private static void assignDoctorToPatient() {
        System.out.println("\n===== Assign Doctor to Patient =====");
        
        // Get list of all doctors and patients
        List<Doctor> doctors = userManager.getAllDoctors();
        List<Patient> patients = userManager.getAllPatients();
        
        // Check if we have both doctors and patients
        if (doctors.isEmpty()) {
            System.out.println("No doctors registered in the system.");
            return;
        }
        
        if (patients.isEmpty()) {
            System.out.println("No patients registered in the system.");
            return;
        }
        
        // Display available doctors
        System.out.println("\nAvailable Doctors:");
        for (int i = 0; i < doctors.size(); i++) {
            Doctor doctor = doctors.get(i);
            System.out.println((i + 1) + ". Dr. " + doctor.getName() + 
                               " (" + doctor.getSpecialization() + ")");
        }
        
        // Select a doctor
        System.out.print("\nSelect a doctor (number): ");
        int doctorIndex = scanner.nextInt() - 1;
        scanner.nextLine(); // Consume newline
        
        if (doctorIndex < 0 || doctorIndex >= doctors.size()) {
            System.out.println("Invalid doctor selection.");
            return;
        }
        
        Doctor selectedDoctor = doctors.get(doctorIndex);
        
        // Display available patients
        System.out.println("\nAvailable Patients:");
        for (int i = 0; i < patients.size(); i++) {
            Patient patient = patients.get(i);
            System.out.println((i + 1) + ". " + patient.getName() + " (ID: " + patient.getUserID() + ")");
        }
        
        // Select a patient
        System.out.print("\nSelect a patient (number): ");
        int patientIndex = scanner.nextInt() - 1;
        scanner.nextLine(); // Consume newline
        
        if (patientIndex < 0 || patientIndex >= patients.size()) {
            System.out.println("Invalid patient selection.");
            return;
        }
        
        Patient selectedPatient = patients.get(patientIndex);
        
        // Assign the doctor to the patient
        selectedPatient.addAssignedDoctor(selectedDoctor);
        System.out.println("Dr. " + selectedDoctor.getName() + " has been assigned to patient " + 
                           selectedPatient.getName() + ".");
    }

    /**
     * Create an initial admin if none exists.
     */
    private static void createInitialAdminIfNeeded() {
        // Check if any administrators exist in the local list first
        if (userManager.getAllAdministrators().isEmpty()) {
            // Then check the database specifically for the default email
            User existingAdmin = userManager.findUserByEmail("admin@rhms.com");
            if (existingAdmin == null) {
                System.out.println("No default administrator found. Creating one...");
                // Use the UserManager registration method which handles database interaction
                Administrator admin = userManager.registerAdministrator(
                    "System Admin", "admin@rhms.com", "admin123", "123-456-7890", "RHMS Headquarters"
                );
                if (admin != null) {
                    System.out.println("Default administrator account created. Username: " + admin.getUsername());
                    // Re-sync after creating the admin to update local lists
                    userManager.syncUsersFromDatabase();
                } else {
                    System.err.println("Error: Failed to create default administrator during initialization.");
                }
            } else {
                System.out.println("Default administrator found in the database.");
                // Ensure local lists are populated even if admin was found only in DB
                userManager.syncUsersFromDatabase();
            }
        } else {
             System.out.println("Administrator(s) already loaded.");
        }
    }

    /**
     * Logout functionality.
     */
    private static void logout() {
        System.out.println("Logging out...");
        currentUser = null;
    }

    // Placeholder methods for functionality
    private static void scheduleAppointment() { System.out.println("Scheduling appointment..."); }
    private static void viewFeedback() { System.out.println("Viewing feedback..."); }
    private static void triggerEmergencyAlert() { System.out.println("Triggering emergency alert..."); }
    private static void togglePanicButton() { System.out.println("Toggling panic button..."); }
    private static void joinVideoConsultation() { System.out.println("Joining video consultation..."); }
    private static void approveAppointment() { System.out.println("Approving appointment..."); }
    private static void viewPatientHistory() { System.out.println("Viewing patient history..."); }
    private static void startVideoConsultation() { System.out.println("Starting video consultation..."); }
    private static void openChat() { System.out.println("Opening chat..."); }
    private static void viewSystemLogs() { System.out.println("Viewing system logs..."); }

    /**
     * Uploads vital signs from a CSV file for the current patient.
     */
    private static void uploadVitalSigns() {
        if (!(currentUser instanceof Patient)) {
            System.out.println("Error: Only patients can upload vital signs.");
            return;
        }
        
        Patient patient = (Patient) currentUser;
        
        System.out.println("\n===== Upload Vital Signs =====");
        System.out.println("Please provide the path to your CSV file.");
        System.out.println("CSV format: [timestamp(optional)], heart rate, oxygen level, temperature, blood pressure");
        System.out.println("Example: 75.0, 98.5, 36.8, 120.0");
        System.out.print("File path: ");
        
        String filePath = scanner.nextLine();
        
        try {
            VitalsUploadReport report = patient.uploadVitalsFromCSVWithReport(filePath);
            System.out.println(report.generateReport());
            
            // Show a summary of the vitals if successful uploads
            if (report.getSuccessCount() > 0) {
                System.out.println("\nWould you like to view your vital signs history? (y/n): ");
                String response = scanner.nextLine().trim().toLowerCase();
                if (response.equals("y") || response.equals("yes")) {
                    viewVitalSignsHistory();
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading the CSV file: " + e.getMessage());
            System.out.println("Please check the file path and try again.");
        }
    }
    
    /**
     * Displays the vital signs history for the current patient.
     */
    private static void viewVitalSignsHistory() {
        if (!(currentUser instanceof Patient)) {
            System.out.println("Error: Only patients can view their vital signs history.");
            return;
        }
        
        Patient patient = (Patient) currentUser;
        System.out.println("\n===== Vital Signs History =====");
        patient.getVitalsDatabase().displayAllVitals();
    }
}
