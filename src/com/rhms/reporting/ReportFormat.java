package com.rhms.reporting;

/**
 * Defines available report formats 
 */
public enum ReportFormat {
    TEXT("txt"),
    CSV("csv"),
    PDF("pdf");
    
    private final String extension;
    
    ReportFormat(String extension) {
        this.extension = extension;
    }
    
    /**
     * Get file extension for this format
     * @return File extension without dot
     */
    public String getExtension() {
        return extension;
    }
}
