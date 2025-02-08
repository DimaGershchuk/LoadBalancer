/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;
import org.eclipse.paho.client.mqttv3.*;
import com.google.gson.Gson;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import java.io.IOException;
/**
 *
 * @author ntu-user
 */
public class MQTTClient {
    
     
    private static final String BROKER_URL = "tcp://mqtt-broker:1883"; 
    private static final String CLIENT_ID = MqttClient.generateClientId();
    private static final String TOPIC = "load-balancer/file-operation";

    private MqttClient client;
    private Gson gson;

    public MQTTClient() throws MqttException {
        gson = new Gson();
        client = new MqttClient(BROKER_URL, CLIENT_ID);
        connect();
    }

    private void connect() throws MqttException {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        client.connect(options);
    }

    // Send request to LoadBalancer
    public void sendRequest(Request request) throws MqttException {
        try {
            if (!client.isConnected()) {
                System.out.println("MQTT client disconnected, reconnecting...");
                reconnect();
            }
            Gson gson = new Gson();
            String jsonRequest = gson.toJson(request);
            MqttMessage message = new MqttMessage(jsonRequest.getBytes());
            client.publish("load-balancer/file-operation", message);
            System.out.println("✅Sent MQTT request: " + jsonRequest);
        } catch (MqttException e) {
            e.printStackTrace();
            System.err.println("❌Failed to send MQTT request: " + e.getMessage());
        }
    }

    public void subscribe() throws MqttException {
        client.subscribe(TOPIC, (topic, message) -> {
            String payload = new String(message.getPayload());
            handleResponse(payload);
        });
    }

    private void handleResponse(String payload) {
        try {
            Response response = gson.fromJson(payload, Response.class);
            System.out.println("Processing response: " + response);
        } catch (Exception e) {
            System.err.println("Failed to parse MQTT response: " + e.getMessage());
        }
    }
    
    public void subscribeToDeletionConfirmation(String fileId, Runnable onSuccess) {
        try {
            client.subscribe("load-balancer/file-operation/confirmation", (topic, message) -> {
                String payload = new String(message.getPayload());
                Gson gson = new Gson();
                Response response = gson.fromJson(payload, Response.class);

                if (response.getFileId().equals(fileId) && response.getStatus().equals("DELETED")) {
                    onSuccess.run(); // Виконується видалення з бази після підтвердження
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() throws MqttException {
        if (client.isConnected()) {
            client.disconnect();
            System.out.println("Disconnected from MQTT broker.");
        }
    }
    
    public void reconnect() {
    try {
        client.reconnect();
        System.out.println("✅Reconnected to MQTT broker.");
    } catch (MqttException e) {
        System.err.println("❌Failed to reconnect to MQTT broker: " + e.getMessage());
    }
}
}
