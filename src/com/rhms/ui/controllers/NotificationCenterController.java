package com.rhms.ui.controllers;

import com.rhms.notifications.Notification;
import com.rhms.notifications.NotificationService;
import com.rhms.userManagement.User;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;

public class NotificationCenterController implements Initializable {
    @FXML private TableView<Notification> notificationsTable;
    @FXML private TableColumn<Notification, String> titleColumn;
    @FXML private TableColumn<Notification, String> messageColumn;
    @FXML private TableColumn<Notification, String> typeColumn;
    @FXML private TableColumn<Notification, String> dateColumn;
    @FXML private TableColumn<Notification, Void> actionColumn;

    private NotificationService notificationService;
    private User currentUser;
    private ObservableList<Notification> notificationList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public void setData(User user, NotificationService notificationService) {
        this.currentUser = user;
        this.notificationService = notificationService;
        loadNotifications();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        messageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        dateColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                dateFormat.format(cellData.getValue().getCreatedAt())));
        actionColumn.setCellFactory(col -> new TableCell<Notification, Void>() {
            private final Button markReadBtn = new Button("Mark as Read");
            {
                markReadBtn.setOnAction(e -> {
                    Notification notif = getTableView().getItems().get(getIndex());
                    if (!notif.isRead()) {
                        notificationService.markNotificationAsRead(notif.getNotificationId());
                        notif.markAsRead();
                        getTableView().refresh();
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Notification notif = getTableView().getItems().get(getIndex());
                    markReadBtn.setDisable(notif.isRead());
                    setGraphic(markReadBtn);
                }
            }
        });
        notificationsTable.setRowFactory(tv -> new TableRow<Notification>() {
            @Override
            protected void updateItem(Notification notif, boolean empty) {
                super.updateItem(notif, empty);
                if (notif == null || empty) {
                    setStyle("");
                } else if (!notif.isRead()) {
                    setStyle("-fx-background-color: #fffbe6;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    private void loadNotifications() {
        if (currentUser == null || notificationService == null) return;
        List<Notification> notifications = notificationService.getNotificationsForUser(currentUser.getUserID());
        notificationList = FXCollections.observableArrayList(notifications);
        notificationsTable.setItems(notificationList);
    }

    @FXML
    public void handleMarkAllAsRead() {
        if (notificationList == null) return;
        for (Notification notif : notificationList) {
            if (!notif.isRead()) {
                notificationService.markNotificationAsRead(notif.getNotificationId());
                notif.markAsRead();
            }
        }
        notificationsTable.refresh();
    }
}
