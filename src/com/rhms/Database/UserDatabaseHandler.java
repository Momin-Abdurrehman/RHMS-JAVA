package com.rhms.Database;


import com.rhms.userManagement.User;
import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;
import com.rhms.userManagement.Administrator;
import com.rhms.doctorPatientInteraction.Feedback;
import com.rhms.doctorPatientInteraction.Prescription;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDatabaseHandler {
    public Connection connection;

    public UserDatabaseHandler() {
        this.connection = DatabaseConnection.getConnection();
    }

    // Add a new user to the database
    public boolean addUser(User user) {
        String sql = "INSERT INTO Users (username, password_hash, name, email, phone, address, user_type) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getName());
            stmt.setString(4, user.getEmail());
            stmt.setString(5, user.getPhone());
            stmt.setString(6, user.getAddress());
            stmt.setString(7, user.getClass().getSimpleName());
            stmt.executeUpdate();

            // Get generated user_id
            int userId = -1;
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    userId = generatedKeys.getInt(1);
                }
            }

            // If user is a Doctor, insert into Doctors table as well
            if (user instanceof Doctor) {
                Doctor doctor = (Doctor) user;
                int doctorUserId = (userId != -1) ? userId : doctor.getUserID();
                String doctorSql = "INSERT INTO Doctors (user_id, specialization, experience_years) VALUES (?, ?, ?)";
                try (PreparedStatement doctorStmt = connection.prepareStatement(doctorSql)) {
                    doctorStmt.setInt(1, doctorUserId);
                    doctorStmt.setString(2, doctor.getSpecialization());
                    doctorStmt.setInt(3, doctor.getExperienceYears());
                    doctorStmt.executeUpdate();
                }
            }

            // If user is a Patient, insert into Patients table as well
            if (user instanceof Patient) {
                Patient patient = (Patient) user;
                int patientUserId = (userId != -1) ? userId : patient.getUserID();
                // Ensure patient_id and user_id are the same and insert only if not exists
                String checkSql = "SELECT COUNT(*) FROM Patients WHERE patient_id = ?";
                try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                    checkStmt.setInt(1, patientUserId);
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next() && rs.getInt(1) == 0) {
                        String patientSql = "INSERT INTO Patients (user_id, patient_id, emergency_contact) VALUES (?, ?, ?)";
                        try (PreparedStatement patientStmt = connection.prepareStatement(patientSql)) {
                            patientStmt.setInt(1, patientUserId);
                            patientStmt.setInt(2, patientUserId); // patient_id same as user_id
                            patientStmt.setString(3, patient.getEmergencyContact() != null ? patient.getEmergencyContact() : "");
                            patientStmt.executeUpdate();
                        }
                    }
                }
            }

            return true;
        } catch (SQLException e) {
            System.err.println("Error adding user to database: " + e.getMessage());
            return false;
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


    // Fetch a user by their ID
    public User getUserById(int userId) {
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

    // Add feedback given by a patient to a doctor
    public boolean addPatientFeedback(int patientId, int doctorUserId, String feedbackText, int rating) {
        // Ensure patient exists in Patients table before inserting feedback
        String checkPatientSql = "SELECT COUNT(*) FROM Patients WHERE patient_id = ?";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkPatientSql)) {
            checkStmt.setInt(1, patientId);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                System.err.println("Cannot add feedback: patient_id " + patientId + " does not exist in Patients table.");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Error checking patient existence before adding feedback: " + e.getMessage());
            return false;
        }

        // Retrieve the doctor's doctor_id from the Doctors table using user_id
        int doctorId = -1;
        String getDoctorIdSql = "SELECT doctor_id FROM Doctors WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(getDoctorIdSql)) {
            stmt.setInt(1, doctorUserId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                doctorId = rs.getInt("doctor_id");
            } else {
                System.err.println("Cannot add feedback: doctor with user_id " + doctorUserId + " does not exist in Doctors table.");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving doctor_id from Doctors table: " + e.getMessage());
            return false;
        }

        String sql = "INSERT INTO feedback_by_patient (patient_id, doctor_id, feedback_text, rating, submitted_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, patientId);
            stmt.setInt(2, doctorId); // Use doctor_id from Doctors table
            stmt.setString(3, feedbackText);
            stmt.setInt(4, rating);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error adding patient feedback: " + e.getMessage());
            return false;
        }
    }

    // Retrieve feedbacks given by a patient (optionally for a specific doctor)
    public List<String> getPatientFeedbacks(int patientId, Integer doctorId) {
        List<String> feedbacks = new ArrayList<>();
        String sql = "SELECT doctor_id, feedback_text, rating, submitted_at FROM feedback_by_patient WHERE patient_id = ?";
        if (doctorId != null) {
            sql += " AND doctor_id = ?";
        }
        sql += " ORDER BY submitted_at DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, patientId);
            if (doctorId != null) {
                stmt.setInt(2, doctorId);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int docId = rs.getInt("doctor_id");
                String text = rs.getString("feedback_text");
                int rating = rs.getInt("rating");
                String submittedAt = rs.getString("submitted_at");
                feedbacks.add("Doctor ID: " + docId + " | Rating: " + rating + " | " + text + " (" + submittedAt + ")");
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving patient feedbacks: " + e.getMessage());
        }
        return feedbacks;
    }

    // Fetch a patient by email
    public Patient getPatientByEmail(String email) {
        String sql = "SELECT * FROM Users WHERE email = ? AND user_type = 'Patient'";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
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
            System.err.println("Error fetching patient by email: " + e.getMessage());
        }
        return null;
    }

    // Fetch a doctor by email
    public Doctor getDoctorByEmail(String email) {
        String sql = "SELECT * FROM Users WHERE email = ? AND user_type = 'Doctor'";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String specialization = "General";
                int experienceYears = 0;
                try {
                    specialization = rs.getString("specialization");
                } catch (SQLException | IllegalArgumentException e) {}
                try {
                    experienceYears = rs.getInt("experience_years");
                } catch (SQLException | IllegalArgumentException e) {}
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
            System.err.println("Error fetching doctor by email: " + e.getMessage());
        }
        return null;
    }

    // Fetch an administrator by email
    public Administrator getAdminByEmail(String email) {
        String sql = "SELECT * FROM Users WHERE email = ? AND user_type = 'Administrator'";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Administrator(
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
            System.err.println("Error fetching admin by email: " + e.getMessage());
        }
        return null;
    }

    // Delete a user by email (for rollback)
    public boolean deleteUserByEmail(String email) {
        String sql = "DELETE FROM Users WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting user by email: " + e.getMessage());
            return false;
        }
    }

    // Store feedback and prescription given by a doctor to a patient
    public boolean addDoctorFeedback(Feedback feedback) {
        if (feedback == null || feedback.getPrescription() == null) {
            System.err.println("Prescription is required for feedback.");
            return false;
        }
        Connection conn = null;
        PreparedStatement feedbackStmt = null;
        PreparedStatement prescriptionStmt = null;
        ResultSet feedbackKeys = null;
        ResultSet prescriptionKeys = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // --- Fetch doctor_id from Doctors table using user_id ---
            int doctorUserId = feedback.getDoctor().getUserID();
            int doctorId = -1;
            String getDoctorIdSql = "SELECT doctor_id FROM Doctors WHERE user_id = ?";
            try (PreparedStatement getDoctorIdStmt = conn.prepareStatement(getDoctorIdSql)) {
                getDoctorIdStmt.setInt(1, doctorUserId);
                try (ResultSet rs = getDoctorIdStmt.executeQuery()) {
                    if (rs.next()) {
                        doctorId = rs.getInt("doctor_id");
                    } else {
                        conn.rollback();
                        System.err.println("Cannot add feedback: doctor with user_id " + doctorUserId + " does not exist in Doctors table.");
                        return false;
                    }
                }
            }

            // --- Insert into feedback_by_doctor using doctor_id ---
            String feedbackSql = "INSERT INTO feedback_by_doctor (doctor_id, patient_id, comments, timestamp) VALUES (?, ?, ?, ?)";
            feedbackStmt = conn.prepareStatement(feedbackSql, Statement.RETURN_GENERATED_KEYS);
            feedbackStmt.setInt(1, doctorId); // Use doctor_id from Doctors table
            feedbackStmt.setInt(2, feedback.getPatient().getUserID());
            feedbackStmt.setString(3, feedback.getMessage());
            feedbackStmt.setTimestamp(4, new Timestamp(feedback.getTimestamp().getTime()));
            int affectedRows = feedbackStmt.executeUpdate();
            if (affectedRows == 0) {
                conn.rollback();
                System.err.println("Failed to insert feedback.");
                return false;
            }
            feedbackKeys = feedbackStmt.getGeneratedKeys();
            int feedbackId = -1;
            if (feedbackKeys.next()) {
                feedbackId = feedbackKeys.getInt(1);
                feedback.setFeedbackId(feedbackId);
            } else {
                conn.rollback();
                System.err.println("Failed to retrieve feedback_id.");
                return false;
            }

            // Insert into prescription
            Prescription prescription = feedback.getPrescription();
            String prescriptionSql = "INSERT INTO prescription (feedback_id, medication_name, dosage, schedule, duration, instructions) VALUES (?, ?, ?, ?, ?, ?)";
            prescriptionStmt = conn.prepareStatement(prescriptionSql, Statement.RETURN_GENERATED_KEYS);
            prescriptionStmt.setInt(1, feedbackId);
            prescriptionStmt.setString(2, prescription.getMedicationName());
            prescriptionStmt.setString(3, prescription.getDosage());
            prescriptionStmt.setString(4, prescription.getSchedule());
            prescriptionStmt.setString(5, prescription.getDuration());
            prescriptionStmt.setString(6, prescription.getInstructions());
            int presRows = prescriptionStmt.executeUpdate();
            if (presRows == 0) {
                conn.rollback();
                System.err.println("Failed to insert prescription.");
                return false;
            }
            prescriptionKeys = prescriptionStmt.getGeneratedKeys();
            if (prescriptionKeys.next()) {
                int prescriptionId = prescriptionKeys.getInt(1);
                feedback.setPrescriptionId(prescriptionId);
                prescription.setPrescriptionId(prescriptionId);
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (Exception ex) {}
            System.err.println("Error adding doctor feedback and prescription: " + e.getMessage());
            e.printStackTrace(); // Add stack trace for debugging
            return false;
        } finally {
            try { if (feedbackKeys != null) feedbackKeys.close(); } catch (Exception e) {}
            try { if (prescriptionKeys != null) prescriptionKeys.close(); } catch (Exception e) {}
            try { if (feedbackStmt != null) feedbackStmt.close(); } catch (Exception e) {}
            try { if (prescriptionStmt != null) prescriptionStmt.close(); } catch (Exception e) {}
            try { if (conn != null) conn.setAutoCommit(true); } catch (Exception e) {}
        }
    }

}

