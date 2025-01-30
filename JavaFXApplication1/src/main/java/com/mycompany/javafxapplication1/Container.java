/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import java.util.concurrent.TimeUnit;
import com.jcraft.jsch.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

    public synchronized void processRequest(Request request) {
        currentLoad++;
        System.out.println("Processing request " + request.getId() + " on container " + id);
        try {
            sshClient.executeCommand("echo Processing request " + request.getId());

            for (String chunk : request.getChunks()) {
                sendFileToContainer(chunk);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            currentLoad--;
        }
    }
    
    private void sendFileToContainer(String chunk) {
        String localFilePath = "chunks/" + chunk;
        String remoteFilePath = "/files/" + chunk; // Папка в контейнері

        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(username, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();
            sftp.put(localFilePath, remoteFilePath);

            System.out.println("Uploaded " + chunk + " to " + id);
            sftp.disconnect();
            session.disconnect();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized int getCurrentLoad() {
        return currentLoad;
    }

    public void disconnect() {
        sshClient.disconnect();
    }
}
    


