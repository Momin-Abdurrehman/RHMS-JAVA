package com.rhms.ui.controllers;

import com.rhms.Database.ChatMessageDatabaseHandler;
import com.rhms.Database.ChatMessageDatabaseHandler.ChatMessage;
import com.rhms.Database.UserDatabaseHandler;
import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;
import com.rhms.userManagement.User;
import com.rhms.userManagement.UserManager;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Callback;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import java.io.IOException;

import javafx.stage.Stage;
import javafx.scene.control.Alert;

public class ChatWithDoctorDashboard {

    @FXML private ListView<ChatMessage> messagesListView;
    @FXML private TextField messageField;
    @FXML private Button sendButton;
    @FXML private ComboBox<Doctor> doctorComboBox;
    @FXML private Label statusLabel;

    private Patient currentPatient;
    private UserManager userManager;
    private ChatMessageDatabaseHandler chatDbHandler;
    private UserDatabaseHandler userDbHandler;
    private Timer refreshTimer;
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Initialize the controller with a patient and user manager
     */
    public void initialize(Patient patient, UserManager userManager) {
        this.currentPatient = patient;
        this.userManager = userManager;
        this.chatDbHandler = new ChatMessageDatabaseHandler();
        this.userDbHandler = new UserDatabaseHandler();
        
        // Initialize UI components
        initializeUI();
        
        // Load the patient's assigned doctors
        loadAssignedDoctors();
        
        // Set up auto-refresh for messages
        startAutoRefresh();
    }

    /**
     * Initialize UI components and event handlers
     */
    private void initializeUI() {
        // Set up the messages list view with a custom cell factory for message bubbles
        messagesListView.setCellFactory(new Callback<ListView<ChatMessage>, ListCell<ChatMessage>>() {
            @Override
            public ListCell<ChatMessage> call(ListView<ChatMessage> param) {
                return new ListCell<ChatMessage>() {
                    @Override
                    protected void updateItem(ChatMessage message, boolean empty) {
                        super.updateItem(message, empty);

                        if (empty || message == null) {
                            setText(null);
                            setGraphic(null);
                            return;
                        }

                        // Create message container
                        VBox messageContainer = new VBox(5);
                        messageContainer.setPadding(new Insets(10));
                        messageContainer.setMaxWidth(400);

                        // Determine if this message is from the current patient or the doctor
                        boolean isFromCurrentPatient = message.getSenderId() == currentPatient.getUserID();

                        // Create message text label
                        Label messageText = new Label(message.getMessageText());
                        messageText.setWrapText(true);

                        // Format timestamp
                        String formattedTime = message.getSentAt().format(timeFormatter);
                        String formattedDate = message.getSentAt().format(dateFormatter);
                        Label timeLabel = new Label(formattedDate + " " + formattedTime);
                        timeLabel.setFont(Font.font("System", FontWeight.NORMAL, 10));

                        // Add sender name if it's a message from doctor
                        if (!isFromCurrentPatient) {
                            Label senderLabel = new Label("Dr. " + getSenderName(message.getSenderId()));
                            senderLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
                            messageContainer.getChildren().add(senderLabel);
                        }

                        messageContainer.getChildren().addAll(messageText, timeLabel);

                        // Create outer container for alignment
                        HBox wrapper = new HBox();
                        wrapper.setPrefWidth(messagesListView.getWidth() - 20);

                        // Position and style based on sender
                        if (isFromCurrentPatient) {
                            wrapper.setAlignment(Pos.CENTER_RIGHT);
                            messageContainer.getStyleClass().add("sent-message");
                        } else {
                            wrapper.setAlignment(Pos.CENTER_LEFT);
                            messageContainer.getStyleClass().add("received-message");
                        }

                        wrapper.getChildren().add(messageContainer);
                        setGraphic(wrapper);
                    }
                };
            }
        });

        // Apply CSS to message list view
        messagesListView.getStyleClass().add("chat-list");
        
        // Set up send button action
        sendButton.setOnAction(this::handleSendMessage);

        // Allow Enter key to send message
        messageField.setOnAction(this::handleSendMessage);

        // Display initial status
        statusLabel.setText("Please select a doctor to start chatting");
    }

    /**
     * Load doctors assigned to the patient in the combo box
     */
    private void loadAssignedDoctors() {
        // Ensure assignments are loaded for the patient
        if (userManager != null) {
            userManager.loadAssignmentsForPatient(currentPatient);
        }

        // Get the patient's assigned doctors
        List<Doctor> assignedDoctors = currentPatient.getAssignedDoctors();

        if (assignedDoctors.isEmpty()) {
            statusLabel.setText("You don't have any assigned doctors. Please request a doctor assignment.");
            doctorComboBox.setDisable(true);
            sendButton.setDisable(true);
            messageField.setDisable(true);
            return;
        }

        // Set the items in the combo box
        ObservableList<Doctor> doctorsList = FXCollections.observableArrayList(assignedDoctors);
        doctorComboBox.setItems(doctorsList);

        // Format how doctors appear in the dropdown
        doctorComboBox.setCellFactory(new Callback<ListView<Doctor>, ListCell<Doctor>>() {
            @Override
            public ListCell<Doctor> call(ListView<Doctor> param) {
                return new ListCell<Doctor>() {
                    @Override
                    protected void updateItem(Doctor doctor, boolean empty) {
                        super.updateItem(doctor, empty);
                        if (empty || doctor == null) {
                            setText(null);
                        } else {
                            setText("Dr. " + doctor.getName() + " (" + doctor.getSpecialization() + ")");
                        }
                    }
                };
            }
        });

        // Format the selected doctor in the combo box button
        doctorComboBox.setButtonCell(new ListCell<Doctor>() {
            @Override
            protected void updateItem(Doctor doctor, boolean empty) {
                super.updateItem(doctor, empty);
                if (empty || doctor == null) {
                    setText("Select Doctor");
                } else {
                    setText("Dr. " + doctor.getName() + " (" + doctor.getSpecialization() + ")");
                }
            }
        });

        // Add a listener to load messages when a doctor is selected
        doctorComboBox.setOnAction(e -> {
            Doctor selectedDoctor = doctorComboBox.getValue();
            if (selectedDoctor != null) {
                loadMessagesWithDoctor(selectedDoctor);
                messageField.setDisable(false);
                sendButton.setDisable(false);
            } else {
                messageField.setDisable(true);
                sendButton.setDisable(true);
            }
        });
    }

    /**
     * Handle send button click or Enter key press
     */
    @FXML
    private void handleSendMessage(ActionEvent event) {
        Doctor selectedDoctor = doctorComboBox.getValue();
        
        if (selectedDoctor == null) {
            statusLabel.setText("Please select a doctor first");
            return;
        }

        String messageText = messageField.getText().trim();
        if (messageText.isEmpty()) {
            return;
        }

        // Save the message to the database
        int messageId = chatDbHandler.saveMessage(currentPatient.getUserID(), selectedDoctor.getUserID(), messageText);

        if (messageId > 0) {
            // Clear the message field
            messageField.clear();

            // Refresh messages
            loadMessagesWithDoctor(selectedDoctor);

            // Scroll to bottom
            Platform.runLater(() -> {
                messagesListView.scrollTo(messagesListView.getItems().size() - 1);
            });
        } else {
            statusLabel.setText("Failed to send message. Please try again.");
        }
    }

    /**
     * Load messages between patient and selected doctor
     */
    private void loadMessagesWithDoctor(Doctor doctor) {
        if (doctor == null) return;
        
        // Mark messages from this doctor as read
        chatDbHandler.markMessagesAsRead(currentPatient.getUserID(), doctor.getUserID());

        // Load messages
        List<ChatMessage> messages = chatDbHandler.getMessagesBetweenUsers(
                currentPatient.getUserID(), doctor.getUserID());

        // Update the list view
        ObservableList<ChatMessage> messageList = FXCollections.observableArrayList(messages);
        messagesListView.setItems(messageList);

        // Scroll to the bottom
        if (!messages.isEmpty()) {
            Platform.runLater(() -> {
                messagesListView.scrollTo(messages.size() - 1);
            });
        }

        // Update status
        statusLabel.setText("Chatting with Dr. " + doctor.getName());
    }

    /**
     * Get the name of a user by ID
     */
    private String getSenderName(int userId) {
        User user = userDbHandler.getUserById(userId);
        if (user != null) {
            return user.getName();
        }
        return "Unknown";
    }

    /**
     * Start automatic refresh of messages
     */
    private void startAutoRefresh() {
        stopAutoRefresh(); // Stop any existing timer

        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    Doctor selectedDoctor = doctorComboBox.getValue();
                    if (selectedDoctor != null) {
                        loadMessagesWithDoctor(selectedDoctor);
                    }
                });
            }
        }, 5000, 5000); // Refresh every 5 seconds
    }

    /**
     * Stop the auto-refresh timer
     */
    private void stopAutoRefresh() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
    }

    /**
     * Handle refresh button click
     */
    @FXML
    public void handleRefresh() {
        Doctor selectedDoctor = doctorComboBox.getValue();
        if (selectedDoctor != null) {
            loadMessagesWithDoctor(selectedDoctor);
            statusLabel.setText("Messages refreshed");
        } else {
            statusLabel.setText("Please select a doctor first");
        }
    }

    /**
     * Handle back button click
     */
    @FXML
    public void handleBack() {
        // Stop refresh timer before navigating away
        stopAutoRefresh();

        // Get current stage
        Stage stage = (Stage) messageField.getScene().getWindow();
        stage.close();
    }

    /**
     * Helper method to find a resource file
     */
    private URL findResource(String path) {
        URL url = getClass().getClassLoader().getResource(path);

        if (url == null) {
            url = getClass().getResource("/" + path);
        }

        if (url == null) {
            try {
                File file = new File("src/" + path);
                if (file.exists()) {
                    url = file.toURI().toURL();
                }

                if (url == null) {
                    file = new File("target/classes/" + path);
                    if (file.exists()) {
                        url = file.toURI().toURL();
                    }
                }
            } catch (Exception e) {
                // Silently handle exception
            }
        }

        return url;
    }

    /**
     * Show an error message
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
