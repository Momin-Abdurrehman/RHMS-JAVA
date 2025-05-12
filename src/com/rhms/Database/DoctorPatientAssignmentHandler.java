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
        try {
            this.connection = DatabaseConnection.getConnection();
            if (this.connection == null) {
                LOGGER.log(Level.SEVERE, "Failed to establish database connection in constructor");
                throw new SQLException("Could not establish database connection");
            }
            ensureTableExists();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error initializing DoctorPatientAssignmentHandler: {0}", e.getMessage());
        }
    }
    
    /**
     * Ensures the doctor_patient_assignments table exists in the database
     */
    private void ensureTableExists() {
        if (connection == null) {
            LOGGER.log(Level.SEVERE, "Cannot ensure table exists: Database connection is null");
            return;
        }
        
        try {
            if (connection.isClosed()) {
                LOGGER.log(Level.WARNING, "Database connection is closed, attempting to refresh");
                connection = DatabaseConnection.getConnection();
                if (connection == null || connection.isClosed()) {
                    LOGGER.log(Level.SEVERE, "Failed to refresh database connection");
                    return;
                }
            }
            
            String createTableSQL = 
                "CREATE TABLE IF NOT EXISTS doctor_patient_assignments (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "doctor_id INT NOT NULL," +
                "patient_id INT NOT NULL," +
                "assignment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "UNIQUE(doctor_id, patient_id)" +
                ")";
                
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createTableSQL);
                LOGGER.log(Level.INFO, "doctor_patient_assignments table checked/created successfully");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error ensuring assignment table exists: {0}", e.getMessage());
            LOGGER.log(Level.SEVERE, "SQL State: {0}, Error Code: {1}", 
                      new Object[]{e.getSQLState(), e.getErrorCode()});
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error ensuring table exists: {0}", e.getMessage());
        }
    }

    /**
     * Get a fresh database connection if current one is closed
     * @throws SQLException if connection cannot be established
     */
    private void refreshConnectionIfNeeded() throws SQLException {
        boolean needsRefresh = false;
        
        try {
            if (connection == null || connection.isClosed()) {
                needsRefresh = true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error checking connection status: {0}", e.getMessage());
            needsRefresh = true;
        }
        
        if (needsRefresh) {
            LOGGER.log(Level.INFO, "Refreshing database connection");
            Connection newConnection = DatabaseConnection.getConnection();
            if (newConnection == null) {
                LOGGER.log(Level.SEVERE, "Failed to get new database connection");
                throw new SQLException("Failed to establish database connection");
            }
            connection = newConnection;
        }
    }

    public boolean assignDoctorToPatient(Doctor doctor, Patient patient) throws SQLException {
        if (doctor == null || patient == null) {
            LOGGER.log(Level.SEVERE, "Cannot assign null doctor or patient");
            throw new IllegalArgumentException("Doctor and patient must not be null");
        }
        
        try {
            refreshConnectionIfNeeded();
            
            // Changed from INSERT OR IGNORE INTO (SQLite syntax) to INSERT IGNORE INTO (MySQL syntax)
            String sql = "INSERT IGNORE INTO doctor_patient_assignments (doctor_id, patient_id) VALUES (?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, doctor.getUserID());
                stmt.setInt(2, patient.getUserID());
                int rowsAffected = stmt.executeUpdate();
                LOGGER.log(Level.INFO, "Assignment added: Doctor {0} to Patient {1}, rows affected: {2}", 
                          new Object[]{doctor.getUserID(), patient.getUserID(), rowsAffected});
                return rowsAffected > 0;
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error assigning doctor to patient: {0}", e.getMessage());
                LOGGER.log(Level.SEVERE, "SQL State: {0}, Error Code: {1}",
                          new Object[]{e.getSQLState(), e.getErrorCode()});
                throw e;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in assignDoctorToPatient: {0}", e.getMessage());
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in assignDoctorToPatient: {0}", e.getMessage());
            throw new SQLException("Unexpected error occurred", e);
        }
    }

    public boolean removeDoctorFromPatient(Doctor doctor, Patient patient) throws SQLException {
        if (doctor == null || patient == null) {
            LOGGER.log(Level.SEVERE, "Cannot remove assignment for null doctor or patient");
            throw new IllegalArgumentException("Doctor and patient must not be null");
        }
        
        try {
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
                LOGGER.log(Level.SEVERE, "Error removing doctor from patient: {0}", e.getMessage());
                LOGGER.log(Level.SEVERE, "SQL State: {0}, Error Code: {1}",
                          new Object[]{e.getSQLState(), e.getErrorCode()});
                throw e;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in removeDoctorFromPatient: {0}", e.getMessage());
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in removeDoctorFromPatient: {0}", e.getMessage());
            throw new SQLException("Unexpected error occurred", e);
        }
    }

    public List<Doctor> getAssignedDoctorsForPatient(int patientId, UserDatabaseHandler userDbHandler) throws SQLException {
        if (patientId <= 0) {
            LOGGER.log(Level.SEVERE, "Invalid patient ID: {0}", patientId);
            throw new IllegalArgumentException("Patient ID must be positive");
        }
        
        if (userDbHandler == null) {
            LOGGER.log(Level.SEVERE, "UserDatabaseHandler is null. Cannot load doctors for patient.");
            throw new IllegalArgumentException("UserDatabaseHandler must not be null");
        }

        List<Doctor> doctors = new ArrayList<>();
        try {
            refreshConnectionIfNeeded();
            
            String sql = "SELECT doctor_id FROM doctor_patient_assignments WHERE patient_id = ?";
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
                            LOGGER.log(Level.WARNING, "Doctor with ID {0} not found in database but assigned to patient {1}",
                                      new Object[]{doctorId, patientId});
                        }
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error processing result set for patient {0}: {1}",
                              new Object[]{patientId, e.getMessage()});
                    throw e;
                }

                LOGGER.log(Level.INFO, "Found {0} assigned doctors for patient {1}",
                          new Object[]{doctors.size(), patientId});
                return doctors;
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error executing query for patient {0}: {1}",
                          new Object[]{patientId, e.getMessage()});
                LOGGER.log(Level.SEVERE, "SQL State: {0}, Error Code: {1}",
                          new Object[]{e.getSQLState(), e.getErrorCode()});
                throw e;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in getAssignedDoctorsForPatient: {0}", e.getMessage());
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in getAssignedDoctorsForPatient: {0}", e.getMessage());
            throw new SQLException("Unexpected error occurred", e);
        }
    }

    public List<Patient> getPatientsForDoctor(int doctorId, UserDatabaseHandler userDbHandler) throws SQLException {
        if (doctorId <= 0) {
            LOGGER.log(Level.SEVERE, "Invalid doctor ID: {0}", doctorId);
            throw new IllegalArgumentException("Doctor ID must be positive");
        }

        if (userDbHandler == null) {
            LOGGER.log(Level.SEVERE, "UserDatabaseHandler is null. Cannot load patients for doctor.");
            throw new IllegalArgumentException("UserDatabaseHandler must not be null");
        }

        List<Patient> patients = new ArrayList<>();
        try {
            refreshConnectionIfNeeded();

            String sql = "SELECT patient_id FROM doctor_patient_assignments WHERE doctor_id = ?";
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
                            LOGGER.log(Level.WARNING, "Patient with ID {0} not found in database but assigned to doctor {1}",
                                      new Object[]{patientId, doctorId});
                        }
                    }
                    LOGGER.log(Level.INFO, "Found {0} rows and {1} valid patients for doctor {2}",
                              new Object[]{count, patients.size(), doctorId});
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error processing result set for doctor {0}: {1}",
                              new Object[]{doctorId, e.getMessage()});
                    throw e;
                }

                return patients;
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error executing query for doctor {0}: {1}",
                          new Object[]{doctorId, e.getMessage()});
                LOGGER.log(Level.SEVERE, "SQL State: {0}, Error Code: {1}",
                          new Object[]{e.getSQLState(), e.getErrorCode()});
                throw e;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in getPatientsForDoctor: {0}", e.getMessage());
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in getPatientsForDoctor: {0}", e.getMessage());
            throw new SQLException("Unexpected error occurred", e);
        }
    }

    /**
     * Gets all doctor-patient assignments from the database
     */
    public List<DoctorPatientAssignment> getAllAssignments(UserDatabaseHandler userDbHandler) throws SQLException {
        if (userDbHandler == null) {
            LOGGER.log(Level.SEVERE, "UserDatabaseHandler is null. Cannot load assignments.");
            throw new IllegalArgumentException("UserDatabaseHandler must not be null");
        }

        List<DoctorPatientAssignment> assignments = new ArrayList<>();
        try {
            refreshConnectionIfNeeded();

            String sql = "SELECT doctor_id, patient_id FROM doctor_patient_assignments";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                int count = 0;
                while (rs.next()) {
                    count++;
                    int doctorId = rs.getInt("doctor_id");
                    int patientId = rs.getInt("patient_id");

                    try {
                        Doctor doctor = userDbHandler.getDoctorById(doctorId);
                        Patient patient = userDbHandler.getPatientById(patientId);

                        if (doctor != null && patient != null) {
                            assignments.add(new DoctorPatientAssignment(doctor, patient));
                            LOGGER.log(Level.FINE, "Found assignment: Doctor {0} to Patient {1}",
                                     new Object[]{doctorId, patientId});
                        } else {
                            LOGGER.log(Level.WARNING, "Invalid assignment in database: Doctor {0} to Patient {1} - One or both not found",
                                      new Object[]{doctorId, patientId});
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error processing assignment for Doctor {0}, Patient {1}: {2}",
                                  new Object[]{doctorId, patientId, e.getMessage()});
                    }
                }

                LOGGER.log(Level.INFO, "Loaded {0} total assignments ({1} valid) from database",
                          new Object[]{count, assignments.size()});
                return assignments;
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error executing query for all assignments: {0}", e.getMessage());
                LOGGER.log(Level.SEVERE, "SQL State: {0}, Error Code: {1}",
                          new Object[]{e.getSQLState(), e.getErrorCode()});
                throw e;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in getAllAssignments: {0}", e.getMessage());
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in getAllAssignments: {0}", e.getMessage());
            throw new SQLException("Unexpected error occurred", e);
        }
    }

    /**
     * Simple class to hold doctor-patient assignment data
     */
    public static class DoctorPatientAssignment {
        private Doctor doctor;
        private Patient patient;

        public DoctorPatientAssignment(Doctor doctor, Patient patient) {
            if (doctor == null || patient == null) {
                throw new IllegalArgumentException("Doctor and patient cannot be null");
            }
            this.doctor = doctor;
            this.patient = patient;
        }

        public Doctor getDoctor() { return doctor; }
        public Patient getPatient() { return patient; }
    }
}
