package com.rhms.notifications;

import com.rhms.appointmentScheduling.Appointment;
import com.rhms.userManagement.Patient;
import java.util.*;
import java.time.LocalDateTime;

// Manages automated reminders for appointments and medications
public class ReminderService {
    private final SMSNotification smsNotification;    // Service for sending SMS notifications
    private final Timer scheduler;                    // Scheduler for timing reminders

    // Initialize reminder service with SMS capability
    public ReminderService() {
        this.smsNotification = new SMSNotification();
        this.scheduler = new Timer(true);            // Daemon thread for background scheduling
    }

    // Schedule reminder 24 hours before appointment
    public void scheduleAppointmentReminder(Appointment appointment) {
        Date appointmentDate = appointment.getAppointmentDate();
        // Calculate reminder time as 24 hours before appointment
        Date reminderTime = new Date(appointmentDate.getTime() - (24 * 60 * 60 * 1000));

        Patient patient = appointment.getPatient();
        if (patient != null) {
            // Prepare reminder message with doctor and appointment details
            final String subject = "Appointment Reminder";
            final String message = String.format(
                "Reminder: You have an appointment with Dr. %s tomorrow at %s",
                appointment.getDoctor().getName(),
                appointmentDate.toString()
            );

            // Schedule SMS reminder task
            scheduler.schedule(new TimerTask() {
                @Override
                public void run() {
                    smsNotification.sendNotification(patient.getPhone(), subject, message);
                }
            }, reminderTime);
        }
    }

    // Set up recurring medication reminders
    public void scheduleMedicationReminder(Patient patient, String medication, String schedule) {
        // Prepare medication reminder message
        final String subject = "Medication Reminder";
        final String message = String.format(
            "Reminder: Time to take your %s as per schedule: %s",
            medication,
            schedule
        );

        // Schedule daily recurring reminder
        scheduler.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                smsNotification.sendNotification(patient.getPhone(), subject, message);
            }
        }, 0, 24 * 60 * 60 * 1000); // Set for daily intervals
    }

    // Send one-time immediate reminder
    public void sendImmediateReminder(Patient patient, String subject, String message) {
        smsNotification.sendNotification(patient.getPhone(), subject, message);
    }

    // Cancel all scheduled reminders for a patient
    public void cancelReminders(Patient patient) {
        scheduler.purge();
    }
}