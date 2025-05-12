package com.rhms.ui.controllers;

import com.rhms.appointmentScheduling.Appointment;
import com.rhms.appointmentScheduling.AppointmentManager;
import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;
import com.rhms.userManagement.UserManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.time.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScheduleAppointmentController implements Initializable {
    private static final Logger LOGGER = Logger.getLogger(ScheduleAppointmentController.class.getName());

    @FXML private DatePicker appointmentDate;
    @FXML private ComboBox<String> timeComboBox;
    @FXML private ComboBox<String> doctorComboBox;
    @FXML private TextArea purposeTextArea;
    @FXML private Button scheduleButton;
    @FXML private Button cancelButton;

    private Patient currentPatient;
    private UserManager userManager;
    private List<Doctor> availableDoctors;
    private AppointmentManager appointmentManager;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Only FXML field setup here
        appointmentDate.setValue(LocalDate.now());
        populateTimeSlots();
        setupValidation();
    }

    /**
     * Call this after FXML loading to provide userManager and patient.
     */
    public void setData(UserManager userManager, Patient patient) {
        this.userManager = userManager;
        this.currentPatient = patient;
        this.appointmentManager = new AppointmentManager(userManager.getAppointmentDbHandler());
        loadDoctors();
        updateScheduleButtonState();
    }

    private void populateTimeSlots() {
        ObservableList<String> timeSlots = FXCollections.observableArrayList();
        LocalTime startTime = LocalTime.of(8, 0);
        LocalTime endTime = LocalTime.of(17, 0);
        while (startTime.isBefore(endTime)) {
            timeSlots.add(formatTime(startTime));
            startTime = startTime.plusMinutes(30);
        }
        timeComboBox.setItems(timeSlots);
        timeComboBox.getSelectionModel().select("9:00 AM");
    }

    private void loadDoctors() {
        availableDoctors = new ArrayList<>();
        if (currentPatient != null) {
            availableDoctors.addAll(currentPatient.getAssignedDoctors());
        }
        if (userManager != null) {
            for (Doctor doctor : userManager.getAllDoctors()) {
                if (!availableDoctors.contains(doctor)) {
                    availableDoctors.add(doctor);
                }
            }
        }
        ObservableList<String> doctorNames = FXCollections.observableArrayList();
        for (Doctor doctor : availableDoctors) {
            doctorNames.add("Dr. " + doctor.getName() + " (" + doctor.getSpecialization() + ")");
        }
        doctorComboBox.setItems(doctorNames);
        if (!doctorNames.isEmpty()) {
            doctorComboBox.getSelectionModel().select(0);
        }
    }

    private void setupValidation() {
        updateScheduleButtonState();
        appointmentDate.valueProperty().addListener((observable, oldValue, newValue) -> updateScheduleButtonState());
        timeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> updateScheduleButtonState());
        doctorComboBox.valueProperty().addListener((observable, oldValue, newValue) -> updateScheduleButtonState());
        purposeTextArea.textProperty().addListener((observable, oldValue, newValue) -> updateScheduleButtonState());
    }

    private void updateScheduleButtonState() {
        boolean isValid = appointmentDate.getValue() != null &&
                timeComboBox.getValue() != null &&
                doctorComboBox.getValue() != null &&
                !purposeTextArea.getText().trim().isEmpty();
        scheduleButton.setDisable(!isValid);
    }

    @FXML
    public void handleScheduleAppointment(ActionEvent event) {
        try {
            // Collect input data
            LocalDate selectedDate = appointmentDate.getValue();
            String timeStr = timeComboBox.getValue();
            LocalTime selectedTime = parseTime(timeStr);
            LocalDateTime appointmentDateTime = LocalDateTime.of(selectedDate, selectedTime);
            Date appointmentDate = Date.from(appointmentDateTime.atZone(ZoneId.systemDefault()).toInstant());
            
            // Validate doctor selection
            int doctorIndex = doctorComboBox.getSelectionModel().getSelectedIndex();
            if (doctorIndex < 0 || doctorIndex >= availableDoctors.size()) {
                showError("Invalid Selection", "Please select a valid doctor from the list.");
                return;
            }
            
            Doctor selectedDoctor = availableDoctors.get(doctorIndex);
            if (selectedDoctor == null || selectedDoctor.getUserID() <= 0) {
                showError("Doctor Error", "The selected doctor does not have a valid ID. Cannot schedule appointment.");
                LOGGER.log(Level.SEVERE, "Invalid doctor selected. Doctor: {0}, ID: {1}", 
                          new Object[]{selectedDoctor, selectedDoctor != null ? selectedDoctor.getUserID() : "null"});
                return;
            }

            String purpose = purposeTextArea.getText().trim();
            
            // Create appointment object
            Appointment appointment = new Appointment(
                appointmentDate,     // appointment date and time
                currentPatient,      // patient
                selectedDoctor,      // doctor
                purpose,             // purpose 
                "Pending",           // default status
                ""                   // empty notes
            );
            
            // Set created timestamp to current time
            appointment.setCreatedAt(new Date());
            
            LOGGER.log(Level.INFO, "Creating appointment for patient {0} with doctor {1} on {2}", 
                      new Object[]{currentPatient.getName(), selectedDoctor.getName(), appointmentDate});
            
            // Use AppointmentManager to schedule the appointment
            Appointment savedAppointment = appointmentManager.scheduleAppointment(appointment);
            
            if (savedAppointment == null || !savedAppointment.isStoredInDatabase()) {
                showError("Scheduling Failed", "The appointment could not be saved to the database.");
                return;
            }

            // Add appointment to patient's list
            currentPatient.addAppointment(savedAppointment);
            
            // Show success message
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Appointment Scheduled");
            alert.setHeaderText(null);
            alert.setContentText("Your appointment has been scheduled successfully for " + 
                                 selectedDate.toString() + " at " + timeStr + " with Dr. " + 
                                 selectedDoctor.getName() + ".");
            alert.showAndWait();
            
            // Close dialog
            closeDialog();
            
        } catch (AppointmentManager.AppointmentException e) {
            LOGGER.log(Level.SEVERE, "Error scheduling appointment", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Could not schedule appointment");
            alert.setContentText("An error occurred: " + e.getMessage() + 
                               (e.getCause() != null ? "\nDetails: " + e.getCause().getMessage() : ""));
            alert.showAndWait();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error scheduling appointment", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Unexpected Error");
            alert.setHeaderText("An unexpected error occurred during scheduling.");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private String formatTime(LocalTime time) {
        int hour = time.getHour();
        String ampm = hour >= 12 ? "PM" : "AM";
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;
        return String.format("%d:%02d %s", displayHour, time.getMinute(), ampm);
    }

    private LocalTime parseTime(String timeStr) {
        String[] parts = timeStr.split(" ");
        String[] timeParts = parts[0].split(":");
        int hours = Integer.parseInt(timeParts[0]);
        int minutes = Integer.parseInt(timeParts[1]);
        if (parts[1].equals("PM") && hours < 12) {
            hours += 12;
        } else if (parts[1].equals("AM") && hours == 12) {
            hours = 0;
        }
        return LocalTime.of(hours, minutes);
    }

    // Helper method for showing errors (if not already present)
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
