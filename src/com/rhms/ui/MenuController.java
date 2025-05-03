package com.rhms.ui;

import com.rhms.userManagement.*;
import com.rhms.healthDataHandling.VitalsUploadReport;
import com.rhms.appointmentScheduling.Appointment;
import java.util.Scanner;
import java.util.List;
import java.io.IOException;

public class MenuController {
    private static final Scanner scanner = new Scanner(System.in);
    private final UserManager userManager;
    private User currentUser;

    public MenuController(UserManager userManager) {
        this.userManager = userManager;
        this.currentUser = null;
    }

    public void showMainMenu() {
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
                    handleLogin();
                    break;
                case 2:
                    handleRegistration();
                    break;
                case 0:
                    System.out.println("Thank you for using RHMS. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid choice! Please try again.");
                    break;
            }
        }
    }

    private void handleLogin() {
        LoginHandler loginHandler = new LoginHandler(userManager);
        currentUser = loginHandler.login(scanner);
        if (currentUser != null) {
            showUserMenu();
        }
    }

    private void handleRegistration() {
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
                registerAdmin(name, email, password, phone, address);
                break;
            default:
                System.out.println("Invalid choice!");
                break;
        }
    }

    private void showUserMenu() {
        if (currentUser instanceof Patient) {
            showPatientMenu((Patient) currentUser);
        } else if (currentUser instanceof Doctor) {
            showDoctorMenu((Doctor) currentUser);
        } else if (currentUser instanceof Administrator) {
            showAdminMenu((Administrator) currentUser);
        }
        currentUser = null; // Log out after menu completes
    }

    private void showPatientMenu(Patient patient) {
        while (true) {
            System.out.println("\n===== Patient Menu =====");
            System.out.println("1. Schedule Appointment");
            System.out.println("2. View Feedback");
            System.out.println("3. Trigger Emergency Alert");
            System.out.println("4. Enable/Disable Panic Button");
            System.out.println("5. Upload Vital Signs (CSV)");
            System.out.println("6. View Vital Signs History");
            System.out.println("0. Logout");
            System.out.print("Choose an option: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    handleScheduleAppointment(patient);
                    break;
                case 2:
                    patient.viewDoctorFeedback();
                    break;
                case 3:
                    handleEmergencyAlert(patient);
                    break;
                case 4:
                    togglePanicButton(patient);
                    break;
                case 5:
                    handleVitalsUpload(patient);
                    break;
                case 6:
                    patient.getVitalsDatabase().displayAllVitals();
                    break;
                case 0:
                    return;
                default:
                    System.out.println("Invalid choice!");
                    break;
            }
        }
    }

    private void showDoctorMenu(Doctor doctor) {
        while (true) {
            System.out.println("\n===== Doctor Menu =====");
            System.out.println("1. View Assigned Patients");
            System.out.println("2. View Patient History");
            System.out.println("3. Provide Feedback");
            System.out.println("4. Manage Appointments");
            System.out.println("0. Logout");
            System.out.print("Choose an option: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    doctor.viewAllPatients();
                    break;
                case 2:
                    handleViewPatientHistory(doctor);
                    break;
                case 3:
                    handleProvideFeedback(doctor);
                    break;
                case 4:
                    handleManageAppointments(doctor);
                    break;
                case 0:
                    return;
                default:
                    System.out.println("Invalid choice!");
                    break;
            }
        }
    }

    private void showAdminMenu(Administrator admin) {
        while (true) {
            System.out.println("\n===== Admin Menu =====");
            System.out.println("1. Register New User");
            System.out.println("2. View All Users");
            System.out.println("3. Assign Doctor to Patient");
            System.out.println("4. View System Logs");
            System.out.println("0. Logout");
            System.out.print("Choose an option: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    handleRegistration();
                    break;
                case 2:
                    displayAllUsers();
                    break;
                case 3:
                    handleDoctorPatientAssignment();
                    break;
                case 4:
                    admin.viewSystemLogs();
                    break;
                case 0:
                    return;
                default:
                    System.out.println("Invalid choice!");
                    break;
            }
        }
    }

    // Helper methods for menu operations
    private void handleScheduleAppointment(Patient patient) {
        // Implementation for scheduling appointments
    }

    private void handleEmergencyAlert(Patient patient) {
        if (!patient.getPanicButton().isActive()) {
            System.out.println("Error: Panic button is currently disabled. Please enable it first.");
            return;
        }

        System.out.print("Enter reason for emergency: ");
        String reason = scanner.nextLine().trim();
        if (reason.isEmpty()) {
            System.out.println("Error: Emergency reason cannot be empty.");
            return;
        }

        patient.getPanicButton().triggerAlert(reason);
    }

    private void togglePanicButton(Patient patient) {
        System.out.println("Current status: " + patient.getPanicButton().getStatus());
        System.out.print("Do you want to " + 
                        (patient.getPanicButton().isActive() ? "disable" : "enable") + 
                        " the panic button? (y/n): ");
        
        String choice = scanner.nextLine().trim().toLowerCase();
        if (choice.equals("y")) {
            if (patient.getPanicButton().isActive()) {
                patient.getPanicButton().disable();
            } else {
                patient.getPanicButton().enable();
            }
        }
    }

    private void handleVitalsUpload(Patient patient) {
        System.out.println("\nUpload Vital Signs from CSV");
        System.out.print("Enter CSV file path: ");
        String filePath = scanner.nextLine();

        try {
            VitalsUploadReport report = patient.uploadVitalsFromCSVWithReport(filePath);
            System.out.println(report.toString());
        } catch (IOException e) {
            System.out.println("Error uploading vitals: " + e.getMessage());
        }
    }

    private void handleViewPatientHistory(Doctor doctor) {
        List<Patient> patients = doctor.getAssignedPatients();
        if (patients.isEmpty()) {
            System.out.println("No patients assigned.");
            return;
        }

        System.out.println("\nSelect a patient:");
        for (int i = 0; i < patients.size(); i++) {
            System.out.println((i + 1) + ". " + patients.get(i).getName());
        }

        System.out.print("Enter patient number: ");
        int choice = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        if (choice > 0 && choice <= patients.size()) {
            doctor.viewPatientHistory(patients.get(choice - 1));
        } else {
            System.out.println("Invalid choice!");
        }
    }

    private void handleProvideFeedback(Doctor doctor) {
        // Implementation for providing feedback
    }

    private void handleManageAppointments(Doctor doctor) {
        // Implementation for managing appointments
    }

    private void registerPatient(String name, String email, String password, String phone, String address) {
        Patient patient = userManager.registerPatient(name, email, password, phone, address);
        if (patient != null) {
            System.out.println("Patient registered successfully! Username: " + patient.getUsername());
        }
    }

    private void registerDoctor(String name, String email, String password, String phone, String address) {
        System.out.print("Enter specialization: ");
        String specialization = scanner.nextLine();

        System.out.print("Enter years of experience: ");
        int experienceYears = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        Doctor doctor = userManager.registerDoctor(name, email, password, phone, address, 
                                                specialization, experienceYears);
        if (doctor != null) {
            System.out.println("Doctor registered successfully! Username: " + doctor.getUsername());
        }
    }

    private void registerAdmin(String name, String email, String password, String phone, String address) {
        Administrator admin = userManager.registerAdministrator(name, email, password, phone, address);
        if (admin != null) {
            System.out.println("Administrator registered successfully! Username: " + admin.getUsername());
        }
    }

    private void displayAllUsers() {
        System.out.println("\n=== All Users ===");
        System.out.println("\nDoctors:");
        for (Doctor doctor : userManager.getAllDoctors()) {
            System.out.println("- Dr. " + doctor.getName() + " (" + doctor.getSpecialization() + ")");
        }

        System.out.println("\nPatients:");
        for (Patient patient : userManager.getAllPatients()) {
            System.out.println("- " + patient.getName());
        }

        System.out.println("\nAdministrators:");
        for (Administrator admin : userManager.getAllAdministrators()) {
            System.out.println("- " + admin.getName());
        }
    }

    private void handleDoctorPatientAssignment() {
        List<Doctor> doctors = userManager.getAllDoctors();
        List<Patient> patients = userManager.getAllPatients();

        if (doctors.isEmpty() || patients.isEmpty()) {
            System.out.println("Not enough users to perform assignment.");
            return;
        }

        System.out.println("\nAvailable Doctors:");
        for (int i = 0; i < doctors.size(); i++) {
            System.out.println((i + 1) + ". Dr. " + doctors.get(i).getName());
        }

        System.out.print("Select doctor (number): ");
        int doctorChoice = scanner.nextInt() - 1;
        scanner.nextLine(); // Consume newline

        if (doctorChoice < 0 || doctorChoice >= doctors.size()) {
            System.out.println("Invalid doctor selection!");
            return;
        }

        System.out.println("\nAvailable Patients:");
        for (int i = 0; i < patients.size(); i++) {
            System.out.println((i + 1) + ". " + patients.get(i).getName());
        }

        System.out.print("Select patient (number): ");
        int patientChoice = scanner.nextInt() - 1;
        scanner.nextLine(); // Consume newline

        if (patientChoice < 0 || patientChoice >= patients.size()) {
            System.out.println("Invalid patient selection!");
            return;
        }

        Doctor selectedDoctor = doctors.get(doctorChoice);
        Patient selectedPatient = patients.get(patientChoice);
        selectedPatient.addAssignedDoctor(selectedDoctor);
        System.out.println("Successfully assigned Dr. " + selectedDoctor.getName() + 
                         " to patient " + selectedPatient.getName());
    }
}
