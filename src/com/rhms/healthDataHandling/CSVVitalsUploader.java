package com.rhms.healthDataHandling;

import com.rhms.userManagement.Patient;
import com.rhms.emergencyAlert.EmergencyAlert;
import com.rhms.Database.VitalSignDatabaseHandler;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
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

        // Create a single emergency alert instance for this upload
        EmergencyAlert emergencyAlert = new EmergencyAlert();
        
        // Database handler for storing vital signs
        VitalSignDatabaseHandler vitalDbHandler = new VitalSignDatabaseHandler();
        
        // Track if we've already seen an abnormal vital in this batch
        boolean abnormalVitalFound = false;
        VitalSign mostCriticalVital = null;
        
        boolean firstLineSkipped = false; // Flag to skip header

        while ((line = reader.readLine()) != null) {
            if (!firstLineSkipped) {
                firstLineSkipped = true;
                if (isHeaderRow(line)) { // Check if the first line is a header
                    continue; // Skip header row
                }
            }
            try {
                VitalSign vitalSign = parseVitalSignLine(line);
                
                // Add to patient's memory model
                patient.addVitalSign(vitalSign);
                
                // Save to database
                int vitalId = vitalDbHandler.addVitalSign(patient.getUserID(), vitalSign);
                
                if (vitalId > 0) {
                    successCount++;
                    
                    // If this vital is abnormal and it's the first abnormal one or more critical
                    // than previous ones, mark it for notification
                    if (vitalSign.isAbnormal()) {
                        if (!abnormalVitalFound || isMoreCritical(vitalSign, mostCriticalVital)) {
                            mostCriticalVital = vitalSign;
                            abnormalVitalFound = true;
                        }
                    }
                }
            } catch (Exception e) {
                // Skip invalid lines
                System.err.println("Error parsing line: " + line + " - " + e.getMessage());
            }
        }
        reader.close();
        
        // After processing all vitals, send a single notification for the most critical vital
        if (abnormalVitalFound && mostCriticalVital != null) {
            emergencyAlert.alertDoctorOfAbnormalVitals(patient, mostCriticalVital);
        }
        
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
        VitalsUploadReport report = new VitalsUploadReport();
        BufferedReader reader = null;
        String line = null;
        int lineNumber = 0;
        
        // Create a single emergency alert instance for this upload
        EmergencyAlert emergencyAlert = new EmergencyAlert();
        
        // Database handler for storing vital signs
        VitalSignDatabaseHandler vitalDbHandler = new VitalSignDatabaseHandler();
        
        // Track if we've already seen an abnormal vital in this batch
        boolean abnormalVitalFound = false;
        VitalSign mostCriticalVital = null;
        
        boolean firstLineSkipped = false; // Flag to skip header

        try {
            reader = new BufferedReader(new FileReader(filePath));
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (!firstLineSkipped) {
                    firstLineSkipped = true;
                    if (isHeaderRow(line)) { // Check if the first line is a header
                        report.addError(lineNumber, line, "Skipped header row");
                        continue; // Skip header row
                    }
                }
                try {
                    VitalSign vitalSign = parseVitalSignLine(line);
                    
                    // Add to patient's memory model
                    patient.addVitalSign(vitalSign);
                    
                    // Save to database
                    int vitalId = vitalDbHandler.addVitalSign(patient.getUserID(), vitalSign);
                    
                    if (vitalId > 0) {
                        report.addSuccess(vitalSign);
                        
                        // If this vital is abnormal, mark it for notification
                        if (vitalSign.isAbnormal()) {
                            if (!abnormalVitalFound || isMoreCritical(vitalSign, mostCriticalVital)) {
                                mostCriticalVital = vitalSign;
                                abnormalVitalFound = true;
                            }
                        }
                    } else {
                        report.addError(lineNumber, line, "Failed to save to database");
                    }
                } catch (Exception e) {
                    report.addError(lineNumber, line, e.getMessage());
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        
        // After processing all vitals, send a single notification for the most critical vital
        if (abnormalVitalFound && mostCriticalVital != null) {
            emergencyAlert.alertDoctorOfAbnormalVitals(patient, mostCriticalVital);
        }

        return report;
    }
    
    /**
     * Checks if the given line is likely a header row.
     * @param line The line to check.
     * @return True if the line seems to be a header, false otherwise.
     */
    private static boolean isHeaderRow(String line) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        String lowerCaseLine = line.toLowerCase();
        // Common header keywords
        return lowerCaseLine.contains("timestamp") ||
               lowerCaseLine.contains("heart_rate") || // Adjusted to match common CSV header format
               lowerCaseLine.contains("oxygen_level") || // Adjusted
               lowerCaseLine.contains("blood_pressure") || // Adjusted
               lowerCaseLine.contains("temperature");
    }
    
    /**
     * Determine if one vital sign is more critical than another
     * @param newVital The new vital sign being compared
     * @param existingVital The existing most critical vital sign
     * @return True if the new vital sign is more critical
     */
    private static boolean isMoreCritical(VitalSign newVital, VitalSign existingVital) {
        if (existingVital == null) return true;
        
        // Count how many abnormal values in each
        int newAbnormalCount = countAbnormalValues(newVital);
        int existingAbnormalCount = countAbnormalValues(existingVital);
        
        // The one with more abnormal values is more critical
        return newAbnormalCount > existingAbnormalCount;
    }
    
    /**
     * Count how many vital signs are abnormal in a VitalSign object
     * @param vital The vital sign to check
     * @return Count of abnormal values (0-4)
     */
    private static int countAbnormalValues(VitalSign vital) {
        int count = 0;
        if (!vital.isHeartRateNormal()) count++;
        if (!vital.isOxygenLevelNormal()) count++;
        if (!vital.isBloodPressureNormal()) count++;
        if (!vital.isTemperatureNormal()) count++;
        return count;
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
                // Calling 4-argument constructor
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
