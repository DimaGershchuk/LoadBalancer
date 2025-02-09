/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package com.mycompany.javafxapplication1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;



public class TerminalEmulationController{

    @FXML
    private Button executeCommand;
    
    @FXML
    private TextArea resultTextArea;
    
    @FXML
    private TextField commandTextField;
    
    private static final Set<String> ALLOWED_COMMANDS = new HashSet<>(Arrays.asList(
            "mv", "cp", "ls", "mkdir", "ps", "whoami", "tree", "nano"
    ));
    
    @FXML
    private void executeCommandHandler(ActionEvent event){
        String command = commandTextField.getText().trim();
        if (command.isEmpty()) {
            resultTextArea.setText("Please enter a command.");
            return;
        }
        String[] parts = command.split("\\s+");
        String cmd = parts[0].toLowerCase();
        
        if (!ALLOWED_COMMANDS.contains(cmd)) {
            resultTextArea.setText("Command not allowed: " + cmd);
            return;
        }
        
        executeCommand(command);
    }
    
    private void executeCommand(String command) {
        String[] parts = command.split("\\s+");
        String cmd = parts[0].toLowerCase();
        try {
            Process process;
            if ("nano".equals(cmd)) {
                process = new ProcessBuilder("terminator", "-e", "nano").start();
                resultTextArea.setText("Launched nano in a new terminal.");
                return;
            } else {
                ProcessBuilder pb = new ProcessBuilder(parts);
                pb.redirectErrorStream(true);
                process = pb.start();
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            output.append("Exit code: ").append(exitCode);
            resultTextArea.setText(output.toString());
            
        } catch (IOException | InterruptedException e) {
            resultTextArea.setText("Error executing command: " + e.getMessage());
            e.printStackTrace();
        }
    }
   
}
