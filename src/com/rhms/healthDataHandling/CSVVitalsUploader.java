package com.rhms.healthDataHandling;

import com.rhms.userManagement.Patient;
import com.rhms.emergencyAlert.EmergencyAlert;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.text.ParseException;

public class CSVVitalsUploader {

    /**
     * Uploads vital signs from a CSV file and returns a count of successful records
     * @param patient Patient whose vital signs are being uploaded
     * @param filePath Path to the CSV file
     * @return Number of records successfully uploaded
     * @throws IOException If file cannot be read
     */
    public static int uploadVitalsFromCSV(Patient patient, String filePath) throws IOException {
        int successCount = 0;
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;

        // Create emergency alert for abnormal vitals detection
        EmergencyAlert emergencyAlert = new EmergencyAlert();
        
        while ((line = reader.readLine()) != null) {
            try {
                VitalSign vitalSign = parseVitalSignLine(line);
                patient.addVitalSign(vitalSign);
                successCount++;
                
                // Check if vital signs are abnormal and alert doctor if needed
                if (vitalSign.isAbnormal()) {
                    emergencyAlert.alertDoctorOfAbnormalVitals(patient, vitalSign);
                }
            } catch (Exception e) {
                // Skip invalid lines
                System.err.println("Error parsing line: " + line + " - " + e.getMessage());
            }
        }
        reader.close();
        return successCount;
    }

    /**
     * Uploads vital signs from a CSV file and returns a detailed report
     * @param patient Patient whose vital signs are being uploaded
     * @param filePath Path to the CSV file
     * @return Report containing success count, error count, and error details
     * @throws IOException If file cannot be read
     */
    public static VitalsUploadReport uploadVitalsFromCSVWithReport(Patient patient, String filePath) throws IOException {
        // Instantiate VitalsUploadReport using the no-arg constructor
        VitalsUploadReport report = new VitalsUploadReport();
        BufferedReader reader = null; // Initialize reader to null
        String line = null;
        int lineNumber = 0;
        
        // Create emergency alert for abnormal vitals detection
        EmergencyAlert emergencyAlert = new EmergencyAlert();

        try {
             reader = new BufferedReader(new FileReader(filePath));
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    VitalSign vitalSign = parseVitalSignLine(line);
                    patient.addVitalSign(vitalSign);
                    // Use addSuccess method to report success
                    report.addSuccess(vitalSign);
                    
                    // Check if vital signs are abnormal and alert doctor if needed
                    if (vitalSign.isAbnormal()) {
                        emergencyAlert.alertDoctorOfAbnormalVitals(patient, vitalSign);
                    }
                } catch (Exception e) {
                    // Use addError method to report errors
                    report.addError(lineNumber, line, e.getMessage());
                }
            }
        } finally {
             if (reader != null) {
                 reader.close(); // Ensure reader is closed even if exceptions occur
             }
        }

        // Return the populated report object
        return report;
    }

    /**
     * Parse a CSV line into a VitalSign object
     * @param line CSV line containing vital sign data
     * @return VitalSign object populated with the data
     * @throws Exception If the line format is invalid
     */
    private static VitalSign parseVitalSignLine(String line) throws Exception {
        String[] parts = line.split(",");
        if (parts.length < 4) {
            throw new Exception("Not enough values. Expected at least 4 values for heart rate, oxygen level, temperature, and blood pressure.");
        }
        
        try {
            double heartRate, oxygenLevel, temperature, bloodPressure;
            Date timestamp = null;
            
            if (parts.length >= 5) {
                // Format with timestamp
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    timestamp = dateFormat.parse(parts[0].trim());
                    heartRate = Double.parseDouble(parts[1].trim());
                    oxygenLevel = Double.parseDouble(parts[2].trim());
                    temperature = Double.parseDouble(parts[3].trim());
                    bloodPressure = Double.parseDouble(parts[4].trim());
                } catch (ParseException pe) {
                    // If date parsing fails, treat it as a format without timestamp
                    heartRate = Double.parseDouble(parts[0].trim());
                    oxygenLevel = Double.parseDouble(parts[1].trim());
                    temperature = Double.parseDouble(parts[2].trim());
                    bloodPressure = Double.parseDouble(parts[3].trim());
                    timestamp = null; // Ensure timestamp is null if parsing failed
                } catch (NumberFormatException nfe) {
                     // Handle number format errors specifically if date parsing succeeded but numbers failed
                     throw new Exception("Invalid number format after parsing timestamp: " + nfe.getMessage());
                }
            } else {
                // Format without timestamp
                heartRate = Double.parseDouble(parts[0].trim());
                oxygenLevel = Double.parseDouble(parts[1].trim());
                temperature = Double.parseDouble(parts[2].trim());
                bloodPressure = Double.parseDouble(parts[3].trim());
                timestamp = null; // Explicitly set timestamp to null
            }
            
            // Create VitalSign based on whether we have a timestamp or not
            if (timestamp != null) {
                // Calling 5-argument constructor
                return new VitalSign(heartRate, oxygenLevel, bloodPressure, temperature, timestamp);
            } else {
                // Calling 4-argument constructor (Line 84 context)
                return new VitalSign(heartRate, oxygenLevel, bloodPressure, temperature);
            }
            
        } catch (NumberFormatException e) {
            // Catch number format errors for the case without timestamp
            throw new Exception("Invalid number format: " + e.getMessage());
        } catch (Exception e) {
             // Catch any other unexpected errors during parsing
             throw new Exception("Error parsing vital sign line: " + e.getMessage());
        }
    }
}
