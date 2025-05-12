package com.rhms.ui.controllers;

import com.rhms.Database.AppointmentDatabaseHandler;
import com.rhms.Database.VitalSignDatabaseHandler;
import com.rhms.appointmentScheduling.AppointmentManager;
import com.rhms.reporting.DownloadHandler;
import com.rhms.reporting.ReportFormat;
import com.rhms.reporting.ReportGenerator;
import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;
import com.rhms.userManagement.User;
import com.rhms.userManagement.UserManager;
import com.rhms.healthDataHandling.VitalSign;
import com.rhms.healthDataHandling.CSVVitalsUploader;
import com.rhms.healthDataHandling.VitalsUploadReport;
import com.rhms.appointmentScheduling.Appointment;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Insets;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Date;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.sql.SQLException;

public class PatientDashboardController implements DashboardController {

    @FXML private Label nameLabel;
    @FXML private Label heartRateLabel;
    @FXML private Label oxygenLabel;
    @FXML private Label bloodPressureLabel;
    @FXML private Label temperatureLabel;
    @FXML private VBox contentArea;
    @FXML private Button notificationCenterButton;

    @FXML private TableView<Appointment> appointmentsTable;
    @FXML private TableColumn<Appointment, String> dateColumn;
    @FXML private TableColumn<Appointment, String> timeColumn;
    @FXML private TableColumn<Appointment, String> doctorColumn;
    @FXML private TableColumn<Appointment, String> statusColumn;

    private Patient currentPatient;
    private UserManager userManager;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private DecimalFormat decimalFormat = new DecimalFormat("#.0");
    private AppointmentManager appointmentManager;

    @Override
    public void setUser(User user) {
        if (user instanceof Patient) {
            this.currentPatient = (Patient) user;
        } else {
            throw new IllegalArgumentException("User must be a Patient");
        }
    }

    @Override
    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;

        // Initialize the appointment manager for database operations
        AppointmentDatabaseHandler dbHandler = userManager.getAppointmentDbHandler();
        this.appointmentManager = new AppointmentManager(dbHandler);

        // Load appointments and doctor assignments for the patient if they're already set
        if (currentPatient != null) {
            userManager.loadAppointmentsForPatient(currentPatient);
            userManager.loadAssignmentsForPatient(currentPatient);  // Load assignments specifically for this patient
        }
    }

    @Override
    public void initializeDashboard() {
        nameLabel.setText(currentPatient.getName());

        // Force reload of doctor-patient assignments to ensure they're up to date
        if (userManager != null) {
            userManager.loadAssignmentsForPatient(currentPatient);  // Load assignments for this specific patient
        }

        // Initialize table columns
        dateColumn.setCellValueFactory(cellData -> {
            return new SimpleStringProperty(dateFormat.format(cellData.getValue().getAppointmentDate()));
        });

        timeColumn.setCellValueFactory(cellData -> {
            return new SimpleStringProperty(timeFormat.format(cellData.getValue().getAppointmentDate()));
        });

        doctorColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getDoctor() == null) {
                return new SimpleStringProperty("Not Assigned");
            } else {
                return new SimpleStringProperty("Dr. " + cellData.getValue().getDoctor().getName());
            }
        });

        statusColumn.setCellValueFactory(cellData -> {
            return new SimpleStringProperty(cellData.getValue().getStatus());
        });

        // Add color-coding to status column
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

        // Make sure appointments are loaded from database
        if (userManager != null && currentPatient != null) {
            userManager.loadAppointmentsForPatient(currentPatient);
        }

        // Load vital signs data
        loadLatestVitals();

        // Load appointment data
        loadAppointments();

        loadVitalsFromDatabase();


    }

    private void loadLatestVitals() {
        // Get the latest vital signs from the patient
        List<VitalSign> vitalSigns = currentPatient.getVitalsDatabase().getAllVitals();

        if (!vitalSigns.isEmpty()) {
            // Get the latest entry
            VitalSign latestVital = vitalSigns.get(vitalSigns.size() - 1);

            // Update UI with formatted values
            heartRateLabel.setText(decimalFormat.format(latestVital.getHeartRate()));
            oxygenLabel.setText(decimalFormat.format(latestVital.getOxygenLevel()));
            bloodPressureLabel.setText(decimalFormat.format(latestVital.getBloodPressure()));
            temperatureLabel.setText(decimalFormat.format(latestVital.getTemperature()));

            // Set appropriate styles based on vital sign status using inline styles instead of styleClass
            if (latestVital.isHeartRateNormal()) {
                heartRateLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
            } else {
                heartRateLabel.setStyle("-fx-text-fill: #ff6666; -fx-font-size: 18px; -fx-font-weight: bold;");
            }

            if (latestVital.isOxygenLevelNormal()) {
                oxygenLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
            } else {
                oxygenLabel.setStyle("-fx-text-fill: #ff6666; -fx-font-size: 18px; -fx-font-weight: bold;");
            }

            if (latestVital.isBloodPressureNormal()) {
                bloodPressureLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
            } else {
                bloodPressureLabel.setStyle("-fx-text-fill: #ff6666; -fx-font-size: 18px; -fx-font-weight: bold;");
            }

            if (latestVital.isTemperatureNormal()) {
                temperatureLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
            } else {
                temperatureLabel.setStyle("-fx-text-fill: #ff6666; -fx-font-size: 18px; -fx-font-weight: bold;");
            }
        }
    }

    private void loadAppointments() {
        // Get appointments for the patient
        List<Appointment> appointments = currentPatient.getAppointments();

        if (appointments == null) {
            appointments = new ArrayList<>();
        }

        // Filter for only upcoming and recent appointments, sorted by date
        Date now = new Date();
        List<Appointment> filteredAppointments = appointments.stream()
            .filter(a -> {
                // Show appointments that are:
                // 1. Coming up in the future
                // 2. Completed/cancelled in the last 30 days
                if (a.getAppointmentDate().after(now)) {
                    return true;
                } else {
                    long diffInMillies = now.getTime() - a.getAppointmentDate().getTime();
                    long diffInDays = diffInMillies / (1000 * 60 * 60 * 24);
                    return diffInDays <= 30;
                }
            })
            .sorted(Comparator.comparing(Appointment::getAppointmentDate))
            .collect(Collectors.toList());

        // Only update the table if it's available
        if (appointmentsTable != null) {
            ObservableList<Appointment> appointmentData = FXCollections.observableArrayList(filteredAppointments);
            appointmentsTable.setItems(appointmentData);
        } else {
            System.err.println("Warning: appointmentsTable is null - cannot display appointments");
        }
    }

    @FXML
    public void handleAppointments(ActionEvent event) {
        try {
            // Get appointments for the patient
            List<Appointment> appointments = currentPatient.getAppointments();

            if (appointments == null) {
                appointments = new ArrayList<>();
            }

            // Create a new stage for the appointments view
            Stage appointmentsStage = new Stage();
            appointmentsStage.setTitle("Appointments - " + currentPatient.getName());
            appointmentsStage.initModality(Modality.WINDOW_MODAL);
            appointmentsStage.initOwner(((Node)event.getSource()).getScene().getWindow());

            // Load the appointments view - Fix the resource path
            URL appointmentsViewUrl = findResource("com/rhms/ui/views/PatientAppointmentsDashboard.fxml");

            if (appointmentsViewUrl == null) {
                showMessage("Could not find PatientAppointmentsDashboard.fxml. Make sure the file exists in src/com/rhms/ui/views/ directory and rebuild the project.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(appointmentsViewUrl);
            Parent appointmentsView = loader.load();

            // Get controller and pass data
            PatientAppointmentsController controller = loader.getController();
            controller.initialize(currentPatient, userManager);

            // Set up the scene
            Scene scene = new Scene(appointmentsView);
            URL cssUrl = findResource("com/rhms/ui/resources/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            appointmentsStage.setScene(scene);
            appointmentsStage.setMinWidth(800);
            appointmentsStage.setMinHeight(600);
            appointmentsStage.setOnHidden(e -> {
                // Refresh the appointments table when the appointments view is closed
                loadAppointments();
            });
            appointmentsStage.show();

        } catch (Exception e) {
            showMessage("Error displaying appointments: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleVitals(ActionEvent event) {
        try {
            // Create a new stage for the vitals history view
            Stage vitalsStage = new Stage();
            vitalsStage.setTitle("Vital Signs Dashboard - " + currentPatient.getName());
            vitalsStage.initModality(Modality.WINDOW_MODAL);
            vitalsStage.initOwner(((Node)event.getSource()).getScene().getWindow());

            // Load the vitals dashboard view
            URL vitalsViewUrl = findResource("com/rhms/ui/views/VitalSignsDashboard.fxml");

            if (vitalsViewUrl == null) {
                showMessage("Could not find VitalSignsDashboard.fxml. Make sure the file exists in the views directory.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(vitalsViewUrl);
            Parent vitalsView = loader.load();

            // Get controller and pass data
            VitalSignsDashboard controller = loader.getController();
            controller.initialize(currentPatient, userManager);

            // Set up the scene
            Scene scene = new Scene(vitalsView);
            URL cssUrl = findResource("com/rhms/ui/resources/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            vitalsStage.setScene(scene);
            vitalsStage.setMinWidth(1000);
            vitalsStage.setMinHeight(700);
            vitalsStage.show();

        } catch (Exception e) {
            showMessage("Error displaying vital signs dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleFeedback(ActionEvent event) {
        try {
            // Create a dialog for viewing and adding doctor feedback
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Doctor Feedback");
            dialog.setHeaderText("View and submit feedback for your doctors");

            // Set the button types
            ButtonType submitButtonType = new ButtonType("Submit New Feedback", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(submitButtonType, ButtonType.CLOSE);

            // Create content
            VBox content = new VBox(15);
            content.setPadding(new Insets(20, 20, 10, 20));

            // Section for viewing existing feedback
            Label existingFeedbackLabel = new Label("Existing Feedback:");
            existingFeedbackLabel.setStyle("-fx-font-weight: bold;");
            content.getChildren().add(existingFeedbackLabel);

            // Get feedback from the database instead of only from the patient object
            ArrayList<String> feedbackList = new ArrayList<>();
            if (userManager != null && currentPatient != null && userManager.dbHandler != null) {
                feedbackList.addAll(userManager.dbHandler.getPatientFeedbacks(currentPatient.getUserID(), null));
            } else {
                feedbackList = currentPatient.getDoctorFeedback();
            }

            if (feedbackList.isEmpty()) {
                Label noFeedbackLabel = new Label("No feedback records found.");
                noFeedbackLabel.setStyle("-fx-font-style: italic;");
                content.getChildren().add(noFeedbackLabel);
            } else {
                // Create a list view to display existing feedback
                ListView<String> feedbackListView = new ListView<>();
                feedbackListView.setPrefHeight(200);
                feedbackListView.setItems(FXCollections.observableArrayList(feedbackList));
                content.getChildren().add(feedbackListView);
            }

            // Section for adding new feedback
            Label newFeedbackLabel = new Label("Add New Feedback:");
            newFeedbackLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 0 0;");
            content.getChildren().add(newFeedbackLabel);

            // Create doctor selection dropdown
            Label doctorLabel = new Label("Select Doctor:");
            ComboBox<String> doctorComboBox = new ComboBox<>();

            // Populate with the patient's doctors
            ObservableList<String> doctorNames = FXCollections.observableArrayList();
            List<Doctor> assignedDoctors = currentPatient.getAssignedDoctors();
            for (Doctor doctor : assignedDoctors) {
                doctorNames.add("Dr. " + doctor.getName() + " (" + doctor.getSpecialization() + ")");
            }

            if (doctorNames.isEmpty()) {
                doctorNames.add("No assigned doctors");
            }

            doctorComboBox.setItems(doctorNames);
            doctorComboBox.getSelectionModel().selectFirst();

            // Create text area for feedback content
            Label feedbackLabel = new Label("Your Feedback:");
            TextArea feedbackTextArea = new TextArea();
            feedbackTextArea.setPromptText("Enter your feedback here...");
            feedbackTextArea.setPrefHeight(100);
            feedbackTextArea.setWrapText(true);

            // Star rating for doctor
            Label ratingLabel = new Label("Rating (1-5 stars):");
            HBox ratingBox = new HBox(5);
            ToggleGroup ratingGroup = new ToggleGroup();

            for (int i = 1; i <= 5; i++) {
                final int rating = i;
                RadioButton rb = new RadioButton(Integer.toString(i));
                rb.setToggleGroup(ratingGroup);
                rb.setUserData(rating);
                if (i == 5) {
                    rb.setSelected(true);
                }
                ratingBox.getChildren().add(rb);
            }

            // Add fields to the form
            GridPane form = new GridPane();
            form.setHgap(10);
            form.setVgap(10);

            form.add(doctorLabel, 0, 0);
            form.add(doctorComboBox, 1, 0);
            form.add(ratingLabel, 0, 1);
            form.add(ratingBox, 1, 1);
            form.add(feedbackLabel, 0, 2);
            form.add(feedbackTextArea, 0, 3, 2, 1);

            content.getChildren().add(form);

            // Set content
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().setPrefWidth(600);
            dialog.getDialogPane().setPrefHeight(500);

            // Enable/disable submit button based on text area content
            Node submitButton = dialog.getDialogPane().lookupButton(submitButtonType);
            submitButton.setDisable(doctorNames.get(0).equals("No assigned doctors"));

            feedbackTextArea.textProperty().addListener((observable, oldValue, newValue) -> {
                submitButton.setDisable(newValue.trim().isEmpty() ||
                                       doctorNames.get(0).equals("No assigned doctors"));
            });

            // Set result converter
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == submitButtonType) {
                    String selectedDoctor = doctorComboBox.getValue();
                    Toggle selectedRating = ratingGroup.getSelectedToggle();
                    int rating = selectedRating != null ? (int)selectedRating.getUserData() : 5;

                    return selectedDoctor + " - " + rating + " stars: " + feedbackTextArea.getText();
                }
                return null;
            });

            // Apply CSS
            URL cssUrl = findResource("com/rhms/ui/resources/styles.css");
            if (cssUrl != null) {
                dialog.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            }

            // Show dialog and process result
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(feedback -> {
                // Save the feedback to the database
                int doctorIndex = doctorComboBox.getSelectionModel().getSelectedIndex();
                if (doctorIndex >= 0 && doctorIndex < assignedDoctors.size()) {
                    Doctor selectedDoctor = assignedDoctors.get(doctorIndex);
                    Toggle selectedRating = ratingGroup.getSelectedToggle();
                    int rating = selectedRating != null ? (int)selectedRating.getUserData() : 5;
                    String feedbackText = feedbackTextArea.getText().trim();

                    // Save to DB
                    boolean success = false;
                    if (userManager != null && userManager.dbHandler != null) {
                        success = userManager.dbHandler.addPatientFeedback(
                            currentPatient.getUserID(),
                            selectedDoctor.getUserID(),
                            feedbackText,
                            rating
                        );
                    }
                    if (success) {
                        showMessage("Feedback submitted successfully!");
                    } else {
                        showMessage("Failed to submit feedback. Please try again.");
                    }
                } else {
                    showMessage("Invalid doctor selection.");
                }
            });

        } catch (Exception e) {
            showMessage("Error displaying feedback dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleEmergencyAlert(ActionEvent event) {
        // Show emergency alert dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Emergency Alert");
        alert.setHeaderText("Send Emergency Alert?");
        alert.setContentText("This will send an immediate alert to your assigned doctors. Continue?");

        ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(yesButton, noButton);

        alert.showAndWait().ifPresent(buttonType -> {
            if (buttonType == yesButton) {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Emergency Reason");
                dialog.setHeaderText("Provide reason for emergency alert");
                dialog.setContentText("Reason:");

                dialog.showAndWait().ifPresent(reason -> {
                    if (!reason.trim().isEmpty()) {
                        currentPatient.getPanicButton().triggerAlert(reason);
                        showMessage("Emergency alert sent successfully!");
                    } else {
                        showMessage("Emergency reason cannot be empty.");
                    }
                });
            }
        });
    }

    @FXML
    public void handleUploadVitals(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select CSV File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        File selectedFile = fileChooser.showOpenDialog(((Node)event.getSource()).getScene().getWindow());
        if (selectedFile != null) {
            try {
                VitalsUploadReport report = CSVVitalsUploader.uploadVitalsFromCSVWithReport(
                        currentPatient, selectedFile.getAbsolutePath());

                // Show upload results in dialog
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Upload Results");
                alert.setHeaderText("Vitals Upload Report");
                alert.setContentText("Successfully uploaded " + report.getSuccessCount() + " records.\n" +
                        "Failed to upload " + report.getErrorCount() + " records.");

                // If there were errors, show details in expandable content
                if (report.getErrorCount() > 0) {
                    TextArea textArea = new TextArea(report.generateReport());
                    textArea.setEditable(false);
                    textArea.setWrapText(true);
                    textArea.setPrefWidth(550);
                    textArea.setPrefHeight(300);

                    alert.getDialogPane().setExpandableContent(new VBox(textArea));
                    alert.getDialogPane().setExpanded(true);
                }

                alert.showAndWait();

                // Refresh vitals display
                loadLatestVitals();

            } catch (IOException e) {
                showMessage("Error uploading vitals: " + e.getMessage());
            }
        }
    }

    @Override
    public void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/rhms/ui/views/LoginView.fxml"));
            Parent loginView = loader.load();

            // Pass userManager to login controller
            LoginViewController controller = loader.getController();
            controller.setUserManager(userManager);

            // Get the current stage
            Scene scene = nameLabel.getScene();
            Stage stage = (Stage) scene.getWindow();

            // Setup new scene
            scene = new Scene(loginView);
            scene.getStylesheets().add(getClass().getResource("/com/rhms/ui/resources/styles.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("RHMS - Login");
            stage.show();

        } catch (IOException e) {
            showMessage("Error returning to login screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
                    System.out.println("Found resource at: " + file.getAbsolutePath());
                }
            }

            // Try in the target/classes directory
            if (url == null) {
                File file = new File("target/classes/" + path);
                if (file.exists()) {
                    url = file.toURI().toURL();
                    System.out.println("Found resource at: " + file.getAbsolutePath());
                }
            }

            if (url == null) {
                System.err.println("RESOURCE NOT FOUND: " + path);
                File dir = new File("src/com/rhms/ui/views");
                if (dir.exists() && dir.isDirectory()) {
                    System.out.println("Available files in views directory:");
                    for (String file : dir.list()) {
                        System.out.println(" - " + file);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error finding resource '" + path + "': " + e.getMessage());
        }

        return url;
    }

    @FXML
    public void handleViewAssignedDoctors(ActionEvent event) {
        try {
            // Force reload of assignments before displaying
            if (userManager != null) {
                userManager.loadAssignmentsForPatient(currentPatient);  // Load for this patient specifically
            }

            // Create a dialog to show assigned doctors
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("My Assigned Doctors");
            dialog.setHeaderText("Doctors assigned to " + currentPatient.getName());

            // Set the button types
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

            // Create content
            VBox content = new VBox(10);
            content.setPadding(new Insets(20, 20, 10, 20));

            // Get the updated list of doctors
            List<Doctor> doctors = currentPatient.getAssignedDoctors();
            System.out.println("Patient " + currentPatient.getName() + " has " +
                              doctors.size() + " assigned doctors");

            if (doctors.isEmpty()) {
                Label noDocsLabel = new Label("No doctors currently assigned to you.");
                noDocsLabel.setStyle("-fx-font-style: italic;");
                content.getChildren().add(noDocsLabel);
            } else {
                TableView<Doctor> doctorsTable = new TableView<>();
                doctorsTable.setMinHeight(300);

                TableColumn<Doctor, String> nameCol = new TableColumn<>("Name");
                nameCol.setCellValueFactory(data -> new SimpleStringProperty("Dr. " + data.getValue().getName()));
                nameCol.setPrefWidth(150);

                TableColumn<Doctor, String> specCol = new TableColumn<>("Specialization");
                specCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSpecialization()));
                specCol.setPrefWidth(150);

                TableColumn<Doctor, String> expCol = new TableColumn<>("Experience");
                expCol.setCellValueFactory(data -> new SimpleStringProperty(
                        data.getValue().getExperienceYears() + " years"));
                expCol.setPrefWidth(100);

                TableColumn<Doctor, String> emailCol = new TableColumn<>("Email");
                emailCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
                emailCol.setPrefWidth(200);

                doctorsTable.getColumns().addAll(nameCol, specCol, expCol, emailCol);
                doctorsTable.setItems(FXCollections.observableArrayList(doctors));

                content.getChildren().add(doctorsTable);
            }

            // Set content
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().setPrefWidth(600);
            dialog.getDialogPane().setPrefHeight(400);

            // Apply CSS
            URL cssUrl = findResource("com/rhms/ui/resources/styles.css");
            if (cssUrl != null) {
                dialog.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            }

            // Show dialog
            dialog.showAndWait();

        } catch (Exception e) {
            showMessage("Error displaying assigned doctors: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleRequestDoctor(ActionEvent event) {
        try {
            // Create doctor request dialog
            Dialog<Map<String, String>> dialog = new Dialog<>();
            dialog.setTitle("Request Doctor Assignment");
            dialog.setHeaderText("Request a new doctor or change your current doctor");

            // Set the button types
            ButtonType requestButtonType = new ButtonType("Submit Request", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(requestButtonType, ButtonType.CANCEL);

            // Create fields
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 20, 10, 20));

            // Request type dropdown (New Doctor or Change Doctor)
            ComboBox<String> requestTypeCombo = new ComboBox<>();
            requestTypeCombo.getItems().addAll("New Doctor", "Change Doctor");
            requestTypeCombo.setValue("New Doctor");
            grid.add(new Label("Request Type:"), 0, 0);
            grid.add(requestTypeCombo, 1, 0);

            // Doctor specialization dropdown
            ComboBox<String> specializationCombo = new ComboBox<>();
            specializationCombo.getItems().addAll(
                "General Practice",
                "Cardiology",
                "Dermatology",
                "Endocrinology",
                "Gastroenterology",
                "Neurology",
                "Obstetrics",
                "Oncology",
                "Ophthalmology",
                "Orthopedics",
                "Pediatrics",
                "Psychiatry",
                "Radiology",
                "Urology"
            );
            specializationCombo.setEditable(true); // Allow custom entries
            specializationCombo.setValue("General Practice");
            grid.add(new Label("Doctor Specialization:"), 0, 1);
            grid.add(specializationCombo, 1, 1);

            // Additional details text area
            TextArea detailsArea = new TextArea();
            detailsArea.setPromptText("Provide any additional details about your request");
            detailsArea.setPrefRowCount(5);
            detailsArea.setWrapText(true);
            grid.add(new Label("Additional Details:"), 0, 2);
            grid.add(detailsArea, 1, 2);

            // Enable/Disable request button depending on required fields
            Node requestButton = dialog.getDialogPane().lookupButton(requestButtonType);

            // Initial validation
            requestButton.setDisable(specializationCombo.getValue() == null ||
                                    specializationCombo.getValue().trim().isEmpty());

            // Validation on specialization change
            specializationCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
                requestButton.setDisable(newValue == null || newValue.trim().isEmpty());
            });

            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().setPrefWidth(450);

            // Convert result when request button is clicked
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == requestButtonType) {
                    Map<String, String> result = new HashMap<>();
                    result.put("requestType", requestTypeCombo.getValue());
                    result.put("specialization", specializationCombo.getValue());
                    result.put("details", detailsArea.getText());
                    return result;
                }
                return null;
            });

            // Apply CSS
            URL cssUrl = findResource("com/rhms/ui/resources/styles.css");
            if (cssUrl != null) {
                dialog.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            }

            // Show dialog and process result
            Optional<Map<String, String>> result = dialog.showAndWait();
            result.ifPresent(requestData -> {
                try {
                    // Get data from the form
                    String requestType = requestData.get("requestType");
                    String specialization = requestData.get("specialization");
                    String details = requestData.get("details");

                    // Create database handler if needed
                    com.rhms.Database.DoctorRequestDatabaseHandler requestDbHandler =
                        new com.rhms.Database.DoctorRequestDatabaseHandler();

                    // Save the request to database
                    int requestId = requestDbHandler.addDoctorRequest(
                        currentPatient.getUserID(),
                        requestType,
                        specialization,
                        details
                    );

                    if (requestId > 0) {
                        showMessage("Doctor request submitted successfully!\nYour request ID: " + requestId);
                    } else {
                        showMessage("Failed to submit doctor request. Please try again.");
                    }
                } catch (SQLException e) {
                    showMessage("Database error: " + e.getMessage());
                    e.printStackTrace();
                } catch (Exception e) {
                    showMessage("Error submitting doctor request: " + e.getMessage());
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            showMessage("Error creating doctor request form: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleChatWithDoctor(ActionEvent event) {
        try {
            // Create a new stage for the chat view
            Stage chatStage = new Stage();
            chatStage.setTitle("Chat with Doctor - " + currentPatient.getName());
            chatStage.initModality(Modality.WINDOW_MODAL);
            chatStage.initOwner(((Node)event.getSource()).getScene().getWindow());

            // Load the chat view
            URL chatViewUrl = findResource("com/rhms/ui/views/ChatWithDoctorDashboard.fxml");

            if (chatViewUrl == null) {
                showMessage("Could not find ChatWithDoctorDashboard.fxml. Make sure the file exists in the views directory.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(chatViewUrl);
            Parent chatView = loader.load();

            // Get controller and pass data
            ChatWithDoctorDashboard controller = loader.getController();
            controller.initialize(currentPatient, userManager);

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
    private void loadVitalsFromDatabase() {
        try {
            // Get the current patient
            Patient currentPatient = getCurrentPatient();
            if (currentPatient == null) {
                return;
            }

            // Create database handler
            VitalSignDatabaseHandler vitalDbHandler = new VitalSignDatabaseHandler();

            // Load vital signs for the patient from database
            List<VitalSign> vitals = vitalDbHandler.getVitalSignsForPatient(currentPatient.getUserID());

            // Update the patient's vitals database with loaded data
            if (vitals != null && !vitals.isEmpty()) {
                currentPatient.getVitalsDatabase().addVitalRecords(vitals);

                // Update dashboard with latest vitals
                updateVitalsDisplay();
            }
        } catch (Exception e) {
            System.err.println("Error loading vitals from database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Updates the dashboard display with the latest vital signs
     */
    private void updateVitalsDisplay() {
        Patient currentPatient = getCurrentPatient();
        if (currentPatient == null || !currentPatient.getVitalsDatabase().hasVitalsData()) {
            return;
        }

        VitalSign latestVital = currentPatient.getVitalsDatabase().getLatestVitalSigns();
        if (latestVital != null) {
            // Update dashboard labels with latest values
            heartRateLabel.setText(String.format("%.1f bpm", latestVital.getHeartRate()));
            oxygenLabel.setText(String.format("%.1f%%", latestVital.getOxygenLevel()));
            bloodPressureLabel.setText(String.format("%.1f mmHg", latestVital.getBloodPressure()));
            temperatureLabel.setText(String.format("%.1f°C", latestVital.getTemperature()));

            // Apply appropriate style classes based on vital status
            applyVitalStatusStyles(latestVital);
        }
    }

    /**
     * Apply appropriate CSS classes to vitals based on their values
     */
    private void applyVitalStatusStyles(VitalSign vitalSign) {
        // Remove any existing status classes
        heartRateLabel.getStyleClass().removeAll("vitals-normal", "vitals-warning", "vitals-critical");
        oxygenLabel.getStyleClass().removeAll("vitals-normal", "vitals-warning", "vitals-critical");
        bloodPressureLabel.getStyleClass().removeAll("vitals-normal", "vitals-warning", "vitals-critical");
        temperatureLabel.getStyleClass().removeAll("vitals-normal", "vitals-warning", "vitals-critical");

        // Apply heart rate styles
        heartRateLabel.getStyleClass().add(vitalSign.isHeartRateNormal() ?
                "vitals-normal" : "vitals-critical");

        // Apply oxygen level styles
        oxygenLabel.getStyleClass().add(vitalSign.isOxygenLevelNormal() ?
                "vitals-normal" : "vitals-critical");

        // Apply blood pressure styles
        bloodPressureLabel.getStyleClass().add(vitalSign.isBloodPressureNormal() ?
                "vitals-normal" : "vitals-critical");

        // Apply temperature styles
        temperatureLabel.getStyleClass().add(vitalSign.isTemperatureNormal() ?
                "vitals-normal" : "vitals-critical");
    }

    /**
     * Helper method to get the current patient
     */
    private Patient getCurrentPatient() {
        // Use the current patient field that was already set during initialization
        // instead of trying to get it from userManager
        return this.currentPatient;
    }

    @FXML
    public void handleGenerateReport(ActionEvent event) {
        try {
            // Create dialog for report options
            Dialog<ReportOptions> dialog = new Dialog<>();
            dialog.setTitle("Generate Health Report");
            dialog.setHeaderText("Create a downloadable report with your health data");

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
            CheckBox feedbackCheckbox = new CheckBox("Doctor's Feedback");
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
            javafx.scene.Node generateButton = dialog.getDialogPane().lookupButton(generateButtonType);
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
                    return new ReportOptions(
                            vitalsCheckbox.isSelected(),
                            feedbackCheckbox.isSelected(),
                            trendsCheckbox.isSelected(),
                            formatCombo.getValue(),
                            startDatePicker.getValue(),
                            endDatePicker.getValue()
                    );
                }
                return null;
            });

            // Show dialog and handle result
            Optional<ReportOptions> result = dialog.showAndWait();
            result.ifPresent(options -> {
                try {
                    // Show directory chooser for save location
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Select Save Location");
                    File directory = directoryChooser.showDialog(((javafx.scene.Node)event.getSource()).getScene().getWindow());

                    if (directory != null) {
                        // Create the report generator
                        ReportGenerator generator = new ReportGenerator(currentPatient);

                        // Generate the report based on selected options
                        File reportFile = generator.generateReport(
                                directory.getAbsolutePath(),
                                options.includeVitals,
                                options.includeFeedback,
                                options.includeTrends,
                                options.format
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
     * Helper class to store report generation options
     */
    private static class ReportOptions {
        final boolean includeVitals;
        final boolean includeFeedback;
        final boolean includeTrends;
        final ReportFormat format;
        final java.time.LocalDate startDate;
        final java.time.LocalDate endDate;

        public ReportOptions(boolean includeVitals, boolean includeFeedback, boolean includeTrends,
                             ReportFormat format, java.time.LocalDate startDate, java.time.LocalDate endDate) {
            this.includeVitals = includeVitals;
            this.includeFeedback = includeFeedback;
            this.includeTrends = includeTrends;
            this.format = format;
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }

    @FXML
    public void handleViewMedicalHistory(ActionEvent event) {
        try {
            // Create a dialog to show medical history
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Medical History");
            dialog.setHeaderText("Medical History for " + currentPatient.getName());

            // Set the button types
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

            // Create content
            VBox content = new VBox(15);
            content.setPadding(new Insets(20, 20, 10, 20));

            // --- Doctor Feedback & Prescriptions Section ---
            Label feedbackLabel = new Label("Doctor Feedback & Prescriptions:");
            feedbackLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-underline: true;");
            content.getChildren().add(feedbackLabel);

            // Load feedback and prescriptions from database (if available)
            List<String> feedbackDisplayList = new ArrayList<>();
            try {
                if (userManager != null && userManager.dbHandler != null) {
                    String sql = "SELECT f.timestamp, u.name AS doctor_name, f.comments, " +
                            "p.medication_name, p.dosage, p.schedule, p.duration, p.instructions " +
                            "FROM feedback_by_doctor f " +
                            "JOIN Doctors d ON f.doctor_id = d.doctor_id " +
                            "JOIN Users u ON d.user_id = u.user_id " +
                            "LEFT JOIN prescription p ON f.feedback_id = p.feedback_id " +
                            "WHERE f.patient_id = ? ORDER BY f.timestamp DESC";
                    try (PreparedStatement stmt = userManager.dbHandler.connection.prepareStatement(sql)) {
                        stmt.setInt(1, currentPatient.getUserID());
                        ResultSet rs = stmt.executeQuery();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                        while (rs.next()) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("🗓 Date: ").append(sdf.format(rs.getTimestamp("timestamp"))).append("\n");
                            sb.append("👨‍⚕️ Doctor: Dr. ").append(rs.getString("doctor_name")).append("\n\n");
                            sb.append("💬 Feedback:\n").append(rs.getString("comments")).append("\n");
                            String med = rs.getString("medication_name");
                            if (med != null && !med.isEmpty()) {
                                sb.append("\n💊 Prescription:\n");
                                sb.append("   • Medication: ").append(med).append("\n");
                                String dosage = rs.getString("dosage");
                                if (dosage != null && !dosage.isEmpty())
                                    sb.append("   • Dosage: ").append(dosage).append("\n");
                                String schedule = rs.getString("schedule");
                                if (schedule != null && !schedule.isEmpty())
                                    sb.append("   • Schedule: ").append(schedule).append("\n");
                                String duration = rs.getString("duration");
                                if (duration != null && !duration.isEmpty())
                                    sb.append("   • Duration: ").append(duration).append("\n");
                                String instructions = rs.getString("instructions");
                                if (instructions != null && !instructions.trim().isEmpty())
                                    sb.append("   • Instructions: ").append(instructions).append("\n");
                            }
                            feedbackDisplayList.add(sb.toString());
                        }
                    }
                }
            } catch (Exception ex) {
                feedbackDisplayList.clear();
                feedbackDisplayList.add("Error loading feedback and prescriptions: " + ex.getMessage());
            }

            if (feedbackDisplayList.isEmpty()) {
                Label noFeedback = new Label("No feedback or prescription records found.");
                noFeedback.setStyle("-fx-font-style: italic;");
                content.getChildren().add(noFeedback);
            } else {
                ListView<String> feedbackListView = new ListView<>();
                feedbackListView.setPrefHeight(350);
                feedbackListView.setItems(FXCollections.observableArrayList(feedbackDisplayList));
                feedbackListView.setCellFactory(listView -> new ListCell<String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle("-fx-background-color: transparent;");
                        } else {
                            setText(item);
                            setWrapText(true);
                            setPrefWidth(0);
                            // Alternate background for card effect
                            if (getIndex() % 2 == 0) {
                                setStyle("-fx-background-color: #f8f9fa; -fx-padding: 12; -fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 14px;");
                            } else {
                                setStyle("-fx-background-color: #eaf3fb; -fx-padding: 12; -fx-border-color: #d0d7de; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 14px;");
                            }
                        }
                    }
                });
                content.getChildren().add(feedbackListView);
            }

            // Set content
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().setPrefWidth(700);
            dialog.getDialogPane().setPrefHeight(500);

            // Apply CSS
            URL cssUrl = findResource("com/rhms/ui/resources/styles.css");
            if (cssUrl != null) {
                dialog.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            }

            // Show dialog
            dialog.showAndWait();

        } catch (Exception e) {
            showMessage("Error displaying medical history: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
