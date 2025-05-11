package com.rhms.Database;

import com.rhms.healthDataHandling.VitalSign;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

/**
 * Handles database operations for patient vital signs
 */
public class VitalSignDatabaseHandler {
    private Connection connection;

    public VitalSignDatabaseHandler() {
        this.connection = DatabaseConnection.getConnection();
    }

    /**
     * Adds a new vital sign record to the database
     * @param userId The ID of the user (patient)
     * @param vitalSign The vital sign to add
     * @return The ID of the newly created vital sign record, or -1 if insertion fails
     */
    public int addVitalSign(int userId, VitalSign vitalSign) {
        String sql = "INSERT INTO Patient_Vitals (user_id, heart_rate, oxygen_level, blood_pressure, temperature, timestamp) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, userId);
            stmt.setDouble(2, vitalSign.getHeartRate());
            stmt.setDouble(3, vitalSign.getOxygenLevel());
            stmt.setDouble(4, vitalSign.getBloodPressure());
            stmt.setDouble(5, vitalSign.getTemperature());
            
            // Handle the timestamp
            if (vitalSign.getTimestamp() != null) {
                stmt.setTimestamp(6, new Timestamp(vitalSign.getTimestamp().getTime()));
            } else {
                stmt.setTimestamp(6, new Timestamp(new Date().getTime()));
            }
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                return -1;
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    return -1;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error adding vital sign to database: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Gets all vital signs for a specific patient
     * @param userId The ID of the user (patient)
     * @return List of vital signs for the patient
     */
    public List<VitalSign> getVitalSignsForPatient(int userId) {
        List<VitalSign> vitals = new ArrayList<>();
        String sql = "SELECT * FROM Patient_Vitals WHERE user_id = ? ORDER BY timestamp ASC";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    double heartRate = rs.getDouble("heart_rate");
                    double oxygenLevel = rs.getDouble("oxygen_level");
                    double bloodPressure = rs.getDouble("blood_pressure");
                    double temperature = rs.getDouble("temperature");
                    Date timestamp = new Date(rs.getTimestamp("timestamp").getTime());
                    
                    VitalSign vital = new VitalSign(heartRate, oxygenLevel, bloodPressure, temperature, timestamp);
                    vitals.add(vital);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving vital signs from database: " + e.getMessage());
            e.printStackTrace();
        }
        
        return vitals;
    }

    /**
     * Gets the latest vital sign record for a patient
     * @param userId The ID of the user (patient)
     * @return The latest vital sign record, or null if none exists
     */
    public VitalSign getLatestVitalSign(int userId) {
        String sql = "SELECT * FROM Patient_Vitals WHERE user_id = ? ORDER BY timestamp DESC LIMIT 1";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double heartRate = rs.getDouble("heart_rate");
                    double oxygenLevel = rs.getDouble("oxygen_level");
                    double bloodPressure = rs.getDouble("blood_pressure");
                    double temperature = rs.getDouble("temperature");
                    Date timestamp = new Date(rs.getTimestamp("timestamp").getTime());
                    
                    return new VitalSign(heartRate, oxygenLevel, bloodPressure, temperature, timestamp);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving latest vital sign from database: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Deletes all vital signs for a specific patient
     * @param userId The ID of the user (patient)
     * @return True if deletion was successful, false otherwise
     */
    public boolean deleteVitalSignsForPatient(int userId) {
        String sql = "DELETE FROM Patient_Vitals WHERE user_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error deleting vital signs from database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Gets vital signs for a patient within a date range
     * @param userId The ID of the user (patient)
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     * @return List of vital signs within the specified date range
     */
    public List<VitalSign> getVitalSignsInDateRange(int userId, Date startDate, Date endDate) {
        List<VitalSign> vitals = new ArrayList<>();
        String sql = "SELECT * FROM Patient_Vitals WHERE user_id = ? AND timestamp BETWEEN ? AND ? ORDER BY timestamp ASC";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setTimestamp(2, new Timestamp(startDate.getTime()));
            stmt.setTimestamp(3, new Timestamp(endDate.getTime()));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    double heartRate = rs.getDouble("heart_rate");
                    double oxygenLevel = rs.getDouble("oxygen_level");
                    double bloodPressure = rs.getDouble("blood_pressure");
                    double temperature = rs.getDouble("temperature");
                    Date timestamp = new Date(rs.getTimestamp("timestamp").getTime());
                    
                    VitalSign vital = new VitalSign(heartRate, oxygenLevel, bloodPressure, temperature, timestamp);
                    vitals.add(vital);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving vital signs in date range from database: " + e.getMessage());
            e.printStackTrace();
        }
        
        return vitals;
    }
    
    /**
     * Creates the Patient_Vitals table if it doesn't exist
     * @return True if table was created or already exists, false on error
     */
    public boolean createVitalsTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS Patient_Vitals (" +
                     "vital_id INT AUTO_INCREMENT PRIMARY KEY, " +
                     "user_id INT NOT NULL, " +
                     "heart_rate DOUBLE, " +
                     "oxygen_level DOUBLE, " +
                     "blood_pressure DOUBLE, " +
                     "temperature DOUBLE, " +
                     "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                     "FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE" +
                     ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            return true;
        } catch (SQLException e) {
            System.err.println("Error creating Patient_Vitals table: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets vital signs for a patient within a date range (alias for getVitalSignsInDateRange)
     * @param userID The ID of the user (patient)
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     * @return List of vital signs within the specified date range
     */
    public List<VitalSign> getVitalsInDateRange(int userID, Date startDate, Date endDate) {
        return getVitalSignsInDateRange(userID, startDate, endDate);
    }


}

