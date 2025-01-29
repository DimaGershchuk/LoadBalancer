/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Timer;
import java.util.TimerTask;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
/**
 *
 * @author ntu-user
 */
public class SessionManager {
     private static final long TIMEOUT = 30 * 1000; // 30 секунд
    private Timer inactivityTimer;
    private Stage primaryStage;

    public SessionManager(Stage stage) {
        this.primaryStage = stage;
    }

    public void startSessionTracking(Scene scene) {
        // Ініціалізація таймера
        resetInactivityTimer();

        // Відстеження подій миші та клавіатури
        scene.addEventFilter(javafx.scene.input.MouseEvent.ANY, event -> resetInactivityTimer());
        scene.addEventFilter(javafx.scene.input.KeyEvent.ANY, event -> resetInactivityTimer());
    }

    private void resetInactivityTimer() {
        if (inactivityTimer != null) {
            inactivityTimer.cancel();
        }

        inactivityTimer = new Timer(true); // Фоновий потік
        inactivityTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> logoutUser());
            }
        }, TIMEOUT);
    }

    private void logoutUser() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("primary.fxml"));
            Parent root = loader.load();

            primaryStage.setScene(new Scene(root, 640, 480));
            primaryStage.setTitle("Login");
            primaryStage.show();

            showAlert("Session Timeout", "Your session has expired. Please log in again.", Alert.AlertType.WARNING);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
