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
import javafx.application.Platform;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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
    
    @FXML
    private Button selectContainer1;
    
    @FXML
    private Button selectContainer2;
    
    
    @FXML
    private Button selectContainer3;
    
    @FXML
    private Button selectContainer4;
    
    @FXML
    private Button downloadFile;
    
    @FXML 
    private Button openTerminal;
    
    
    private File selectedFile;
   
    private MQTTClient mqttClient;
    
    private int userId;
    private DB db;
    
    public Container container1;
    public Container container2;
    public Container container3;
    public Container container4;
    
    @FXML
    public void initialize() {
        container1 = new Container("container1", "soft40051-files-container1", 22, "ntu-user", "ntu-user");
        container2 = new Container("container2", "soft40051-files-container2", 22, "ntu-user", "ntu-user");
        container3 = new Container("container3", "soft40051-files-container3", 22, "ntu-user", "ntu-user");
        container4 = new Container("container4", "soft40051-files-container4", 22, "ntu-user", "ntu-user");
    }
    

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
            
            Request request = new Request(userId, fileId, Request.OperationType.UPLOAD, fileLength, 1, new ArrayList<>(), filePath);
            System.out.println("Sending MQTT request for file: " + fileId);
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
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("CreateFileDialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Create New File");
            dialogStage.setScene(new Scene(root));

            CreateFileDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);

            dialogStage.showAndWait();

            if (controller.isConfirmed()) {
                String fileName = controller.getFileName();
                String content = controller.getFileContent();
                File newFile = new File(System.getProperty("user.home") + File.separator + fileName);

                if (newFile.createNewFile()) {
                    FileWriter writer = new FileWriter(newFile);
                    writer.write(content);
                    writer.close();

                    long fileLength = newFile.length();
                    int crc32 = calculateCRC32(content);

                    String filePath = newFile.getAbsolutePath();
                    String fileId = db.addFileToUser(fileName, fileLength, crc32, filePath, this.userId);

            
                    Request request = new Request(userId, fileId, Request.OperationType.UPLOAD, fileLength, 1, new ArrayList<>(), filePath);
                    mqttClient.sendRequest(request);

                    FileModel newFileModel = new FileModel(fileId, fileName, fileLength, crc32, filePath);
                    fileTableView.getItems().add(newFileModel);

                    showAlert("Success", "File created successfully!", Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Warning", "File already exists!", Alert.AlertType.WARNING);
                }
            }

        } catch (IOException | SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to create the file.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void updateFileHandler(ActionEvent event) throws IOException, ClassNotFoundException, MqttException {
        
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
            
            Request request = new Request(userId, selectedFile.getFileId(), Request.OperationType.UPDATE, newFileLength, 1, new ArrayList<>(), filePath.toString());
            mqttClient.sendRequest(request);
            
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


                List<String> chunks = db.getChunksForFile(selectedFile.getFileId());
                Request deleteRequest = new Request(userId, selectedFile.getFileId(), Request.OperationType.DELETE, 0, 3, chunks, selectedFile.getFilePath());
                mqttClient.sendRequest(deleteRequest);
                
                mqttClient.subscribeToDeletionConfirmation(selectedFile.getFileId(), () -> {
                    try {
                        
                        System.out.println("✅Deleting file from DB after chunk deletion: " + selectedFile.getFileId());
                        db.deleteFileForUser(selectedFile.getFileId(), this.userId);
                        
                        Platform.runLater(() -> fileTableView.getItems().remove(selectedFile));
                        
                        showAlert("Success", "File and its chunks deleted successfully!", Alert.AlertType.INFORMATION);
                        
                    } catch (ClassNotFoundException ex) {
                        Logger.getLogger(FileselectionController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });

                showAlert("Success", "File deleted successfully!", Alert.AlertType.INFORMATION);
                
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
    
    @FXML
    public void downloadFileHandler(ActionEvent event){
        
        FileModel selectedFile = (FileModel) fileTableView.getSelectionModel().getSelectedItem();

        if (selectedFile != null) {
            try {

                List<String> chunks = db.getChunksForFile(selectedFile.getFileId());

                Request request = new Request(userId, selectedFile.getFileId(), Request.OperationType.DOWNLOAD, selectedFile.getFileLength(), 2, null, selectedFile.getFilePath());

                mqttClient.sendRequest(request);

                showAlert("Info", "Download request sent!", Alert.AlertType.INFORMATION);

            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to send download request!", Alert.AlertType.ERROR);
                }
            } else {
                showAlert("Warning", "Please select a file to download.", Alert.AlertType.WARNING);
            }
    }
    
    
    
    public void openContainerTerminal(Container container) {
        new Thread(container::openRemoteTerminal).start(); 
    }
    
    @FXML
    public void selectContainer1Handler(ActionEvent event){
        openContainerTerminal(container1);
    }
    
    @FXML
    public void selectContainer2Handler(ActionEvent event){
        openContainerTerminal(container2);
    }
    
    @FXML
    public void selectContainer3Handler(ActionEvent event){
        openContainerTerminal(container3);
    }
    
    @FXML
    public void selectContainer4Handler(ActionEvent event){
        openContainerTerminal(container4);
    }
    
    @FXML
    private void openTerminalHandler(ActionEvent event) throws IOException{
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) openTerminal.getScene().getWindow();
        
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("TerminalEmulation.fxml"));
        Parent root = loader.load();
        
        TerminalEmulationController controller = loader.getController();

        Scene scene = new Scene(root, 640, 480);
        secondaryStage.setScene(scene);
        secondaryStage.setTitle("Select file");
        secondaryStage.show();
        primaryStage.close();
    }
    
    
    
    public void initialiseUsers(String[] credentials) throws ClassNotFoundException, SQLException{
     
        this.userId = Integer.parseInt(credentials[1]);
        
        welcomeText.setText(credentials[0]);
        
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
