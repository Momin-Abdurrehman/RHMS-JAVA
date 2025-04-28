package com.rhms.healthDataHandling;

import com.rhms.userManagement.Patient;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Handles uploading patient vital signs from CSV files
 * CSV format expected: timestamp, heart rate, oxygen level, temperature, blood pressure
 */
public class CSVVitalsUploader {
    
    // Define normal ranges for vital signs validation
    private static final double MIN_HEART_RATE = 40.0;   // bpm
    private static final double MAX_HEART_RATE = 120.0;  // bpm
    private static final double MIN_OXYGEN = 90.0;       // percentage
    private static final double MAX_OXYGEN = 100.0;      // percentage
    private static final double MIN_BP = 90.0;           // mmHg
    private static final double MAX_BP = 140.0;          // mmHg
    private static final double MIN_TEMP = 35.0;         // °C
    private static final double MAX_TEMP = 40.0;         // °C
    
    /**
     * Uploads vital signs from a CSV file to a patient's records
     * @param patient The patient whose vitals are being uploaded
     * @param filePath Path to the CSV file
     * @return Number of records successfully uploaded
     * @throws IOException If file cannot be read
     */
    public static int uploadVitalsFromCSV(Patient patient, String filePath) throws IOException {
        VitalsUploadReport report = processCSVFile(filePath);
        
        // Add all valid vital signs to the database
        if (report.getSuccessCount() > 0) {
            patient.getVitalsDatabase().addVitalRecords(report.getSuccessfulVitals());
        }
        
        // Print the report
        System.out.println(report.generateReport());
        
        return report.getSuccessCount();
    }
    
    /**
     * Enhanced version of CSV upload that returns a detailed report
     * @param patient The patient whose vitals are being uploaded
     * @param filePath Path to the CSV file
     * @return A VitalsUploadReport with detailed success/error information
     * @throws IOException If file cannot be read
     */
    public static VitalsUploadReport uploadVitalsFromCSVWithReport(Patient patient, String filePath) throws IOException {
        VitalsUploadReport report = processCSVFile(filePath);
        
        // Add all valid vital signs to the database
        if (report.getSuccessCount() > 0) {
            patient.getVitalsDatabase().addVitalRecords(report.getSuccessfulVitals());
        }
        
        return report;
    }
    
    /**
     * Process a CSV file and return a report without adding to the database
     * @param filePath Path to the CSV file
     * @return A VitalsUploadReport with detailed success/error information
     * @throws IOException If file cannot be read
     */
    public static VitalsUploadReport processCSVFile(String filePath) throws IOException {
        VitalsUploadReport report = new VitalsUploadReport();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isHeader = true;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                // Skip header row if present
                if (isHeader) {
                    isHeader = false;
                    // Check if this is actually a header row and not data
                    if (line.toLowerCase().contains("heart") || 
                        line.toLowerCase().contains("oxygen") ||
                        line.toLowerCase().contains("timestamp")) {
                        continue;
                    }
                }
                
                try {
                    // First parse the raw values without creating VitalSign object
                    double[] vitalValues = parseVitalValues(line);
                    
                    // Validate vital values before creating VitalSign object
                    String validationError = validateVitalValues(
                        vitalValues[0], vitalValues[1], vitalValues[2], vitalValues[3]);
                    
                    if (validationError != null) {
                        report.addError(lineNumber, line, validationError);
                    } else {
                        // Create VitalSign only after validation passes
                        VitalSign vitalSign = new VitalSign(
                            vitalValues[0], vitalValues[1], vitalValues[2], vitalValues[3]);
                        
                        report.addSuccess(vitalSign);
                    }
                } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
                    report.addError(lineNumber, line, e.getMessage());
                }
            }
            
            return report;
        }
    }
    
    /**
     * Parse vital values from a CSV line without creating a VitalSign object
     * @param csvLine The line to parse
     * @return Array of vital sign values [heartRate, oxygenLevel, bloodPressure, temperature]
     * @throws IllegalArgumentException If data format is invalid
     */
    private static double[] parseVitalValues(String csvLine) throws IllegalArgumentException {
        String[] data = csvLine.split(",");
        
        if (data.length < 4) {
            throw new IllegalArgumentException("CSV line must contain at least 4 values");
        }
        
        try {
            // Parse values from CSV (may contain timestamp as first column)
            double heartRate, oxygenLevel, temperature, bloodPressure;
            
            // If first column contains a timestamp (common in monitoring device exports)
            if (data.length >= 5 && isTimestamp(data[0])) {
                heartRate = Double.parseDouble(data[1].trim());
                oxygenLevel = Double.parseDouble(data[2].trim());
                temperature = Double.parseDouble(data[3].trim());
                bloodPressure = Double.parseDouble(data[4].trim());
            } else {
                // If no timestamp, assume just the vital values
                heartRate = Double.parseDouble(data[0].trim());
                oxygenLevel = Double.parseDouble(data[1].trim());
                temperature = Double.parseDouble(data[2].trim());
                bloodPressure = Double.parseDouble(data[3].trim());
            }
            
            return new double[]{heartRate, oxygenLevel, bloodPressure, temperature};
            
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric format: " + e.getMessage());
        }
    }
    
    /**
     * Validates vital signs before creating a VitalSign object
     * @param heartRate Heart rate in beats per minute
     * @param oxygenLevel Oxygen level in percentage
     * @param bloodPressure Blood pressure in mmHg
     * @param temperature Body temperature in Celsius
     * @return null if validation passes, error message otherwise
     */
    private static String validateVitalValues(double heartRate, double oxygenLevel, 
                                            double bloodPressure, double temperature) {
        if (heartRate < MIN_HEART_RATE || heartRate > MAX_HEART_RATE) {
            return String.format("Invalid heart rate: %.1f bpm (valid range: %.1f-%.1f)", 
                    heartRate, MIN_HEART_RATE, MAX_HEART_RATE);
        }
        
        if (oxygenLevel < MIN_OXYGEN || oxygenLevel > MAX_OXYGEN) {
            return String.format("Invalid oxygen level: %.1f%% (valid range: %.1f-%.1f)", 
                    oxygenLevel, MIN_OXYGEN, MAX_OXYGEN);
        }
        
        if (bloodPressure < MIN_BP || bloodPressure > MAX_BP) {
            return String.format("Invalid blood pressure: %.1f mmHg (valid range: %.1f-%.1f)", 
                    bloodPressure, MIN_BP, MAX_BP);
        }
        
        if (temperature < MIN_TEMP || temperature > MAX_TEMP) {
            return String.format("Invalid temperature: %.1f°C (valid range: %.1f-%.1f)", 
                    temperature, MIN_TEMP, MAX_TEMP);
        }
        
        return null; // Validation passed
    }
    
    /**
     * Check if a string might be a timestamp
     */
    private static boolean isTimestamp(String value) {
        // Common datetime formats
        SimpleDateFormat[] formats = {
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
            new SimpleDateFormat("MM/dd/yyyy HH:mm:ss"),
            new SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
        };
        
        for (SimpleDateFormat format : formats) {
            try {
                format.parse(value.trim());
                return true;
            } catch (ParseException e) {
                // Try next format
            }
        }
        return false;
    }
}
