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
     
    private static final ConcurrentHashMap<String, Request.Status> fileOperationStatus = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> fileOperationEndTime = new ConcurrentHashMap<>();
     
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

                    Thread.sleep(25000);
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
    // Перевіряємо, чи вже існує операція для цього файлу, що не завершилася успішно.
        Request.Status currentStatus = fileOperationStatus.get(request.getFileId());
        if (currentStatus != null && currentStatus != Request.Status.COMPLETED) {
            System.out.println("Previous operation for file " + request.getFileId() + " is still in progress or failed. Aborting upload.");
            return;
        }

        // Встановлюємо статус операції в IN_PROGRESS
        fileOperationStatus.put(request.getFileId(), Request.Status.IN_PROGRESS);
        FileLockManager.lockFile(request.getFileId());
        request.setStatus(Request.Status.IN_PROGRESS);

        try {
            System.out.println("Uploading file: " + request.getFileId());


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
                request.getChunks().addAll(chunks);
            }
            
            distributeChunks(request);
            request.setStatus(Request.Status.COMPLETED);
            
            fileOperationStatus.put(request.getFileId(), Request.Status.COMPLETED);
            
            System.out.println("File upload completed: " + request.getFileId());
            
        } catch (Exception e) {
            System.err.println("Error during upload of file: " + request.getFileId());
            e.printStackTrace();
            request.setStatus(Request.Status.FAILED);
            fileOperationStatus.put(request.getFileId(), Request.Status.FAILED);
            
        } finally {
            FileLockManager.unlockFile(request.getFileId());
        }
    }
        
           
    
    private void deleteChunks(Request request) throws SQLException, ClassNotFoundException, InterruptedException {
        while (true) {
            Request.Status status = fileOperationStatus.get(request.getFileId());
            if (status == null || status == Request.Status.COMPLETED) {
                break;
            } else if (status == Request.Status.FAILED) {
                System.out.println("Previous operation for file " + request.getFileId() + " failed. Aborting deletion.");
                return;
            } else {
                System.out.println("Waiting for previous operation to complete for file: " + request.getFileId());
                Thread.sleep(1000);
            }
        }
    
        fileOperationStatus.put(request.getFileId(), Request.Status.IN_PROGRESS);
        FileLockManager.lockFile(request.getFileId());

        try {
            List<String> chunksToDelete = new ArrayList<>(request.getChunks());
            for (String chunk : chunksToDelete) {
                Container container = selectContainerForChunk(chunk);
                if (container != null) {
                    container.deleteChunk(chunk);
                } else {
                    System.err.println("Container not found for chunk: " + chunk);
                }
            }
            
            db.deleteChunksFromDb(request.getFileId());
            sendDeletionConfirmation(request.getFileId());
            fileOperationStatus.put(request.getFileId(), Request.Status.COMPLETED);
            
        } catch (Exception e) {
            e.printStackTrace();
            fileOperationStatus.put(request.getFileId(), Request.Status.FAILED);
        } finally {
            FileLockManager.unlockFile(request.getFileId());
            fileOperationStatus.remove(request.getFileId());
        }
    }
    
    private void downloadChunks(Request request) throws IOException, InterruptedException{
        
         while (true) {
            Request.Status status = fileOperationStatus.get(request.getFileId());
            if (status == null || status == Request.Status.COMPLETED) {
                break;
            } else if (status == Request.Status.FAILED) {
                System.out.println("Previous operation for file " + request.getFileId() + " failed. Aborting download.");
                return;
            } else {
                System.out.println("Waiting for previous operation to complete for file: " + request.getFileId());
                Thread.sleep(1000);
            }
        }
    
        fileOperationStatus.put(request.getFileId(), Request.Status.IN_PROGRESS);
        FileLockManager.lockFile(request.getFileId());

        try {
            
            if (request.getChunks() == null || request.getChunks().isEmpty()) {
                List<String> chunksFromDb = db.getChunksForFile(request.getFileId());
                if (chunksFromDb != null) {
                    request.getChunks().addAll(chunksFromDb);
                }
            }
            
            List<String> chunks = request.getChunks();
            for (String chunk : chunks) {
                Container container = selectContainerForChunk(chunk);
                if (container != null) {
                    container.downloadChunk(chunk, "/home/ntu-user/Downloads/" + chunk);
                    System.out.println("Downloaded chunk " + chunk + " from " + container.getId());
                } else {
                    System.err.println("Container not found for chunk: " + chunk);
                }
            }
            combineChunks(chunks, request.getFileId());
            fileOperationStatus.put(request.getFileId(), Request.Status.COMPLETED);
            
        } catch (Exception e) {
            e.printStackTrace();
            fileOperationStatus.put(request.getFileId(), Request.Status.FAILED);
            
        } finally {
            FileLockManager.unlockFile(request.getFileId());
            fileOperationStatus.remove(request.getFileId());
        }
    }
        
    
    private void distributeChunks(Request request) throws JSchException, IOException, SftpException {
        List<String> chunks = request.getChunks();
        for (String chunk : chunks) {
            Container selectedContainer = selectContainerForChunk(chunk);
            if (selectedContainer != null) {
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
        
        if (fileOperationStatus.containsKey(request.getFileId()) &&
            fileOperationStatus.get(request.getFileId()) != Request.Status.COMPLETED) {
            System.out.println("Operation already in progress for file: " + request.getFileId() + ". New request will wait.");
            // Можна відкласти додавання або об'єднати запит з існуючим.
            return;
        }

        long baseDelay = simulateDelay();
        long now = System.currentTimeMillis();
        long delay;
        Long prevEndTime = fileOperationEndTime.get(request.getFileId());

        // Якщо попередня операція ще не завершилася, новий запит повинен починати свою затримку після її завершення
        if (prevEndTime != null && prevEndTime > now) {
            delay = (prevEndTime - now) + baseDelay;
            System.out.println("Adjusted delay for file " + request.getFileId() + ": " + delay + " ms (base: " + baseDelay + " ms)");
        } else {
            delay = baseDelay;
        }

        long scheduledStart = now + delay;
        fileOperationEndTime.put(request.getFileId(), scheduledStart);

        Task task = new Task(request, delay);
        synchronized (waitingQueue) {
            waitingQueue.offer(task);
        }
        System.out.println("Added task with delay " + delay + " ms for file " + request.getFileId());

    }
    
     private void processRequests() throws JSchException, IOException, SftpException, SQLException, ClassNotFoundException, InterruptedException {
           synchronized (waitingQueue) {
            while (!waitingQueue.isEmpty() && processingQueue.size() < maxConcurrentRequests) {
                Task task = selectNextTask();
                if (task == null) {
                    break;
                }
                if (task.isReady()) {
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
            //System.out.println("Adjusted scheduling algorithm to SJN (Total time: " + sjfTime + "s vs FCFS: " + fcfsTime + "s).");
        } else {
            schedulingAlgorithm = SchedulingAlgorithm.FCFS;
            //System.out.println("Adjusted scheduling algorithm to FCFS (Total time: " + fcfsTime + "s vs SJN: " + sjfTime + "s).");
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
    
    private void executeTask(Task task) throws JSchException, IOException, SftpException, SQLException, ClassNotFoundException, InterruptedException {
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