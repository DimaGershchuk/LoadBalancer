package com.mycompany.javafxapplication1;

import com.google.gson.Gson;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import java.util.*;
import java.io.*;
import java.nio.file.Files;
import org.eclipse.paho.client.mqttv3.*;


public class LoadBalancer{
    
     private final List<Container> containers;
    private final Queue<Request> waitingQueue;
    private final Queue<Request> processingQueue;
    private final Queue<Request> readyQueue;
    private final Random random;
    private final int maxConcurrentRequests;
    private int roundRobinIndex = 0;
    
    private MqttClient mqttClient;
    

    public enum TrafficLevel { LOW, MEDIUM, HIGH }
    
    private TrafficLevel trafficLevel = TrafficLevel.LOW;
    
    private double trafficMultiplier = 1.0;  

    public LoadBalancer(List<Container> containers, int maxConcurrentRequests) {
        this.containers = containers;
        this.waitingQueue = new LinkedList<>();
        this.processingQueue = new LinkedList<>();
        this.readyQueue = new LinkedList<>();
        this.random = new Random();
        this.maxConcurrentRequests = maxConcurrentRequests;

        try {
            mqttClient = new MqttClient("tcp://mqtt-broker:1883", MqttClient.generateClientId());
            mqttClient.connect();
            mqttClient.subscribe("load-balancer/file-operation", this::handleMqttMessage);
            System.out.println("✅Load Balancer connected to MQTT broker!");
            mqttClient.subscribe("load-balancer/traffic-level", this::handleTrafficLevelMessage);
            System.out.println("Subscribed to topic: load-balancer/file-operation");
            
            
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    
    private List<Container> getHealthyContainers() {
    List<Container> healthyContainers = new ArrayList<>();
    for (Container container : containers) {
        if (container.isHealthy()) {
            healthyContainers.add(container);
        }
    }
    return healthyContainers;
}

    private void handleMqttMessage(String topic, MqttMessage message) throws JSchException, IOException, SftpException {
        String payload = new String(message.getPayload());
        System.out.println("Received MQTT message: " + payload);

        Gson gson = new Gson();
        Request request = gson.fromJson(payload, Request.class);

        switch (request.getOperationType()) {
            case UPLOAD:
                uploadFile(request);
                break;
            case DELETE:
                deleteChunks(request);
                break;
            case DOWNLOAD:
                downloadChunks(request);
                break;
            default:
                System.out.println("Unknown operation type: " + request.getOperationType());
        }
    }
    
    private Container selectContainerForChunk(String chunk) {
        int containerIndex = Math.abs(chunk.hashCode()) % containers.size();
        return containers.get(containerIndex);
    }
    
    private void uploadFile(Request request) {
        if (FileLockManager.isFileLocked(request.getFileId())) {
            System.out.println("File is currently being processed. Waiting to upload: " + request.getFileId());
            return;
        }

        FileLockManager.lockFile(request.getFileId()); 

        try {
            System.out.println("Uploading file: " + request.getFileId());
            distributeChunks(request);  
            System.out.println("File upload completed: " + request.getFileId());
        } catch (Exception e) {
            System.err.println("Error during upload of file: " + request.getFileId());
            e.printStackTrace();
        } finally {
            FileLockManager.unlockFile(request.getFileId());  
        }
    }
    
    private void deleteChunks(Request request) {
        
        if (FileLockManager.isFileLocked(request.getFileId())) {
            System.out.println("File is currently in use. Waiting to delete: " + request.getFileId());
            return;
        }

        FileLockManager.lockFile(request.getFileId());
        try {
            for (String chunk : request.getChunks()) {
                Container container = selectContainerForChunk(chunk);
                if (container != null) {
                    container.deleteChunk(chunk);
                }
            }
        System.out.println("File deletion completed: " + request.getFileId());
        
        } finally {
            FileLockManager.unlockFile(request.getFileId()); 
        }
    }
    
    private void downloadChunks(Request request) throws IOException{
        
        if (FileLockManager.isFileLocked(request.getFileId())) {
            System.out.println("File is currently in use. Waiting to delete: " + request.getFileId());
            return;
        }

        FileLockManager.lockFile(request.getFileId());
        
        try {
            List<String> chunks = request.getChunks();

            for(String chunk : chunks){
                Container container = selectContainerForChunk(chunk);
                if (container != null) {
                container.downloadChunk(chunk, "/home/ntu-user/Downloads/" + chunk); 
                System.out.println("Downloaded chunk " + chunk + " from " + container.getId());
                }
            }
                combineChunks(chunks, request.getFileId());
                
        } finally{
            FileLockManager.unlockFile(request.getFileId()); 
        }
        
    }
        

    public void combineChunks(List<String> chunks, String fileId) throws FileNotFoundException, IOException{
        
        try(FileOutputStream fos = new FileOutputStream("/home/ntu-user/Downloads/" + fileId)){
            for(String chunk : chunks){
                File chunkFile = new File("/home/ntu-user/Downloads/" + chunk);
                Files.copy(chunkFile.toPath(), fos);
                chunkFile.delete();
            }
        } catch (IOException e) {
            System.err.println("Failed to combine chunks for file " + fileId);
            e.printStackTrace();
    }}
    
    /*private void updateFile(Request request) throws JSchException, IOException, SftpException {
    
         if (FileLockManager.isFileLocked(request.getFileId())) {
            System.out.println("File is currently in use. Waiting to delete: " + request.getFileId());
            return;
        }

        FileLockManager.lockFile(request.getFileId());
    
        System.out.println("Updating file: " + request.getId());

        deleteChunks(request);

        distributeChunks(request);

        System.out.println("File " + request.getId() + " successfully updated.");
    }*/

    private void handleTrafficLevelMessage(String topic, MqttMessage message) {
        
        String payload = new String(message.getPayload());
        System.out.println("Traffic level message: " + payload);

        switch (payload.trim().toUpperCase()) {
            case "LOW":
                this.trafficLevel = TrafficLevel.LOW;
                this.trafficMultiplier = 1.0;
                break;
            case "MEDIUM":
                this.trafficLevel = TrafficLevel.MEDIUM;
                this.trafficMultiplier = 1.5;
                break;
            case "HIGH":
                this.trafficLevel = TrafficLevel.HIGH;
                this.trafficMultiplier = 2.0;
                break;
            default:
                System.out.println("Unknown traffic level: " + payload);
        }
    }

    public void addRequest(Request request) throws JSchException, IOException, SftpException{
        synchronized (waitingQueue) {
            waitingQueue.add(request);
            System.out.println("Added to waiting queue: " + request.getId());
        }
        processRequests();
    }

    private void processRequests() throws JSchException, IOException, SftpException {
         synchronized (waitingQueue) {
            while (!waitingQueue.isEmpty() && processingQueue.size() < maxConcurrentRequests) {
                Request request = waitingQueue.poll();
                processingQueue.add(request);

                if (request.getOperationType() == Request.OperationType.UPLOAD) {
                    uploadFile(request); 
                } else {
                    distributeChunks(request); 
                }
            }
        }
    }

    private void distributeChunks(Request request) throws JSchException, IOException, SftpException {
        List<String> chunks = request.getChunks();
        for (String chunk : chunks) {
            Container selectedContainer = roundRobin();
            if (selectedContainer != null) {
                simulateDelay(); 
                selectedContainer.sendFileToContainer(chunk);
         
            }
        }
        finalizeRequest(request);
    }

    private void finalizeRequest(Request request) {
        synchronized (processingQueue) {
            processingQueue.remove(request);
            readyQueue.add(request);
            System.out.println("Request completed: " + request.getId());
        }
    }

    private Container roundRobin() {
        List<Container> healthyContainers = getHealthyContainers();
        if (healthyContainers.isEmpty()) {
            System.out.println("❌ No healthy containers available.");
            return null;
        }
        Container selectedContainer = healthyContainers.get(roundRobinIndex % healthyContainers.size());
        roundRobinIndex = (roundRobinIndex + 1) % healthyContainers.size();
        return selectedContainer;
    }

    
    private void simulateDelay() {
        
        int baseDelay = 30 + random.nextInt(61);
        int adjustedDelay = (int) (baseDelay * trafficMultiplier);
        System.out.println("Simulated delay: " + adjustedDelay + " seconds (Traffic Level: " + trafficLevel + ")");

        try {
            Thread.sleep(adjustedDelay * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void showQueueStatus() {
        System.out.println("Traffic Level: " + trafficLevel);
        System.out.println("Waiting Queue: " + waitingQueue.size());
        System.out.println("Processing Queue: " + processingQueue.size());
        System.out.println("Ready Queue: " + readyQueue.size());
    }
    
    public static void main(String[] args) {
        try {
            List<Container> containers = new ArrayList<>();
            containers.add(new Container("container1", "soft40051-files-container1", 22, "ntu-user", "ntu-user"));
            containers.add(new Container("container2", "soft40051-files-container2", 22, "ntu-user", "ntu-user"));
            containers.add(new Container("container3", "soft40051-files-container3", 22, "ntu-user", "ntu-user"));
            containers.add(new Container("container4", "soft40051-files-container4", 22, "ntu-user", "ntu-user"));
            
            
            LoadBalancer loadBalancer = new LoadBalancer(containers, 2);
            System.out.println("✅Load Balancer connect to MQTT borker!");
            
            
       

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        
    }
    
}