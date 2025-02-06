/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import com.jcraft.jsch.*;

/**
 *
 * @author ntu-user
 */
public class Container {
    
   private final String id;
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final SSHClient sshClient;
    private int currentLoad;

    public Container(String id, String host, int port, String username, String password) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.sshClient = new SSHClient(host, port, username, password);
        this.currentLoad = 0;

        try {
            sshClient.connect();
        } catch (JSchException e) {
            throw new RuntimeException("Failed to connect to container " + id, e);
        }
    }
    
    public void deleteChunk(String chunk) {
        String remoteFilePath = "/files/" + chunk;
            try {
                sshClient.executeCommand("rm -f " + remoteFilePath);
                System.out.println("Deleted chunk " + chunk + " from " + id);
                
            } catch (Exception e) {
                System.err.println("Failed to delete chunk " + chunk + " from " + id);
                e.printStackTrace();
            }
        }

    public synchronized void sendFileToContainer(String chunk) {
        currentLoad++;
        try {
            System.out.println("Uploading " + chunk + " to " + id);
            sshClient.uploadFile("chunks/" + chunk, "/files/" + chunk);
        } catch (Exception e) {
            System.err.println("Failed to upload chunk " + chunk + " to " + id);
            e.printStackTrace();
        } finally {
            currentLoad--;
        }
    }
    
    public void downloadChunk(String chunk, String destinationPath){
        
        try{
            sshClient.downloadFile("/files/" + chunk, destinationPath);  
            
        } catch(Exception e ){
            System.err.println("Failed to download chunk " + chunk + " from " + id);
            e.printStackTrace();
        }
    }
    
    public boolean isHealthy() {
        
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(username, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setTimeout(5000);
            session.connect();

            session.disconnect();
            //System.out.println(id + " is healthy.");
            return true;
            
        } catch (Exception e) {
            System.err.println(id + " is not responding.");
            return false;
        }
    }
    
    public void openRemoteTerminal() {
        RemoteTerminal terminal = new RemoteTerminal(host, port, username, password);
        terminal.startSession();
    }
    
    public String getId(){
        return id;
    }

    public synchronized int getCurrentLoad() {
        return currentLoad;
    }

    public void disconnect() {
        sshClient.disconnect();
    }
}
    


