package com.rhms.ui.controllers;

import com.rhms.Database.VitalSignDatabaseHandler;
import com.rhms.userManagement.Patient;
import com.rhms.userManagement.UserManager;
import com.rhms.healthDataHandling.VitalSign;
import com.rhms.reporting.ReportFormat;
import com.rhms.reporting.ReportGenerator;
import com.rhms.reporting.DownloadHandler;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * Controller for the Vital Signs Dashboard view
 * Displays patient vital signs in both table and graph format
 */
public class VitalSignsDashboard {

    @FXML private TabPane tabPane;
    @FXML private Tab tableTab;
    @FXML private Tab chartTab;
    @FXML private TableView<VitalSign> vitalsTable;
    @FXML private TableColumn<VitalSign, Date> timestampCol;
    @FXML private TableColumn<VitalSign, Double> heartRateCol;
    @FXML private TableColumn<VitalSign, Double> oxygenCol;
    @FXML private TableColumn<VitalSign, Double> bloodPressureCol;
    @FXML private TableColumn<VitalSign, Double> temperatureCol;
    @FXML private TableColumn<VitalSign, String> statusCol;
    @FXML private LineChart<Number, Number> lineChart;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private Label summaryLabel;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button filterButton;
    @FXML private Button resetFilterButton;
    @FXML private Button refreshButton;
    @FXML private ComboBox<ReportFormat> reportFormatComboBox;
    @FXML private Button generateReportButton;

    private Patient currentPatient;
    private UserManager userManager;
    private VitalSignDatabaseHandler vitalSignDbHandler;
    private DecimalFormat decimalFormat = new DecimalFormat("#.0");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Initialize the controller with a patient and user manager
     */
    public void initialize(Patient patient, UserManager userManager) {
        this.currentPatient = patient;
        this.userManager = userManager;
        this.vitalSignDbHandler = new VitalSignDatabaseHandler();

        // Initialize UI components
        initializeUI();

        // Load the vital signs from database
        loadVitalSigns();

        // Set event handlers
        setupEventHandlers();

        // Add report generation components if not already in FXML
        addReportGenerationComponents();
    }

    private void loadVitalSigns() {
        // Fetch all vitals for the current patient from the database
        List<VitalSign> vitals = vitalSignDbHandler.getVitalSignsForPatient(currentPatient.getUserID());
        updateTableAndChart(vitals);
    }

    /**
     * Initialize UI components
     */
    private void initializeUI() {
        // Table tab setup
        setupTableColumns();

        // Chart tab setup
        xAxis.setLabel("Measurement Number");
        yAxis.setLabel("Value");
        lineChart.setTitle("Vital Signs Trends");

        // Set up date pickers with default values (last 30 days)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        startDatePicker.setValue(startDate);
        endDatePicker.setValue(endDate);
    }

    private void setupTableColumns() {
        // Set up table columns to display vital sign properties
        timestampCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getTimestamp()));
        heartRateCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getHeartRate()));
        oxygenCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getOxygenLevel()));
        bloodPressureCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getBloodPressure()));
        temperatureCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getTemperature()));
        statusCol.setCellValueFactory(cellData -> {
            String status = cellData.getValue().isAbnormal() ? "Abnormal" : "Normal";
            return new SimpleStringProperty(status);
        });
    }

    /**
     * Add report generation UI components if they don't exist
     */
    private void addReportGenerationComponents() {
        // Check if components already exist (e.g., from FXML)
        if (reportFormatComboBox == null) {
            reportFormatComboBox = new ComboBox<>();
            reportFormatComboBox.getItems().addAll(ReportFormat.values());
            reportFormatComboBox.setValue(ReportFormat.PDF);
            reportFormatComboBox.setPromptText("Report Format");
        }

        if (generateReportButton == null) {
            generateReportButton = new Button("Generate Report");
            generateReportButton.setOnAction(event -> handleGenerateReport());
        }

        // Create an HBox to hold the components
        HBox reportBox = new HBox(10);
        reportBox.setAlignment(Pos.CENTER_RIGHT);
        reportBox.setPadding(new Insets(10, 0, 0, 0));
        reportBox.getChildren().addAll(new Label("Report Format:"), reportFormatComboBox, generateReportButton);

        // Add to the scene graph
        // Find the parent of the existing buttons
        if (refreshButton != null && refreshButton.getParent() != null) {
            // Add after the existing filter controls
            HBox filterBox = (HBox) refreshButton.getParent();
            if (filterBox.getParent() instanceof VBox) {
                VBox parent = (VBox) filterBox.getParent();
                parent.getChildren().add(reportBox);
            }
        } else {
            // Fallback: add directly to the root
            VBox root = (VBox) vitalsTable.getParent();
            while (!(root instanceof VBox) && root != null) {
                root = (VBox) root.getParent();
            }
            if (root != null) {
                root.getChildren().add(reportBox);
            }
        }
    }

    // ... existing code ...

    /**
     * Set up event handlers for buttons and controls
     */
    private void setupEventHandlers() {
        // Filter button action
        filterButton.setOnAction(event -> {
            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();

            if (start != null && end != null) {
                // Convert LocalDate to Date
                Date startDate = Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant());
                // Add a day to end date to include the end date in the results
                Date endDate = Date.from(end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

                if (startDate.after(endDate)) {
                    showAlert("Start date must be before end date.");
                    return;
                }

                // Load vitals within the date range
                loadVitalSignsInDateRange(startDate, endDate);
            } else {
                showAlert("Please select both start and end dates.");
            }
        });

        // Reset filter button action
        resetFilterButton.setOnAction(event -> {
            // Reset date pickers
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            startDatePicker.setValue(startDate);
            endDatePicker.setValue(endDate);

            // Reload all vitals
            loadVitalSigns();
        });

        // Refresh button action
        refreshButton.setOnAction(event -> {
            // Reload vitals based on current filter settings
            if (startDatePicker.getValue() != null && endDatePicker.getValue() != null) {
                Date startDate = Date.from(startDatePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());
                Date endDate = Date.from(endDatePicker.getValue().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
                loadVitalSignsInDateRange(startDate, endDate);
            } else {
                loadVitalSigns();
            }
        });

        // Generate report button action
        if (generateReportButton != null) {
            generateReportButton.setOnAction(event -> handleGenerateReport());
        }
    }

    private void loadVitalSignsInDateRange(Date startDate, Date endDate) {
        // Fetch vitals from database
        List<VitalSign> vitals = vitalSignDbHandler.getVitalsInDateRange(currentPatient.getUserID(), startDate, endDate);

        // Update table and chart with filtered data
        updateTableAndChart(vitals);
    }

    private void updateTableAndChart(List<VitalSign> vitals) {
        // Clear existing data
        vitalsTable.getItems().clear();
        lineChart.getData().clear();

        // Populate table
        ObservableList<VitalSign> observableVitals = FXCollections.observableArrayList(vitals);
        vitalsTable.setItems(observableVitals);

        // Create a series for each vital sign
        XYChart.Series<Number, Number> heartRateSeries = new XYChart.Series<>();
        heartRateSeries.setName("Heart Rate (bpm)");
        XYChart.Series<Number, Number> oxygenSeries = new XYChart.Series<>();
        oxygenSeries.setName("Oxygen (%)");
        XYChart.Series<Number, Number> bloodPressureSeries = new XYChart.Series<>();
        bloodPressureSeries.setName("Blood Pressure (mmHg)");
        XYChart.Series<Number, Number> temperatureSeries = new XYChart.Series<>();
        temperatureSeries.setName("Temperature (°C)");

        for (int i = 0; i < vitals.size(); i++) {
            VitalSign vital = vitals.get(i);
            int xValue = i + 1;
            heartRateSeries.getData().add(new XYChart.Data<>(xValue, vital.getHeartRate()));
            oxygenSeries.getData().add(new XYChart.Data<>(xValue, vital.getOxygenLevel()));
            bloodPressureSeries.getData().add(new XYChart.Data<>(xValue, vital.getBloodPressure()));
            temperatureSeries.getData().add(new XYChart.Data<>(xValue, vital.getTemperature()));
        }

        lineChart.getData().addAll(heartRateSeries, oxygenSeries, bloodPressureSeries, temperatureSeries);

        // Update summary label
        summaryLabel.setText("Showing " + vitals.size() + " records");
    }

    /**
     * Handle generate report button click
     */
    private void handleGenerateReport() {
        // Get the selected report format
        ReportFormat format = reportFormatComboBox.getValue();
        if (format == null) {
            showAlert("Please select a report format.");
            return;
        }

        // Show directory chooser for save location
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Save Location");
        File directory = directoryChooser.showDialog(vitalsTable.getScene().getWindow());

        if (directory != null) {
            try {
                // Create report generator
                ReportGenerator reportGenerator = new ReportGenerator(currentPatient);

                // Get date range if set
                boolean hasDateRange = startDatePicker.getValue() != null && endDatePicker.getValue() != null;
                Date startDate = null;
                Date endDate = null;

                if (hasDateRange) {
                    startDate = Date.from(startDatePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());
                    endDate = Date.from(endDatePicker.getValue().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
                }

                // Generate report
                File reportFile = reportGenerator.generateVitalsOnlyReport(directory.getAbsolutePath(), format);

                // Show success message
                showAlert("Report generated successfully!\nFile: " + reportFile.getAbsolutePath());

                // Try to open the file
                DownloadHandler.openFile(reportFile);

            } catch (IOException e) {
                showAlert("Error generating report: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void showAlert(String s) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(s);
        alert.showAndWait();
    }

}

