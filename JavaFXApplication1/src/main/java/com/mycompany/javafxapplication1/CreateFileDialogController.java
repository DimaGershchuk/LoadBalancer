package com.mycompany.javafxapplication1;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author ntu-user
 */
public class CreateFileDialogController  {

    @FXML
    private TextField fileNameField;

    @FXML
    private TextArea fileContentArea;

    private Stage dialogStage;
    private boolean isConfirmed = false;

    private String fileName;
    private String fileContent;

    @FXML
    private void handleCreateFile() {
        fileName = fileNameField.getText().trim();
        fileContent = fileContentArea.getText().trim();

        if (!fileName.isEmpty() && !fileContent.isEmpty()) {
            isConfirmed = true;
            dialogStage.close();
        } else {
            System.out.println("File name or content cannot be empty.");
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isConfirmed() {
        return isConfirmed;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileContent() {
        return fileContent;
    }
 
    
}
