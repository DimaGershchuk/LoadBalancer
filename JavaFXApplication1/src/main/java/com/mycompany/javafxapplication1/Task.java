/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

/**
 *
 * @author ntu-user
 */
public class Task {
    
    private final Request request;
    private long startTime; 
    private final long delay;

    public Task(Request request, long delay) {
        this.request = request;
        this.startTime = System.currentTimeMillis();
        this.delay = delay;
    }

    public Request getRequest() {
        return request;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getDelay() {
        return delay;
    }
    
    public String getName() {
        return request.getFileId();
    }

    public void resetStartTime() {
        this.startTime = System.currentTimeMillis();
    }

    public long getRemainingTime() {
        long remaining = (startTime + delay) - System.currentTimeMillis();
        return remaining > 0 ? remaining : 0;
    }

    public boolean isReady() {
        return getRemainingTime() == 0;
    }
}
