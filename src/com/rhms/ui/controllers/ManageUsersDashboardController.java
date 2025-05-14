package com.rhms.ui.controllers;

import com.rhms.userManagement.*;
import com.rhms.Database.UserDatabaseHandler;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.util.List;
import java.util.Optional;

public class ManageUsersDashboardController {

    @FXML private ComboBox<String> userTypeFilter;
    @FXML private TextField searchField;
    @FXML private TableView<User> usersTableView;
    @FXML private TableColumn<User, String> idColumn;
    @FXML private TableColumn<User, String> nameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> userTypeColumn;
    @FXML private Label totalUsersLabel;
    @FXML private Label statusLabel;

    private UserManager userManager;
    private ObservableList<User> allUsers = FXCollections.observableArrayList();
    private FilteredList<User> filteredUsers;

    /**
     * Initialize the controller
     */
    public void initialize() {
        // Setup user type options
        userTypeFilter.getItems().addAll("All Users", "Administrators", "Doctors", "Patients");
        userTypeFilter.setValue("All Users");

        // Setup table columns
        setupTableColumns();

        // Setup filtering
        setupFiltering();
    }

    /**
     * Set the UserManager for this controller
     * @param userManager The UserManager to use
     */
    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
        loadUsers();
    }

    /**
     * Load users from the UserManager
     */
    private void loadUsers() {
        allUsers.clear();

        if (userManager != null) {
            allUsers.addAll(userManager.getAllAdministrators());
            allUsers.addAll(userManager.getAllDoctors());
            allUsers.addAll(userManager.getAllPatients());

            totalUsersLabel.setText("Total Users: " + allUsers.size());
        }
    }

    /**
     * Setup table columns for displaying user data
     */
    private void setupTableColumns() {
        idColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.valueOf(cellData.getValue().getUserID())));

        nameColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getName()));

        emailColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getEmail()));

        userTypeColumn.setCellValueFactory(cellData -> {
            User user = cellData.getValue();
            String userType = "User";

            if (user instanceof Administrator) {
                userType = "Administrator";
            } else if (user instanceof Doctor) {
                userType = "Doctor";
            } else if (user instanceof Patient) {
                userType = "Patient";
            }

            return new SimpleStringProperty(userType);
        });
    }

    /**
     * Setup filtering functionality for the users table
     */
    private void setupFiltering() {
        filteredUsers = new FilteredList<>(allUsers, p -> true);

        userTypeFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateFilter();
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            updateFilter();
        });

        usersTableView.setItems(filteredUsers);
    }

    /**
     * Update the filter based on search text and selected user type
     */
    private void updateFilter() {
        String userType = userTypeFilter.getValue();
        String searchText = searchField.getText().toLowerCase();

        filteredUsers.setPredicate(user -> {
            // Filter by user type
            if (!"All Users".equals(userType)) {
                if ("Administrators".equals(userType) && !(user instanceof Administrator)) {
                    return false;
                }
                if ("Doctors".equals(userType) && !(user instanceof Doctor)) {
                    return false;
                }
                if ("Patients".equals(userType) && !(user instanceof Patient)) {
                    return false;
                }
            }

            // Filter by search text
            if (searchText == null || searchText.isEmpty()) {
                return true;
            }

            return user.getName().toLowerCase().contains(searchText) ||
                   user.getEmail().toLowerCase().contains(searchText);
        });

        totalUsersLabel.setText("Total Users: " + filteredUsers.size());
    }

    /**
     * Handle searching for users
     */
    @FXML
    public void handleSearch(ActionEvent event) {
        updateFilter();
    }

    /**
     * Handle removing a user
     */
    @FXML
    public void handleRemoveUser(ActionEvent event) {
        User selectedUser = usersTableView.getSelectionModel().getSelectedItem();

        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "No User Selected",
                    "Please select a user to remove from the table.");
            return;
        }

        // Confirm deletion
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Removal");
        confirmDialog.setHeaderText("Remove User");
        confirmDialog.setContentText("Are you sure you want to remove " + selectedUser.getName() +
                                    " from the system? This action cannot be undone.");

        Optional<ButtonType> result = confirmDialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean removed = false;
            try {
                // Remove from database
                com.rhms.Database.DatabaseConnection.getConnection().setAutoCommit(false);
                java.sql.Connection conn = com.rhms.Database.DatabaseConnection.getConnection();

                // --- Delete from chat_messages (as sender or receiver) ---
                try (java.sql.PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM chat_messages WHERE sender_id = ? OR receiver_id = ?")) {
                    stmt.setInt(1, selectedUser.getUserID());
                    stmt.setInt(2, selectedUser.getUserID());
                    stmt.executeUpdate();
                }

                // --- Delete from Doctor_Requests ---
                try (java.sql.PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM Doctor_Requests WHERE user_id = ?")) {
                    stmt.setInt(1, selectedUser.getUserID());
                    stmt.executeUpdate();
                }

                // --- Delete from doctor_patient_assignments (as doctor or patient) ---
                try (java.sql.PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM doctor_patient_assignments WHERE doctor_id = ? OR patient_id = ?")) {
                    stmt.setInt(1, selectedUser.getUserID());
                    stmt.setInt(2, selectedUser.getUserID());
                    stmt.executeUpdate();
                }
                // --- Delete prescriptions first (that reference feedback by doctor) ---
                if (selectedUser instanceof Doctor || selectedUser instanceof Patient) {
                    try (java.sql.PreparedStatement stmt = conn.prepareStatement(
                            "DELETE p FROM prescription p " +
                                    "JOIN feedback_by_doctor f ON p.feedback_id = f.feedback_id " +
                                    "WHERE f.doctor_id = ? OR f.patient_id = ?")) {
                        stmt.setInt(1, selectedUser.getUserID());
                        stmt.setInt(2, selectedUser.getUserID());
                        stmt.executeUpdate();
                    }
                }

                // --- Delete from feedback_by_patient (as doctor or patient) ---
                try (java.sql.PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM feedback_by_patient WHERE doctor_id = ? OR patient_id = ?")) {
                    stmt.setInt(1, selectedUser.getUserID());
                    stmt.setInt(2, selectedUser.getUserID());
                    stmt.executeUpdate();
                }
                // --- Delete from feedback_by_doctor (as doctor or patient) ---
                try (java.sql.PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM feedback_by_doctor WHERE doctor_id = ? OR patient_id = ?")) {
                    stmt.setInt(1, selectedUser.getUserID());
                    stmt.setInt(2, selectedUser.getUserID());
                    stmt.executeUpdate();
                }



                // --- Delete from appointments (as doctor or patient) ---
                try (java.sql.PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM appointments WHERE doctor_id = ? OR patient_id = ?")) {
                    stmt.setInt(1, selectedUser.getUserID());
                    stmt.setInt(2, selectedUser.getUserID());
                    stmt.executeUpdate();
                }

                // --- Delete from patient_vitals (if patient) ---
                if (selectedUser instanceof Patient) {
                    try (java.sql.PreparedStatement stmt = conn.prepareStatement(
                            "DELETE FROM patient_vitals WHERE user_id = ?")) {
                        stmt.setInt(1, selectedUser.getUserID());
                        stmt.executeUpdate();
                    }
                }

                // --- Delete from Patients table (if patient) ---
                if (selectedUser instanceof Patient) {
                    try (java.sql.PreparedStatement stmt = conn.prepareStatement(
                            "DELETE FROM Patients WHERE patient_id = ?")) {
                        stmt.setInt(1, selectedUser.getUserID());
                        stmt.executeUpdate();
                    }
                }

                // --- Delete from Doctors table (if doctor) ---
                if (selectedUser instanceof Doctor) {
                    try (java.sql.PreparedStatement stmt = conn.prepareStatement(
                            "DELETE FROM Doctors WHERE user_id = ?")) {
                        stmt.setInt(1, selectedUser.getUserID());
                        stmt.executeUpdate();
                    }
                }

                // --- Finally, delete from Users table ---
                try (java.sql.PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM Users WHERE user_id = ?")) {
                    stmt.setInt(1, selectedUser.getUserID());
                    int rows = stmt.executeUpdate();
                    removed = rows > 0;
                }
                conn.commit();

                // Remove from UserManager's collections
                if (removed && userManager != null) {
                    if (selectedUser instanceof Doctor) {
                        userManager.getAllDoctors().removeIf(d -> d.getUserID() == selectedUser.getUserID());
                    } else if (selectedUser instanceof Patient) {
                        userManager.getAllPatients().removeIf(p -> p.getUserID() == selectedUser.getUserID());
                    } else if (selectedUser instanceof Administrator) {
                        userManager.getAllAdministrators().removeIf(a -> a.getUserID() == selectedUser.getUserID());
                    }
                    allUsers.remove(selectedUser);
                    statusLabel.setText("User removed successfully!");
                    statusLabel.setStyle("-fx-text-fill: green;");
                    updateFilter();
                } else {
                    statusLabel.setText("Failed to remove user.");
                    statusLabel.setStyle("-fx-text-fill: red;");
                }
            } catch (Exception e) {
                statusLabel.setText("Error removing user: " + e.getMessage());
                statusLabel.setStyle("-fx-text-fill: red;");
                e.printStackTrace();
                try {
                    com.rhms.Database.DatabaseConnection.getConnection().rollback();
                } catch (Exception ex) {
                    // ignore
                }
            } finally {
                try {
                    com.rhms.Database.DatabaseConnection.getConnection().setAutoCommit(true);
                } catch (Exception ex) {
                    // ignore
                }
            }
        }
    }

    /**
     * Handle closing the dialog
     */
    @FXML
    public void handleClose(ActionEvent event) {
        Stage stage = (Stage) totalUsersLabel.getScene().getWindow();
        stage.close();
    }

    /**
     * Show an alert dialog
     */
    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Refresh the user list from the database
     */
    @FXML
    public void handleRefresh(ActionEvent event) {
        if (userManager != null) {
            userManager.syncUsersFromDatabase();
            loadUsers();
            statusLabel.setText("User list refreshed from database.");
            statusLabel.setStyle("-fx-text-fill: blue;");
        }
    }
}
