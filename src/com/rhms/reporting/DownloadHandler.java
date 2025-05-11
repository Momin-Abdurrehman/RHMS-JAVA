package com.rhms.reporting;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles downloading and opening of generated report files
 */
public class DownloadHandler {
    private static final Logger LOGGER = Logger.getLogger(DownloadHandler.class.getName());
    
    /**
     * Open a file with the system's default application
     * @param file The file to open
     * @return true if successful, false otherwise
     */
    public static boolean openFile(File file) {
        if (!file.exists()) {
            LOGGER.log(Level.WARNING, "File does not exist: {0}", file.getAbsolutePath());
            return false;
        }
        
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder processBuilder = null;
            
            if (os.contains("win")) {
                // Windows
                processBuilder = new ProcessBuilder("cmd", "/c", file.getAbsolutePath());
            } else if (os.contains("mac")) {
                // macOS
                processBuilder = new ProcessBuilder("open", file.getAbsolutePath());
            } else {
                // Linux/Unix
                processBuilder = new ProcessBuilder("xdg-open", file.getAbsolutePath());
            }
            
            processBuilder.start();
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error opening file: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Copy a file to a custom location
     * @param sourceFile The source file
     * @param destinationDir The destination directory
     * @param newFileName Optional new filename (if null, original name is used)
     * @return The copied file or null if failed
     */
    public static File copyToLocation(File sourceFile, String destinationDir, String newFileName) {
        if (!sourceFile.exists() || !sourceFile.isFile()) {
            LOGGER.log(Level.WARNING, "Source file does not exist: {0}", sourceFile.getAbsolutePath());
            return null;
        }
        
        try {
            File destDir = new File(destinationDir);
            if (!destDir.exists()) {
                destDir.mkdirs();
            }
            
            String fileName = (newFileName != null) ? newFileName : sourceFile.getName();
            File destFile = new File(destDir, fileName);
            
            try (FileInputStream fis = new FileInputStream(sourceFile);
                 FileOutputStream fos = new FileOutputStream(destFile);
                 FileChannel source = fis.getChannel();
                 FileChannel destination = fos.getChannel()) {
                
                destination.transferFrom(source, 0, source.size());
            }
            
            LOGGER.log(Level.INFO, "File successfully copied to: {0}", destFile.getAbsolutePath());
            return destFile;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error copying file: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Get the default download directory for the current platform
     * @return Path to the default download directory
     */
    public static String getDefaultDownloadDirectory() {
        String userHome = System.getProperty("user.home");
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("win")) {
            // Windows
            return userHome + "\\Downloads";
        } else if (os.contains("mac")) {
            // macOS
            return userHome + "/Downloads";
        } else {
            // Linux/Unix
            return userHome + "/Downloads";
        }
    }
    
    /**
     * Check if a file exists and is readable
     * @param filePath Path to the file
     * @return true if the file exists and is readable
     */
    public static boolean isFileAccessible(String filePath) {
        File file = new File(filePath);
        return file.exists() && file.isFile() && file.canRead();
    }
    
    /**
     * Get file extension from a file
     * @param file The file
     * @return The file extension (without the dot) or empty string if none
     */
    public static String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // No extension
        }
        return name.substring(lastIndexOf + 1);
    }
    
    /**
     * Check if the operating system supports a particular file type
     * @param fileExtension The file extension to check
     * @return true if the OS likely supports this file type
     */
    public static boolean isFileTypeSupported(String fileExtension) {
        // Most common file types are supported by all operating systems
        // This is a simplified check - in reality you would check against
        // registered file types in the OS
        String[] commonTypes = {"pdf", "txt", "csv", "html", "jpg", "png", "doc", "docx", "xls", "xlsx"};
        
        fileExtension = fileExtension.toLowerCase().trim();
        
        for (String type : commonTypes) {
            if (type.equals(fileExtension)) {
                return true;
            }
        }
        
        // For less common types, we assume they might not be supported
        return false;
    }
}
