/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package com.mycompany.javafxapplication1;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * FXML Controller class
 *
 * @author ntu-user
 */
public class UpdateUserFormController {
    
    @FXML
    private TextField userNameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField roleField;
    
    
    private User selectedUser;
    
    public void setUser(User user){
        this.selectedUser = user;
        
        userNameField.setText(user.getUser());
        passwordField.setText(user.getPass());
        roleField.setText(user.getRole());
        
    }
    
    @FXML
    private void handleUpdateUser(){
        String username = userNameField.getText();
        String password = passwordField.getText();
        String role = roleField.getText();
        
         DB myObj = new DB();
        try {
            boolean success = myObj.updateUser(selectedUser.getUser(), username, password, role);
            if (success) {
                showAlert("Success", "User info has been updated");
            } else {
                showAlert("Error", "Info hasn`t been updated");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Error has been occured during info update");
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
}
