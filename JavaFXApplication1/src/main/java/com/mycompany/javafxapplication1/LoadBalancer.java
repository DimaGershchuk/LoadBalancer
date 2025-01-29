package com.mycompany.javafxapplication1;

import com.google.gson.Gson;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import org.eclipse.paho.client.mqttv3.*;


public class LoadBalancer{
    private final List<Container> containers;
    private final Queue<Request> requestQueue;
    private final Random random;
    private final int maxRequestsPerContainer;
    
    private MqttClient mqttClient;

    public LoadBalancer(List<Container> containers, int maxRequestsPerContainer) {
        this.containers = containers;
        this.requestQueue = new LinkedList<>();
        this.random = new Random();
        this.maxRequestsPerContainer = maxRequestsPerContainer;

        try {
            mqttClient = new MqttClient("tcp://localhost:1883", MqttClient.generateClientId());
            mqttClient.connect();
            mqttClient.subscribe("load-balancer/file-operation", this::handleMqttMessage);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void handleMqttMessage(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        System.out.println("Received MQTT message: " + payload);

        Request request = parseRequest(payload);
        addRequest(request);
    }

    private Request parseRequest(String payload) {
        Gson gson = new Gson();
        return gson.fromJson(payload, Request.class);
    }

    public void addRequest(Request request) {
        synchronized (requestQueue) {
            requestQueue.add(request);
        }
        processRequests();
    }

    private void processRequests() {
        synchronized (requestQueue) {
            while (!requestQueue.isEmpty()) {
                Request request = requestQueue.poll();
                Container selectedContainer = selectContainer();

                if (selectedContainer != null) {
                    simulateDelay();
                    selectedContainer.processRequest(request);
                } else {
                    System.out.println("All containers are busy. Retrying...");
                    requestQueue.add(request);
                    break;
                }
            }
        }
    }

    private Container selectContainer() {
        for (Container container : containers) {
            if (container.getCurrentLoad() < maxRequestsPerContainer) {
                return container;
            }
        }
        return null;
    }

    private void simulateDelay() {
        int delay = 30 + random.nextInt(61); 
        try {
            TimeUnit.SECONDS.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
}