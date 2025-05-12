package com.rhms.ui.controllers;

import com.rhms.Database.AppointmentDatabaseHandler;
import com.rhms.appointmentScheduling.Appointment;
import com.rhms.appointmentScheduling.AppointmentManager;
import com.rhms.emergencyAlert.NotificationService;
import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;
import com.rhms.userManagement.UserManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PatientAppointmentsController implements Initializable {
    private static final Logger LOGGER = Logger.getLogger(PatientAppointmentsController.class.getName());

    @FXML private TableView<Appointment> appointmentsTable;
    @FXML private TableColumn<Appointment, String> dateColumn;
    @FXML private TableColumn<Appointment, String> timeColumn;
    @FXML private TableColumn<Appointment, String> doctorColumn;
    @FXML private TableColumn<Appointment, String> purposeColumn;
    @FXML private TableColumn<Appointment, String> statusColumn;
    @FXML private TableColumn<Appointment, String> notesColumn;

    @FXML private Label totalAppointmentsLabel;
    @FXML private Label upcomingAppointmentsLabel;
    @FXML private Label notificationCountLabel;

    @FXML private Label dateValueLabel;
    @FXML private Label timeValueLabel;
    @FXML private Label doctorValueLabel;
    @FXML private Label purposeValueLabel;
    @FXML private Label statusValueLabel;
    @FXML private TextArea notesTextArea;

    @FXML private Button scheduleButton;
    @FXML private Button cancelButton;
    @FXML private Button closeButton;
    @FXML private Button viewNotificationsButton;

    private Patient currentPatient;
    private UserManager userManager;
    private AppointmentManager appointmentManager;
    private ObservableList<Appointment> appointmentList;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // This method will be called by JavaFX after @FXML fields are injected
        // We don't do much here since we need the patient and userManager objects first
        // Those will be set in the initialize(Patient, UserManager) method
    }

    public void initialize(Patient patient, UserManager userManager) {
        this.currentPatient = patient;
        this.userManager = userManager;

        // Initialize services
        AppointmentDatabaseHandler dbHandler = new AppointmentDatabaseHandler(userManager);
        this.appointmentManager = new AppointmentManager(dbHandler);

        setupTableColumns();
        setupEventHandlers();
        loadAppointments();
        updateNotificationBadge();
    }

    private void setupTableColumns() {
        dateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(dateFormat.format(cellData.getValue().getAppointmentDate())));

        timeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(timeFormat.format(cellData.getValue().getAppointmentDate())));

        doctorColumn.setCellValueFactory(cellData -> {
            Doctor doctor = cellData.getValue().getDoctor();
            return new SimpleStringProperty(doctor != null ? "Dr. " + doctor.getName() : "Not Assigned");
        });

        purposeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPurpose()));

        statusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatus()));

        notesColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getNotes()));

        // Add custom cell factory for status column to highlight accepted (confirmed) appointments
        statusColumn.setCellFactory(column -> new TableCell<Appointment, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    
                    // Set style based on status
                    if ("Confirmed".equalsIgnoreCase(status)) {
                        setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    } else if ("Pending".equalsIgnoreCase(status)) {
                        setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                    } else if ("Cancelled".equalsIgnoreCase(status)) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else if ("Completed".equalsIgnoreCase(status)) {
                        setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                    
                    // Special styling for newly accepted appointments that have not been seen
                    Appointment appointment = getTableView().getItems().get(getIndex());
                    if ("Confirmed".equalsIgnoreCase(status) && 
                        !appointment.isNotificationSent()) {
                        setStyle("-fx-text-fill: green; -fx-font-weight: bold; -fx-background-color: #e6ffe6;");
                    }
                }
            }
        });
    }
    
    private void setupEventHandlers() {
        appointmentsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> displayAppointmentDetails(newSelection));

        cancelButton.setDisable(true);

        appointmentsTable.setRowFactory(tv -> {
            TableRow<Appointment> row = new TableRow<Appointment>() {
                @Override
                protected void updateItem(Appointment appointment, boolean empty) {
                    super.updateItem(appointment, empty);
                    
                    if (empty || appointment == null) {
                        setStyle("");
                    } else if ("Confirmed".equals(appointment.getStatus()) && 
                               !appointment.isNotificationSent()) {
                        // Highlight newly confirmed appointments
                        setStyle("-fx-background-color: #e6ffe6;");
                    } else {
                        setStyle("");
                    }
                }
            };
            
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    displayAppointmentDetails(row.getItem());
                    
                    // Mark this appointment's notification as seen if it's newly confirmed
                    Appointment appointment = row.getItem();
                    if ("Confirmed".equals(appointment.getStatus()) && 
                        !appointment.isNotificationSent()) {
                        try {
                            // Update the notification status in the database
                            appointmentManager.getDbHandler().updateNotificationStatus(
                                appointment.getAppointmentId(), true);
                                
                            // Update in-memory object
                            appointment.markNotificationSent();
                            
                            // Refresh the table to update highlighting
                            appointmentsTable.refresh();
                            
                            // Update notification badge
                            updateNotificationBadge();
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Failed to mark notification as seen", e);
                        }
                    }
                }
            });
            
            return row;
        });

        appointmentsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    boolean isCancelable = newVal != null &&
                            ("Pending".equals(newVal.getStatus()) || "Confirmed".equals(newVal.getStatus()));
                    cancelButton.setDisable(!isCancelable);
                });
    }

    private void loadAppointments() {
        try {
            List<Appointment> appointments = appointmentManager.loadPatientAppointments(currentPatient);
            appointmentList = FXCollections.observableArrayList(appointments);
            appointmentsTable.setItems(appointmentList);
            updateAppointmentCounts();
        } catch (AppointmentManager.AppointmentException e) {
            showError("Error loading appointments", e.getMessage());
            appointmentList = FXCollections.observableArrayList();
            appointmentsTable.setItems(appointmentList);
        }
    }

    private void updateAppointmentCounts() {
        totalAppointmentsLabel.setText("Total: " + appointmentList.size());

        long upcoming = appointmentList.stream()
                .filter(appointment -> appointment.getAppointmentDate().after(new Date()) &&
                        ("Pending".equals(appointment.getStatus()) || "Confirmed".equals(appointment.getStatus())))
                .count();
        upcomingAppointmentsLabel.setText("Upcoming: " + upcoming);
    }
    
    /**
     * Update the notification badge/count label with the number of unread appointment notifications
     */
    private void updateNotificationBadge() {
        if (notificationCountLabel != null) {
            try {
                // Count appointments with unseen notifications (Confirmed but not marked as notification sent)
                long unreadCount = appointmentList.stream()
                    .filter(a -> "Confirmed".equals(a.getStatus()) && !a.isNotificationSent())
                    .count();
                
                if (unreadCount > 0) {
                    notificationCountLabel.setText(String.valueOf(unreadCount));
                    notificationCountLabel.setVisible(true);
                    
                    // Update the viewNotificationsButton to show there are new notifications
                    if (viewNotificationsButton != null) {
                        viewNotificationsButton.setStyle("-fx-base: #ffcc00;"); // Highlight the button
                    }
                } else {
                    notificationCountLabel.setVisible(false);
                    
                    // Reset button style
                    if (viewNotificationsButton != null) {
                        viewNotificationsButton.setStyle("");
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error updating notification badge", e);
                notificationCountLabel.setVisible(false);
            }
        }
    }

    private void displayAppointmentDetails(Appointment appointment) {
        if (appointment == null) {
            clearAppointmentDetails();
            return;
        }

        dateValueLabel.setText(dateFormat.format(appointment.getAppointmentDate()));
        timeValueLabel.setText(timeFormat.format(appointment.getAppointmentDate()));

        Doctor doctor = appointment.getDoctor();
        doctorValueLabel.setText(doctor != null && doctor.getName() != null && doctor.getSpecialization() != null ?
                "Dr. " + doctor.getName() + " (" + doctor.getSpecialization() + ")" :
                "Not assigned");

        purposeValueLabel.setText(appointment.getPurpose());
        statusValueLabel.setText(appointment.getStatus());
        notesTextArea.setText(appointment.getNotes());

        String status = appointment.getStatus();
        if ("Confirmed".equals(status)) {
            statusValueLabel.setStyle("-fx-text-fill: green;");
        } else if ("Cancelled".equals(status)) {
            statusValueLabel.setStyle("-fx-text-fill: red;");
        } else if ("Completed".equals(status)) {
            statusValueLabel.setStyle("-fx-text-fill: blue;");
        } else {
            statusValueLabel.setStyle("-fx-text-fill: black;");
        }
    }

    private void clearAppointmentDetails() {
        dateValueLabel.setText("");
        timeValueLabel.setText("");
        doctorValueLabel.setText("");
        purposeValueLabel.setText("");
        statusValueLabel.setText("");
        notesTextArea.setText("");
        statusValueLabel.setStyle("-fx-text-fill: black;");
    }

    @FXML
    public void handleScheduleAppointment(ActionEvent event) {
        try {
            // Fix the resource path - note the correct filename
            URL schedulingViewUrl = findResource("com/rhms/ui/views/ScheduleAppointmentView.fxml");

            if (schedulingViewUrl == null) {
                showError("Resource Not Found", "Could not find ScheduleAppointmentView.fxml resource. Make sure the file exists in src/com/rhms/ui/views/ directory and rebuild the project.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(schedulingViewUrl);
            Parent schedulingView = loader.load(); // FXML loaded, controller instantiated, @FXML injected, Initializable.initialize called

            ScheduleAppointmentController controller = loader.getController();
            if (controller == null) {
                showError("Controller Error", "Could not load the ScheduleAppointmentController.");
                return;
            }

            // Call setData to pass necessary objects AFTER FXML loading and standard initialization
            controller.setData(userManager, currentPatient);

            Stage stage = new Stage();
            stage.setTitle("Schedule New Appointment");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(scheduleButton.getScene().getWindow());
            stage.setScene(new Scene(schedulingView));

            // Apply CSS if needed
            URL cssUrl = findResource("com/rhms/ui/resources/styles.css");
            if (cssUrl != null) {
                stage.getScene().getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.err.println("Warning: Could not find styles.css for ScheduleAppointmentView.");
            }

            stage.showAndWait();
            loadAppointments(); // Refresh the list after the dialog closes
        } catch (IOException e) {
            showError("Error", "Could not open appointment scheduling view: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) { // Catch broader exceptions during setup
            showError("Setup Error", "An error occurred setting up the scheduling view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleViewNotifications(ActionEvent event) {
        try {
            // Create a dialog for viewing notifications
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Appointment Notifications");
            dialog.setHeaderText("Recent Appointment Updates");
            
            // Set button types
            ButtonType markAllReadType = new ButtonType("Mark All as Read", ButtonBar.ButtonData.APPLY);
            ButtonType closeButtonType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(markAllReadType, closeButtonType);
            
            // Create content area
            VBox contentBox = new VBox(10);
            contentBox.setPadding(new Insets(20));
            
            // Find appointments with unseen notifications
            List<Appointment> unreadNotifications = appointmentList.stream()
                .filter(a -> "Confirmed".equals(a.getStatus()) && !a.isNotificationSent())
                .collect(java.util.stream.Collectors.toList());
                
            // Find recent status changes (even if marked as read)
            Date cutoffDate = new Date(System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)); // 7 days ago
            List<Appointment> recentStatusChanges = appointmentList.stream()
                .filter(a -> a.getLastStatusChangeDate() != null && 
                           a.getLastStatusChangeDate().after(cutoffDate))
                .collect(java.util.stream.Collectors.toList());
            
            // Add unread notifications section
            Label unreadLabel = new Label("Unread Notifications (" + unreadNotifications.size() + ")");
            unreadLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            contentBox.getChildren().add(unreadLabel);
            
            if (unreadNotifications.isEmpty()) {
                Label emptyUnreadLabel = new Label("No unread notifications");
                emptyUnreadLabel.setStyle("-fx-font-style: italic;");
                contentBox.getChildren().add(emptyUnreadLabel);
            } else {
                VBox notificationsBox = new VBox(5);
                
                for (Appointment appointment : unreadNotifications) {
                    HBox notificationEntry = createNotificationEntry(appointment, true);
                    notificationsBox.getChildren().add(notificationEntry);
                }
                
                contentBox.getChildren().add(notificationsBox);
            }
            
            // Add separator
            Separator separator = new Separator();
            separator.setPadding(new Insets(10, 0, 10, 0));
            contentBox.getChildren().add(separator);
            
            // Add recent status changes section
            Label recentLabel = new Label("Recent Appointment Updates (" + recentStatusChanges.size() + ")");
            recentLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            contentBox.getChildren().add(recentLabel);
            
            if (recentStatusChanges.isEmpty()) {
                Label emptyRecentLabel = new Label("No recent appointment updates");
                emptyRecentLabel.setStyle("-fx-font-style: italic;");
                contentBox.getChildren().add(emptyRecentLabel);
            } else {
                VBox recentBox = new VBox(5);
                
                for (Appointment appointment : recentStatusChanges) {
                    boolean isUnread = !appointment.isNotificationSent();
                    HBox notificationEntry = createNotificationEntry(appointment, isUnread);
                    recentBox.getChildren().add(notificationEntry);
                }
                
                ScrollPane scrollPane = new ScrollPane(recentBox);
                scrollPane.setFitToWidth(true);
                scrollPane.setPrefHeight(300);
                scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                contentBox.getChildren().add(scrollPane);
            }
            
            dialog.getDialogPane().setContent(contentBox);
            dialog.getDialogPane().setPrefWidth(600);
            
            // Handle mark all as read button
            Button markAllReadButton = (Button) dialog.getDialogPane().lookupButton(markAllReadType);
            markAllReadButton.setOnAction(e -> {
                try {
                    for (Appointment appointment : unreadNotifications) {
                        appointmentManager.getDbHandler().updateNotificationStatus(
                            appointment.getAppointmentId(), true);
                        appointment.markNotificationSent();
                    }
                    
                    showInfo("All notifications marked as read.");
                    
                    // Refresh the table to update highlighting
                    appointmentsTable.refresh();
                    
                    // Update notification badge
                    updateNotificationBadge();
                    
                    // Close the dialog
                    dialog.setResult(null);
                    dialog.close();
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Failed to mark notifications as read", ex);
                    showError("Error", "Failed to mark notifications as read: " + ex.getMessage());
                }
            });
            
            // Apply CSS if available
            URL cssUrl = findResource("com/rhms/ui/resources/styles.css");
            if (cssUrl != null) {
                dialog.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            }
            
            // Show dialog
            dialog.showAndWait();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error displaying notifications", e);
            showError("Error", "Failed to display notifications: " + e.getMessage());
        }
    }

    /**
     * Create a notification entry UI element for an appointment
     */
    private HBox createNotificationEntry(Appointment appointment, boolean isUnread) {
        HBox entryBox = new HBox(10);
        entryBox.setPadding(new Insets(8, 5, 8, 5));
        
        // Unread indicator
        Circle statusDot = new Circle(6);
        if (isUnread) {
            statusDot.setFill(Color.ORANGE);
            entryBox.setStyle("-fx-background-color: #f9f9e0;");
        } else {
            statusDot.setFill(Color.LIGHTGRAY);
        }
        
        // Appointment info
        VBox infoBox = new VBox(3);
        
        // Title with date and time
        String appointmentDate = dateFormat.format(appointment.getAppointmentDate());
        String appointmentTime = timeFormat.format(appointment.getAppointmentDate());
        Label titleLabel = new Label("Appointment on " + appointmentDate + " at " + appointmentTime);
        titleLabel.setStyle("-fx-font-weight: bold;");
        
        // Status info
        String doctorName = appointment.getDoctor() != null ? 
                "Dr. " + appointment.getDoctor().getName() : "Not assigned";
        Label statusLabel = new Label("Status: " + appointment.getStatus() + " with " + doctorName);
        
        // Date of status change
        Label dateLabel = new Label("Updated: " + 
                (appointment.getLastStatusChangeDate() != null ? 
                    new SimpleDateFormat("MMM d, yyyy h:mm a").format(appointment.getLastStatusChangeDate()) : 
                    "Unknown"));
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666;");
        
        infoBox.getChildren().addAll(titleLabel, statusLabel, dateLabel);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        
        // Mark as read button (only for unread notifications)
        if (isUnread) {
            Button markReadButton = new Button("Mark Read");
            markReadButton.setStyle("-fx-font-size: 10px;");
            
            markReadButton.setOnAction(e -> {
                try {
                    appointmentManager.getDbHandler().updateNotificationStatus(
                        appointment.getAppointmentId(), true);
                    appointment.markNotificationSent();
                    
                    // Update UI
                    statusDot.setFill(Color.LIGHTGRAY);
                    entryBox.setStyle("");
                    markReadButton.setDisable(true);
                    
                    // Refresh the table to update highlighting
                    appointmentsTable.refresh();
                    
                    // Update notification badge
                    updateNotificationBadge();
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Failed to mark notification as read", ex);
                }
            });
            
            entryBox.getChildren().addAll(statusDot, infoBox, markReadButton);
        } else {
            entryBox.getChildren().addAll(statusDot, infoBox);
        }
        
        return entryBox;
    }

    @FXML
    public void handleCancelAppointment(ActionEvent event) {
        Appointment selectedAppointment = appointmentsTable.getSelectionModel().getSelectedItem();
        if (selectedAppointment == null) {
            showInfo("Please select an appointment to cancel.");
            return;
        }

        if (!("Pending".equals(selectedAppointment.getStatus()) || "Confirmed".equals(selectedAppointment.getStatus()))) {
            showInfo("Only pending or confirmed appointments can be cancelled.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Cancel Appointment");
        confirmation.setHeaderText("Are you sure you want to cancel this appointment?");
        confirmation.setContentText("Date: " + dateFormat.format(selectedAppointment.getAppointmentDate()) +
                "\nTime: " + timeFormat.format(selectedAppointment.getAppointmentDate()));

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean success = appointmentManager.updateAppointmentStatus(
                        selectedAppointment, "Cancelled");

                if (success) {
                    showInfo("Appointment successfully cancelled.");
                    loadAppointments();
                } else {
                    showError("Error", "Could not cancel appointment.");
                }
            } catch (AppointmentManager.AppointmentException e) {
                showError("Error", "Failed to cancel appointment: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleClose(ActionEvent event) {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    private URL findResource(String path) {
        URL url = getClass().getClassLoader().getResource(path);
        if (url == null) {
            url = getClass().getResource("/" + path);
        }
        return url;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
