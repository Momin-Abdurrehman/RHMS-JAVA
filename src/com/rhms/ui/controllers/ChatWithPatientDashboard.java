package com.rhms.ui.controllers;

import com.rhms.Database.ChatMessageDatabaseHandler;
import com.rhms.Database.ChatMessageDatabaseHandler.ChatMessage;
import com.rhms.Database.UserDatabaseHandler;
import com.rhms.userManagement.Doctor;
import com.rhms.userManagement.Patient;
import com.rhms.userManagement.User;
import com.rhms.userManagement.UserManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Callback;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javafx.stage.Stage;

public class ChatWithPatientDashboard {

    @FXML private ListView<ChatMessage> messagesListView;
    @FXML private TextField messageField;
    @FXML private Button sendButton;
    @FXML private ComboBox<Patient> patientComboBox;
    @FXML private Label statusLabel;

    private Doctor currentDoctor;
    private UserManager userManager;
    private ChatMessageDatabaseHandler chatDbHandler;
    private UserDatabaseHandler userDbHandler;
    private Timer refreshTimer;
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Initialize the controller with a doctor and user manager
     */
    public void initialize(Doctor doctor, UserManager userManager) {
        this.currentDoctor = doctor;
        this.userManager = userManager;
        this.chatDbHandler = new ChatMessageDatabaseHandler();
        this.userDbHandler = new UserDatabaseHandler();
        
        // Initialize UI components
        initializeUI();
        
        // Load the doctor's assigned patients
        loadAssignedPatients();
        
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

                        // Determine if this message is from the current doctor or the patient
                        boolean isFromCurrentDoctor = message.getSenderId() == currentDoctor.getUserID();

                        // Create message text label
                        Label messageText = new Label(message.getMessageText());
                        messageText.setWrapText(true);

                        // Format timestamp
                        String formattedTime = message.getSentAt().format(timeFormatter);
                        String formattedDate = message.getSentAt().format(dateFormatter);
                        Label timeLabel = new Label(formattedDate + " " + formattedTime);
                        timeLabel.setFont(Font.font("System", FontWeight.NORMAL, 10));

                        // Add sender name if it's a message from patient
                        if (!isFromCurrentDoctor) {
                            Label senderLabel = new Label(getSenderName(message.getSenderId()));
                            senderLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
                            messageContainer.getChildren().add(senderLabel);
                        }

                        messageContainer.getChildren().addAll(messageText, timeLabel);

                        // Create outer container for alignment
                        HBox wrapper = new HBox();
                        wrapper.setPrefWidth(messagesListView.getWidth() - 20);

                        // Position and style based on sender
                        if (isFromCurrentDoctor) {
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
        statusLabel.setText("Please select a patient to start chatting");
        
        // Disable message field and send button until a patient is selected
        messageField.setDisable(true);
        sendButton.setDisable(true);
    }

    /**
     * Load patients assigned to the doctor in the combo box
     */
    private void loadAssignedPatients() {
        // Ensure assignments are loaded for the doctor
        if (userManager != null) {
            userManager.loadAssignmentsForDoctor(currentDoctor);
        }

        // Get the doctor's assigned patients
        List<Patient> assignedPatients = currentDoctor.getAssignedPatients();

        if (assignedPatients.isEmpty()) {
            statusLabel.setText("You don't have any assigned patients. Please contact an administrator.");
            patientComboBox.setDisable(true);
            sendButton.setDisable(true);
            messageField.setDisable(true);
            return;
        }

        // Set the items in the combo box
        ObservableList<Patient> patientsList = FXCollections.observableArrayList(assignedPatients);
        patientComboBox.setItems(patientsList);

        // Format how patients appear in the dropdown
        patientComboBox.setCellFactory(new Callback<ListView<Patient>, ListCell<Patient>>() {
            @Override
            public ListCell<Patient> call(ListView<Patient> param) {
                return new ListCell<Patient>() {
                    @Override
                    protected void updateItem(Patient patient, boolean empty) {
                        super.updateItem(patient, empty);
                        if (empty || patient == null) {
                            setText(null);
                        } else {
                            setText(patient.getName());
                        }
                    }
                };
            }
        });

        // Format the selected patient in the combo box button
        patientComboBox.setButtonCell(new ListCell<Patient>() {
            @Override
            protected void updateItem(Patient patient, boolean empty) {
                super.updateItem(patient, empty);
                if (empty || patient == null) {
                    setText("Select Patient");
                } else {
                    setText(patient.getName());
                }
            }
        });

        // Add a listener to load messages when a patient is selected
        patientComboBox.setOnAction(e -> {
            Patient selectedPatient = patientComboBox.getValue();
            if (selectedPatient != null) {
                loadMessagesWithPatient(selectedPatient);
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
        Patient selectedPatient = patientComboBox.getValue();
        
        if (selectedPatient == null) {
            statusLabel.setText("Please select a patient first");
            return;
        }

        String messageText = messageField.getText().trim();
        if (messageText.isEmpty()) {
            return;
        }

        // Save the message to the database
        int messageId = chatDbHandler.saveMessage(currentDoctor.getUserID(), selectedPatient.getUserID(), messageText);

        if (messageId > 0) {
            // Clear the message field
            messageField.clear();

            // Refresh messages
            loadMessagesWithPatient(selectedPatient);

            // Scroll to bottom
            Platform.runLater(() -> {
                messagesListView.scrollTo(messagesListView.getItems().size() - 1);
            });
        } else {
            statusLabel.setText("Failed to send message. Please try again.");
        }
    }

    /**
     * Load messages between doctor and selected patient
     */
    private void loadMessagesWithPatient(Patient patient) {
        if (patient == null) return;
        
        // Mark messages from this patient as read
        chatDbHandler.markMessagesAsRead(currentDoctor.getUserID(), patient.getUserID());

        // Load messages
        List<ChatMessage> messages = chatDbHandler.getMessagesBetweenUsers(
                currentDoctor.getUserID(), patient.getUserID());

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
        statusLabel.setText("Chatting with " + patient.getName());
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
                    Patient selectedPatient = patientComboBox.getValue();
                    if (selectedPatient != null) {
                        loadMessagesWithPatient(selectedPatient);
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
        Patient selectedPatient = patientComboBox.getValue();
        if (selectedPatient != null) {
            loadMessagesWithPatient(selectedPatient);
            statusLabel.setText("Messages refreshed");
        } else {
            statusLabel.setText("Please select a patient first");
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

}
