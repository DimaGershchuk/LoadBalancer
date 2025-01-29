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
    private final SSHClient sshClient;
    private int currentLoad;

    public Container(String id, String host, int port, String username, String password) {
        this.id = id;
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
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            currentLoad--;
        }
    }

    public synchronized int getCurrentLoad() {
        return currentLoad;
    }

    public void disconnect() {
        sshClient.disconnect();
    }
    
}
    


