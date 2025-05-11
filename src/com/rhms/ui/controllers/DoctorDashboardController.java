package com.rhms.ui.controllers;

import com.rhms.Database.AppointmentDatabaseHandler;
import com.rhms.appointmentScheduling.AppointmentManager;
import com.rhms.reporting.DownloadHandler;
import com.rhms.reporting.ReportFormat;
import com.rhms.reporting.ReportGenerator;
import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;
import com.rhms.userManagement.User;
import com.rhms.userManagement.UserManager;
import com.rhms.appointmentScheduling.Appointment;
import com.rhms.doctorPatientInteraction.VideoCall;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class DoctorDashboardController implements DashboardController {

    @FXML private Label nameLabel;
    @FXML private VBox contentArea;

    @FXML private TableView<Patient> patientsTable;
    @FXML private TableColumn<Patient, String> patientNameColumn;
    @FXML private TableColumn<Patient, String> patientContactColumn;
    @FXML private TableColumn<Patient, String> lastCheckupColumn;
    @FXML private TableColumn<Patient, String> vitalStatusColumn;

    @FXML private TableView<Appointment> appointmentsTable;
    @FXML private TableColumn<Appointment, String> dateColumn;
    @FXML private TableColumn<Appointment, String> timeColumn;
    @FXML private TableColumn<Appointment, String> patientColumn;
    @FXML private TableColumn<Appointment, String> statusColumn;

    private AppointmentManager appointmentManager;
    private Doctor currentDoctor;
    private UserManager userManager;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    @Override
    public void setUser(User user) {
        if (user instanceof Doctor) {
            this.currentDoctor = (Doctor) user;
        } else {
            throw new IllegalArgumentException("User must be a Doctor");
        }
    }

    @Override
    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;

        // Create appointment manager with database handler
        AppointmentDatabaseHandler dbHandler = new AppointmentDatabaseHandler(userManager);
        this.appointmentManager = new AppointmentManager(dbHandler);

        // If doctor is already set, load their assignments
        if (currentDoctor != null) {
            loadAssignmentsForDoctor();
        }
    }

    @Override
    public void initializeDashboard() {
        nameLabel.setText("Dr. " + currentDoctor.getName());

        // Explicitly load assignments for this doctor
        loadAssignmentsForDoctor();

        // Initialize patients table
        patientNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getName()));

        patientContactColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPhone()));

        lastCheckupColumn.setCellValueFactory(cellData -> {
            return new SimpleStringProperty("N/A");
        });

        vitalStatusColumn.setCellValueFactory(cellData -> {
            return new SimpleStringProperty("Normal");
        });

        // Initialize appointments table
        dateColumn.setCellValueFactory(cellData -> {
            return new SimpleStringProperty(dateFormat.format(cellData.getValue().getAppointmentDate()));
        });

        timeColumn.setCellValueFactory(cellData -> {
            return new SimpleStringProperty(timeFormat.format(cellData.getValue().getAppointmentDate()));
        });

        patientColumn.setCellValueFactory(cellData -> {
            return new SimpleStringProperty(cellData.getValue().getPatient().getName());
        });

        statusColumn.setCellValueFactory(cellData -> {
            return new SimpleStringProperty(cellData.getValue().getStatus());
        });

        // Load data
        loadPatients();
        loadAppointments();
    }

    /**
     * Load doctor-patient assignments from database for the current doctor
     */
    private void loadAssignmentsForDoctor() {
        if (userManager == null || currentDoctor == null) {
            System.out.println("Cannot load assignments: UserManager or Doctor is null");
            return;
        }

        System.out.println("Loading assignments for doctor: " + currentDoctor.getName() + " (ID: " + currentDoctor.getUserID() + ")");
        userManager.loadAssignmentsForDoctor(currentDoctor);
    }

    private void loadPatients() {
        List<Patient> patients = currentDoctor.getAssignedPatients();
        System.out.println("Doctor " + currentDoctor.getName() + " has " + patients.size() + " assigned patients");

        // Debug: print all assigned patients
        for (Patient patient : patients) {
            System.out.println("  - Patient: " + patient.getName() + " (ID: " + patient.getUserID() + ")");
        }

        ObservableList<Patient> patientData = FXCollections.observableArrayList(patients);
        patientsTable.setItems(patientData);

        // If no patients, set a placeholder message
        if (patients.isEmpty()) {
            patientsTable.setPlaceholder(new Label("No patients assigned to you. Please contact an administrator."));
        }
    }

    private void loadAppointments() {
        try {
            List<Appointment> appointments;

            if (appointmentManager != null) {
                // Load from database
                appointments = appointmentManager.loadDoctorAppointments(currentDoctor);
            } else {
                // Fallback to empty list
                appointments = new ArrayList<>();
            }

            ObservableList<Appointment> appointmentData = FXCollections.observableArrayList(appointments);
            appointmentsTable.setItems(appointmentData);
        } catch (AppointmentManager.AppointmentException e) {
            showMessage("Error loading appointments: " + e.getMessage());
            appointmentsTable.setItems(FXCollections.observableArrayList());
        }
    }

    /**
     * Update the status of an appointment
     */
    private boolean updateAppointmentStatus(Appointment appointment, String newStatus) {
        try {
            if (appointmentManager != null) {
                return appointmentManager.updateAppointmentStatus(appointment, newStatus);
            } else {
                appointment.setStatus(newStatus);
                return true;
            }
        } catch (AppointmentManager.AppointmentException e) {
            showMessage("Error updating appointment: " + e.getMessage());
            return false;
        }
    }

    @FXML
    public void handlePatients(ActionEvent event) {
        Patient selectedPatient = patientsTable.getSelectionModel().getSelectedItem();
        if (selectedPatient == null) {
            showMessage("Please select a patient first.");
            return;
        }

        try {
            // Load the patient details view
            URL patientDetailsUrl = getClass().getResource("/com/rhms/ui/views/DoctorPatientDetailsView.fxml");
            if (patientDetailsUrl == null) {
                showMessage("Could not find patient details view resource");
                return;
            }

            FXMLLoader loader = new FXMLLoader(patientDetailsUrl);
            Parent patientDetailsView = loader.load();

            // Initialize the controller with the selected patient and current doctor
            DoctorPatientDetailsController controller = loader.getController();
            controller.initializeData(currentDoctor, selectedPatient, userManager);

            // Show the view in a new window
            Stage stage = new Stage();
            stage.setTitle("Patient Details - " + selectedPatient.getName());
            stage.setScene(new Scene(patientDetailsView));
            stage.show();
        } catch (IOException e) {
            showMessage("Error loading patient details view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleAppointments(ActionEvent event) {
        try {
            // Load the appointments management view
            URL appointmentsUrl = getClass().getResource("/com/rhms/ui/views/DoctorAppointmentsView.fxml");
            if (appointmentsUrl == null) {
                showMessage("Could not find appointments view resource");
                return;
            }

            FXMLLoader loader = new FXMLLoader(appointmentsUrl);
            Parent appointmentsView = loader.load();

            // Initialize the controller with the current doctor
            DoctorAppointmentsController controller = loader.getController();
            controller.initializeData(currentDoctor, userManager, appointmentManager);

            // Show the view in a new window
            Stage stage = new Stage();
            stage.setTitle("Appointment Management - Dr. " + currentDoctor.getName());
            Scene scene = new Scene(appointmentsView);

            // Apply CSS if available
            URL cssUrl = getClass().getResource("/com/rhms/ui/resources/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            stage.setScene(scene);

            // Add listener to refresh main dashboard appointments when appointments view is closed
            stage.setOnHidden(e -> loadAppointments());

            stage.show();
        } catch (IOException e) {
            showMessage("Error loading appointments view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleProvideFeedback(ActionEvent event) {
        Patient selectedPatient = patientsTable.getSelectionModel().getSelectedItem();
        if (selectedPatient == null) {
            showMessage("Please select a patient first.");
            return;
        }

        try {
            // Load the feedback form view
            URL feedbackFormUrl = getClass().getResource("/com/rhms/ui/views/DoctorFeedbackView.fxml");
            if (feedbackFormUrl == null) {
                showMessage("Could not find feedback form resource");
                return;
            }

            FXMLLoader loader = new FXMLLoader(feedbackFormUrl);
            Parent feedbackFormView = loader.load();

            // Initialize the controller with the selected patient and current doctor
            DoctorFeedbackController controller = loader.getController();
            controller.initializeData(currentDoctor, selectedPatient);

            // Show the view in a new window
            Stage stage = new Stage();
            stage.setTitle("Provide Feedback - " + selectedPatient.getName());
            stage.setScene(new Scene(feedbackFormView));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL); // Make it modal

            // Add CSS styles if available
            URL cssUrl = getClass().getResource("/com/rhms/ui/resources/styles.css");
            if (cssUrl != null) {
                stage.getScene().getStylesheets().add(cssUrl.toExternalForm());
            }

            stage.showAndWait();

            // After closing the feedback form, refresh data if needed
            loadPatients(); // This will refresh the patient list to show any updates
        } catch (IOException e) {
            showMessage("Error loading feedback form: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleVideoCall(ActionEvent event) {
        Patient selectedPatient = patientsTable.getSelectionModel().getSelectedItem();
        if (selectedPatient == null) {
            showMessage("Please select a patient to call.");
            return;
        }

        String meetingId = VideoCall.generateMeetingId();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Start Video Call");
        alert.setHeaderText("Start a video call with " + selectedPatient.getName());
        alert.setContentText("Meeting ID: " + meetingId + "\n\nClick OK to open the video call in your browser.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            VideoCall.startVideoCall(meetingId);
        }
    }
    @FXML
    public void handleChatWithPatient(ActionEvent event) {
        try {
            // Check if doctor has any assigned patients
            if (currentDoctor.getAssignedPatients() == null || currentDoctor.getAssignedPatients().isEmpty()) {
                showMessage("You don't have any assigned patients to chat with.");
                return;
            }

            // Create a new stage for the chat view
            Stage chatStage = new Stage();
            chatStage.setTitle("Chat with Patient - Dr. " + currentDoctor.getName());
            chatStage.initModality(Modality.WINDOW_MODAL);
            chatStage.initOwner(((Node)event.getSource()).getScene().getWindow());

            // Load the chat view
            URL chatViewUrl = findResource("com/rhms/ui/views/ChatWithPatientDashboard.fxml");

            if (chatViewUrl == null) {
                showMessage("Could not find ChatWithPatientDashboard.fxml. Make sure the file exists in the views directory.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(chatViewUrl);
            Parent chatView = loader.load();

            // Get controller and pass data
            ChatWithPatientDashboard controller = loader.getController();
            controller.initialize(currentDoctor, userManager);

            // Set up the scene
            Scene scene = new Scene(chatView);
            URL cssUrl = findResource("com/rhms/ui/resources/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            chatStage.setScene(scene);
            chatStage.setMinWidth(900);
            chatStage.setMinHeight(700);
            chatStage.show();

        } catch (Exception e) {
            showMessage("Error opening chat: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleGeneratePatientReport(ActionEvent event) {
        // Get the selected patient
        Patient selectedPatient = patientsTable.getSelectionModel().getSelectedItem();
        if (selectedPatient == null) {
            showMessage("Please select a patient first.");
            return;
        }

        try {
            // Create dialog for report options
            Dialog<Map<String, Object>> dialog = new Dialog<>();
            dialog.setTitle("Generate Patient Health Report");
            dialog.setHeaderText("Create a report for " + selectedPatient.getName());

            // Set the button types
            ButtonType generateButtonType = new ButtonType("Generate", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(generateButtonType, ButtonType.CANCEL);

            // Create the report options form
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            // Report sections checkboxes
            CheckBox vitalsCheckbox = new CheckBox("Vitals History");
            vitalsCheckbox.setSelected(true);
            CheckBox feedbackCheckbox = new CheckBox("Your Feedback");
            feedbackCheckbox.setSelected(true);
            CheckBox trendsCheckbox = new CheckBox("Health Trends & Graphs");
            trendsCheckbox.setSelected(true);

            // Report format selection
            ComboBox<ReportFormat> formatCombo = new ComboBox<>();
            formatCombo.getItems().addAll(ReportFormat.values());
            formatCombo.setValue(ReportFormat.PDF);

            // Date range selection
            DatePicker startDatePicker = new DatePicker(java.time.LocalDate.now().minusMonths(1));
            DatePicker endDatePicker = new DatePicker(java.time.LocalDate.now());

            // Add elements to grid
            grid.add(new Label("Include in Report:"), 0, 0);
            grid.add(vitalsCheckbox, 0, 1);
            grid.add(feedbackCheckbox, 0, 2);
            grid.add(trendsCheckbox, 0, 3);

            grid.add(new Label("Report Format:"), 0, 4);
            grid.add(formatCombo, 1, 4);

            grid.add(new Label("Date Range:"), 0, 5);
            grid.add(new Label("From:"), 0, 6);
            grid.add(startDatePicker, 1, 6);
            grid.add(new Label("To:"), 0, 7);
            grid.add(endDatePicker, 1, 7);

            dialog.getDialogPane().setContent(grid);

            // Enable/Disable generate button based on form validation
            Node generateButton = dialog.getDialogPane().lookupButton(generateButtonType);
            generateButton.setDisable(false);

            // Validate at least one section is selected
            Runnable validateForm = () -> {
                boolean valid = vitalsCheckbox.isSelected() ||
                        feedbackCheckbox.isSelected() ||
                        trendsCheckbox.isSelected();
                generateButton.setDisable(!valid);
            };

            vitalsCheckbox.setOnAction(e -> validateForm.run());
            feedbackCheckbox.setOnAction(e -> validateForm.run());
            trendsCheckbox.setOnAction(e -> validateForm.run());

            // Convert dialog result
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == generateButtonType) {
                    Map<String, Object> options = new HashMap<>();
                    options.put("includeVitals", vitalsCheckbox.isSelected());
                    options.put("includeFeedback", feedbackCheckbox.isSelected());
                    options.put("includeTrends", trendsCheckbox.isSelected());
                    options.put("format", formatCombo.getValue());
                    options.put("startDate", startDatePicker.getValue());
                    options.put("endDate", endDatePicker.getValue());
                    return options;
                }
                return null;
            });

            // Show dialog and handle result
            Optional<Map<String, Object>> result = dialog.showAndWait();
            result.ifPresent(options -> {
                try {
                    // Show directory chooser for save location
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Select Save Location");
                    File directory = directoryChooser.showDialog(((Node)event.getSource()).getScene().getWindow());

                    if (directory != null) {
                        // Create the report generator
                        ReportGenerator generator = new ReportGenerator(selectedPatient);

                        // Generate the report based on selected options
                        File reportFile = generator.generateReport(
                                directory.getAbsolutePath(),
                                (Boolean) options.get("includeVitals"),
                                (Boolean) options.get("includeFeedback"),
                                (Boolean) options.get("includeTrends"),
                                (ReportFormat) options.get("format")
                        );

                        // Show success message with file path
                        showMessage("Report generated successfully!\nSaved to: " + reportFile.getAbsolutePath());

                        // Try to open the file
                        DownloadHandler.openFile(reportFile);
                    }
                } catch (IOException e) {
                    showMessage("Error generating report: " + e.getMessage());
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            showMessage("Error creating report options dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Helper method to find resources using multiple approaches
     */
    private URL findResource(String path) {
        URL url = null;

        // Try multiple approaches to find the resource
        try {
            // Try the class loader first
            url = getClass().getClassLoader().getResource(path);

            // If not found, try with leading slash
            if (url == null) {
                url = getClass().getResource("/" + path);
            }

            // If still not found, try with direct file access
            if (url == null) {
                File file = new File("src/" + path);
                if (file.exists()) {
                    url = file.toURI().toURL();
                }
            }

            // Try in the target/classes directory
            if (url == null) {
                File file = new File("target/classes/" + path);
                if (file.exists()) {
                    url = file.toURI().toURL();
                }
            }
        } catch (Exception e) {
            // Silently handle exception
        }

        return url;
    }

    @Override
    public void handleLogout() {
        try {
            URL loginUrl = getClass().getResource("/com/rhms/ui/views/LoginView.fxml");

            if (loginUrl == null) {
                showMessage("Could not find login view resource");
                return;
            }

            FXMLLoader loader = new FXMLLoader(loginUrl);
            Parent loginView = loader.load();

            LoginViewController controller = loader.getController();
            controller.setUserManager(userManager);

            Stage stage = (Stage) nameLabel.getScene().getWindow();
            Scene scene = new Scene(loginView);
            scene.getStylesheets().add(getClass().getResource("/com/rhms/ui/resources/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("RHMS - Login");
            stage.show();
        } catch (IOException e) {
            showMessage("Error loading login view: " + e.getMessage());
        }
    }

    private void showMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
