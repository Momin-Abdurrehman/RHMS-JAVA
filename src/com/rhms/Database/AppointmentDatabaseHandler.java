package com.rhms.Database;

import com.rhms.appointmentScheduling.Appointment;
import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;
import com.rhms.userManagement.UserManager;
import com.rhms.notifications.EmailNotification;

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

            // --- Send email notifications to patient and doctor ---
            EmailNotification emailNotification = new EmailNotification();
            // Notify patient
            if (appointment.getPatient() != null && appointment.getPatient().getEmail() != null) {
                String subject = "Appointment Scheduled";
                String message = "Dear " + appointment.getPatient().getName() + ",\n\n"
                        + "Your appointment has been scheduled"
                        + (appointment.getDoctor() != null ? " with Dr. " + appointment.getDoctor().getName() : "")
                        + " on " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(appointment.getAppointmentDate())
                        + ".\nStatus: " + appointment.getStatus()
                        + "\nPurpose: " + (appointment.getPurpose() != null ? appointment.getPurpose() : "")
                        + "\n\nThank you,\nRHMS Team";
                emailNotification.sendNotification(appointment.getPatient().getEmail(), subject, message);
            }
            // Notify doctor
            if (appointment.getDoctor() != null && appointment.getDoctor().getEmail() != null) {
                String subject = "New Appointment Assigned";
                String message = "Dear Dr. " + appointment.getDoctor().getName() + ",\n\n"
                        + "You have a new appointment with patient " + appointment.getPatient().getName()
                        + " on " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(appointment.getAppointmentDate())
                        + ".\nStatus: " + appointment.getStatus()
                        + "\nPurpose: " + (appointment.getPurpose() != null ? appointment.getPurpose() : "")
                        + "\n\nThank you,\nRHMS Team";
                emailNotification.sendNotification(appointment.getDoctor().getEmail(), subject, message);
            }
            // --- end email notifications ---

            return appointment;
        }
    }
    
    /**
     * Updates the status of an existing appointment in the database
     * Also updates the updated_at timestamp to current time
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
        
        String sql = "UPDATE appointments SET status = ?, updated_at = CURRENT_TIMESTAMP, " +
                     "notification_sent = false WHERE appointment_id = ?";
                     
        boolean result = false;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, appointmentId);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                LOGGER.log(Level.INFO, "Appointment ID {0} status updated to: {1}", 
                          new Object[]{appointmentId, status});
                result = true;
            } else {
                LOGGER.log(Level.WARNING, "No appointment found with ID: {0}", appointmentId);
                result = false;
            }
        }
        // --- Send email notifications to patient and doctor about status update ---
        if (result && userManager != null) {
            Appointment updatedAppointment = null;
            // Try to fetch updated appointment details
            for (Doctor doctor : userManager.getAllDoctors()) {
                List<Appointment> doctorAppointments = doctor.getAppointments();
                if (doctorAppointments != null) {
                    for (Appointment appt : doctorAppointments) {
                        if (appt.getAppointmentId() == appointmentId) {
                            updatedAppointment = appt;
                            break;
                        }
                    }
                }
                if (updatedAppointment != null) break;
            }
            if (updatedAppointment == null) {
                for (Patient patient : userManager.getAllPatients()) {
                    List<Appointment> patientAppointments = patient.getAppointments();
                    if (patientAppointments != null) {
                        for (Appointment appt : patientAppointments) {
                            if (appt.getAppointmentId() == appointmentId) {
                                updatedAppointment = appt;
                                break;
                            }
                        }
                    }
                    if (updatedAppointment != null) break;
                }
            }
            if (updatedAppointment != null) {
                EmailNotification emailNotification = new EmailNotification();
                // Notify patient
                if (updatedAppointment.getPatient() != null && updatedAppointment.getPatient().getEmail() != null) {
                    String subject = "Appointment Status Updated";
                    String message = "Dear " + updatedAppointment.getPatient().getName() + ",\n\n"
                            + "Your appointment"
                            + (updatedAppointment.getDoctor() != null ? " with Dr. " + updatedAppointment.getDoctor().getName() : "")
                            + " on " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(updatedAppointment.getAppointmentDate())
                            + " has been updated to status: " + status
                            + ".\n\nThank you,\nRHMS Team";
                    emailNotification.sendNotification(updatedAppointment.getPatient().getEmail(), subject, message);
                }
                // Notify doctor
                if (updatedAppointment.getDoctor() != null && updatedAppointment.getDoctor().getEmail() != null) {
                    String subject = "Appointment Status Updated";
                    String message = "Dear Dr. " + updatedAppointment.getDoctor().getName() + ",\n\n"
                            + "The appointment with patient " + updatedAppointment.getPatient().getName()
                            + " on " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(updatedAppointment.getAppointmentDate())
                            + " has been updated to status: " + status
                            + ".\n\nThank you,\nRHMS Team";
                    emailNotification.sendNotification(updatedAppointment.getDoctor().getEmail(), subject, message);
                }
            }
        }
        // --- end email notifications ---
        return result;
    }
    
    /**
     * Updates an appointment status and records who made the change
     * 
     * @param appointmentId The ID of the appointment to update
     * @param status The new status value
     * @param doctorId The ID of the doctor making the change
     * @return true if the update was successful, false otherwise
     * @throws SQLException If there is an error updating the database
     */
    public boolean acceptAppointment(int appointmentId, String status, int doctorId) throws SQLException {
        Connection conn = null;
        boolean autoCommit = true;
        
        try {
            conn = DatabaseConnection.getConnection();
            autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);  // Start transaction
            
            // First, verify appointment is in "Pending" status
            String checkSql = "SELECT status FROM appointments WHERE appointment_id = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, appointmentId);
                ResultSet rs = checkStmt.executeQuery();
                
                if (!rs.next()) {
                    conn.rollback();
                    return false; // Appointment not found
                }
                
                String currentStatus = rs.getString("status");
                if (!"Pending".equalsIgnoreCase(currentStatus)) {
                    conn.rollback();
                    LOGGER.log(Level.WARNING, 
                        "Cannot accept appointment ID {0}: current status is {1}, not Pending", 
                        new Object[]{appointmentId, currentStatus});
                    return false;
                }
            }
            
            // Update the appointment status
            String updateSql = "UPDATE appointments SET " +
                              "status = ?, " + 
                              "updated_at = CURRENT_TIMESTAMP, " +
                              "notification_sent = false, " +  // Reset notification flag
                              "accepted_by = ? " +
                              "WHERE appointment_id = ?";
                         
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, status);
                updateStmt.setInt(2, doctorId);
                updateStmt.setInt(3, appointmentId);
                
                int affectedRows = updateStmt.executeUpdate();
                
                if (affectedRows == 0) {
                    conn.rollback();
                    return false; // No rows affected
                }
            }
            
            // Commit the transaction
            conn.commit();
            LOGGER.log(Level.INFO, "Appointment ID {0} accepted by doctor ID {1}", 
                      new Object[]{appointmentId, doctorId});
            return true;
            
        } catch (SQLException e) {
            // Roll back on error
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error rolling back transaction", ex);
                }
            }
            throw e;
        } finally {
            // Restore auto-commit state
            if (conn != null) {
                try {
                    conn.setAutoCommit(autoCommit);
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error restoring auto-commit state", e);
                }
            }
        }
    }
    
    /**
     * Updates the status of an appointment and logs the change
     * 
     * @param appointmentId The ID of the appointment to update
     * @param status The new status value
     * @param actorId The ID of the user making the change (doctor or patient)
     * @param isDoctor True if the actor is a doctor, false if patient
     * @return true if the update was successful, false otherwise
     * @throws SQLException If there is an error updating the database
     */
    public boolean updateAppointmentStatusWithLog(int appointmentId, String status, int actorId, boolean isDoctor) throws SQLException {
        Connection conn = null;
        PreparedStatement updateStmt = null;
        boolean autoCommit = true;
        
        try {
            conn = DatabaseConnection.getConnection();
            autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);  // Start transaction
            
            // First update the appointment status
            String updateSql = "UPDATE appointments SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE appointment_id = ?";
            updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setString(1, status);
            updateStmt.setInt(2, appointmentId);
            
            int affectedRows = updateStmt.executeUpdate();
            
            if (affectedRows == 0) {
                conn.rollback();
                return false;  // No appointment found with that ID
            }
            
            // Commit the transaction
            conn.commit();
            return true;
            
        } catch (SQLException e) {
            // Roll back transaction on error
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error rolling back transaction", ex);
                }
            }
            throw e;
        } finally {
            // Restore auto-commit state
            if (conn != null) {
                try {
                    conn.setAutoCommit(autoCommit);
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error restoring auto-commit state", e);
                }
            }
            
            // Close resources
            if (updateStmt != null) {
                try {
                    updateStmt.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error closing statement", e);
                }
            }
        }
    }
    
    /**
     * Retrieves all pending appointment requests for a doctor
     * 
     * @param doctorId The ID of the doctor
     * @return A list of pending appointment requests
     * @throws SQLException If there is an error querying the database
     */
    public List<Appointment> getPendingAppointmentRequests(int doctorId) throws SQLException {
        if (userManager == null) {
            throw new IllegalStateException("UserManager not set, cannot resolve patient references");
        }
        
        String sql = "SELECT * FROM appointments WHERE doctor_id = ? AND status = 'Pending' ORDER BY appointment_date";
                     
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, doctorId);
            
            ResultSet rs = stmt.executeQuery();
            
            List<Appointment> pendingRequests = new ArrayList<>();
            Doctor doctor = userManager.getDoctorById(doctorId);
            
            if (doctor == null) {
                LOGGER.log(Level.WARNING, "Doctor with ID {0} not found in UserManager", doctorId);
                return pendingRequests; // Return empty list if doctor not found
            }
            
            while (rs.next()) {
                // Get patient ID from result set
                int patientId = rs.getInt("patient_id");
                Patient patient = userManager.getPatientById(patientId);
                
                if (patient == null) {
                    LOGGER.log(Level.WARNING, "Patient with ID {0} not found for appointment {1}",
                              new Object[]{patientId, rs.getInt("appointment_id")});
                    continue; // Skip this appointment if patient not found
                }
                
                Appointment appointment = createAppointmentFromResultSet(rs, patient);
                if (appointment != null) {
                    // Set doctor explicitly since we already have it
                    appointment.setDoctor(doctor);
                    pendingRequests.add(appointment);
                }
            }
            
            LOGGER.log(Level.INFO, "Loaded {0} pending appointment requests for doctor ID: {1}", 
                      new Object[]{pendingRequests.size(), doctorId});
            return pendingRequests;
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
        
        // Create the Appointment object
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
        
        // Set notification status if the column exists in the result set
        try {
            boolean notificationSent = rs.getBoolean("notification_sent");
            if (notificationSent) {
                appointment.markNotificationSent();
            }
        } catch (SQLException e) {
            // Column might not exist in older database schema, ignore this error
            LOGGER.log(Level.FINE, "notification_sent column not found in result set", e);
        }
        
        return appointment;
    }
    
    /**
     * Updates an appointment's notification status
     * 
     * @param appointmentId The ID of the appointment
     * @param notificationSent Whether a notification has been sent
     * @return true if the update was successful, false otherwise
     * @throws SQLException If there is an error updating the database
     */
    public boolean updateNotificationStatus(int appointmentId, boolean notificationSent) throws SQLException {
        String sql = "UPDATE appointments SET notification_sent = ?, notification_sent_at = ? WHERE appointment_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBoolean(1, notificationSent);
            
            if (notificationSent) {
                stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            } else {
                stmt.setNull(2, Types.TIMESTAMP);
            }
            
            stmt.setInt(3, appointmentId);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                LOGGER.log(Level.INFO, "Appointment ID {0} notification status updated to: {1}", 
                          new Object[]{appointmentId, notificationSent});
                return true;
            } else {
                LOGGER.log(Level.WARNING, "No appointment found with ID: {0}", appointmentId);
                return false;
            }
        }
    }
}

