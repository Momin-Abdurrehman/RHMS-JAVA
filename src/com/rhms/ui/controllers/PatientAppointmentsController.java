package com.rhms.ui.controllers;

import com.rhms.appointmentScheduling.Appointment;
import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class PatientAppointmentsController {

    @FXML private TableView<Appointment> appointmentsTable;
    @FXML private TableColumn<Appointment, Date> dateColumn;
    @FXML private TableColumn<Appointment, Date> timeColumn;
    @FXML private TableColumn<Appointment, String> doctorColumn;
    @FXML private TableColumn<Appointment, String> purposeColumn;
    @FXML private TableColumn<Appointment, String> statusColumn;
    @FXML private TableColumn<Appointment, String> notesColumn;
    
    @FXML private Label dateValueLabel;
    @FXML private Label timeValueLabel;
    @FXML private Label doctorValueLabel;
    @FXML private Label purposeValueLabel;
    @FXML private Label statusValueLabel;
    @FXML private TextArea notesTextArea;
    
    @FXML private Button scheduleButton;
    @FXML private Button cancelButton;
    @FXML private Button closeButton;
    
    @FXML private Label totalAppointmentsLabel;
    @FXML private Label upcomingAppointmentsLabel;
    
    private Patient currentPatient;
    private ObservableList<Appointment> appointmentsList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy");
    private SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a");
    
    public void initialize() {
        // Set up table columns
        dateColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getAppointmentDate()));
        dateColumn.setCellFactory(column -> new TableCell<Appointment, Date>() {
            private final SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
            
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(format.format(item));
                }
            }
        });
        
        timeColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getAppointmentDate()));
        timeColumn.setCellFactory(column -> new TableCell<Appointment, Date>() {
            private final SimpleDateFormat format = new SimpleDateFormat("h:mm a");
            
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(format.format(item));
                }
            }
        });
        
        doctorColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getDoctor() == null) {
                return new SimpleStringProperty("Not Assigned");
            } else {
                return new SimpleStringProperty("Dr. " + cellData.getValue().getDoctor().getName());
            }
        });
        
        purposeColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getPurpose()));
        
        statusColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getStatus()));
        statusColumn.setCellFactory(column -> new TableCell<Appointment, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item.toLowerCase()) {
                        case "confirmed":
                            setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                            break;
                        case "pending":
                            setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                            break;
                        case "cancelled":
                            setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                            break;
                        case "completed":
                            setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                            break;
                    }
                }
            }
        });
        
        notesColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getNotes()));
        
        // Setup selection listener
        appointmentsTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    updateDetailsPane(newSelection);
                } else {
                    clearDetailsPane();
                }
            }
        );
        
        // Setup button handlers
        cancelButton.setDisable(true);
        
        scheduleButton.setOnAction(this::handleScheduleAppointment);
        cancelButton.setOnAction(this::handleCancelAppointment);
        closeButton.setOnAction(event -> {
            ((Stage) closeButton.getScene().getWindow()).close();
        });
    }
    
    public void initializeAppointments(Patient patient, List<Appointment> appointments) {
        this.currentPatient = patient;
        
        // Sort appointments by date (newest first)
        if (appointments == null) {
            appointments = new ArrayList<>();
        }
        appointments.sort(Comparator.comparing(Appointment::getAppointmentDate).reversed());
        
        // Create observable list
        this.appointmentsList = FXCollections.observableArrayList(appointments);
        appointmentsTable.setItems(appointmentsList);
        
        // Update summary labels
        updateSummaryLabels();
    }
    
    private void updateDetailsPane(Appointment appointment) {
        dateValueLabel.setText(dateFormat.format(appointment.getAppointmentDate()));
        timeValueLabel.setText(timeFormat.format(appointment.getAppointmentDate()));
        
        if (appointment.getDoctor() != null) {
            doctorValueLabel.setText("Dr. " + appointment.getDoctor().getName() + 
                                   " (" + appointment.getDoctor().getSpecialization() + ")");
        } else {
            doctorValueLabel.setText("Not Assigned");
        }
        
        purposeValueLabel.setText(appointment.getPurpose());
        statusValueLabel.setText(appointment.getStatus());
        notesTextArea.setText(appointment.getNotes() != null ? appointment.getNotes() : "");
        
        // Enable/disable cancel button based on status
        String status = appointment.getStatus().toLowerCase();
        cancelButton.setDisable(status.equals("cancelled") || status.equals("completed"));
        
        // Set status color
        if (status.equals("confirmed")) {
            statusValueLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        } else if (status.equals("pending")) {
            statusValueLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
        } else if (status.equals("cancelled")) {
            statusValueLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        } else if (status.equals("completed")) {
            statusValueLabel.setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
        } else {
            statusValueLabel.setStyle("-fx-font-weight: bold;");
        }
    }
    
    private void clearDetailsPane() {
        dateValueLabel.setText("");
        timeValueLabel.setText("");
        doctorValueLabel.setText("");
        purposeValueLabel.setText("");
        statusValueLabel.setText("");
        statusValueLabel.setStyle("");
        notesTextArea.setText("");
        cancelButton.setDisable(true);
    }
    
    private void updateSummaryLabels() {
        int totalAppointments = appointmentsList.size();
        
        // Count upcoming appointments (not cancelled and date in future)
        Date now = new Date();
        long upcomingCount = appointmentsList.stream()
            .filter(a -> !a.getStatus().equalsIgnoreCase("cancelled") && 
                        !a.getStatus().equalsIgnoreCase("completed") && 
                        a.getAppointmentDate().after(now))
            .count();
        
        totalAppointmentsLabel.setText("Total: " + totalAppointments);
        upcomingAppointmentsLabel.setText("Upcoming: " + upcomingCount);
    }
    
    @FXML
    private void handleScheduleAppointment(ActionEvent event) {
        // Create dialog window with form
        Dialog<Appointment> dialog = new Dialog<>();
        dialog.setTitle("Schedule New Appointment");
        dialog.setHeaderText("Enter appointment details");
        
        // Set buttons
        ButtonType scheduleButtonType = new ButtonType("Schedule", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(scheduleButtonType, ButtonType.CANCEL);
        
        // Create form grid
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));
        
        // Date picker
        DatePicker datePicker = new DatePicker(LocalDate.now());
        
        // Time picker (combo box with time slots)
        ComboBox<String> timeComboBox = new ComboBox<>();
        for (int hour = 8; hour <= 17; hour++) {
            timeComboBox.getItems().add(String.format("%02d:00", hour));
            timeComboBox.getItems().add(String.format("%02d:30", hour));
        }
        timeComboBox.getSelectionModel().selectFirst();
        
        // Doctor selection
        ComboBox<Doctor> doctorComboBox = new ComboBox<>();
        List<Doctor> assignedDoctors = currentPatient.getAssignedDoctors();
        
        if (assignedDoctors != null && !assignedDoctors.isEmpty()) {
            doctorComboBox.setItems(FXCollections.observableArrayList(assignedDoctors));
            
            doctorComboBox.setCellFactory(param -> new ListCell<Doctor>() {
                @Override
                protected void updateItem(Doctor item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText("Dr. " + item.getName() + " (" + item.getSpecialization() + ")");
                    }
                }
            });
            
            doctorComboBox.setButtonCell(new ListCell<Doctor>() {
                @Override
                protected void updateItem(Doctor item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText("Dr. " + item.getName() + " (" + item.getSpecialization() + ")");
                    }
                }
            });
            
            doctorComboBox.getSelectionModel().selectFirst();
        } else {
            doctorComboBox.setPromptText("No assigned doctors");
            doctorComboBox.setDisable(true);
        }
        
        // Purpose field
        TextField purposeField = new TextField();
        purposeField.setPromptText("Reason for appointment");
        
        // Notes field
        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Additional notes or information");
        notesArea.setPrefRowCount(3);
        
        // Add fields to grid
        grid.add(new Label("Date:"), 0, 0);
        grid.add(datePicker, 1, 0);
        grid.add(new Label("Time:"), 0, 1);
        grid.add(timeComboBox, 1, 1);
        grid.add(new Label("Doctor:"), 0, 2);
        grid.add(doctorComboBox, 1, 2);
        grid.add(new Label("Purpose:"), 0, 3);
        grid.add(purposeField, 1, 3);
        grid.add(new Label("Notes:"), 0, 4);
        grid.add(notesArea, 1, 4);
        
        dialog.getDialogPane().setContent(grid);
        
        // Convert result to appointment when button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == scheduleButtonType) {
                if (datePicker.getValue() == null) {
                    showAlert("Please select a date");
                    return null;
                }
                
                if (purposeField.getText().trim().isEmpty()) {
                    showAlert("Please enter a purpose");
                    return null;
                }
                
                try {
                    // Parse date and time
                    LocalDate date = datePicker.getValue();
                    String timeString = timeComboBox.getValue();
                    String[] timeParts = timeString.split(":");
                    int hour = Integer.parseInt(timeParts[0]);
                    int minute = Integer.parseInt(timeParts[1]);
                    
                    // Combine to timestamp
                    LocalDateTime dateTime = LocalDateTime.of(
                        date.getYear(), date.getMonth(), date.getDayOfMonth(),
                        hour, minute
                    );
                    Date appointmentDate = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
                    
                    // Create and return appointment
                    return new Appointment(
                        appointmentDate,
                        currentPatient,
                        doctorComboBox.getValue(),
                        purposeField.getText().trim(),
                        "Pending",
                        notesArea.getText().trim()
                    );
                } catch (Exception e) {
                    showAlert("Error creating appointment: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });
        
        // Show dialog and process result
        Optional<Appointment> result = dialog.showAndWait();
        result.ifPresent(appointment -> {
            // Add to patient's appointments
            currentPatient.scheduleAppointment(appointment);
            
            // Add to the table
            appointmentsList.add(0, appointment);  // Add at top
            appointmentsTable.getSelectionModel().select(appointment);
            
            // Update summary
            updateSummaryLabels();
        });
    }
    
    @FXML
    private void handleCancelAppointment(ActionEvent event) {
        Appointment appointment = appointmentsTable.getSelectionModel().getSelectedItem();
        if (appointment == null) {
            return;
        }
        
        // Confirm cancellation
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cancel Appointment");
        alert.setHeaderText("Cancel Selected Appointment?");
        alert.setContentText("Are you sure you want to cancel the appointment on " + 
                           dateFormat.format(appointment.getAppointmentDate()) + "?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Cancel the appointment
            appointment.setStatus("Cancelled");
            
            // Refresh UI
            appointmentsTable.refresh();
            updateDetailsPane(appointment);
            updateSummaryLabels();
        }
    }
    
    /**
     * Handle the close button click
     */
    @FXML
    public void handleClose(ActionEvent event) {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
    
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
