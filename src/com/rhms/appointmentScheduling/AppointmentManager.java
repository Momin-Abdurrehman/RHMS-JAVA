package com.rhms.appointmentScheduling;

import com.rhms.Database.AppointmentDatabaseHandler;
import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;

import java.sql.SQLException;
import java.util.ArrayList;
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
        this.dbHandler = dbHandler;
    }

    /**
     * Schedule a new appointment and save it to the database
     *
     * @param appointment The appointment to schedule
     * @return The scheduled appointment with ID set from database
     * @throws AppointmentException If there's an error scheduling the appointment
     */
    public Appointment scheduleAppointment(Appointment appointment) throws AppointmentException {
        // --- Add Detailed Logging ---
        if (appointment == null) {
            LOGGER.log(Level.SEVERE, "Attempted to schedule a null appointment.");
            throw new AppointmentException("Cannot schedule a null appointment.");
        }
        Patient patient = appointment.getPatient();
        Doctor doctor = appointment.getDoctor();
        int patientId = (patient != null) ? patient.getUserID() : -1;
        int doctorId = (doctor != null) ? doctor.getUserID() : -1; // Get the doctor ID

        LOGGER.log(Level.INFO, "Attempting to schedule appointment: PatientID={0}, DoctorID={1}, Date={2}, Purpose={3}",
                new Object[]{patientId, doctorId, appointment.getAppointmentDate(), appointment.getPurpose()});

        // Validate Doctor ID before proceeding
        if (doctorId <= 0) {
             LOGGER.log(Level.SEVERE, "Invalid DoctorID ({0}) provided for appointment scheduling. Doctor object: {1}", new Object[]{doctorId, doctor});
             throw new AppointmentException("Invalid Doctor ID provided. Cannot schedule appointment.");
        }
        // --- End Detailed Logging ---

        try {
            // Save to database
            Appointment savedAppointment = dbHandler.saveAppointment(appointment);

            // Add to patient's appointments in memory
            if (patient != null && savedAppointment != null && savedAppointment.isStoredInDatabase()) {
                patient.addAppointment(savedAppointment);
                 LOGGER.log(Level.INFO, "Successfully added appointment {0} to patient {1}'s in-memory list.", new Object[]{savedAppointment.getAppointmentId(), patientId});
            } else if (savedAppointment == null || !savedAppointment.isStoredInDatabase()) {
                 LOGGER.log(Level.WARNING, "Appointment was potentially saved but failed to get a valid ID back, or patient object was null. PatientID={0}", patientId);
            }


            return savedAppointment;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error scheduling appointment in database for DoctorID=" + doctorId + ", PatientID=" + patientId, e);
            // Check specifically for foreign key constraint failure
            if (e.getMessage() != null && e.getMessage().contains("FOREIGN KEY constraint fails")) {
                 LOGGER.log(Level.SEVERE, "FOREIGN KEY constraint failed. Check if DoctorID {0} exists in the doctors table.", doctorId);
                 throw new AppointmentException("Failed to schedule appointment: The selected doctor (ID: " + doctorId + ") does not exist in the database.", e);
            }
            throw new AppointmentException("Failed to schedule appointment: " + e.getMessage(), e);
        }
    }

    /**
     * Update the status of an appointment
     *
     * @param appointment The appointment to update
     * @param newStatus The new status value
     * @return true if updated successfully, false otherwise
     * @throws AppointmentException If there's an error updating the appointment
     */
    public boolean updateAppointmentStatus(Appointment appointment, String newStatus)
            throws AppointmentException {
        if (!appointment.isStoredInDatabase()) {
            throw new AppointmentException("Cannot update appointment that has not been saved to database");
        }

        try {
            boolean updated = dbHandler.updateAppointmentStatus(appointment.getAppointmentId(), newStatus);
            if (updated) {
                // Update in-memory object
                appointment.setStatus(newStatus);
            }
            return updated;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating appointment status in database", e);
            throw new AppointmentException("Failed to update appointment status: " + e.getMessage(), e);
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
        try {
            return dbHandler.loadAppointmentsForPatient(patient.getUserID());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading patient appointments from database", e);
            throw new AppointmentException("Failed to load appointments: " + e.getMessage(), e);
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
        try {
            return dbHandler.loadAppointmentsForDoctor(doctor.getUserID());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading doctor appointments from database", e);
            throw new AppointmentException("Failed to load appointments: " + e.getMessage(), e);
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
