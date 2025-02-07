package com.mycompany.javafxapplication1;

import com.google.gson.Gson;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import java.util.*;
import java.io.*;
import java.nio.file.Files;
import org.eclipse.paho.client.mqttv3.*;
import com.mycompany.javafxapplication1.Container;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;



public class LoadBalancer{
    
    private final List<Container> containers;
    private final Queue<Task> waitingQueue;
    private final Queue<Task> processingQueue;
    private final Queue<Task> readyQueue;
    private final Random random;
    private final int maxConcurrentRequests;
    private int roundRobinIndex = 0;
    private DB db;
    
    private MqttClient mqttClient;
    private FileChunking fileChunking;
    
    public Thread processingThread;
    private volatile boolean running = true;
    
    public enum TrafficLevel { LOW, MEDIUM, HIGH }
    
    private TrafficLevel trafficLevel = TrafficLevel.LOW;
    
    private double trafficMultiplier = 1.0;
    
     public enum SchedulingAlgorithm {
        FCFS, SJN, PRIORITY
    }
     
    private static final ConcurrentHashMap<String, Boolean> fileOperationInProgress = new ConcurrentHashMap<>();
     
    private SchedulingAlgorithm schedulingAlgorithm = SchedulingAlgorithm.FCFS;

    public LoadBalancer(List<Container> containers, int maxConcurrentRequests) {
        
        this.containers = containers;
        this.waitingQueue = new LinkedList<>();
        this.processingQueue = new LinkedList<>();
        this.readyQueue = new LinkedList<>();
        this.random = new Random();
        this.maxConcurrentRequests = maxConcurrentRequests;
        this.db = new DB();
        this.fileChunking = new FileChunking(this);
        
        

        try {
            mqttClient = new MqttClient("tcp://mqtt-broker:1883", MqttClient.generateClientId());
            mqttClient.connect();
            mqttClient.subscribe("load-balancer/file-operation", this::handleMqttMessage);
            mqttClient.subscribe("load-balancer/traffic-level", this::handleTrafficLevelMessage);
            
            System.out.println("✅Load Balancer connected to MQTT broker!");
            
            
        } catch (MqttException e) {
            e.printStackTrace();
        }
        
        processingThread = new Thread(() -> {
            while (running) {
                try {
                    processRequests();

                    Thread.sleep(20000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        processingThread.setDaemon(true); 
        processingThread.start();
    
    }
    
    
    
    public void shutdown() {
        running = false;
        if (processingThread != null) {
            processingThread.interrupt();
        }
        try {
            mqttClient.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        System.out.println("Load Balancer shutdown.");
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

    private void handleMqttMessage(String topic, MqttMessage message) throws JSchException, IOException, SftpException, SQLException, ClassNotFoundException {
        
        String payload = new String(message.getPayload());
        System.out.println("Received MQTT message: " + payload);

        Gson gson = new Gson();
        Request request = gson.fromJson(payload, Request.class);

        addRequest(request);
    }
    
    public Container selectContainerForChunk(String chunkId) {
       String containerId = db.getContainerIdForChunk(chunkId);
        if (containerId != null) {
            for (Container container : containers) {
                if (container.getId().equals(containerId)) {
                return container;
            }
        }
    }
        System.err.println("Container not found for chunk: " + chunkId);
        return null;
    }
    
    private void uploadFile(Request request) {
        
        if (FileLockManager.isFileLocked(request.getFileId())) {
        System.out.println("File is currently being processed. Waiting to upload: " + request.getFileId());
        return;
        }
        
        fileOperationInProgress.put(request.getFileId(), true);
        FileLockManager.lockFile(request.getFileId());

        try {
            System.out.println("Uploading file: " + request.getFileId());
            System.out.println("File path for chunking: " + request.getFilePath());
            System.out.println("Calling chunkFile for: " + request.getFileId());
            
            List<String> existingChunks = db.getChunksForFile(request.getFileId());
            
            if (!existingChunks.isEmpty()) {
                System.out.println("Chunks already exist for file: " + request.getFileId());
                request.getChunks().addAll(existingChunks); 
            } else {
                List<String> chunks = fileChunking.chunkFile(
                    new File(request.getFilePath()),
                    "chunks/",
                    4,
                    request.getFileId()
                );
                System.out.println("Chunks before adding to request: " + chunks.size());
                request.getChunks().addAll(chunks);
                System.out.println("Chunks after adding to request: " + request.getChunks().size());
            }
            distributeChunks(request);

            System.out.println("File upload completed: " + request.getFileId());
        } catch (Exception e) {
            System.err.println("Error during upload of file: " + request.getFileId());
            e.printStackTrace();
        } finally {
            FileLockManager.unlockFile(request.getFileId());
            fileOperationInProgress.remove(request.getFileId());
        }
    }
        
           
    
    private void deleteChunks(Request request) throws SQLException, ClassNotFoundException {
        if (FileLockManager.isFileLocked(request.getFileId())) {
            System.out.println("File is currently in use. Waiting to delete: " + request.getFileId());
            return;
        }
        
        fileOperationInProgress.put(request.getFileId(), true);
        FileLockManager.lockFile(request.getFileId());

        try {
            List<String> chunksToDelete = new ArrayList<>(request.getChunks());

            for (String chunk : chunksToDelete) {
                Container container = selectContainerForChunk(chunk);
                if (container != null) {
                    container.deleteChunk(chunk);
                    System.out.println("Deleted chunk " + chunk + " from " + container.getId());
                } else {
                    System.err.println("Container not found for chunk: " + chunk);
                }
            }

            // Тепер видаляємо з бази після видалення з контейнерів
            db.deleteChunksFromDb(request.getFileId());

            sendDeletionConfirmation(request.getFileId());

        } finally {
            FileLockManager.unlockFile(request.getFileId());
            fileOperationInProgress.remove(request.getFileId());
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
    
    private void distributeChunks(Request request) throws JSchException, IOException, SftpException {
        List<String> chunks = request.getChunks();
        for (String chunk : chunks) {
            Container selectedContainer = selectContainerForChunk(chunk);
            if (selectedContainer != null) {
                simulateDelay(); 
                selectedContainer.sendFileToContainer(chunk);
         
            }
        }
    }
    
    private void sendDeletionConfirmation(String fileId) {
        try {
            Gson gson = new Gson();
            Response response = new Response(fileId,"DELETED");
            MqttMessage message = new MqttMessage(gson.toJson(response).getBytes());
            mqttClient.publish("load-balancer/file-operation/confirmation", message);
            System.out.println("Deletion confirmation sent for fileId: " + fileId);
        } catch (MqttException e) {
            e.printStackTrace();
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
    
       

    public void addRequest(Request request) throws JSchException, IOException, SftpException, SQLException, ClassNotFoundException{
           if (fileOperationInProgress.getOrDefault(request.getFileId(), false)) {
                System.out.println("Operation already in progress for file: " + request.getFileId() + ". Request added to waitingQueue.");
            }
           
        synchronized (waitingQueue) {
            long delay = simulateDelay();
            Task task = new Task(request, delay);
            waitingQueue.offer(task);
            System.out.println("Added task with delay " + delay + " ms for file " + request.getFileId());
        }

    }
    
     private void processRequests() throws JSchException, IOException, SftpException, SQLException, ClassNotFoundException {
           synchronized (waitingQueue) {
            while (!waitingQueue.isEmpty() && processingQueue.size() < maxConcurrentRequests) {
                Task task = selectNextTask();
                if (task == null) {
                    // Немає готових завдань, виходимо з циклу
                    break;
                }
                if (task.isReady()) {
                    // Видаляємо завдання з waitingQueue (краще використовувати remove(task), оскільки selectNextTask() може повертати не перший елемент)
                    waitingQueue.remove(task);
                    processingQueue.offer(task);
                    executeTask(task);
                } else {
                    break;
                }
            }
        }
        showQueueStatus();
     }
     
     private void adjustSchedulingAlgorithm() {

        List<Task> currentTasks = new ArrayList<>(waitingQueue);
        ShedulerModule simulator = new ShedulerModule(currentTasks, containers.size());

        long fcfsTime = simulator.simulateFCFS();
        long sjfTime = simulator.simulateSJF();

        // Вибираємо алгоритм з меншим загальним часом обробки
        if (sjfTime < fcfsTime) {
            schedulingAlgorithm = SchedulingAlgorithm.SJN;
            System.out.println("Adjusted scheduling algorithm to SJN (Total time: " + sjfTime + "s vs FCFS: " + fcfsTime + "s).");
        } else {
            schedulingAlgorithm = SchedulingAlgorithm.FCFS;
            System.out.println("Adjusted scheduling algorithm to FCFS (Total time: " + fcfsTime + "s vs SJN: " + sjfTime + "s).");
        }
    }
     
      private Task selectNextTask() {
         adjustSchedulingAlgorithm();
    
        List<Task> readyTasks = new ArrayList<>();
        for (Task t : waitingQueue) {
            if (t != null && t.isReady()) {
                readyTasks.add(t);
            }
        }
        if (readyTasks.isEmpty()) {
            return null;
        }

        switch (schedulingAlgorithm) {
            case FCFS:
                // Повертаємо перше готове завдання у waitingQueue
                for (Task t : waitingQueue) {
                    if (t != null && t.isReady()) {
                        return t;
                    }
                }
                break;
            case SJN:
                // Обираємо завдання з найменшим часом обробки (delay)
                return readyTasks.stream()
                        .min(Comparator.comparingLong(t -> t.getDelay()))
                        .orElse(null);
            case PRIORITY:
                // Якщо потрібен PRIORITY scheduling, використовуйте priority з Request
                return readyTasks.stream()
                        .min(Comparator.comparingInt(t -> t.getRequest().getPriority()))
                        .orElse(null);
            default:
                return null;
        }
        return null;
    }

    private void finalizeRequest(Task task) {
        synchronized (processingQueue) {
            if (processingQueue.remove(task)) {
                readyQueue.offer(task);
                System.out.println("Request completed: " + task.getRequest().getFileId());
            }
        }
    }
    
    private void executeTask(Task task) throws JSchException, IOException, SftpException, SQLException, ClassNotFoundException {
        Request request = task.getRequest();
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
        }
        finalizeRequest(task);
    }

    public Container roundRobin() {
        List<Container> healthyContainers = getHealthyContainers();
        if (healthyContainers.isEmpty()) {
            System.out.println("No healthy containers available.");
            return null;
        }
        Container selectedContainer = healthyContainers.get(roundRobinIndex % healthyContainers.size());
        roundRobinIndex = (roundRobinIndex + 1) % healthyContainers.size();
        return selectedContainer;
    }

    
    private long simulateDelay() {
        int baseDelay = (30 + random.nextInt(61)) / 4;
        
        int adjustedDelay = (int) (baseDelay * trafficMultiplier);
        System.out.println("Simulated delay: " + adjustedDelay + " seconds (Traffic Level: " + trafficLevel + ")");

        return adjustedDelay * 1000L;
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

            LoadBalancer loadBalancer = new LoadBalancer(containers, 5);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}