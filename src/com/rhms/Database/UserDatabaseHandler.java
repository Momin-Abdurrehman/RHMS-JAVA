package com.rhms.Database;


import com.rhms.userManagement.User;
import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;
import com.rhms.userManagement.Administrator;
import com.rhms.loginSystem.Session;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDatabaseHandler {
    private Connection connection;

    public UserDatabaseHandler() {
        this.connection = DatabaseConnection.getConnection();
    }

    // Add a new user to the database
    public boolean addUser(User user) {
        if (user == null) return false;

        Connection conn = null;
        PreparedStatement userStmt = null;
        PreparedStatement doctorStmt = null;
        ResultSet generatedKeys = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Insert into users table
            String userSql = "INSERT INTO users (name, email, password_hash, phone, address, username, user_type) VALUES (?, ?, ?, ?, ?, ?, ?)";
            userStmt = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS);
            userStmt.setString(1, user.getName());
            userStmt.setString(2, user.getEmail());
            userStmt.setString(3, user.getPassword());
            userStmt.setString(4, user.getPhone());
            userStmt.setString(5, user.getAddress());
            userStmt.setString(6, user.getUsername());
            userStmt.setString(7, user.getClass().getSimpleName().toLowerCase()); // e.g., "doctor", "patient", etc.

            int affectedRows = userStmt.executeUpdate();
            if (affectedRows == 0) {
                conn.rollback();
                return false;
            }

            generatedKeys = userStmt.getGeneratedKeys();
            int userId;
            if (generatedKeys.next()) {
                userId = generatedKeys.getInt(1);
                user.setUserID(userId); // Set the generated ID in the object
            } else {
                conn.rollback();
                return false;
            }

            // If user is a doctor, insert into doctors table
            if (user instanceof Doctor) {
                Doctor doctor = (Doctor) user;
                String doctorSql = "INSERT INTO doctors (user_id, specialization, experience_years) VALUES (?, ?, ?)";
                doctorStmt = conn.prepareStatement(doctorSql);
                doctorStmt.setInt(1, userId);
                doctorStmt.setString(2, doctor.getSpecialization());
                doctorStmt.setInt(3, doctor.getExperienceYears());
                doctorStmt.executeUpdate();
            }

            // Add similar logic for Patient/Administrator if needed

            conn.commit();
            return true;
        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { /* ignore */ }
            e.printStackTrace();
            return false;
        } finally {
            try { if (generatedKeys != null) generatedKeys.close(); } catch (SQLException e) {}
            try { if (userStmt != null) userStmt.close(); } catch (SQLException e) {}
            try { if (doctorStmt != null) doctorStmt.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.setAutoCommit(true); } catch (SQLException e) {}
        }


    }

    // Check if an email already exists in the database
    public boolean isEmailExists(String email) {
        String sql = "SELECT COUNT(*) FROM Users WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("Error checking email existence: " + e.getMessage());
            return false;
        }
    }

    // Check if a username already exists in the database
    public boolean isUsernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM Users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("Error checking username existence: " + e.getMessage());
            return false;
        }
    }

    // Fetch a user by their username
    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM Users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String userType = rs.getString("user_type");

                // Create the appropriate user type based on the database value
                if ("Administrator".equals(userType)) {
                    return new Administrator(
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getInt("user_id"),
                        rs.getString("username")
                    );
                } else if ("Doctor".equals(userType)) {
                    String specialization = "General";
                    int experienceYears = 0;
                    try {
                        specialization = rs.getString("specialization");
                        experienceYears = rs.getInt("experience_years");
                    } catch (SQLException e) {
                        // Missing doctor-specific columns; use default values.
                    }
                    return new Doctor(
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        specialization,
                        experienceYears
                    );
                } else if ("Patient".equals(userType)) {
                    return new Patient(
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getInt("user_id"),
                        rs.getString("username")
                    );
                } else {
                    // Default fallback for unknown user types
                    return new User(
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getInt("user_id"),
                        rs.getString("username")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user by username: " + e.getMessage());
        }
        return null;
    }

    // Save a session to the database
    public boolean saveSession(Session session) {
        String sql = "INSERT INTO Sessions (session_token, user_id, creation_time, last_activity_time) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, session.getSessionToken());
            stmt.setInt(2, session.getUser().getUserID());
            stmt.setTimestamp(3, Timestamp.valueOf(session.getCreationTime()));
            stmt.setTimestamp(4, Timestamp.valueOf(session.getLastActivityTime()));
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error saving session: " + e.getMessage());
            return false;
        }
    }

    // Fetch a session by its token
    public Session getSessionByToken(String sessionToken) {
        String sql = "SELECT * FROM Sessions WHERE session_token = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, sessionToken);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                User user = getUserById(rs.getInt("user_id"));
                if (user != null) {
                    return new Session(
                        sessionToken,
                        user,
                        rs.getTimestamp("creation_time").toLocalDateTime(),
                        rs.getTimestamp("last_activity_time").toLocalDateTime()
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching session by token: " + e.getMessage());
        }
        return null;
    }

    // Update session activity in the database
    public boolean updateSessionActivity(Session session) {
        String sql = "UPDATE Sessions SET last_activity_time = ? WHERE session_token = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(session.getLastActivityTime()));
            stmt.setString(2, session.getSessionToken());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error updating session activity: " + e.getMessage());
            return false;
        }
    }

    // Delete a session by its token
    public boolean deleteSession(String sessionToken) {
        String sql = "DELETE FROM Sessions WHERE session_token = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, sessionToken);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error deleting session: " + e.getMessage());
            return false;
        }
    }

    // Fetch a user by their ID
    private User getUserById(int userId) {
        String sql = "SELECT * FROM Users WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String userType = rs.getString("user_type");

                // Create the appropriate user type based on the database value
                if ("Administrator".equals(userType)) {
                    return new Administrator(
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getInt("user_id"),
                        rs.getString("username")
                    );
                } else if ("Doctor".equals(userType)) {
                    String specialization = "General";
                    int experienceYears = 0;
                    try {
                        specialization = rs.getString("specialization");
                        experienceYears = rs.getInt("experience_years");
                    } catch (SQLException e) {
                        // Missing doctor-specific columns; use default values.
                    }
                    return new Doctor(
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        specialization,
                        experienceYears
                    );
                } else if ("Patient".equals(userType)) {
                    return new Patient(
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getInt("user_id"),
                        rs.getString("username")
                    );
                } else {
                    // Default fallback for unknown user types
                    return new User(
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        userId,
                        rs.getString("username")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user by ID: " + e.getMessage());
        }
        return null;
    }

    // Fetch all users from the database
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM Users";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String userType = rs.getString("user_type");
                User user = null;

                if ("Doctor".equals(userType)) {
                    String specialization = "General";
                    int experienceYears = 0;
                    try {
                        specialization = rs.getString("specialization");
                        experienceYears = rs.getInt("experience_years");
                    } catch (SQLException e) {
                        // Use default values if columns are missing.
                    }
                    user = new Doctor(
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        specialization,
                        experienceYears
                    );
                } else if ("Patient".equals(userType)) {
                    user = new Patient(
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getInt("user_id"),
                        rs.getString("username")
                    );
                } else if ("Administrator".equals(userType)) {
                    user = new Administrator(
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getInt("user_id"),
                        rs.getString("username")
                    );
                } else {
                    user = new User(
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getInt("user_id"),
                        rs.getString("username")
                    );
                }

                if (user != null) {
                    users.add(user);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all users: " + e.getMessage());
        }
        return users;
    }
    public Patient getPatientById(int patientId) {
        String sql = "SELECT * FROM Users WHERE user_id = ? AND user_type = 'Patient'";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, patientId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Patient(
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getInt("user_id"),
                        rs.getString("username")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching patient by ID: " + e.getMessage());
        }
        return null;
    }

    public Doctor getDoctorById(int doctorId) {
        String sql = "SELECT * FROM Users WHERE user_id = ? AND user_type = 'Doctor'";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, doctorId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String specialization = "General";
                int experienceYears = 0;
                try {
                    // Try to get specialization and experience_years columns if they exist
                    specialization = rs.getString("specialization");
                } catch (SQLException | IllegalArgumentException e) {
                    // Column not found or not present, use default
                }
                try {
                    experienceYears = rs.getInt("experience_years");
                } catch (SQLException | IllegalArgumentException e) {
                    // Column not found or not present, use default
                }
                return new Doctor(
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        specialization,
                        experienceYears
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching doctor by ID: " + e.getMessage());
        }
        return null;
    }

}

