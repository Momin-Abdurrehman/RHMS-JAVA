package com.rhms.ui.controllers;

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
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Date;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

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
    }

    @Override
    public void initializeDashboard() {
        nameLabel.setText(currentPatient.getName());

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
            
            // Load the appointments view
            URL appointmentsViewUrl = findResource("com/rhms/ui/views/PatientAppointmentsView.fxml");
            
            if (appointmentsViewUrl == null) {
                showMessage("Could not find appointments view resource");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(appointmentsViewUrl);
            Parent appointmentsView = loader.load();
            
            // Get controller and pass data
            PatientAppointmentsController controller = loader.getController();
            controller.initializeAppointments(currentPatient, appointments);
            
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
        // Code to show feedback view
        showMessage("Feedback view not implemented yet.");
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
        URL url = getClass().getClassLoader().getResource(path);
        
        // Try alternate approaches if the resource wasn't found
        if (url == null) {
            url = getClass().getResource("/" + path);
        }
        
        if (url == null) {
            try {
                // Try source folder
                File file = new File("src/" + path);
                if (file.exists()) {
                    url = file.toURI().toURL();
                }
                
                // Try target/classes folder
                if (url == null) {
                    file = new File("target/classes/" + path);
                    if (file.exists()) {
                        url = file.toURI().toURL();
                    }
                }
            } catch (Exception e) {
                // Silently handle exception - will return null if file not found
                System.err.println("Error finding resource: " + e.getMessage());
            }
        }
        
        return url;
    }
}
