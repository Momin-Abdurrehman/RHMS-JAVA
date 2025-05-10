package com.rhms.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler class for database operations related to doctor requests
 */
public class DoctorRequestDatabaseHandler {
    private static final Logger LOGGER = Logger.getLogger(DoctorRequestDatabaseHandler.class.getName());
    private Connection connection;

    /**
     * Constructor that initializes the database connection
     */
    public DoctorRequestDatabaseHandler() {
        try {
            this.connection = DatabaseConnection.getConnection();
            if (this.connection == null) {
                LOGGER.log(Level.SEVERE, "Failed to establish database connection in constructor");
                throw new SQLException("Could not establish database connection");
            }
            ensureTableExists();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error initializing DoctorRequestDatabaseHandler: {0}", e.getMessage());
        }
    }

    /**
     * Ensures the Doctor_Requests table exists in the database
     */
    private void ensureTableExists() {
        if (connection == null) {
            LOGGER.log(Level.SEVERE, "Cannot ensure table exists: Database connection is null");
            return;
        }

        try {
            if (connection.isClosed()) {
                LOGGER.log(Level.WARNING, "Database connection is closed, attempting to refresh");
                connection = DatabaseConnection.getConnection();
                if (connection == null || connection.isClosed()) {
                    LOGGER.log(Level.SEVERE, "Failed to refresh database connection");
                    return;
                }
            }

            String createTableSQL = 
                "CREATE TABLE IF NOT EXISTS Doctor_Requests (" +
                "request_id INT AUTO_INCREMENT PRIMARY KEY," +
                "user_id INT NOT NULL," +
                "request_type VARCHAR(50) NOT NULL," +
                "doctor_specialization VARCHAR(100)," +
                "additional_details TEXT," +
                "request_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "status VARCHAR(20) DEFAULT 'Pending'," +
                "FOREIGN KEY (user_id) REFERENCES Users(user_id)" +
                ")";

            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createTableSQL);
                LOGGER.log(Level.INFO, "Doctor_Requests table checked/created successfully");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error ensuring Doctor_Requests table exists: {0}", e.getMessage());
            LOGGER.log(Level.SEVERE, "SQL State: {0}, Error Code: {1}", 
                      new Object[]{e.getSQLState(), e.getErrorCode()});
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error ensuring table exists: {0}", e.getMessage());
        }
    }

    /**
     * Get a fresh database connection if current one is closed
     * @throws SQLException if connection cannot be established
     */
    private void refreshConnectionIfNeeded() throws SQLException {
        boolean needsRefresh = false;
        
        try {
            if (connection == null || connection.isClosed()) {
                needsRefresh = true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error checking connection status: {0}", e.getMessage());
            needsRefresh = true;
        }
        
        if (needsRefresh) {
            LOGGER.log(Level.INFO, "Refreshing database connection");
            Connection newConnection = DatabaseConnection.getConnection();
            if (newConnection == null) {
                LOGGER.log(Level.SEVERE, "Failed to get new database connection");
                throw new SQLException("Failed to establish database connection");
            }
            connection = newConnection;
        }
    }

    /**
     * Adds a new doctor request to the database
     * 
     * @param userId The ID of the user (patient) making the request
     * @param requestType The type of request (e.g., "New Doctor", "Change Doctor")
     * @param doctorSpecialization The requested doctor specialization (can be null)
     * @param additionalDetails Additional details about the request (can be null)
     * @return The ID of the newly created request, or -1 if the operation failed
     * @throws SQLException If a database error occurs
     */
    public int addDoctorRequest(int userId, String requestType, String doctorSpecialization, 
                              String additionalDetails) throws SQLException {
        if (userId <= 0 || requestType == null || requestType.isEmpty()) {
            LOGGER.log(Level.SEVERE, "Invalid parameters for doctor request: userId={0}, requestType={1}", 
                      new Object[]{userId, requestType});
            throw new IllegalArgumentException("User ID and request type are required");
        }
        
        try {
            refreshConnectionIfNeeded();
            
            String sql = "INSERT INTO Doctor_Requests (user_id, request_type, doctor_specialization, additional_details) " +
                         "VALUES (?, ?, ?, ?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, userId);
                stmt.setString(2, requestType);
                stmt.setString(3, doctorSpecialization);
                stmt.setString(4, additionalDetails);
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int requestId = generatedKeys.getInt(1);
                            LOGGER.log(Level.INFO, "Added doctor request with ID {0} for user {1}", 
                                      new Object[]{requestId, userId});
                            return requestId;
                        }
                    }
                }
                
                LOGGER.log(Level.WARNING, "Failed to add doctor request for user {0}", userId);
                return -1;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in addDoctorRequest: {0}", e.getMessage());
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in addDoctorRequest: {0}", e.getMessage());
            throw new SQLException("Unexpected error occurred", e);
        }
    }

    /**
     * Updates the status of an existing doctor request
     * 
     * @param requestId The ID of the request to update
     * @param newStatus The new status to set (e.g., "Approved", "Rejected", "Pending")
     * @return true if the update was successful, false otherwise
     * @throws SQLException If a database error occurs
     */
    public boolean updateRequestStatus(int requestId, String newStatus) throws SQLException {
        if (requestId <= 0 || newStatus == null || newStatus.isEmpty()) {
            LOGGER.log(Level.SEVERE, "Invalid parameters for updating request status: requestId={0}, newStatus={1}", 
                      new Object[]{requestId, newStatus});
            throw new IllegalArgumentException("Request ID and new status are required");
        }
        
        try {
            refreshConnectionIfNeeded();
            
            String sql = "UPDATE Doctor_Requests SET status = ? WHERE request_id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, newStatus);
                stmt.setInt(2, requestId);
                
                int rowsAffected = stmt.executeUpdate();
                boolean success = rowsAffected > 0;
                
                if (success) {
                    LOGGER.log(Level.INFO, "Updated request {0} status to {1}", new Object[]{requestId, newStatus});
                } else {
                    LOGGER.log(Level.WARNING, "Failed to update request {0} status: Request not found", requestId);
                }
                
                return success;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in updateRequestStatus: {0}", e.getMessage());
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in updateRequestStatus: {0}", e.getMessage());
            throw new SQLException("Unexpected error occurred", e);
        }
    }

    /**
     * Retrieves all pending doctor requests from the database
     * 
     * @return A list of pending doctor requests as maps of column name to value
     * @throws SQLException If a database error occurs
     */
    public List<DoctorRequest> getPendingRequests() throws SQLException {
        return getRequestsByStatus("Pending");
    }

    /**
     * Retrieves all doctor requests from the database
     * 
     * @return A list of all doctor requests as maps of column name to value
     * @throws SQLException If a database error occurs
     */
    public List<DoctorRequest> getAllRequests() throws SQLException {
        try {
            refreshConnectionIfNeeded();
            
            String sql = "SELECT * FROM Doctor_Requests ORDER BY request_date DESC";
            
            try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
                
                return extractRequestsFromResultSet(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in getAllRequests: {0}", e.getMessage());
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in getAllRequests: {0}", e.getMessage());
            throw new SQLException("Unexpected error occurred", e);
        }
    }

    /**
     * Retrieves doctor requests with a specific status
     * 
     * @param status The status to filter by
     * @return A list of doctor requests with the specified status
     * @throws SQLException If a database error occurs
     */
    public List<DoctorRequest> getRequestsByStatus(String status) throws SQLException {
        if (status == null || status.isEmpty()) {
            LOGGER.log(Level.SEVERE, "Invalid status parameter: {0}", status);
            throw new IllegalArgumentException("Status cannot be null or empty");
        }
        
        try {
            refreshConnectionIfNeeded();
            
            String sql = "SELECT * FROM Doctor_Requests WHERE status = ? ORDER BY request_date DESC";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, status);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    return extractRequestsFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in getRequestsByStatus: {0}", e.getMessage());
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in getRequestsByStatus: {0}", e.getMessage());
            throw new SQLException("Unexpected error occurred", e);
        }
    }

    /**
     * Retrieves doctor requests for a specific patient/user
     * 
     * @param userId The ID of the user/patient
     * @return A list of doctor requests for the specified user
     * @throws SQLException If a database error occurs
     */
    public List<DoctorRequest> getRequestsByPatient(int userId) throws SQLException {
        if (userId <= 0) {
            LOGGER.log(Level.SEVERE, "Invalid user ID: {0}", userId);
            throw new IllegalArgumentException("User ID must be positive");
        }
        
        try {
            refreshConnectionIfNeeded();
            
            String sql = "SELECT * FROM Doctor_Requests WHERE user_id = ? ORDER BY request_date DESC";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    return extractRequestsFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in getRequestsByPatient: {0}", e.getMessage());
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in getRequestsByPatient: {0}", e.getMessage());
            throw new SQLException("Unexpected error occurred", e);
        }
    }

    /**
     * Retrieves a specific doctor request by ID
     * 
     * @param requestId The ID of the request to retrieve
     * @return The doctor request, or null if not found
     * @throws SQLException If a database error occurs
     */
    public DoctorRequest getRequestById(int requestId) throws SQLException {
        if (requestId <= 0) {
            LOGGER.log(Level.SEVERE, "Invalid request ID: {0}", requestId);
            throw new IllegalArgumentException("Request ID must be positive");
        }
        
        try {
            refreshConnectionIfNeeded();
            
            String sql = "SELECT * FROM Doctor_Requests WHERE request_id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, requestId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    List<DoctorRequest> requests = extractRequestsFromResultSet(rs);
                    return requests.isEmpty() ? null : requests.get(0);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in getRequestById: {0}", e.getMessage());
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in getRequestById: {0}", e.getMessage());
            throw new SQLException("Unexpected error occurred", e);
        }
    }

    /**
     * Helper method to extract doctor requests from a ResultSet
     */
    private List<DoctorRequest> extractRequestsFromResultSet(ResultSet rs) throws SQLException {
        List<DoctorRequest> requests = new ArrayList<>();
        
        while (rs.next()) {
            DoctorRequest request = new DoctorRequest(
                rs.getInt("request_id"),
                rs.getInt("user_id"),
                rs.getString("request_type"),
                rs.getString("doctor_specialization"),
                rs.getString("additional_details"),
                rs.getTimestamp("request_date"),
                rs.getString("status")
            );
            requests.add(request);
        }
        
        LOGGER.log(Level.INFO, "Retrieved {0} doctor requests", requests.size());
        return requests;
    }

    /**
     * Inner class to represent a Doctor Request with all its attributes
     */
    public static class DoctorRequest {
        private int requestId;
        private int userId;
        private String requestType;
        private String doctorSpecialization;
        private String additionalDetails;
        private Timestamp requestDate;
        private String status;
        
        public DoctorRequest(int requestId, int userId, String requestType, String doctorSpecialization,
                           String additionalDetails, Timestamp requestDate, String status) {
            this.requestId = requestId;
            this.userId = userId;
            this.requestType = requestType;
            this.doctorSpecialization = doctorSpecialization;
            this.additionalDetails = additionalDetails;
            this.requestDate = requestDate;
            this.status = status;
        }
        
        // Getters
        public int getRequestId() { return requestId; }
        public int getUserId() { return userId; }
        public String getRequestType() { return requestType; }
        public String getDoctorSpecialization() { return doctorSpecialization; }
        public String getAdditionalDetails() { return additionalDetails; }
        public Timestamp getRequestDate() { return requestDate; }
        public String getStatus() { return status; }
        
        @Override
        public String toString() {
            return "DoctorRequest{" +
                   "requestId=" + requestId +
                   ", userId=" + userId +
                   ", requestType='" + requestType + '\'' +
                   ", doctorSpecialization='" + doctorSpecialization + '\'' +
                   ", requestDate=" + requestDate +
                   ", status='" + status + '\'' +
                   '}';
        }
    }
}
