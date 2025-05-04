package com.rhms.ui.controllers;

import com.rhms.appointmentScheduling.Appointment;
import com.rhms.appointmentScheduling.AppointmentManager;
import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;
import com.rhms.userManagement.UserManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for managing doctor appointments
 */
public class DoctorAppointmentsController {
    private static final Logger LOGGER = Logger.getLogger(DoctorAppointmentsController.class.getName());
    
    @FXML private TableView<Appointment> appointmentsTable;
    @FXML private TableColumn<Appointment, String> dateColumn;
    @FXML private TableColumn<Appointment, String> timeColumn;
    @FXML private TableColumn<Appointment, String> patientColumn;
    @FXML private TableColumn<Appointment, String> purposeColumn;
    @FXML private TableColumn<Appointment, String> statusColumn;
    
    @FXML private Label totalAppointmentsLabel;
    @FXML private Label pendingAppointmentsLabel;
    @FXML private Label completedAppointmentsLabel;
    @FXML private Label cancelledAppointmentsLabel;
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    
    @FXML private Button markCompletedButton;
    @FXML private Button cancelAppointmentButton;
    @FXML private Button provideFeedbackButton;
    @FXML private Button refreshButton;
    @FXML private Button closeButton;
    
    private Doctor currentDoctor;
    private UserManager userManager;
    private AppointmentManager appointmentManager;
    private List<Appointment> allAppointments;
    private ObservableList<Appointment> filteredAppointments;
    
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    /**
     * Initialize controller with doctor, user manager, and appointment manager
     */
    public void initializeData(Doctor doctor, UserManager userManager, AppointmentManager appointmentManager) {
        this.currentDoctor = doctor;
        this.userManager = userManager;
        this.appointmentManager = appointmentManager;
        
        setupTableColumns();
        setupStatusFilter();
        setupButtonHandlers();
        loadAppointments();
        
        LOGGER.log(Level.INFO, "DoctorAppointmentsController initialized for doctor: {0}", doctor.getName());
    }

    /**
     * Set up table columns for the appointments table
     */
    private void setupTableColumns() {
        dateColumn.setCellValueFactory(cellData -> {
            Date appointmentDate = cellData.getValue().getAppointmentDate();
            if (appointmentDate != null) {
                return new SimpleStringProperty(dateFormat.format(appointmentDate));
            }
            return new SimpleStringProperty("Unknown");
        });

        timeColumn.setCellValueFactory(cellData -> {
            Date appointmentDate = cellData.getValue().getAppointmentDate();
            if (appointmentDate != null) {
                return new SimpleStringProperty(timeFormat.format(appointmentDate));
            }
            return new SimpleStringProperty("Unknown");
        });

        patientColumn.setCellValueFactory(cellData -> {
            Patient patient = cellData.getValue().getPatient();
            if (patient != null) {
                return new SimpleStringProperty(patient.getName());
            }
            return new SimpleStringProperty("Unknown");
        });

        purposeColumn.setCellValueFactory(cellData -> {
            String purpose = cellData.getValue().getPurpose();
            if (purpose != null && !purpose.isEmpty()) {
                return new SimpleStringProperty(purpose);
            }
            return new SimpleStringProperty("Not specified");
        });

        statusColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getStatus()));
        
        // Add row selection listener
        appointmentsTable.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> updateButtonStates(newValue)
        );
    }

    /**
     * Set up the status filter ComboBox
     */
    private void setupStatusFilter() {
        statusFilterComboBox.getItems().addAll(
            "All", "Pending", "Completed", "Cancelled", "Rescheduled"
        );
        statusFilterComboBox.setValue("All");
        
        // Add listener for when status filter changes
        statusFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        
        // Add listener for search field
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    /**
     * Set up button handlers for conditional enabling/disabling
     */
    private void setupButtonHandlers() {
        // Initially disable action buttons until a row is selected
        markCompletedButton.setDisable(true);
        cancelAppointmentButton.setDisable(true);
        provideFeedbackButton.setDisable(true);
    }

    /**
     * Update button states based on the selected appointment
     */
    private void updateButtonStates(Appointment selectedAppointment) {
        if (selectedAppointment == null) {
            markCompletedButton.setDisable(true);
            cancelAppointmentButton.setDisable(true);
            provideFeedbackButton.setDisable(true);
            return;
        }
        
        String status = selectedAppointment.getStatus();
        boolean isPending = "Pending".equalsIgnoreCase(status);
        boolean isCompleted = "Completed".equalsIgnoreCase(status);
        boolean isCancelled = "Cancelled".equalsIgnoreCase(status);
        
        // Only allow marking complete if status is pending
        markCompletedButton.setDisable(!isPending);
        
        // Only allow cancelling if status is pending
        cancelAppointmentButton.setDisable(!isPending);
        
        // Allow feedback for completed appointments
        provideFeedbackButton.setDisable(!isCompleted);
    }

    /**
     * Load all appointments for the current doctor
     */
    private void loadAppointments() {
        try {
            // Load doctor's appointments from database
            allAppointments = appointmentManager.loadDoctorAppointments(currentDoctor);
            
            // Apply filters and update the table
            applyFilters();
            
            // Update summary counts
            updateAppointmentCounts();
            
            LOGGER.log(Level.INFO, "Loaded {0} appointments for doctor {1}", 
                      new Object[]{allAppointments.size(), currentDoctor.getName()});
        } catch (AppointmentManager.AppointmentException e) {
            LOGGER.log(Level.SEVERE, "Error loading appointments", e);
            showAlert(Alert.AlertType.ERROR, "Error", 
                     "Failed to load appointments: " + e.getMessage());
            allAppointments = new ArrayList<>();
            filteredAppointments = FXCollections.observableArrayList(allAppointments);
            appointmentsTable.setItems(filteredAppointments);
        }
    }

    /**
     * Apply filters to the appointments list
     */
    private void applyFilters() {
        if (allAppointments == null) {
            return;
        }
        
        // Create a filtered list
        List<Appointment> filtered = new ArrayList<>();
        String statusFilter = statusFilterComboBox.getValue();
        String searchText = searchField.getText().toLowerCase();
        
        // Apply the filters
        for (Appointment appointment : allAppointments) {
            // Apply status filter
            if (!"All".equals(statusFilter) && !appointment.getStatus().equals(statusFilter)) {
                continue;
            }
            
            // Apply search filter on patient name or purpose
            if (!searchText.isEmpty()) {
                Patient patient = appointment.getPatient();
                String patientName = (patient != null) ? patient.getName().toLowerCase() : "";
                String purpose = appointment.getPurpose() != null ? 
                                appointment.getPurpose().toLowerCase() : "";
                
                boolean matchesSearch = patientName.contains(searchText) || purpose.contains(searchText);
                if (!matchesSearch) {
                    continue;
                }
            }
            
            // Apply date range filter if set
            if (fromDatePicker.getValue() != null && toDatePicker.getValue() != null) {
                Date appointmentDate = appointment.getAppointmentDate();
                java.time.LocalDate appointmentLocalDate = new java.sql.Date(
                    appointmentDate.getTime()).toLocalDate();
                
                if (appointmentLocalDate.isBefore(fromDatePicker.getValue()) || 
                    appointmentLocalDate.isAfter(toDatePicker.getValue())) {
                    continue;
                }
            }
            
            // If passed all filters, add to filtered list
            filtered.add(appointment);
        }
        
        // Update the table with filtered list
        filteredAppointments = FXCollections.observableArrayList(filtered);
        appointmentsTable.setItems(filteredAppointments);
    }

    /**
     * Update appointment count labels
     */
    private void updateAppointmentCounts() {
        int total = allAppointments.size();
        int pending = 0;
        int completed = 0;
        int cancelled = 0;
        
        for (Appointment appointment : allAppointments) {
            String status = appointment.getStatus();
            if ("Pending".equalsIgnoreCase(status)) {
                pending++;
            } else if ("Completed".equalsIgnoreCase(status)) {
                completed++;
            } else if ("Cancelled".equalsIgnoreCase(status)) {
                cancelled++;
            }
        }
        
        totalAppointmentsLabel.setText("Total: " + total);
        pendingAppointmentsLabel.setText("Pending: " + pending);
        completedAppointmentsLabel.setText("Completed: " + completed);
        cancelledAppointmentsLabel.setText("Cancelled: " + cancelled);
    }

    /**
     * Handle marking an appointment as completed
     */
    @FXML
    void handleMarkCompleted(ActionEvent event) {
        Appointment selectedAppointment = appointmentsTable.getSelectionModel().getSelectedItem();
        if (selectedAppointment == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an appointment to mark as completed.");
            return;
        }
        
        if (!"Pending".equalsIgnoreCase(selectedAppointment.getStatus())) {
            showAlert(Alert.AlertType.WARNING, "Invalid Action", 
                     "Only pending appointments can be marked as completed.");
            return;
        }
        
        try {
            boolean success = appointmentManager.updateAppointmentStatus(selectedAppointment, "Completed");
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Success", 
                         "Appointment has been marked as completed.");
                loadAppointments(); // Refresh the view
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", 
                         "Failed to update appointment status.");
            }
        } catch (AppointmentManager.AppointmentException e) {
            LOGGER.log(Level.SEVERE, "Error marking appointment as completed", e);
            showAlert(Alert.AlertType.ERROR, "Error", 
                     "Failed to update appointment: " + e.getMessage());
        }
    }

    /**
     * Handle cancelling an appointment
     */
    @FXML
    void handleCancelAppointment(ActionEvent event) {
        Appointment selectedAppointment = appointmentsTable.getSelectionModel().getSelectedItem();
        if (selectedAppointment == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an appointment to cancel.");
            return;
        }
        
        if (!"Pending".equalsIgnoreCase(selectedAppointment.getStatus())) {
            showAlert(Alert.AlertType.WARNING, "Invalid Action", 
                     "Only pending appointments can be cancelled.");
            return;
        }
        
        // Ask for confirmation
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Cancellation");
        confirmation.setHeaderText("Cancel Appointment");
        confirmation.setContentText("Are you sure you want to cancel this appointment?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean success = appointmentManager.updateAppointmentStatus(selectedAppointment, "Cancelled");
                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", 
                             "Appointment has been cancelled.");
                    loadAppointments(); // Refresh the view
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", 
                             "Failed to cancel appointment.");
                }
            } catch (AppointmentManager.AppointmentException e) {
                LOGGER.log(Level.SEVERE, "Error cancelling appointment", e);
                showAlert(Alert.AlertType.ERROR, "Error", 
                         "Failed to cancel appointment: " + e.getMessage());
            }
        }
    }

    /**
     * Handle providing feedback for a completed appointment
     */
    @FXML
    void handleProvideFeedback(ActionEvent event) {
        Appointment selectedAppointment = appointmentsTable.getSelectionModel().getSelectedItem();
        if (selectedAppointment == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an appointment to provide feedback for.");
            return;
        }
        
        Patient patient = selectedAppointment.getPatient();
        if (patient == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Cannot retrieve patient information for this appointment.");
            return;
        }
        
        try {
            // Load the feedback view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/rhms/ui/views/DoctorFeedbackView.fxml"));
            javafx.scene.Parent feedbackView = loader.load();
            
            DoctorFeedbackController controller = loader.getController();
            controller.initializeData(currentDoctor, patient);
            
            Stage stage = new Stage();
            stage.setTitle("Provide Feedback - " + patient.getName());
            stage.setScene(new javafx.scene.Scene(feedbackView));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            
            // Add CSS if available
            javafx.scene.Scene scene = stage.getScene();
            scene.getStylesheets().add(getClass().getResource("/com/rhms/ui/resources/styles.css").toExternalForm());
            
            stage.showAndWait();
            
        } catch (java.io.IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading feedback form", e);
            showAlert(Alert.AlertType.ERROR, "Error", 
                     "Failed to open feedback form: " + e.getMessage());
        }
    }

    /**
     * Handle applying date filters 
     */
    @FXML
    void handleApplyDateFilter(ActionEvent event) {
        if (fromDatePicker.getValue() == null || toDatePicker.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Missing Date Range", 
                     "Please select both start and end dates for filtering.");
            return;
        }
        
        if (fromDatePicker.getValue().isAfter(toDatePicker.getValue())) {
            showAlert(Alert.AlertType.WARNING, "Invalid Date Range", 
                     "Start date cannot be after end date.");
            return;
        }
        
        applyFilters();
    }

    /**
     * Handle clearing filters
     */
    @FXML
    void handleClearFilters(ActionEvent event) {
        searchField.clear();
        statusFilterComboBox.setValue("All");
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
        applyFilters();
    }

    /**
     * Handle refreshing the appointments list
     */
    @FXML
    void handleRefresh(ActionEvent event) {
        loadAppointments();
    }

    /**
     * Handle closing the window
     */
    @FXML
    void handleClose(ActionEvent event) {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Utility method to show alerts
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
