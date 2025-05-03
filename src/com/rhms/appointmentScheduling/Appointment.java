package com.rhms.appointmentScheduling;

//importing packages
import com.rhms.userManagement.Patient;
import com.rhms.userManagement.Doctor;
import java.util.Date;

public class Appointment {
    private int appointmentId; // New field to store database primary key
    private Date appointmentDate;
    private Doctor doctor;
    private Patient patient;
    private String status; // Pending, Confirmed, Cancelled, Completed
    private String purpose; // Reason for the appointment
    private String notes; // Additional notes about the appointment
    private Date createdAt; // Timestamp when the appointment was created

    /**
     * Complete constructor with all fields including appointmentId
     */
    public Appointment(int appointmentId, Date appointmentDate, Patient patient, Doctor doctor, 
                      String purpose, String status, String notes, Date createdAt) {
        this.appointmentId = appointmentId;
        this.appointmentDate = appointmentDate;
        this.doctor = doctor;
        this.patient = patient;
        this.purpose = purpose;
        this.status = status;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    /**
     * Constructor for creating a new appointment before saving to database (no appointmentId)
     */
    public Appointment(Date appointmentDate, Patient patient, Doctor doctor, String purpose, String status, String notes) {
        this(-1, appointmentDate, patient, doctor, purpose, status, notes, new Date()); // -1 indicates not saved to database yet
    }

    /**
     * Simplified constructor with minimal fields
     */
    public Appointment(Date appointmentDate, Doctor doctor, Patient patient, String status) {
        this(-1, appointmentDate, patient, doctor, "", status, "", new Date());
    }

    // Getters and setters
    public int getAppointmentId() { return appointmentId; }
    public void setAppointmentId(int appointmentId) { this.appointmentId = appointmentId; }
    
    public Date getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(Date appointmentDate) { this.appointmentDate = appointmentDate; }
    
    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }
    
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    /**
     * Check if this appointment has been saved to the database
     * @return true if the appointment has been saved to the database (has valid ID)
     */
    public boolean isStoredInDatabase() {
        return appointmentId > 0;
    }

    // overriding toString to print details of appointment
    @Override
    public String toString() {
        return "Appointment #" + (appointmentId > 0 ? appointmentId : "New") + 
               " with Dr. " + (doctor != null ? doctor.getName() : "Not assigned") + 
               " on " + appointmentDate + " for " + patient.getName() + 
               " - Status: " + status + 
               (purpose != null && !purpose.isEmpty() ? " - Purpose: " + purpose : "");
    }
}
