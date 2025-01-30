/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;
import org.eclipse.paho.client.mqttv3.*;
import com.google.gson.Gson;
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

    // Підключення до MQTT брокера
    private void connect() throws MqttException {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        client.connect(options);
        System.out.println("Connected to MQTT broker at " + BROKER_URL);
    }

    // Відправка запиту на Load Balancer
    public void sendRequest(Request request) throws MqttException {
        String jsonRequest = gson.toJson(request);
        MqttMessage message = new MqttMessage(jsonRequest.getBytes());
        client.publish(TOPIC, message);
        System.out.println("Sent MQTT request: " + jsonRequest);
    }

    // Підписка на Load Balancer для отримання відповідей
    public void subscribe() {
        try {
            client.subscribe(TOPIC, (topic, message) -> {
                String payload = new String(message.getPayload());
                System.out.println("Received MQTT response: " + payload);
                handleResponse(payload);
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // Обробка отриманого JSON-відповіді від Load Balancer
    private void handleResponse(String payload) {
        try {
            Response response = gson.fromJson(payload, Response.class);
            System.out.println("Processing response: " + response);
        } catch (Exception e) {
            System.err.println("Failed to parse MQTT response: " + e.getMessage());
        }
    }

    // Закриття підключення
    public void disconnect() {
        try {
            if (client.isConnected()) {
                client.disconnect();
                System.out.println("Disconnected from MQTT broker.");
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
