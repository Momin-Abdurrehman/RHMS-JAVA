package com.rhms.notifications;

import com.rhms.appointmentScheduling.Appointment;
import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;

import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service handling appointment-specific notifications
 */
public class AppointmentNotificationService {
    private static final Logger LOGGER = Logger.getLogger(AppointmentNotificationService.class.getName());
    private NotificationService notificationService;
    
    // Date formatters for notification messages
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEEE, MMMM d, yyyy");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("h:mm a");
    
    public AppointmentNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    /**
     * Notify patient when their appointment is accepted by a doctor
     * 
     * @param appointment The accepted appointment
     * @return true if notification was sent successfully, false otherwise
     */
    public boolean notifyAppointmentAccepted(Appointment appointment) {
        if (appointment == null) {
            LOGGER.log(Level.WARNING, "Cannot send notification for null appointment");
            return false;
        }
        
        Patient patient = appointment.getPatient();
        Doctor doctor = appointment.getDoctor();
        
        if (patient == null) {
            LOGGER.log(Level.WARNING, "Cannot send notification for appointment with null patient");
            return false;
        }
        
        if (doctor == null) {
            LOGGER.log(Level.WARNING, "Cannot send notification for appointment with null doctor");
            return false;
        }
        
        String title = "Appointment Confirmed";
        String message = String.format(
            "Your appointment with Dr. %s has been confirmed for %s at %s. Please arrive 15 minutes early.",
            doctor.getName(),
            DATE_FORMAT.format(appointment.getAppointmentDate()),
            TIME_FORMAT.format(appointment.getAppointmentDate())
        );
        
        try {
            // Create notification
            Notification notification = new Notification(
                patient.getUserID(),
                title,
                message,
                "appointment_accepted"
            );
            
            // Send notification
            return notificationService.sendNotification(notification);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error sending appointment confirmation notification", e);
            return false;
        }
    }
    
    /**
     * Notify patient when their appointment status changes (cancelled, rescheduled, etc.)
     * 
     * @param appointment The appointment whose status changed
     * @param oldStatus The previous status
     * @param newStatus The new status
     * @return true if notification was sent successfully, false otherwise
     */
    public boolean notifyStatusChange(Appointment appointment, String oldStatus, String newStatus) {
        if (appointment == null || oldStatus == null || newStatus == null) {
            LOGGER.log(Level.WARNING, "Cannot send status change notification with null parameters");
            return false;
        }
        
        // Skip notification if status didn't actually change
        if (oldStatus.equals(newStatus)) {
            return true;
        }
        
        Patient patient = appointment.getPatient();
        if (patient == null) {
            LOGGER.log(Level.WARNING, "Cannot send notification for appointment with null patient");
            return false;
        }
        
        String title = "Appointment Status Updated";
        String message;
        
        // Build appropriate message based on status change
        if (newStatus.equals("Cancelled")) {
            message = String.format(
                "Your appointment for %s at %s has been cancelled.",
                DATE_FORMAT.format(appointment.getAppointmentDate()),
                TIME_FORMAT.format(appointment.getAppointmentDate())
            );
            
            Doctor doctor = appointment.getDoctor();
            if (doctor != null) {
                message += " Please contact Dr. " + doctor.getName() + 
                           " if you need to reschedule.";
            }
        } else if (newStatus.equals("Completed")) {
            message = String.format(
                "Your appointment for %s at %s has been marked as completed.",
                DATE_FORMAT.format(appointment.getAppointmentDate()),
                TIME_FORMAT.format(appointment.getAppointmentDate())
            );
        } else {
            message = String.format(
                "Your appointment status has changed from '%s' to '%s' for %s at %s.",
                oldStatus,
                newStatus,
                DATE_FORMAT.format(appointment.getAppointmentDate()),
                TIME_FORMAT.format(appointment.getAppointmentDate())
            );
        }
        
        try {
            // Create notification
            Notification notification = new Notification(
                patient.getUserID(),
                title,
                message,
                "appointment_status_change"
            );
            
            // Send notification
            return notificationService.sendNotification(notification);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error sending appointment status change notification", e);
            return false;
        }
    }
}
