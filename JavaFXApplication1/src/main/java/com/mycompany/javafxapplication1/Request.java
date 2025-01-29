/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import java.util.List;
import java.util.UUID;
import com.google.gson.Gson;

public class Request {
    
    public enum OperationType { UPLOAD, DOWNLOAD, DELETE }
    public enum Status { PENDING, IN_PROGRESS, COMPLETED, FAILED }

    private final String requestId;
    private final int userId;
    private final String fileId;
    private final OperationType operationType;
    private final long fileSize;
    private final int priority;
    private final long timestamp;
    private List<String> chunks;
    private Status status;

    public Request(int userId, String fileId, OperationType operationType, long fileSize, int priority, List<String> chunks) {
        this.requestId = UUID.randomUUID().toString();
        this.userId = userId;
        this.fileId = fileId;
        this.operationType = operationType;
        this.fileSize = fileSize;
        this.priority = priority;
        this.timestamp = System.currentTimeMillis();
        this.chunks = chunks;
        this.status = Status.PENDING;
    }

   public static Request fromJson(String json) {
        return new Gson().fromJson(json, Request.class);
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public String getId() {
        return fileId;
    }
}
