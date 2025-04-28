package com.rhms.appointmentScheduling;

import com.rhms.userManagement.Patient;
import com.rhms.userManagement.Doctor;
import java.util.ArrayList;
import java.util.Date;

public class AppointmentManager {
    private ArrayList<Appointment> appointments;  // Using ArrayList to store appointments

    public AppointmentManager() {
        appointments = new ArrayList<>();
    }

    // Request an appointment
    public Appointment requestAppointment(Date date, Doctor doctor, Patient patient) {
        Appointment appointment = new Appointment(date, doctor, patient, "Pending");
        appointments.add(appointment);
        System.out.println("Appointment requested successfully!");
        return appointment;
    }

    // Approve an appointment
    public void approveAppointment(Appointment appointment) {
        if (appointments.contains(appointment)) {
            appointment.setStatus("Approved");
            System.out.println("Appointment approved!");
        } else {
            System.out.println("Appointment not found!");
        }
    }

    // Cancel an appointment
    public void cancelAppointment(Appointment appointment) {
        if (appointments.contains(appointment)) {
            appointment.setStatus("Cancelled");
            System.out.println("Appointment cancelled!");
        } else {
            System.out.println("Appointment not found!");
        }
    }

    // Get all appointments
    public ArrayList<Appointment> getAppointments() {
        return appointments;
    }
}
