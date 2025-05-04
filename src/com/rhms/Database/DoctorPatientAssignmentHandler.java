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
    }

    /**
     * Ensures the doctor_patient_assignments table exists in the database
     */
    public void ensureTableExists() {
        if (connection == null) {
            LOGGER.log(Level.SEVERE, "Database connection is null");
            return;
        }

        String createTableSQL = 
            "CREATE TABLE IF NOT EXISTS doctor_patient_assignments (" +
            "assignment_id INT AUTO_INCREMENT PRIMARY KEY," +
            "doctor_id INT NOT NULL," +
            "patient_id INT NOT NULL," +
            "assigned_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "UNIQUE KEY unique_assignment (doctor_id, patient_id)," +
            "CONSTRAINT fk_assignment_doctor " +
            "   FOREIGN KEY (doctor_id) REFERENCES users(user_id) " +
            "   ON DELETE CASCADE," +
            "CONSTRAINT fk_assignment_patient " +
            "   FOREIGN KEY (patient_id) REFERENCES users(user_id) " +
            "   ON DELETE CASCADE" +
            ")";
            
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating doctor_patient_assignments table: " + e.getMessage(), e);
        }
    }

    public boolean assignDoctorToPatient(Doctor doctor, Patient patient) throws SQLException {
        if (connection == null) {
            LOGGER.log(Level.SEVERE, "Database connection is null. Cannot assign doctor to patient.");
            return false;
        }

        String sql = "INSERT INTO doctor_patient_assignments (doctor_id, patient_id) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, doctor.getUserID());
            stmt.setInt(2, patient.getUserID());
            stmt.executeUpdate();
            // Log success only for new assignments
            LOGGER.log(Level.INFO, "Successfully assigned doctor {0} to patient {1}", new Object[]{doctor.getUserID(), patient.getUserID()});
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            // Duplicate assignment detected, treat as success (already assigned)
            // No need to log INFO here as it's expected behavior for duplicates.
            // The calling code handles the UI feedback.
            return true;
        } catch (SQLException e) {
            // Log actual errors
            LOGGER.log(Level.SEVERE, "Error assigning doctor to patient: " + e.getMessage(), e);
            throw new SQLException("Error assigning doctor to patient: " + e.getMessage(), e);
        }
    }

    public boolean removeDoctorFromPatient(Doctor doctor, Patient patient) throws SQLException {
        if (connection == null) {
            LOGGER.log(Level.SEVERE, "Database connection is null. Cannot remove doctor from patient.");
            return false;
        }

        String sql = "DELETE FROM doctor_patient_assignments WHERE doctor_id = ? AND patient_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, doctor.getUserID());
            stmt.setInt(2, patient.getUserID());
            int result = stmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            throw new SQLException("Error removing doctor from patient: " + e.getMessage(), e);
        }
    }

    public List<Doctor> getDoctorsForPatient(int patientId, UserDatabaseHandler userDbHandler) throws SQLException {
        if (userDbHandler == null) {
            LOGGER.log(Level.SEVERE, "UserDatabaseHandler is null. Cannot load doctors for patient.");
            return new ArrayList<>();
        }

        if (connection == null) {
            LOGGER.log(Level.SEVERE, "Database connection is null. Cannot load doctors for patient.");
            return new ArrayList<>();
        }

        String sql = "SELECT doctor_id FROM doctor_patient_assignments WHERE patient_id = ?";
        List<Doctor> doctors = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, patientId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int doctorId = rs.getInt("doctor_id");
                Doctor doctor = userDbHandler.getDoctorById(doctorId);
                if (doctor != null) {
                    doctors.add(doctor);
                }
            }
            return doctors;
        } catch (SQLException e) {
            throw new SQLException("Error loading doctors for patient: " + e.getMessage(), e);
        }
    }

    public List<Patient> getPatientsForDoctor(int doctorId, UserDatabaseHandler userDbHandler) throws SQLException {
        if (userDbHandler == null) {
            LOGGER.log(Level.SEVERE, "UserDatabaseHandler is null. Cannot load patients for doctor.");
            return new ArrayList<>();
        }

        if (connection == null) {
            LOGGER.log(Level.SEVERE, "Database connection is null. Cannot load patients for doctor.");
            return new ArrayList<>();
        }

        String sql = "SELECT patient_id FROM doctor_patient_assignments WHERE doctor_id = ?";
        List<Patient> patients = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, doctorId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int patientId = rs.getInt("patient_id");
                Patient patient = userDbHandler.getPatientById(patientId);
                if (patient != null) {
                    patients.add(patient);
                }
            }
            return patients;
        } catch (SQLException e) {
            throw new SQLException("Error loading patients for doctor: " + e.getMessage(), e);
        }
    }
}
