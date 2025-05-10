package com.rhms.ui.controllers;

import com.rhms.Database.AppointmentDatabaseHandler;
import com.rhms.appointmentScheduling.AppointmentManager;
import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;
import com.rhms.userManagement.User;
import com.rhms.userManagement.UserManager;
import com.rhms.healthDataHandling.VitalSign;
import com.rhms.healthDataHandling.CSVVitalsUploader;
import com.rhms.healthDataHandling.VitalsUploadReport;
import com.rhms.appointmentScheduling.Appointment;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Insets;

import java.io.File;
import java.io.IOException;
import java.net.URL;
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

            // Set appropriate CSS classes based on vital sign status
            if (latestVital.isHeartRateNormal()) {
                heartRateLabel.getStyleClass().add("vitals-normal");
            } else {
                heartRateLabel.getStyleClass().add("vitals-critical");
            }

            if (latestVital.isOxygenLevelNormal()) {
                oxygenLabel.getStyleClass().add("vitals-normal");
            } else {
                oxygenLabel.getStyleClass().add("vitals-critical");
            }

            if (latestVital.isBloodPressureNormal()) {
                bloodPressureLabel.getStyleClass().add("vitals-normal");
            } else {
                bloodPressureLabel.getStyleClass().add("vitals-critical");
            }

            if (latestVital.isTemperatureNormal()) {
                temperatureLabel.getStyleClass().add("vitals-normal");
            } else {
                temperatureLabel.getStyleClass().add("vitals-critical");
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

        ObservableList<Appointment> appointmentData = FXCollections.observableArrayList(filteredAppointments);
        appointmentsTable.setItems(appointmentData);
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
            // Get vital signs data
            List<VitalSign> vitals = currentPatient.getVitalsDatabase().getAllVitals();

            if (vitals == null || vitals.isEmpty()) {
                showMessage("No vital signs data available.");
                return;
            }

            // Create a new stage for the vitals history view
            Stage vitalsStage = new Stage();
            vitalsStage.setTitle("Vital Signs History - " + currentPatient.getName());
            vitalsStage.initModality(Modality.WINDOW_MODAL);
            vitalsStage.initOwner(((Node)event.getSource()).getScene().getWindow());

            // Create tab pane for different views
            TabPane tabPane = new TabPane();

            // Tab 1: Table View of vitals
            Tab tableTab = new Tab("Table View");
            tableTab.setClosable(false);

            TableView<VitalSign> vitalsTable = new TableView<>();

            // Define columns
            TableColumn<VitalSign, Date> timestampCol = new TableColumn<>("Date/Time");
            timestampCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getTimestamp()));
            timestampCol.setCellFactory(column -> new TableCell<VitalSign, Date>() {
                private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

                @Override
                protected void updateItem(Date item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(dateFormat.format(item));
                    }
                }
            });

            TableColumn<VitalSign, Double> hrCol = new TableColumn<>("Heart Rate");
            hrCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getHeartRate()));

            TableColumn<VitalSign, Double> oxygenCol = new TableColumn<>("Oxygen");
            oxygenCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getOxygenLevel()));

            TableColumn<VitalSign, Double> bpCol = new TableColumn<>("Blood Pressure");
            bpCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getBloodPressure()));

            TableColumn<VitalSign, Double> tempCol = new TableColumn<>("Temperature");
            tempCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getTemperature()));

            TableColumn<VitalSign, String> statusCol = new TableColumn<>("Status");
            statusCol.setCellValueFactory(data -> {
                VitalSign vital = data.getValue();
                boolean isNormal = vital.isHeartRateNormal() &&
                                  vital.isOxygenLevelNormal() &&
                                  vital.isBloodPressureNormal() &&
                                  vital.isTemperatureNormal();
                return new SimpleStringProperty(isNormal ? "Normal" : "Abnormal");
            });

            // Add columns to table
            vitalsTable.getColumns().addAll(timestampCol, hrCol, oxygenCol, bpCol, tempCol, statusCol);

            // Set data
            ObservableList<VitalSign> vitalsData = FXCollections.observableArrayList(vitals);
            vitalsTable.setItems(vitalsData);

            // Status column styling
            statusCol.setCellFactory(column -> new TableCell<VitalSign, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        if ("Normal".equals(item)) {
                            setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                        }
                    }
                }
            });

            VBox tableContainer = new VBox(vitalsTable);
            VBox.setVgrow(vitalsTable, Priority.ALWAYS);
            tableTab.setContent(tableContainer);

            // Tab 2: Line Chart View
            Tab chartTab = new Tab("Chart View");
            chartTab.setClosable(false);

            // Create chart
            final NumberAxis xAxis = new NumberAxis();
            final NumberAxis yAxis = new NumberAxis();
            xAxis.setLabel("Measurement Number");

            final LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
            lineChart.setTitle("Vital Signs Trends");

            // Series for each vital sign
            XYChart.Series<Number, Number> heartRateSeries = new XYChart.Series<>();
            heartRateSeries.setName("Heart Rate");

            XYChart.Series<Number, Number> oxygenSeries = new XYChart.Series<>();
            oxygenSeries.setName("Oxygen Level");

            XYChart.Series<Number, Number> bpSeries = new XYChart.Series<>();
            bpSeries.setName("Blood Pressure");

            XYChart.Series<Number, Number> tempSeries = new XYChart.Series<>();
            tempSeries.setName("Temperature");

            // Populate data
            for (int i = 0; i < vitals.size(); i++) {
                VitalSign vital = vitals.get(i);
                heartRateSeries.getData().add(new XYChart.Data<>(i+1, vital.getHeartRate()));
                oxygenSeries.getData().add(new XYChart.Data<>(i+1, vital.getOxygenLevel()));
                bpSeries.getData().add(new XYChart.Data<>(i+1, vital.getBloodPressure()));
                tempSeries.getData().add(new XYChart.Data<>(i+1, vital.getTemperature()));
            }

            // Add series to chart
            lineChart.getData().addAll(heartRateSeries, oxygenSeries, bpSeries, tempSeries);

            VBox chartContainer = new VBox(lineChart);
            VBox.setVgrow(lineChart, Priority.ALWAYS);
            chartTab.setContent(chartContainer);

            // Add tabs to pane
            tabPane.getTabs().addAll(tableTab, chartTab);

            // Add controls
            Button closeButton = new Button("Close");
            closeButton.setOnAction(e -> vitalsStage.close());

            Label summaryLabel = new Label("Total Records: " + vitals.size());
            summaryLabel.setStyle("-fx-font-weight: bold;");

            HBox controlsBox = new HBox(10, summaryLabel, closeButton);
            controlsBox.setStyle("-fx-padding: 10;");
            controlsBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            VBox mainContainer = new VBox(tabPane, controlsBox);
            VBox.setVgrow(tabPane, Priority.ALWAYS);

            // Set up the scene
            Scene scene = new Scene(mainContainer, 800, 600);
            URL cssUrl = findResource("com/rhms/ui/resources/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            vitalsStage.setScene(scene);
            vitalsStage.show();

        } catch (Exception e) {
            showMessage("Error displaying vital signs history: " + e.getMessage());
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

            // Get feedback from the patient object
            ArrayList<String> feedbackList = currentPatient.getDoctorFeedback();
            
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
            for (Doctor doctor : currentPatient.getAssignedDoctors()) {
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
                // Save the feedback
                currentPatient.getDoctorFeedback().add(feedback);
                showMessage("Feedback submitted successfully!");
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
    public void handleViewMedicalHistory(ActionEvent event) {
        try {
            // Create a dialog to show medical history
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Medical History");
            dialog.setHeaderText("Medical History for " + currentPatient.getName());

            // Set the button types
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

            // Create content
            VBox content = new VBox(10);
            content.setPadding(new Insets(20, 20, 10, 20));

            // In a real application, you would load the patient's medical history from a database
            // For now, we'll display a placeholder
            Label placeholder = new Label("Medical history records would be displayed here.");
            placeholder.setStyle("-fx-font-style: italic;");
            content.getChildren().add(placeholder);

            // Create a text area for displaying medical history information
            TextArea historyTextArea = new TextArea();
            historyTextArea.setEditable(false);
            historyTextArea.setPrefHeight(300);
            historyTextArea.setText("Sample medical history information:\n\n" +
                    "- Last physical examination: 06/15/2023\n" +
                    "- Allergies: None\n" +
                    "- Chronic conditions: None\n" +
                    "- Surgeries: Appendectomy (2019)\n" +
                    "- Current medications: Vitamin D supplement\n");

            content.getChildren().add(historyTextArea);

            // Set content
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().setPrefWidth(500);
            dialog.getDialogPane().setPrefHeight(400);

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

