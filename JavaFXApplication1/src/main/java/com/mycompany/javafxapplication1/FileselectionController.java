/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package com.mycompany.javafxapplication1;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.eclipse.paho.client.mqttv3.MqttException;


/**
 * FXML Controller class
 *
 * @author ntu-user
 */
public class FileselectionController {
    
    @FXML
    private TextArea fileTextArea;
    
    @FXML
    private Text fileText;
    
    @FXML 
    private Button updateFile;
    
    @FXML
    private Button deleteFile;
    
    @FXML
    private Button newButtonTest;
    
    @FXML
    private TableView fileTableView;
    
    @FXML
    private TableView userSelectionTable;
    
    @FXML
    private Button createFile;
    
    @FXML
    private Button backToLogin;
    
    @FXML
    private TextField welcomeText;
   
    @FXML
    private Button selectBtn;
    
    @FXML
    private Button shareFile;
    
    @FXML
    private Button givePermWrite;
    
    @FXML
    private Button givePermRead;
    
    
    private File selectedFile;
   
    private MQTTClient mqttClient;
    private LoadBalancer loadBalancer;
    
    private int userId;
    private DB db;

    public FileselectionController() throws MqttException {  
        
    this.mqttClient = new MQTTClient();
    
    }
            
    
    
    
    @FXML
    private void switchToLogin(ActionEvent event) throws IOException{
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) backToLogin.getScene().getWindow();
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
    private void selectBtnHandler(ActionEvent event) throws IOException, ClassNotFoundException, SQLException, FileNotFoundException, Exception {
        
    Stage primaryStage = (Stage) selectBtn.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        selectedFile = fileChooser.showOpenDialog(primaryStage);
        
         if (selectedFile != null) {
      
        Scanner scanner = new Scanner(selectedFile);
        StringBuilder fileContent = new StringBuilder();
            while (scanner.hasNextLine()) {
                fileContent.append(scanner.nextLine()).append("\n");
            }
                scanner.close();

            String filename = selectedFile.getName();
            long fileLength = selectedFile.length();
            String filePath = selectedFile.getAbsolutePath();
            int crc32 = calculateCRC32(fileContent.toString());
            
            DB db = new DB();
            String fileId = db.addFileToUser(filename, fileLength, crc32, filePath,  this.userId);
            
            
            List<String> chunks = FileChunking.chunkFile(selectedFile, "chunks/", 4, fileId);
            
            Request request = new Request(userId, fileId, Request.OperationType.UPLOAD, fileLength, 1, chunks);
            mqttClient.sendRequest(request);
            
            fileTableView.getItems().add(new FileModel(fileId, filename, fileLength, crc32, filePath));
            showAlert("Success", "File added successfully!", Alert.AlertType.INFORMATION);
            
        } else {
            showAlert("Warning", "No file selected.", Alert.AlertType.WARNING);
        }
    }
   
        

    private int calculateCRC32(String content) {
    try {
        CRC32 crc32 = new CRC32();
        crc32.update(content.getBytes(StandardCharsets.UTF_8));
        return (int) crc32.getValue();
    } catch (Exception e) {
        e.printStackTrace();
        return 0;
    }}
    
    @FXML
    private void createFileHandler(ActionEvent event) throws FileNotFoundException, Exception {
        
    TextInputDialog dialog = new TextInputDialog("NewFile.txt");
    dialog.setTitle("Create New File");
    dialog.setHeaderText("Enter the name for the new file:");
    dialog.setContentText("File name:");
    User selectedUser = (User) userSelectionTable.getSelectionModel().getSelectedItem();

    Optional<String> result = dialog.showAndWait();
    if (result.isPresent() && !result.get().isBlank()) {
        String fileName = result.get().trim();
        File newFile = new File(System.getProperty("user.home") + File.separator + fileName);

        try {
            // Створення нового файлу
            if (newFile.createNewFile()) {
                FileWriter writer = new FileWriter(newFile);
                writer.write("This is the initial content of the file.\n"); 
                writer.close();

                long fileLength = newFile.length();
                String content = Files.readString(newFile.toPath());
                int crc32 = calculateCRC32(content);

                String filePath = newFile.getAbsolutePath();
                String fileId = db.addFileToUser(fileName, fileLength, crc32, filePath, this.userId);
                
                String outputDir = "chunks/";  
                int numberOfChunks = 4;  
                FileChunking.chunkFile(selectedFile, outputDir, numberOfChunks, fileId);

                FileModel newFileModel = new FileModel(fileId, fileName, fileLength, crc32, filePath);
                fileTableView.getItems().add(newFileModel);

                showAlert("Success", "File created s34uccessfully!", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Warning", "File already exists!", Alert.AlertType.WARNING);
            }
        } catch (IOException | SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to create the file.", Alert.AlertType.ERROR);
        }
    } else {
        showAlert("Warning", "File name cannot be empty.", Alert.AlertType.WARNING);
    }
    }
    
    @FXML
    private void onFileSelect(){
        
        FileModel selectedFile = (FileModel) fileTableView.getSelectionModel().getSelectedItem();
        
        if (selectedFile != null) {
        try {
            Path filePath = Paths.get(selectedFile.getFilePath());
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            fileTextArea.setText(content);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load file content.", Alert.AlertType.ERROR);
        }
    } else {
        fileTextArea.clear();
        showAlert("Warning", "Please select a file from the table.", Alert.AlertType.WARNING);
    }
    }

    @FXML
    private void updateFileHandler(ActionEvent event) throws IOException, ClassNotFoundException {
        
    FileModel selectedFile = (FileModel) fileTableView.getSelectionModel().getSelectedItem();


    if (selectedFile != null) {
        try {
            
            boolean canWrite = db.checkPermissionForUser(selectedFile.getFileId(), this.userId);
            if (!canWrite) {
                showAlert("Access Denied", "You don't have permission to modify this file.", Alert.AlertType.ERROR);
                return;
            }
            
            String updatedContent = fileTextArea.getText();

          
            Path filePath = Paths.get(selectedFile.getFilePath());
            Files.writeString(filePath, updatedContent, StandardCharsets.UTF_8);

            long newFileLength = Files.size(filePath);
            int newCrc32 = calculateCRC32(updatedContent); 
            db.updateFileForUser(
                selectedFile.getFileId(),
                selectedFile.getFilename(),
                newFileLength,
                String.valueOf(newCrc32),
                selectedFile.getFilePath()
            );

           
            selectedFile.setFileLength(newFileLength);
            selectedFile.setCrc32(newCrc32);
            fileTableView.refresh();

            showAlert("Success", "File updated successfully!", Alert.AlertType.INFORMATION);
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to update the file.", Alert.AlertType.ERROR);
        }
    } else {
        showAlert("Warning", "Please select a file to update.", Alert.AlertType.WARNING);
    }
    }

    @FXML
    private void deleteFileHandler(ActionEvent event) throws IOException, ClassNotFoundException {
         FileModel selectedFile = (FileModel) fileTableView.getSelectionModel().getSelectedItem();
         User selectedUser = (User) userSelectionTable.getSelectionModel().getSelectedItem();

        if (selectedFile != null) {
        
            boolean canWrite = db.checkPermissionForUser(selectedFile.getFileId(), this.userId);
                if (!canWrite) {
                showAlert("Access Denied", "You don't have permission to modify this file.", Alert.AlertType.ERROR);
                return;
            }
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Delete File");
        confirmationAlert.setHeaderText("Are you sure you want to delete this file?");
        confirmationAlert.setContentText("File: " + selectedFile.getFilename());

        Optional<ButtonType> result = confirmationAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                Path filePath = Paths.get(selectedFile.getFilePath());
                Files.deleteIfExists(filePath);

                List<String> chunks = db.getChunksForFile(selectedFile.getFileId());
                
                Request deleteRequest = new Request(userId, selectedFile.getFileId(), Request.OperationType.DELETE, 0, 1, chunks);
                mqttClient.sendRequest(deleteRequest);
                
                db.deleteFileForUser(selectedFile.getFileId(), this.userId);

                fileTableView.getItems().remove(selectedFile);

                showAlert("Success", "File deleted successfully!", Alert.AlertType.INFORMATION);
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Error", "Failed to delete the file from disk.", Alert.AlertType.ERROR);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to delete the file from database.", Alert.AlertType.ERROR);
            }
        }
    } else {
        showAlert("Warning", "Please select a file to delete.", Alert.AlertType.WARNING);
    }
    }
    
    @FXML
    public void shareReadFileWithUser(ActionEvent event) throws ClassNotFoundException, SQLException{
        
        FileModel selectedFile = (FileModel) fileTableView.getSelectionModel().getSelectedItem();
        User selectedUser = (User) userSelectionTable.getSelectionModel().getSelectedItem();
        
        if (selectedFile != null && selectedUser != null) {
            db.addPermissionToFile(selectedFile.getFileId(), selectedUser.getId(), true, false);
            showAlert("Success", "File shared with read permission successfully!", Alert.AlertType.INFORMATION);
        } else {
            showAlert("Warning", "Please select both a file and a user.", Alert.AlertType.WARNING);
        }
    }
    
    
    @FXML
    public void shareWriteFileWithUser(ActionEvent event) throws ClassNotFoundException, SQLException{
        FileModel selectedFile = (FileModel) fileTableView.getSelectionModel().getSelectedItem();
        User selectedUser = (User) userSelectionTable.getSelectionModel().getSelectedItem();

        if (selectedFile != null && selectedUser != null) {
            db.addPermissionToFile(selectedFile.getFileId(), selectedUser.getId(), true, true);
            
            showAlert("Success", "File shared with read permission successfully!", Alert.AlertType.INFORMATION);
            
        } else {
            showAlert("Warning", "Please select both a file and a user.", Alert.AlertType.WARNING);
        }
    }
    
    
    
    public void initialiseUsers(String[] credentials) throws ClassNotFoundException, SQLException{
     
        this.userId = Integer.parseInt(credentials[1]);
        
        welcomeText.setText(credentials[0]);
        System.out.println("Name" + credentials[0]);
        
        DB myObj = new DB();
        ObservableList<User> data;
        

        try {
           
            data = myObj.getUserData();
            
            TableColumn<User, String> userColumn = new TableColumn<>("User");
            userColumn.setCellValueFactory(new PropertyValueFactory<>("user"));
            
            TableColumn<User, String> passColumn = new TableColumn<>("Pass");
            passColumn.setCellValueFactory(new PropertyValueFactory<>("pass"));
            
            TableColumn<User, String> roleColumn = new TableColumn<>("Role");
            roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
            
            userSelectionTable.getColumns().addAll(userColumn, passColumn, roleColumn);
            userSelectionTable.setItems(data);
            
    } catch (ClassNotFoundException ex) {
        Logger.getLogger(SecondaryController.class.getName()).log(Level.SEVERE, null, ex);
    }
}
        
    public void initialiseFiles(String[] credentials) throws ClassNotFoundException, SQLException{
        
        int userId = Integer.parseInt(credentials[1]);
        System.out.println("User ID: " + credentials [0] + " "+ credentials[1]);
        
        db = new DB();
        
        ObservableList<FileModel> userFiles = db.getUserFiles(userId);
            
            TableColumn<FileModel, String> fileIdColumn = new TableColumn<>("FileId");
            fileIdColumn.setCellValueFactory(new PropertyValueFactory<>("fileId"));
            
            TableColumn<FileModel, String> fileNameColumn = new TableColumn<>("Filename");
            fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("filename"));
            
            TableColumn<FileModel, Long> fileLengthColumn = new TableColumn<>("FileLenght");
            fileLengthColumn.setCellValueFactory(new PropertyValueFactory<>("fileLength"));
            
            TableColumn<FileModel, Long> fileCRC32Column = new TableColumn<>("CRC32");
            fileCRC32Column.setCellValueFactory(new PropertyValueFactory<>("crc32"));
            
            TableColumn<FileModel, String> filePathColumn = new TableColumn<>("FilePath");
            filePathColumn.setCellValueFactory(new PropertyValueFactory<>("filePath"));
            
            fileTableView.getColumns().addAll(fileIdColumn, fileNameColumn, fileLengthColumn, fileCRC32Column, filePathColumn);
            fileTableView.setItems(userFiles);
    }
    
    private void showAlert(String title, String content, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }


    
}
