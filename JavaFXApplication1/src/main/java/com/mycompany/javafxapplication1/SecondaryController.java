package com.mycompany.javafxapplication1;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;



public class SecondaryController {
    
    @FXML
    private TextField userTextField;
    
    @FXML
    private TableView dataTableView;

    @FXML
    private Button secondaryButton;
    
    @FXML
    private Button refreshBtn;
    
    @FXML
    private Button createUser;
    
    @FXML
    private Button deleteUser;
    
    @FXML
    private Button updateUser;
    
    @FXML
    private Button makeAdmin;
    
   
    
        
    @FXML
    private void switchToPrimary(){
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) secondaryButton.getScene().getWindow();
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
    
    @FXML
    private void switchToSelectPage(){
        
         Stage secondaryStage = new Stage();
         Stage primaryStage = (Stage) secondaryButton.getScene().getWindow();
         
         
    try {
        
        DB myObj = new DB();
        
        String username = userTextField.getText();
        int userId = myObj.getUserId(username);
        
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("fileselection.fxml"));
        Parent root = loader.load();
        
        FileselectionController controller = loader.getController();
        controller.initialiseUsers(new String[]{username, String.valueOf(userId)});
        controller.initialiseFiles(new String[]{username, String.valueOf(userId)});

        Scene scene = new Scene(root, 640, 480);
        secondaryStage.setScene(scene);
        secondaryStage.setTitle("Select file");
        secondaryStage.show();
        primaryStage.close();
    } catch (Exception e) {
        e.printStackTrace();
    }
    }
    
    @FXML
    private void handleCreateUser(ActionEvent event){
        try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("CreateUserForm.fxml"));
        Parent root = loader.load();
        Stage stage = new Stage();
        stage.setTitle("User Create");
        stage.setScene(new Scene(root));
        stage.show();
    } catch (IOException e) {
        e.printStackTrace();
    }
    }
    
    @FXML
    private void handleUpdateUser(ActionEvent event){
          User selectedUser = (User) dataTableView.getSelectionModel().getSelectedItem();
    if (selectedUser == null) {
        return;
    }
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("UpdateUserForm.fxml"));
        Parent root = loader.load();
        UpdateUserFormController controller = loader.getController();
        controller.setUser(selectedUser);
        Stage stage = new Stage();
        stage.setTitle("Update User");
        stage.setScene(new Scene(root));
        stage.show();
    } catch (IOException e) {
        e.printStackTrace();
    }
    }
    
    @FXML
    private void handleDeleteUser(ActionEvent event){
        User selectedUser = (User) dataTableView.getSelectionModel().getSelectedItem();
    if (selectedUser == null) {
        showAlert("Error", "Please select user for deletion.");
        return;
    }
    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
    confirmAlert.setTitle("Confirmation");
    confirmAlert.setHeaderText(null);
    confirmAlert.setContentText("Do you want to delete user: " + selectedUser.getUser() + "?");
    confirmAlert.showAndWait().ifPresent(response -> {
        if (response == ButtonType.OK) {
            DB myObj = new DB();
            boolean success = myObj.deleteUser(selectedUser.getUser());
            if (success) {
                showAlert("Success", "User has been deleted.");
                refreshTable();
            } else {
                showAlert("Error","Can`t delete user.");
            }
        }
    });
    }
    
    @FXML
    private void handleMakeAdmin(ActionEvent event){
        
    }
    
    @FXML
    private void refreshTable(){
        try {
        DB myObj = new DB();
        ObservableList<User> updatedData = myObj.getUsersWithRoleUser();
        dataTableView.setItems(updatedData);
        dataTableView.refresh();
    } catch (Exception e) {
        e.printStackTrace();
    }
    }
    
    private void showAlert(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
}
    
   
    public void initialise(String[] credentials) throws SQLException {
            
        userTextField.setText(credentials[0]);

        
        DB myObj = new DB();
        
        ObservableList<User> data;

        try {
            
        String role = myObj.getUserRole(credentials[0]);
        
        if (role != null) {
           
            data = myObj.getUsersWithRoleUser();
            
            TableColumn<User, String> userColumn = new TableColumn<>("User");
            userColumn.setCellValueFactory(new PropertyValueFactory<>("user"));
            
            TableColumn<User, String> passColumn = new TableColumn<>("Pass");
            passColumn.setCellValueFactory(new PropertyValueFactory<>("pass"));
            
            TableColumn<User, String> roleColumn = new TableColumn<>("Role");
            roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
            
            dataTableView.getColumns().addAll(userColumn, passColumn, roleColumn);
            dataTableView.setItems(data);
        } else {
            Logger.getLogger(SecondaryController.class.getName())
                  .log(Level.WARNING, "Role not found.");
        }
        
    } catch (ClassNotFoundException ex) {
        Logger.getLogger(SecondaryController.class.getName()).log(Level.SEVERE, null, ex);
    }
        
        dataTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
        if (newValue != null) {
            User selectedUser = (User) newValue;
            userTextField.setText(selectedUser.getUser());
        }
        });
        
        
        
        
    
}}
