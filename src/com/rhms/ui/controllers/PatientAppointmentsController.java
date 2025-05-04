package com.rhms.ui.controllers;

import com.rhms.Database.AppointmentDatabaseHandler;
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
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class PatientAppointmentsController {

    @FXML private TableView<Appointment> appointmentsTable;
    @FXML private TableColumn<Appointment, String> dateColumn;
    @FXML private TableColumn<Appointment, String> timeColumn;
    @FXML private TableColumn<Appointment, String> doctorColumn;
    @FXML private TableColumn<Appointment, String> purposeColumn;
    @FXML private TableColumn<Appointment, String> statusColumn;
    @FXML private TableColumn<Appointment, String> notesColumn;

    @FXML private Label totalAppointmentsLabel;
    @FXML private Label upcomingAppointmentsLabel;

    @FXML private Label dateValueLabel;
    @FXML private Label timeValueLabel;
    @FXML private Label doctorValueLabel;
    @FXML private Label purposeValueLabel;
    @FXML private Label statusValueLabel;
    @FXML private TextArea notesTextArea;

    @FXML private Button scheduleButton;
    @FXML private Button cancelButton;
    @FXML private Button closeButton;

    private Patient currentPatient;
    private UserManager userManager;
    private AppointmentManager appointmentManager;
    private ObservableList<Appointment> appointmentList;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    public void initialize(Patient patient, UserManager userManager) {
        this.currentPatient = patient;
        this.userManager = userManager;

        AppointmentDatabaseHandler dbHandler = new AppointmentDatabaseHandler(userManager);
        this.appointmentManager = new AppointmentManager(dbHandler);

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

        appointmentsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> displayAppointmentDetails(newSelection));

        cancelButton.setDisable(true);

        loadAppointments();

        appointmentsTable.setRowFactory(tv -> {
            TableRow<Appointment> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    displayAppointmentDetails(row.getItem());
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

    private URL findResource(String path) {
        URL url = getClass().getClassLoader().getResource(path);
        if (url == null) {
            url = getClass().getResource("/" + path);
        }
        return url;
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
