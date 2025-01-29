/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package com.mycompany.javafxapplication1;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author ntu-user
 */
public class RegisterController {

    /**
     * Initializes the controller class.
     */
    @FXML
    private Button registerBtn;

    @FXML
    private Button backLoginBtn;

    @FXML
    private PasswordField passPasswordField;

    @FXML
    private PasswordField rePassPasswordField;

    @FXML
    private TextField userTextField;
    
    @FXML
    private RadioButton registerRoleAdmin;
    
    @FXML
    private RadioButton registerUserRole;
    
    @FXML
    private Button selectBtn;
    
  

    private void dialogue(String headerMsg, String contentMsg) {
        Stage secondaryStage = new Stage();
        Group root = new Group();
        Scene scene = new Scene(root, 300, 300, Color.DARKGRAY);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation Dialog");
        alert.setHeaderText(headerMsg);
        alert.setContentText(contentMsg);
        Optional<ButtonType> result = alert.showAndWait();
    }

    @FXML
    private void registerBtnHandler(ActionEvent event) {
        Stage secondaryStage = new Stage();
    Stage primaryStage = (Stage) registerBtn.getScene().getWindow();
    try {
        FXMLLoader loader = new FXMLLoader();
        DB myObj = new DB();
        
        if (passPasswordField.getText().equals(rePassPasswordField.getText())) {
            String userRole = "";

            if (registerRoleAdmin.isSelected()) {
                userRole = "admin";
            } else if (registerUserRole.isSelected()) {
                userRole = "user";
            } else {
                dialogue("Role Selection Error", "Please select a role.");
                return;
            }

            myObj.addDataToDB(userTextField.getText(), passPasswordField.getText(), userRole);
            dialogue("Adding information to the database", "Successful!");

            String[] credentials = {userTextField.getText(), passPasswordField.getText()};

            // Завантажуємо FXML і отримуємо контролера після завантаження
            if (userRole.equals("admin")) {
                loader.setLocation(getClass().getResource("secondary.fxml"));
                Parent root = loader.load();
                SecondaryController controller = loader.getController();
                controller.initialise(credentials);
                Scene scene = new Scene(root, 640, 480);
                secondaryStage.setScene(scene);
                secondaryStage.setTitle("Show users");
                
            } else if (userRole.equals("user")) {
                loader.setLocation(getClass().getResource("fileselection.fxml"));
                Parent root = loader.load();
                FileselectionController controller = loader.getController();
                controller.initialiseUsers(credentials);
                controller.initialiseFiles(credentials);
                Scene scene = new Scene(root, 640, 480);
                secondaryStage.setScene(scene);
                secondaryStage.setTitle("File Selection");
            }

            secondaryStage.show();
            primaryStage.close();
        } else {
            loader.setLocation(getClass().getResource("register.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 640, 480);
            secondaryStage.setScene(scene);
            secondaryStage.setTitle("Register a new User");
            secondaryStage.show();
            primaryStage.close();
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
    }

    @FXML
    private void backLoginBtnHandler(ActionEvent event) {
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) backLoginBtn.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("primary.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 640, 480);
            secondaryStage.setScene(scene);
            secondaryStage.setTitle("Login");
            secondaryStage.show();
            primaryStage.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
