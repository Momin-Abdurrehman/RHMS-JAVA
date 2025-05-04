package com.rhms.Database;

import com.rhms.appointmentScheduling.Appointment;
import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;
import com.rhms.userManagement.UserManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class handles database operations for the appointments table.
 */
public class AppointmentDatabaseHandler {
    private Connection connection;
    private UserManager userManager;
    private static final Logger LOGGER = Logger.getLogger(AppointmentDatabaseHandler.class.getName());

    /**
     * Constructor that initializes database connection
     */
    public AppointmentDatabaseHandler() {
        this.connection = DatabaseConnection.getConnection();
    }
    
    /**
     * Constructor with UserManager for resolving doctor and patient references
     * 
     * @param userManager The UserManager instance for retrieving doctor and patient objects
     */
    public AppointmentDatabaseHandler(UserManager userManager) {
        this.connection = DatabaseConnection.getConnection();
        this.userManager = userManager;
    }
    
    /**
     * Sets the UserManager for this handler
     * 
     * @param userManager The UserManager instance
     */
    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }
    
    /**
     * Saves a new appointment to the database
     * 
     * @param appointment The appointment to save
     * @return The saved appointment with its ID set from the database
     * @throws SQLException If there is an error saving to the database
     */
    public Appointment saveAppointment(Appointment appointment) throws SQLException {
        if (appointment == null || appointment.getPatient() == null) {
            throw new IllegalArgumentException("Cannot save null appointment or appointment without patient");
        }
        
        String sql = "INSERT INTO appointments (doctor_id, patient_id, appointment_date, status, purpose, notes) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
                     
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // Set doctor_id (can be null if not assigned yet)
            if (appointment.getDoctor() != null) {
                stmt.setInt(1, appointment.getDoctor().getUserID());
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            
            // Set patient_id
            stmt.setInt(2, appointment.getPatient().getUserID());
            
            // Set appointment_date
            stmt.setTimestamp(3, new Timestamp(appointment.getAppointmentDate().getTime()));
            
            // Set status
            stmt.setString(4, appointment.getStatus());
            
            // Set purpose (can be null)
            if (appointment.getPurpose() != null && !appointment.getPurpose().isEmpty()) {
                stmt.setString(5, appointment.getPurpose());
            } else {
                stmt.setNull(5, Types.VARCHAR);
            }
            
            // Set notes (can be null)
            if (appointment.getNotes() != null && !appointment.getNotes().isEmpty()) {
                stmt.setString(6, appointment.getNotes());
            } else {
                stmt.setNull(6, Types.VARCHAR);
            }
            
            // Execute the insert
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Creating appointment failed, no rows affected.");
            }
            
            // Get the generated ID
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int appointmentId = generatedKeys.getInt(1);
                    appointment.setAppointmentId(appointmentId);
                    
                    // Set created_at timestamp from database into the object
                    sql = "SELECT created_at FROM appointments WHERE appointment_id = ?";
                    try (PreparedStatement timestampStmt = connection.prepareStatement(sql)) {
                        timestampStmt.setInt(1, appointmentId);
                        ResultSet rs = timestampStmt.executeQuery();
                        if (rs.next()) {
                            appointment.setCreatedAt(rs.getTimestamp("created_at"));
                        }
                    }
                } else {
                    throw new SQLException("Creating appointment failed, no ID obtained.");
                }
            }
            
            System.out.println("Appointment saved successfully with ID: " + appointment.getAppointmentId());
            return appointment;
        }
    }
    
    /**
     * Updates the status of an existing appointment in the database
     * 
     * @param appointmentId The ID of the appointment to update
     * @param status The new status value
     * @return true if the update was successful, false otherwise
     * @throws SQLException If there is an error updating the database
     */
    public boolean updateAppointmentStatus(int appointmentId, String status) throws SQLException {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }
        
        String sql = "UPDATE appointments SET status = ? WHERE appointment_id = ?";
                     
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, appointmentId);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                System.out.println("Appointment ID " + appointmentId + " status updated to: " + status);
                return true;
            } else {
                System.out.println("No appointment found with ID: " + appointmentId);
                return false;
            }
        }
    }
    
    /**
     * Retrieves all appointments for a specific patient
     * 
     * @param patientId The ID of the patient
     * @return A list of appointments for the patient
     * @throws SQLException If there is an error querying the database
     */
    public List<Appointment> loadAppointmentsForPatient(int patientId) throws SQLException {
        if (userManager == null) {
            throw new IllegalStateException("UserManager not set, cannot resolve doctor references");
        }
        
        String sql = "SELECT * FROM appointments WHERE patient_id = ? ORDER BY appointment_date";
                     
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, patientId);
            
            ResultSet rs = stmt.executeQuery();
            
            List<Appointment> appointments = new ArrayList<>();
            Patient patient = userManager.getPatientById(patientId);
            
            if (patient == null) {
                System.err.println("Warning: Patient with ID " + patientId + " not found in UserManager");
                return appointments; // Return empty list if patient not found
            }
            
            while (rs.next()) {
                Appointment appointment = createAppointmentFromResultSet(rs, patient);
                if (appointment != null) {
                    appointments.add(appointment);
                }
            }
            
            System.out.println("Loaded " + appointments.size() + " appointments for patient ID: " + patientId);
            return appointments;
        }
    }

    public Appointment addAppointment(Appointment appointment) throws SQLException {
        String sql = "INSERT INTO appointment (appointment_date, doctor_id, patient_id, status, purpose, notes, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setTimestamp(1, new java.sql.Timestamp(appointment.getAppointmentDate().getTime()));
            stmt.setInt(2, appointment.getDoctor().getUserID());
            stmt.setInt(3, appointment.getPatient().getUserID());
            stmt.setString(4, appointment.getStatus());
            stmt.setString(5, appointment.getPurpose());
            stmt.setString(6, appointment.getNotes());
            stmt.setTimestamp(7, new java.sql.Timestamp(appointment.getCreatedAt().getTime()));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating appointment failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    appointment.setAppointmentId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating appointment failed, no ID obtained.");
                }
            }
            return appointment;
        }
    }


    /**
     * Retrieves all appointments for a specific doctor
     * 
     * @param doctorId The ID of the doctor
     * @return A list of appointments for the doctor
     * @throws SQLException If there is an error querying the database
     */
    public List<Appointment> loadAppointmentsForDoctor(int doctorId) throws SQLException {
        if (userManager == null) {
            throw new IllegalStateException("UserManager not set, cannot resolve patient references");
        }
        
        String sql = "SELECT * FROM appointments WHERE doctor_id = ? ORDER BY appointment_date";
                     
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, doctorId);
            
            ResultSet rs = stmt.executeQuery();
            
            List<Appointment> appointments = new ArrayList<>();
            Doctor doctor = userManager.getDoctorById(doctorId);
            
            if (doctor == null) {
                System.err.println("Warning: Doctor with ID " + doctorId + " not found in UserManager");
                return appointments; // Return empty list if doctor not found
            }
            
            while (rs.next()) {
                // Get patient ID from result set
                int patientId = rs.getInt("patient_id");
                Patient patient = userManager.getPatientById(patientId);
                
                if (patient == null) {
                    System.err.println("Warning: Patient with ID " + patientId + " not found for appointment " + 
                                       rs.getInt("appointment_id"));
                    continue; // Skip this appointment if patient not found
                }
                
                Appointment appointment = createAppointmentFromResultSet(rs, patient);
                if (appointment != null) {
                    // Set doctor explicitly since we already have it
                    appointment.setDoctor(doctor);
                    appointments.add(appointment);
                }
            }
            
            System.out.println("Loaded " + appointments.size() + " appointments for doctor ID: " + doctorId);
            return appointments;
        }
    }
    
    /**
     * Helper method to create an Appointment object from a ResultSet row
     * 
     * @param rs The ResultSet positioned at the row to read
     * @param patient The Patient object for this appointment (already resolved)
     * @return A new Appointment object with data from the ResultSet
     * @throws SQLException If there is an error reading from the ResultSet
     */
    private Appointment createAppointmentFromResultSet(ResultSet rs, Patient patient) throws SQLException {
        // Get the appointment ID
        int appointmentId = rs.getInt("appointment_id");
        
        // Get the doctor ID
        int doctorId = rs.getInt("doctor_id");
        Doctor doctor = null;
        
        if (!rs.wasNull() && userManager != null) {
            doctor = userManager.getDoctorById(doctorId);
        }
        
        // Get other appointment details
        Timestamp appointmentDate = rs.getTimestamp("appointment_date");
        String status = rs.getString("status");
        String purpose = rs.getString("purpose");
        String notes = rs.getString("notes");
        Timestamp createdAt = rs.getTimestamp("created_at");
        
        // Create and return the Appointment object
        Appointment appointment = new Appointment(
            appointmentId,
            appointmentDate,
            patient,
            doctor,
            purpose != null ? purpose : "",
            status,
            notes != null ? notes : "",
            createdAt
        );
        
        return appointment;
    }
}
