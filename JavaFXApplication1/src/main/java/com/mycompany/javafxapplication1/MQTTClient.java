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
    
     
    private static final String BROKER_URL = "tcp://mqtt-broker:1883";  // Адреса брокера MQTT
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
        System.out.println("Connected to MQTT broker at " + BROKER_URL);
    }

    // Відправка запиту в Load Balancer
    public void sendRequest(Request request) throws MqttException {
        String jsonRequest = gson.toJson(request);
        MqttMessage message = new MqttMessage(jsonRequest.getBytes());
        message.setQos(1);  // Гарантія доставки повідомлення
        client.publish(TOPIC, message);
        System.out.println("Sent MQTT request: " + jsonRequest);
    }

    public void subscribe() throws MqttException {
        client.subscribe(TOPIC, (topic, message) -> {
            String payload = new String(message.getPayload());
            System.out.println("Received MQTT response: " + payload);
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

    public void disconnect() throws MqttException {
        if (client.isConnected()) {
            client.disconnect();
            System.out.println("Disconnected from MQTT broker.");
        }
    }
}
