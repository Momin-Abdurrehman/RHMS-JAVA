package com.rhms.reporting;

import com.rhms.doctorPatientInteraction.Feedback;
import com.rhms.healthDataHandling.VitalSign;
import com.rhms.healthDataHandling.VitalsDatabase;
import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Generates downloadable reports for patients and doctors
 * Includes functionality to create reports with vital history,
 * doctor's feedback, and health trends data
 */
public class ReportGenerator {
    private Patient patient;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private DecimalFormat decimalFormat = new DecimalFormat("#0.0");
    
    /**
     * Create a report generator for a specific patient
     * @param patient The patient for whom to generate reports
     */
    public ReportGenerator(Patient patient) {
        this.patient = patient;
    }
    
    /**
     * Generate a complete report with all available data
     * @param outputDirectory Directory where the report file should be saved
     * @param format Format for the report (TXT, CSV, PDF)
     * @return The generated report file
     * @throws IOException If there's an error writing the file
     */
    public File generateCompleteReport(String outputDirectory, ReportFormat format) throws IOException {
        return generateReport(outputDirectory, true, true, true, format);
    }
    
    /**
     * Generate a report with only vitals history
     * @param outputDirectory Directory where the report file should be saved
     * @param format Format for the report
     * @return The generated report file
     * @throws IOException If there's an error writing the file
     */
    public File generateVitalsOnlyReport(String outputDirectory, ReportFormat format) throws IOException {
        return generateReport(outputDirectory, true, false, false, format);
    }
    
    /**
     * Generate a report with only doctor's feedback
     * @param outputDirectory Directory where the report file should be saved
     * @param format Format for the report
     * @return The generated report file
     * @throws IOException If there's an error writing the file
     */
    public File generateFeedbackOnlyReport(String outputDirectory, ReportFormat format) throws IOException {
        return generateReport(outputDirectory, false, true, false, format);
    }
    
    /**
     * Generate a report with only health trends
     * @param outputDirectory Directory where the report file should be saved
     * @param format Format for the report
     * @return The generated report file
     * @throws IOException If there's an error writing the file
     */
    public File generateTrendsOnlyReport(String outputDirectory, ReportFormat format) throws IOException {
        return generateReport(outputDirectory, false, false, true, format);
    }
    
    /**
     * Generate a customized report based on selected content and format
     * @param outputDirectory Directory where the report file should be saved
     * @param includeVitals Include vitals history in the report
     * @param includeFeedback Include doctor's feedback in the report
     * @param includeTrends Include health trends in the report
     * @param format Format for the report
     * @return The generated report file
     * @throws IOException If there's an error writing the file
     */
    public File generateReport(String outputDirectory, boolean includeVitals, 
                              boolean includeFeedback, boolean includeTrends, 
                              ReportFormat format) throws IOException {
        
        // Create filename with timestamp
        SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
        String fileName = patient.getName().replaceAll("\\s+", "_") + "_Medical_Report_" + 
                         fileNameFormat.format(new Date()) + "." + format.getExtension();
        
        // Create output directory if it doesn't exist
        File directory = new File(outputDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        File reportFile = new File(directory, fileName);
        
        switch (format) {
            case TEXT:
                generateTextReport(reportFile, includeVitals, includeFeedback, includeTrends);
                break;
            case CSV:
                generateCsvReport(reportFile, includeVitals, includeFeedback, includeTrends);
                break;
            case PDF:
                generatePdfReport(reportFile, includeVitals, includeFeedback, includeTrends);
                break;
        }
        
        return reportFile;
    }
    
    /**
     * Generate a report in text format
     */
    private void generateTextReport(File reportFile, boolean includeVitals, 
                                  boolean includeFeedback, boolean includeTrends) throws IOException {
        try (FileWriter writer = new FileWriter(reportFile)) {
            // Write report header
            writeReportHeader(writer);
            
            // Write sections based on parameters
            if (includeVitals) {
                writeVitalsHistoryText(writer);
            }
            
            if (includeFeedback) {
                writeDoctorFeedbackText(writer);
            }
            
            if (includeTrends) {
                writeHealthTrendsText(writer);
            }
            
            // Write footer
            writeReportFooter(writer);
        }
    }
    
    /**
     * Generate a report in CSV format
     */
    private void generateCsvReport(File reportFile, boolean includeVitals, 
                                 boolean includeFeedback, boolean includeTrends) throws IOException {
        try (FileWriter writer = new FileWriter(reportFile)) {
            // Write patient info
            writer.write("Patient Information\n");
            writer.write("Name,ID,Contact,Email\n");
            writer.write(patient.getName() + "," + patient.getUserID() + "," + 
                       patient.getPhone() + "," + patient.getEmail() + "\n\n");
            
            // Write vitals section if included
            if (includeVitals) {
                // Directly access the database to get all vitals
                List<VitalSign> vitals = patient.getVitalsDatabase().getAllVitals();
                
                if (vitals != null && !vitals.isEmpty()) {
                    writer.write("Vitals History\n");
                    writer.write("Timestamp,Heart Rate (bpm),Oxygen Level (%),Blood Pressure (mmHg),Temperature (°C),Status\n");
                    
                    for (VitalSign vital : vitals) {
                        writer.write(dateFormat.format(vital.getTimestamp()) + ",");
                        writer.write(decimalFormat.format(vital.getHeartRate()) + ",");
                        writer.write(decimalFormat.format(vital.getOxygenLevel()) + ",");
                        writer.write(decimalFormat.format(vital.getBloodPressure()) + ",");
                        writer.write(decimalFormat.format(vital.getTemperature()) + ",");
                        writer.write(vital.isAbnormal() ? "Abnormal" : "Normal");
                        writer.write("\n");
                    }
                    writer.write("\n");
                } else {
                    writer.write("Vitals History\n");
                    writer.write("No vital signs records found in database.\n\n");
                }
            }
            
            // Write feedback section if included
            if (includeFeedback) {
                List<Feedback> feedback = patient.getAllFeedback();
                if (feedback != null && !feedback.isEmpty()) {
                    writer.write("Doctor's Feedback\n");
                    writer.write("Timestamp,Doctor,Feedback\n");
                    
                    for (Feedback item : feedback) {
                        writer.write(dateFormat.format(item.getTimestamp()) + ",");
                        writer.write("\"" + item.getDoctor().getName() + "\",");
                        writer.write("\"" + item.getMessage().replace("\"", "\"\"") + "\"\n");
                    }
                    writer.write("\n");
                }
            }
            
            // Write trends section if included
            if (includeTrends) {
                VitalsDatabase vitalsDb = patient.getVitalsDatabase();
                if (vitalsDb.hasVitalsData()) {
                    writer.write("Health Trends\n");
                    writer.write("Metric,Value\n");
                    writer.write("Average Heart Rate," + decimalFormat.format(vitalsDb.getAverageHeartRate()) + "\n");
                    writer.write("Average Blood Pressure," + decimalFormat.format(vitalsDb.getAverageBloodPressure()) + "\n");
                    writer.write("Average Oxygen Level," + decimalFormat.format(vitalsDb.getAverageOxygenLevel()) + "\n");
                    writer.write("Average Temperature," + decimalFormat.format(vitalsDb.getAverageTemperature()) + "\n");
                    writer.write("Heart Rate Trend," + vitalsDb.getHeartRateTrend() + "\n");
                    writer.write("Blood Pressure Trend," + vitalsDb.getBloodPressureTrend() + "\n");
                }
            }
        }
    }
    
    /**
     * Generate a report in PDF format (basic implementation)
     * Note: This is a simplified implementation that creates a basic PDF
     */
    private void generatePdfReport(File reportFile, boolean includeVitals, 
                                 boolean includeFeedback, boolean includeTrends) throws IOException {
        // For simplicity, we'll create a text-based PDF like structure
        // In a real implementation, a PDF library like iText would be used
        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write("%PDF-1.4\n");
            writer.write("1 0 obj\n");
            writer.write("<< /Title (Patient Medical Report) /Author (RHMS) >>\n");
            writer.write("endobj\n");
            writer.write("2 0 obj\n");
            writer.write("<< /Length 3 0 R >>\n");
            writer.write("stream\n");
            
            // Content 
            writer.write("PATIENT MEDICAL REPORT\n\n");
            writer.write("Name: " + patient.getName() + "\n");
            writer.write("ID: " + patient.getUserID() + "\n");
            writer.write("Generated: " + dateFormat.format(new Date()) + "\n\n");
            
            if (includeVitals) {
                writer.write("VITALS HISTORY\n\n");
                // Get vitals directly from database
                List<VitalSign> vitals = patient.getVitalsDatabase().getAllVitals();
                if (vitals != null && !vitals.isEmpty()) {
                    writer.write("Total Records: " + vitals.size() + "\n");
                    writer.write("First Record: " + dateFormat.format(vitals.get(0).getTimestamp()) + "\n");
                    writer.write("Latest Record: " + dateFormat.format(vitals.get(vitals.size()-1).getTimestamp()) + "\n");
                } else {
                    writer.write("No vital signs records found in database.\n");
                }
                writer.write("\n");
            }
            
            if (includeFeedback) {
                writer.write("DOCTOR FEEDBACK INCLUDED\n");
            }
            
            if (includeTrends) {
                writer.write("HEALTH TRENDS INCLUDED\n");
            }
            
            writer.write("\nThis is a simple PDF format representation. In a real implementation,\n");
            writer.write("a proper PDF generation library would be used.\n");
            
            writer.write("endstream\n");
            writer.write("endobj\n");
            writer.write("3 0 obj\n");
            writer.write("1000\n");
            writer.write("endobj\n");
            writer.write("trailer\n");
            writer.write("<< /Root 1 0 R >>\n");
            writer.write("%%EOF");
        }
    }
    
    /**
     * Write the report header with patient information
     */
    private void writeReportHeader(FileWriter writer) throws IOException {
        writer.write("=================================================\n");
        writer.write("           REMOTE HEALTHCARE MONITORING          \n");
        writer.write("                 MEDICAL REPORT                  \n");
        writer.write("=================================================\n\n");
        
        writer.write("PATIENT INFORMATION\n");
        writer.write("-------------------\n");
        writer.write("Name: " + patient.getName() + "\n");
        writer.write("ID: " + patient.getUserID() + "\n");
        writer.write("Contact: " + patient.getPhone() + "\n");
        writer.write("Email: " + patient.getEmail() + "\n\n");
        
        writer.write("Report Generated: " + dateFormat.format(new Date()) + "\n\n");
    }
    
    /**
     * Write vitals history section in text format
     */
    private void writeVitalsHistoryText(FileWriter writer) throws IOException {
        writer.write("=================================================\n");
        writer.write("                 VITALS HISTORY                  \n");
        writer.write("=================================================\n\n");
        
        // Get vitals directly from database instead of patient.getVitalsHistory()
        List<VitalSign> vitals = patient.getVitalsDatabase().getAllVitals();
        
        if (vitals == null || vitals.isEmpty()) {
            writer.write("No vitals data found in database for this patient.\n\n");
            return;
        }
        
        for (VitalSign vital : vitals) {
            writer.write("Date: " + dateFormat.format(vital.getTimestamp()) + "\n");
            writer.write("Heart Rate: " + decimalFormat.format(vital.getHeartRate()) + " bpm");
            writer.write(vital.isHeartRateNormal() ? "\n" : " (ABNORMAL)\n");
            
            writer.write("Oxygen Level: " + decimalFormat.format(vital.getOxygenLevel()) + "%");
            writer.write(vital.isOxygenLevelNormal() ? "\n" : " (ABNORMAL)\n");
            
            writer.write("Blood Pressure: " + decimalFormat.format(vital.getBloodPressure()) + " mmHg");
            writer.write(vital.isBloodPressureNormal() ? "\n" : " (ABNORMAL)\n");
            
            writer.write("Temperature: " + decimalFormat.format(vital.getTemperature()) + "°C");
            writer.write(vital.isTemperatureNormal() ? "\n" : " (ABNORMAL)\n");
            
            writer.write("Status: " + (vital.isAbnormal() ? "Abnormal" : "Normal") + "\n");
            writer.write("-------------------------------------------\n\n");
        }
    }
    
    /**
     * Write doctor feedback section in text format
     */
    private void writeDoctorFeedbackText(FileWriter writer) throws IOException {
        writer.write("=================================================\n");
        writer.write("               DOCTOR'S FEEDBACK                 \n");
        writer.write("=================================================\n\n");
        
        List<Feedback> feedback = patient.getAllFeedback();
        if (feedback == null || feedback.isEmpty()) {
            writer.write("No feedback recorded for this patient.\n\n");
            return;
        }
        
        for (Feedback item : feedback) {
            writer.write("Date: " + dateFormat.format(item.getTimestamp()) + "\n");
            writer.write("Doctor: " + item.getDoctor().getName() + "\n");
            writer.write("Feedback: " + item.getMessage() + "\n");
            
            if (item.hasPrescription() && item.getPrescription() != null) {
                writer.write("Prescription: " + item.getPrescription().getMedicationInfo() + "\n");
                writer.write("Dosage: " + item.getPrescription().getDosage() + "\n");
                writer.write("Instructions: " + item.getPrescription().getInstructions() + "\n");
            }
            
            writer.write("-------------------------------------------\n\n");
        }
    }
    
    /**
     * Write health trends section in text format
     */
    private void writeHealthTrendsText(FileWriter writer) throws IOException {
        writer.write("=================================================\n");
        writer.write("                 HEALTH TRENDS                   \n");
        writer.write("=================================================\n\n");
        
        VitalsDatabase vitalsDb = patient.getVitalsDatabase();
        if (!vitalsDb.hasVitalsData()) {
            writer.write("Insufficient data to analyze health trends.\n\n");
            return;
        }
        
        writer.write("AVERAGE VALUES\n");
        writer.write("-------------\n");
        writer.write("Heart Rate: " + decimalFormat.format(vitalsDb.getAverageHeartRate()) + " bpm\n");
        writer.write("Blood Pressure: " + decimalFormat.format(vitalsDb.getAverageBloodPressure()) + " mmHg\n");
        writer.write("Oxygen Level: " + decimalFormat.format(vitalsDb.getAverageOxygenLevel()) + "%\n");
        writer.write("Temperature: " + decimalFormat.format(vitalsDb.getAverageTemperature()) + "°C\n\n");
        
        writer.write("TREND ANALYSIS\n");
        writer.write("-------------\n");
        writer.write("Heart Rate Trend: " + vitalsDb.getHeartRateTrend() + "\n");
        writer.write("Blood Pressure Trend: " + vitalsDb.getBloodPressureTrend() + "\n\n");
        
        writer.write("ABNORMAL READINGS\n");
        writer.write("----------------\n");
        List<VitalSign> abnormalVitals = vitalsDb.getAbnormalVitals();
        writer.write("Total Abnormal Readings: " + abnormalVitals.size() + "\n");
        if (abnormalVitals.size() > 0) {
            writer.write("Most Recent Abnormal: " + 
                      dateFormat.format(abnormalVitals.get(abnormalVitals.size()-1).getTimestamp()) + "\n\n");
        }
    }
    
    /**
     * Write report footer
     */
    private void writeReportFooter(FileWriter writer) throws IOException {
        writer.write("=================================================\n");
        writer.write("This report is generated automatically by the Remote\n");
        writer.write("Healthcare Monitoring System. Please consult with your\n");
        writer.write("healthcare provider for proper medical interpretation.\n");
        writer.write("=================================================\n");
    }
}
