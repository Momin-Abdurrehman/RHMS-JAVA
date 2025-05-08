package com.rhms.healthDataHandling;

import com.rhms.userManagement.Doctor;

import java.util.Date;

/**
 * Represents a medical record entry for a patient
 */
public class MedicalRecord {
    private String condition;
    private String description;
    private Date date;
    private Doctor recordedBy;

    /**
     * Creates a new medical record entry
     */
    public MedicalRecord(String condition) {
        this.condition = condition;
        this.description = description;
        this.date = date;
        this.recordedBy = recordedBy;
    }

    // Getters
    public String getCondition() {
        return condition;
    }

    public String getDescription() {
        return description;
    }

    public Date getDate() {
        return date;
    }

    public Doctor getRecordedBy() {
        return recordedBy;
    }




    // Setters
    public void setCondition(String condition) {
        this.condition = condition;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setRecordedBy(Doctor recordedBy) {
        this.recordedBy = recordedBy;
    }
}
