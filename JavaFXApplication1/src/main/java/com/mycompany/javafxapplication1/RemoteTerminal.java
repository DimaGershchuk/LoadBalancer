/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import com.jcraft.jsch.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;
/**
 *
 * @author ntu-user
 */
public class RemoteTerminal {
    
    private final String host;
    private final int port;
    private final String username;
    private final String password;

    public RemoteTerminal(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public void startSession() {
        
        JSch jsch = new JSch();
            try {
                Session session = jsch.getSession(username, host, port);
                session.setPassword(password);
                session.setConfig("StrictHostKeyChecking", "no");
                session.connect();

                Channel channel = session.openChannel("shell");
                channel.setInputStream(System.in);
                channel.setOutputStream(System.out);
                channel.connect();

                System.out.println("Connected to " + host + ". Type 'exit' inside the terminal to close the session.");

                while (!channel.isClosed()) {
                    Thread.sleep(100);
                }

                channel.disconnect();
                session.disconnect();
                System.out.println("Disconnected from " + host);
            } catch (Exception e) {
                System.err.println("Failed to connect to remote terminal: " + e.getMessage());
                e.printStackTrace();
                // Тут можна отримати первинну причину:
                Throwable cause = e.getCause();
                if (cause != null) {
                    cause.printStackTrace();
                }
            }
        }
    }
