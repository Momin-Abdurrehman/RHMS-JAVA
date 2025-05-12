package com.rhms.appointmentScheduling;

import com.rhms.Database.AppointmentDatabaseHandler;
import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized manager for appointment operations, integrating database operations
 * and in-memory appointment management.
 */
public class AppointmentManager {
    private static final Logger LOGGER = Logger.getLogger(AppointmentManager.class.getName());
    private AppointmentDatabaseHandler dbHandler;


    public AppointmentManager(AppointmentDatabaseHandler dbHandler) {
        if (dbHandler == null) {
            LOGGER.log(Level.SEVERE, "Attempted to create AppointmentManager with null database handler");
            throw new IllegalArgumentException("Database handler cannot be null");
        }
        this.dbHandler = dbHandler;
        

        LOGGER.log(Level.INFO, "AppointmentManager initialized successfully with notification support");
    }

    /**
     * Expose the AppointmentDatabaseHandler for external use (e.g., notification status updates).
     */
    public AppointmentDatabaseHandler getDbHandler() {
        return dbHandler;
    }

    /**
     * Schedule a new appointment and save it to the database
     *
     * @param appointment The appointment to schedule
     * @return The scheduled appointment with ID set from database
     * @throws AppointmentException If there's an error scheduling the appointment
     */
    public Appointment scheduleAppointment(Appointment appointment) throws AppointmentException {
        // Validate appointment
        if (appointment == null) {
            LOGGER.log(Level.SEVERE, "Attempted to schedule a null appointment");
            throw new AppointmentException("Cannot schedule a null appointment");
        }
        
        // Validate patient
        Patient patient = appointment.getPatient();
        if (patient == null) {
            LOGGER.log(Level.SEVERE, "Attempted to schedule appointment with null patient");
            throw new AppointmentException("Cannot schedule appointment without a patient");
        }
        
        // Validate doctor
        Doctor doctor = appointment.getDoctor();
        if (doctor == null) {
            LOGGER.log(Level.SEVERE, "Attempted to schedule appointment with null doctor");
            throw new AppointmentException("Cannot schedule appointment without a doctor");
        }
        
        // Validate appointment date
        Date appointmentDate = appointment.getAppointmentDate();
        if (appointmentDate == null) {
            LOGGER.log(Level.SEVERE, "Attempted to schedule appointment with null date");
            throw new AppointmentException("Cannot schedule appointment without a date");
        }
        
        // Check if appointment date is in the past
        Date currentDate = new Date();
        if (appointmentDate.before(currentDate)) {
            LOGGER.log(Level.WARNING, "Attempted to schedule appointment in the past: {0}", appointmentDate);
            throw new AppointmentException("Cannot schedule appointment in the past");
        }

        int patientId = patient.getUserID();
        int doctorId = doctor.getUserID();

        LOGGER.log(Level.INFO, "Attempting to schedule appointment: PatientID={0}, DoctorID={1}, Date={2}, Purpose={3}",
                new Object[]{patientId, doctorId, appointmentDate, appointment.getPurpose()});

        // Validate IDs
        if (patientId <= 0) {
            LOGGER.log(Level.SEVERE, "Invalid PatientID ({0}) provided for appointment scheduling", patientId);
            throw new AppointmentException("Invalid Patient ID provided. Cannot schedule appointment");
        }
        
        if (doctorId <= 0) {
            LOGGER.log(Level.SEVERE, "Invalid DoctorID ({0}) provided for appointment scheduling", doctorId);
            throw new AppointmentException("Invalid Doctor ID provided. Cannot schedule appointment");
        }

        try {
            // Check if database handler is still valid
            if (dbHandler == null) {
                LOGGER.log(Level.SEVERE, "Database handler is null, cannot save appointment");
                throw new AppointmentException("Database connection is not available");
            }
            
            // Save to database
            Appointment savedAppointment = dbHandler.saveAppointment(appointment);
            
            // Validate saved appointment
            if (savedAppointment == null) {
                LOGGER.log(Level.SEVERE, "Failed to save appointment, database returned null");
                throw new AppointmentException("Failed to save appointment: database operation did not return a valid appointment");
            }
            
            if (!savedAppointment.isStoredInDatabase()) {
                LOGGER.log(Level.WARNING, "Appointment saved but not marked as stored in database");
            }

            // Add to patient's appointments in memory
            try {
                patient.addAppointment(savedAppointment);
                LOGGER.log(Level.INFO, "Successfully added appointment {0} to patient {1}'s in-memory list", 
                          new Object[]{savedAppointment.getAppointmentId(), patientId});
            } catch (Exception e) {
                // This shouldn't prevent the method from returning the saved appointment
                // since the database operation was successful
                LOGGER.log(Level.WARNING, "Failed to add appointment to patient's in-memory list: {0}", e.getMessage());
            }

            return savedAppointment;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error scheduling appointment in database for DoctorID=" + doctorId + 
                      ", PatientID=" + patientId, e);
            
            // Check specifically for different types of database errors
            if (e.getMessage() != null) {
                if (e.getMessage().contains("FOREIGN KEY constraint fails")) {
                    LOGGER.log(Level.SEVERE, "Foreign key constraint failed for doctor ID {0} or patient ID {1}", 
                              new Object[]{doctorId, patientId});
                    throw new AppointmentException("Failed to schedule appointment: The selected doctor or patient does not exist in the database", e);
                } else if (e.getMessage().contains("duplicate")) {
                    throw new AppointmentException("Failed to schedule appointment: A similar appointment already exists", e);
                } else if (e.getMessage().contains("Connection")) {
                    throw new AppointmentException("Database connection error: Unable to connect to the appointment database", e);
                }
            }
            
            // Generic database error
            throw new AppointmentException("Failed to schedule appointment: Database error occurred", e);
        } catch (Exception e) {
            // Catch any other unexpected exceptions
            LOGGER.log(Level.SEVERE, "Unexpected error while scheduling appointment", e);
            throw new AppointmentException("An unexpected error occurred while scheduling the appointment: " + e.getMessage(), e);
        }
    }

    /**
     * Update the status of an appointment and send notifications if needed
     *
     * @param appointment The appointment to update
     * @param newStatus The new status value
     * @return true if updated successfully, false otherwise
     * @throws AppointmentException If there's an error updating the appointment
     */
    public boolean updateAppointmentStatus(Appointment appointment, String newStatus)
            throws AppointmentException {
        // Validate parameters
        if (appointment == null) {
            LOGGER.log(Level.SEVERE, "Attempted to update status of null appointment");
            throw new AppointmentException("Cannot update status of null appointment");
        }
        
        if (newStatus == null || newStatus.trim().isEmpty()) {
            LOGGER.log(Level.SEVERE, "Attempted to update appointment status to null or empty string");
            throw new AppointmentException("Appointment status cannot be empty");
        }
        
        // Save old status for notification purposes
        String oldStatus = appointment.getStatus();
        
        // Validate appointment is in database
        if (!appointment.isStoredInDatabase()) {
            LOGGER.log(Level.WARNING, "Cannot update appointment that has not been saved to database: {0}", 
                      appointment.toString());
            throw new AppointmentException("Cannot update appointment that has not been saved to database");
        }
        
        int appointmentId = appointment.getAppointmentId();
        if (appointmentId <= 0) {
            LOGGER.log(Level.SEVERE, "Invalid appointment ID: {0}", appointmentId);
            throw new AppointmentException("Invalid appointment ID");
        }
        
        LOGGER.log(Level.INFO, "Attempting to update appointment {0} status from {1} to {2}", 
                  new Object[]{appointmentId, oldStatus, newStatus});

        try {
            // Check if database handler is still valid
            if (dbHandler == null) {
                LOGGER.log(Level.SEVERE, "Database handler is null, cannot update appointment status");
                throw new AppointmentException("Database connection is not available");
            }
            
            boolean updated = dbHandler.updateAppointmentStatus(appointmentId, newStatus);
            
            if (updated) {
                // Update in-memory object
                try {
                    appointment.setStatus(newStatus);
                    LOGGER.log(Level.INFO, "Successfully updated appointment {0} status to {1}", 
                              new Object[]{appointmentId, newStatus});
                    
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to update in-memory appointment status: {0}", e.getMessage());
                    // The database was updated successfully, so we still return true
                }
            } else {
                LOGGER.log(Level.WARNING, "Failed to update appointment {0} status, no rows affected", appointmentId);
            }
            
            return updated;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating appointment status in database for ID=" + appointmentId, e);
            
            // Check for specific database errors
            if (e.getMessage() != null) {
                if (e.getMessage().contains("not found") || e.getMessage().contains("does not exist")) {
                    throw new AppointmentException("Failed to update status: Appointment not found in database", e);
                } else if (e.getMessage().contains("Connection")) {
                    throw new AppointmentException("Database connection error: Unable to connect to the appointment database", e);
                }
            }
            
            // Generic database error
            throw new AppointmentException("Failed to update appointment status: Database error occurred", e);
        } catch (Exception e) {
            // Catch any other unexpected exceptions
            LOGGER.log(Level.SEVERE, "Unexpected error while updating appointment status", e);
            throw new AppointmentException("An unexpected error occurred while updating the appointment status: " + e.getMessage(), e);
        }
        
        // This line should not be reached if the exceptions are properly thrown
    }

    /**
     * Accept an appointment request from a patient
     * Updates the status to "Confirmed" and sends a notification to the patient
     * 
     * @param appointment The appointment to accept
     * @return true if accepted successfully, false otherwise
     * @throws AppointmentException If there's an error accepting the appointment
     */
    public boolean acceptAppointmentRequest(Appointment appointment) throws AppointmentException {
        // Validate appointment
        if (appointment == null) {
            LOGGER.log(Level.SEVERE, "Attempted to accept null appointment");
            throw new AppointmentException("Cannot accept null appointment");
        }
        
        // Verify appointment is in "Pending" status
        if (!"Pending".equals(appointment.getStatus())) {
            LOGGER.log(Level.WARNING, "Cannot accept appointment that is not in Pending status: {0}", 
                      appointment.getStatus());
            throw new AppointmentException("Only pending appointments can be accepted");
        }
        
        // Verify appointment has a doctor
        Doctor doctor = appointment.getDoctor();
        if (doctor == null) {
            LOGGER.log(Level.SEVERE, "Cannot accept appointment with null doctor");
            throw new AppointmentException("Appointment doctor cannot be null");
        }
        
        int appointmentId = appointment.getAppointmentId();
        int doctorId = doctor.getUserID();
        
        LOGGER.log(Level.INFO, "Doctor {0} accepting appointment {1}", 
                  new Object[]{doctorId, appointmentId});
        
        try {
            // Use special database method for accepting (could include additional tracking)
            boolean accepted = dbHandler.acceptAppointment(appointmentId, "Confirmed", doctorId);
            
            if (accepted) {
                // Update in-memory status
                String oldStatus = appointment.getStatus();
                appointment.setStatus("Confirmed");
                

                
                LOGGER.log(Level.INFO, "Successfully accepted appointment {0}", appointmentId);
                return true;
            } else {
                LOGGER.log(Level.WARNING, "Failed to accept appointment {0} in database", appointmentId);
                return false;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error accepting appointment " + appointmentId, e);
            throw new AppointmentException("Database error while accepting appointment: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error accepting appointment " + appointmentId, e);
            throw new AppointmentException("Unexpected error while accepting appointment: " + e.getMessage(), e);
        }
    }

    /**
     * Load all appointments for a specific patient
     *
     * @param patient The patient whose appointments to load
     * @return List of appointments for the patient
     * @throws AppointmentException If there's an error loading appointments
     */
    public List<Appointment> loadPatientAppointments(Patient patient) throws AppointmentException {
        // Validate patient
        if (patient == null) {
            LOGGER.log(Level.SEVERE, "Attempted to load appointments for null patient");
            throw new AppointmentException("Cannot load appointments for null patient");
        }
        
        int patientId = patient.getUserID();
        if (patientId <= 0) {
            LOGGER.log(Level.SEVERE, "Invalid patient ID: {0}", patientId);
            throw new AppointmentException("Invalid patient ID");
        }
        
        LOGGER.log(Level.INFO, "Loading appointments for patient ID {0}", patientId);

        try {
            // Check if database handler is still valid
            if (dbHandler == null) {
                LOGGER.log(Level.SEVERE, "Database handler is null, cannot load patient appointments");
                throw new AppointmentException("Database connection is not available");
            }
            
            List<Appointment> appointments = dbHandler.loadAppointmentsForPatient(patientId);
            
            if (appointments == null) {
                LOGGER.log(Level.WARNING, "Database returned null appointment list for patient {0}", patientId);
                // Return empty list instead of null
                return new ArrayList<>();
            }
            
            LOGGER.log(Level.INFO, "Successfully loaded {0} appointments for patient {1}", 
                      new Object[]{appointments.size(), patientId});
            
            // Ensure appointments are associated with the patient
            for (Appointment appointment : appointments) {
                appointment.setPatient(patient);
            }
            
            return appointments;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading patient appointments from database for ID=" + patientId, e);
            
            // Check for specific database errors
            if (e.getMessage() != null && e.getMessage().contains("Connection")) {
                throw new AppointmentException("Database connection error: Unable to connect to the appointment database", e);
            }
            
            // Generic database error
            throw new AppointmentException("Failed to load appointments: Database error occurred", e);
        } catch (Exception e) {
            // Catch any other unexpected exceptions
            LOGGER.log(Level.SEVERE, "Unexpected error while loading patient appointments", e);
            throw new AppointmentException("An unexpected error occurred while loading the appointments: " + e.getMessage(), e);
        }
    }

    /**
     * Load all appointments for a specific doctor
     *
     * @param doctor The doctor whose appointments to load
     * @return List of appointments for the doctor
     * @throws AppointmentException If there's an error loading appointments
     */
    public List<Appointment> loadDoctorAppointments(Doctor doctor) throws AppointmentException {
        // Validate doctor
        if (doctor == null) {
            LOGGER.log(Level.SEVERE, "Attempted to load appointments for null doctor");
            throw new AppointmentException("Cannot load appointments for null doctor");
        }
        
        int doctorId = doctor.getUserID();
        if (doctorId <= 0) {
            LOGGER.log(Level.SEVERE, "Invalid doctor ID: {0}", doctorId);
            throw new AppointmentException("Invalid doctor ID");
        }
        
        LOGGER.log(Level.INFO, "Loading appointments for doctor ID {0}", doctorId);

        try {
            // Check if database handler is still valid
            if (dbHandler == null) {
                LOGGER.log(Level.SEVERE, "Database handler is null, cannot load doctor appointments");
                throw new AppointmentException("Database connection is not available");
            }
            
            List<Appointment> appointments = dbHandler.loadAppointmentsForDoctor(doctorId);
            
            if (appointments == null) {
                LOGGER.log(Level.WARNING, "Database returned null appointment list for doctor {0}", doctorId);
                // Return empty list instead of null
                return new ArrayList<>();
            }
            
            LOGGER.log(Level.INFO, "Successfully loaded {0} appointments for doctor {1}", 
                      new Object[]{appointments.size(), doctorId});
            
            // Ensure appointments are associated with the doctor
            for (Appointment appointment : appointments) {
                appointment.setDoctor(doctor);
            }
            
            return appointments;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading doctor appointments from database for ID=" + doctorId, e);
            
            // Check for specific database errors
            if (e.getMessage() != null && e.getMessage().contains("Connection")) {
                throw new AppointmentException("Database connection error: Unable to connect to the appointment database", e);
            }
            
            // Generic database error
            throw new AppointmentException("Failed to load appointments: Database error occurred", e);
        } catch (Exception e) {
            // Catch any other unexpected exceptions
            LOGGER.log(Level.SEVERE, "Unexpected error while loading doctor appointments", e);
            throw new AppointmentException("An unexpected error occurred while loading the appointments: " + e.getMessage(), e);
        }
    }
    
    /**
     * Cancel an appointment
     * 
     * @param appointmentId The ID of the appointment to cancel
     * @return true if cancelled successfully, false otherwise
     * @throws AppointmentException If there's an error cancelling the appointment
     */
    public boolean cancelAppointment(int appointmentId) throws AppointmentException {
        // Validate appointment ID
        if (appointmentId <= 0) {
            LOGGER.log(Level.SEVERE, "Invalid appointment ID: {0}", appointmentId);
            throw new AppointmentException("Invalid appointment ID");
        }
        
        LOGGER.log(Level.INFO, "Attempting to cancel appointment ID {0}", appointmentId);

        try {
            // Check if database handler is still valid
            if (dbHandler == null) {
                LOGGER.log(Level.SEVERE, "Database handler is null, cannot cancel appointment");
                throw new AppointmentException("Database connection is not available");
            }
            
            boolean cancelled = dbHandler.updateAppointmentStatus(appointmentId, "Cancelled");
            
            if (cancelled) {
                LOGGER.log(Level.INFO, "Successfully cancelled appointment {0}", appointmentId);
            } else {
                LOGGER.log(Level.WARNING, "Failed to cancel appointment {0}, no rows affected", appointmentId);
            }
            
            return cancelled;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error cancelling appointment in database for ID=" + appointmentId, e);
            
            // Check for specific database errors
            if (e.getMessage() != null) {
                if (e.getMessage().contains("not found") || e.getMessage().contains("does not exist")) {
                    throw new AppointmentException("Failed to cancel appointment: Appointment not found in database", e);
                } else if (e.getMessage().contains("Connection")) {
                    throw new AppointmentException("Database connection error: Unable to connect to the appointment database", e);
                }
            }
            
            // Generic database error
            throw new AppointmentException("Failed to cancel appointment: Database error occurred", e);
        } catch (Exception e) {
            // Catch any other unexpected exceptions
            LOGGER.log(Level.SEVERE, "Unexpected error while cancelling appointment", e);
            throw new AppointmentException("An unexpected error occurred while cancelling the appointment: " + e.getMessage(), e);
        }
    }

    /**
     * Custom exception for appointment operations
     */
    public static class AppointmentException extends Exception {
        public AppointmentException(String message) {
            super(message);
        }

        public AppointmentException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
