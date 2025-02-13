/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Timer;
import java.util.TimerTask;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
/**
 *
 * @author ntu-user
 */
public class SessionManager {
    
    private static final long TIMEOUT = 60 * 1000;
    private Timer inactivityTimer;
    private final Stage primaryStage;

    public SessionManager(Stage stage) {
        this.primaryStage = stage;

        if (primaryStage.getScene() != null) {
            startSessionTracking(primaryStage.getScene());
        }
        
        primaryStage.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                startSessionTracking(newScene);
            }
        });
        resetInactivityTimer();
    }

    public void startSessionTracking(Scene scene) {
        scene.addEventFilter(MouseEvent.ANY, event -> resetInactivityTimer());
        scene.addEventFilter(KeyEvent.ANY, event -> resetInactivityTimer());
        resetInactivityTimer();
    }

    private void resetInactivityTimer() {
        if (inactivityTimer != null) {
            inactivityTimer.cancel();
        }
        inactivityTimer = new Timer(true);
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
            Scene loginScene = new Scene(root, 640, 480);
            primaryStage.setScene(loginScene);
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
