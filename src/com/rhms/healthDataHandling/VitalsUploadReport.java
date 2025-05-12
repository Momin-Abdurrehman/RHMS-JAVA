package com.rhms.healthDataHandling;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides detailed reporting for vital signs upload operations
 * Tracks successful uploads, errors, and validation issues
 */
public class VitalsUploadReport {
    private int successCount;
    private int errorCount;
    private List<VitalsUploadError> errors;
    private List<VitalSign> successfulVitals;
    // Removed filePath as it's not used in the constructor anymore

    // Constructor now takes no arguments
    public VitalsUploadReport() {
        this.successCount = 0;
        this.errorCount = 0;
        this.errors = new ArrayList<>();
        this.successfulVitals = new ArrayList<>();
    }

    /**
     * Add a successfully processed vital sign
     * @param vitalSign The valid vital sign record
     */
    public void addSuccess(VitalSign vitalSign) {
        successfulVitals.add(vitalSign);
        successCount++;
    }

    /**
     * Add an error that occurred during processing
     * @param lineNumber The CSV line number where the error occurred
     * @param lineContent The actual line content
     * @param errorMessage Description of the error
     */
    public void addError(int lineNumber, String lineContent, String errorMessage) {
        errors.add(new VitalsUploadError(lineNumber, lineContent, errorMessage));
        errorCount++;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public List<VitalSign> getSuccessfulVitals() {
        return successfulVitals;
    }

    /**
     * Generate a formatted report of the upload process
     * @return A multi-line string with upload statistics and error details
     */
    public String generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("\n==== Vitals Upload Report ====\n");
        report.append("Records processed successfully: ").append(successCount).append("\n");
        report.append("Records with errors: ").append(errorCount).append("\n");
        
        if (errorCount > 0) {
            report.append("\nError Details:\n");
            for (VitalsUploadError error : errors) {
                report.append("Line ").append(error.getLineNumber())
                      .append(": ").append(error.getErrorMessage())
                      .append("\n   Content: [").append(error.getLineContent()).append("]\n");
            }
        }
        
        return report.toString();
    }

    /**
     * Inner class representing a single error during upload
     */
    public static class VitalsUploadError {
        private int lineNumber;
        private String lineContent;
        private String errorMessage;

        public VitalsUploadError(int lineNumber, String lineContent, String errorMessage) {
            this.lineNumber = lineNumber;
            this.lineContent = lineContent;
            this.errorMessage = errorMessage;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getLineContent() {
            return lineContent;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
