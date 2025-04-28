package com.rhms.userManagement;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class Administrator extends User {
    private ArrayList<Doctor> doctors;
    private ArrayList<Patient> patients;
    private ArrayList<String> systemLogs;
    private Map<String, String> systemConfiguration;
    private LocalDateTime lastAuditTime;

    public Administrator(String name, String email, String password, String phone, String address, int userID) {
        super(name, email, password, phone, address, userID);
        this.doctors = new ArrayList<>();
        this.patients = new ArrayList<>();
        this.systemLogs = new ArrayList<>();
        this.systemConfiguration = new HashMap<>();
        this.lastAuditTime = LocalDateTime.now();
    }
    
    // Enhanced constructor with authentication fields
    public Administrator(String name, String email, String password, String phone, String address, 
                        int userID, String username, String passwordHash) {
        super(name, email, password, phone, address, userID, username, passwordHash);
        this.doctors = new ArrayList<>();
        this.patients = new ArrayList<>();
        this.systemLogs = new ArrayList<>();
        this.systemConfiguration = new HashMap<>();
        this.lastAuditTime = LocalDateTime.now();
    }

    // Doctor management
    public void addDoctor(Doctor doctor) {
        doctors.add(doctor);
        logActivity("Added doctor: " + doctor.getName() + " (ID: " + doctor.getUserID() + ")");
    }

    public void removeDoctor(Doctor doctor) {
        doctors.remove(doctor);
        logActivity("Removed doctor: " + doctor.getName() + " (ID: " + doctor.getUserID() + ")");
    }
    
    public ArrayList<Doctor> getDoctors() {
        return doctors;
    }
    
    public Doctor findDoctorById(int doctorId) {
        for (Doctor doctor : doctors) {
            if (doctor.getUserID() == doctorId) {
                return doctor;
            }
        }
        return null;
    }
    
    // Patient management
    public void addPatient(Patient patient) {
        patients.add(patient);
        logActivity("Added patient: " + patient.getName() + " (ID: " + patient.getUserID() + ")");
    }
    
    public void removePatient(Patient patient) {
        patients.remove(patient);
        logActivity("Removed patient: " + patient.getName() + " (ID: " + patient.getUserID() + ")");
    }
    
    public ArrayList<Patient> getPatients() {
        return patients;
    }
    
    public Patient findPatientById(int patientId) {
        for (Patient patient : patients) {
            if (patient.getUserID() == patientId) {
                return patient;
            }
        }
        return null;
    }
    
    // Assign patients to doctors
    public void assignPatientToDoctor(Patient patient, Doctor doctor) {
        doctor.addPatient(patient);
        logActivity("Assigned patient " + patient.getName() + " to Dr. " + doctor.getName());
    }
    
    // System logs and monitoring
    public void viewSystemLogs() {
        System.out.println("=== System Logs ===");
        if (systemLogs.isEmpty()) {
            System.out.println("No logs available.");
        } else {
            for (String log : systemLogs) {
                System.out.println(log);
            }
        }
    }
    
    public void logActivity(String activity) {
        String logEntry = LocalDateTime.now() + " - ADMIN[" + getUserID() + "]: " + activity;
        systemLogs.add(logEntry);
    }
    
    public void clearLogs() {
        int count = systemLogs.size();
        systemLogs.clear();
        System.out.println("Cleared " + count + " log entries.");
        logActivity("System logs cleared");
    }
    
    // System configuration
    public void setSystemConfiguration(String key, String value) {
        systemConfiguration.put(key, value);
        logActivity("Updated system configuration: " + key + " = " + value);
    }
    
    public String getSystemConfiguration(String key) {
        return systemConfiguration.getOrDefault(key, null);
    }
    
    public Map<String, String> getAllConfiguration() {
        return new HashMap<>(systemConfiguration);
    }
    
    // System audit
    public void performSystemAudit() {
        lastAuditTime = LocalDateTime.now();
        logActivity("System audit performed");
        
        // Print audit summary
        System.out.println("=== System Audit Summary ===");
        System.out.println("Registered doctors: " + doctors.size());
        System.out.println("Registered patients: " + patients.size());
        System.out.println("Configuration items: " + systemConfiguration.size());
        System.out.println("Log entries: " + systemLogs.size());
        System.out.println("Audit completed at: " + lastAuditTime);
    }
    
    public LocalDateTime getLastAuditTime() {
        return lastAuditTime;
    }
    
    // Display admin information
    public void displayAdminInfo() {
        System.out.println("=== Administrator Information ===");
        System.out.println("Name: " + getName() + " (ID: " + getUserID() + ")");
        System.out.println("Contact: " + getPhone());
        System.out.println("Email: " + getEmail());
        System.out.println("Managing doctors: " + doctors.size());
        System.out.println("Managing patients: " + patients.size());
        System.out.println("Last system audit: " + lastAuditTime);
    }
}
