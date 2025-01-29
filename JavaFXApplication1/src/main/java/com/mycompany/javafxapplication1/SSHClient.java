/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import java.io.BufferedReader;
import com.jcraft.jsch.*;
import java.io.IOException;
import java.io.InputStreamReader;
/**
 *
 * @author ntu-user
 */
public class SSHClient {
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private Session session;

    public SSHClient(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public void connect() throws JSchException {
        JSch jsch = new JSch();
        session = jsch.getSession(username, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        System.out.println("Connected to " + host);
    }

    public void uploadFile(String localPath, String remotePath) throws JSchException, IOException, SftpException {
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();
        channelSftp.put(localPath, remotePath);
        channelSftp.disconnect();
    }

    public void executeCommand(String command) throws JSchException, IOException {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        channel.setErrStream(System.err);
        channel.connect();

        BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        channel.disconnect();
    }

    public void disconnect() {
        if (session != null && session.isConnected()) {
            session.disconnect();
            System.out.println("Disconnected from " + host);
        }
    }
}
