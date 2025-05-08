package com.rhms.Database;

import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DoctorPatientAssignmentHandler {
    private static final Logger LOGGER = Logger.getLogger(DoctorPatientAssignmentHandler.class.getName());
    private Connection connection;

    public DoctorPatientAssignmentHandler() {
        this.connection = DatabaseConnection.getConnection();
        ensureTableExists();
    }
    
    /**
     * Ensures the doctor_patient_assignments table exists in the database
     */
    private void ensureTableExists() {
        try {
            if (connection == null || connection.isClosed()) {
                LOGGER.log(Level.SEVERE, "Database connection is closed or null when ensuring table exists");
                connection = DatabaseConnection.getConnection();
            }
            
            String createTableSQL = 
                "CREATE TABLE IF NOT EXISTS doctor_patient_assignments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "doctor_id INTEGER NOT NULL," +
                "patient_id INTEGER NOT NULL," +
                "assignment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "UNIQUE(doctor_id, patient_id)" +
                ")";
                
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createTableSQL);
                LOGGER.log(Level.INFO, "doctor_patient_assignments table checked/created successfully");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error ensuring assignment table exists", e);
        }
    }

    /**
     * Get a fresh database connection if current one is closed
     */
    private void refreshConnectionIfNeeded() {
        try {
            if (connection == null || connection.isClosed()) {
                LOGGER.log(Level.INFO, "Refreshing closed database connection");
                connection = DatabaseConnection.getConnection();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking database connection status", e);
            connection = DatabaseConnection.getConnection();
        }
    }

    public boolean assignDoctorToPatient(Doctor doctor, Patient patient) throws SQLException {
        refreshConnectionIfNeeded();
        
        String sql = "INSERT OR IGNORE INTO doctor_patient_assignments (doctor_id, patient_id) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, doctor.getUserID());
            stmt.setInt(2, patient.getUserID());
            int rowsAffected = stmt.executeUpdate();
            LOGGER.log(Level.INFO, "Assignment added: Doctor {0} to Patient {1}, rows affected: {2}", 
                      new Object[]{doctor.getUserID(), patient.getUserID(), rowsAffected});
            return rowsAffected > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error assigning doctor to patient", e);
            throw e;
        }
    }

    public boolean removeDoctorFromPatient(Doctor doctor, Patient patient) throws SQLException {
        refreshConnectionIfNeeded();

        String sql = "DELETE FROM doctor_patient_assignments WHERE doctor_id = ? AND patient_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, doctor.getUserID());
            stmt.setInt(2, patient.getUserID());
            int result = stmt.executeUpdate();
            LOGGER.log(Level.INFO, "Assignment removed: Doctor {0} from Patient {1}, rows affected: {2}",
                      new Object[]{doctor.getUserID(), patient.getUserID(), result});
            return result > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error removing doctor from patient", e);
            throw e;
        }
    }

    public List<Doctor> getAssignedDoctorsForPatient(int patientId, UserDatabaseHandler userDbHandler) throws SQLException {
        refreshConnectionIfNeeded();
        
        if (userDbHandler == null) {
            LOGGER.log(Level.SEVERE, "UserDatabaseHandler is null. Cannot load doctors for patient.");
            return new ArrayList<>();
        }

        String sql = "SELECT doctor_id FROM doctor_patient_assignments WHERE patient_id = ?";
        List<Doctor> doctors = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, patientId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int doctorId = rs.getInt("doctor_id");
                    Doctor doctor = userDbHandler.getDoctorById(doctorId);
                    if (doctor != null) {
                        doctors.add(doctor);
                        LOGGER.log(Level.FINE, "Found doctor {0} assigned to patient {1}", 
                                 new Object[]{doctorId, patientId});
                    } else {
                        LOGGER.log(Level.WARNING, "Doctor with ID {0} not found but assigned to patient {1}",
                                  new Object[]{doctorId, patientId});
                    }
                }
            }
            LOGGER.log(Level.INFO, "Found {0} assigned doctors for patient {1}", 
                      new Object[]{doctors.size(), patientId});
            return doctors;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading doctors for patient " + patientId, e);
            throw e;
        }
    }

    public List<Patient> getPatientsForDoctor(int doctorId, UserDatabaseHandler userDbHandler) throws SQLException {
        refreshConnectionIfNeeded();
        
        if (userDbHandler == null) {
            LOGGER.log(Level.SEVERE, "UserDatabaseHandler is null. Cannot load patients for doctor.");
            return new ArrayList<>();
        }

        String sql = "SELECT patient_id FROM doctor_patient_assignments WHERE doctor_id = ?";
        List<Patient> patients = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, doctorId);
            try (ResultSet rs = stmt.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    int patientId = rs.getInt("patient_id");
                    Patient patient = userDbHandler.getPatientById(patientId);
                    if (patient != null) {
                        patients.add(patient);
                        LOGGER.log(Level.FINE, "Found patient {0} assigned to doctor {1}", 
                                 new Object[]{patientId, doctorId});
                    } else {
                        LOGGER.log(Level.WARNING, "Patient with ID {0} not found but assigned to doctor {1}",
                                  new Object[]{patientId, doctorId});
                    }
                }
                LOGGER.log(Level.INFO, "Found {0} rows and {1} valid patients for doctor {2}", 
                          new Object[]{count, patients.size(), doctorId});
            }
            return patients;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading patients for doctor " + doctorId, e);
            throw e;
        }
    }
    
    /**
     * Gets all doctor-patient assignments from the database
     */
    public List<DoctorPatientAssignment> getAllAssignments(UserDatabaseHandler userDbHandler) throws SQLException {
        refreshConnectionIfNeeded();
        
        if (userDbHandler == null) {
            LOGGER.log(Level.SEVERE, "UserDatabaseHandler is null. Cannot load assignments.");
            return new ArrayList<>();
        }

        String sql = "SELECT doctor_id, patient_id FROM doctor_patient_assignments";
        List<DoctorPatientAssignment> assignments = new ArrayList<>();
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            int count = 0;
            while (rs.next()) {
                count++;
                int doctorId = rs.getInt("doctor_id");
                int patientId = rs.getInt("patient_id");
                
                Doctor doctor = userDbHandler.getDoctorById(doctorId);
                Patient patient = userDbHandler.getPatientById(patientId);
                
                if (doctor != null && patient != null) {
                    assignments.add(new DoctorPatientAssignment(doctor, patient));
                    LOGGER.log(Level.FINE, "Found assignment: Doctor {0} to Patient {1}", 
                             new Object[]{doctorId, patientId});
                } else {
                    LOGGER.log(Level.WARNING, "Invalid assignment in database: Doctor {0} to Patient {1}", 
                              new Object[]{doctorId, patientId});
                }
            }
            
            LOGGER.log(Level.INFO, "Loaded {0} total assignments ({1} valid) from database", 
                      new Object[]{count, assignments.size()});
            return assignments;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading all assignments", e);
            throw e;
        }
    }
    
    /**
     * Simple class to hold doctor-patient assignment data
     */
    public static class DoctorPatientAssignment {
        private Doctor doctor;
        private Patient patient;
        
        public DoctorPatientAssignment(Doctor doctor, Patient patient) {
            this.doctor = doctor;
            this.patient = patient;
        }
        
        public Doctor getDoctor() { return doctor; }
        public Patient getPatient() { return patient; }
    }
}
