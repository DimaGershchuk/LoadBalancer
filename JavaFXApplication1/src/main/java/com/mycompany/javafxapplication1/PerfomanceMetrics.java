/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

/**
 *
 * @author ntu-user
 */
public class PerfomanceMetrics {
    private long uploadCount;
    private long downloadCount;
    private long deleteCount;
    private long updateCount;
    
    private long uploadTime;
    private long downloadTime;
    private long deleteTime;
    private long updateTime;
    
    public PerfomanceMetrics() {
        uploadCount = 0;
        downloadCount = 0;
        deleteCount = 0;
        updateCount = 0;
        uploadTime = 0;
        downloadTime = 0;
        deleteTime = 0;
        updateTime = 0;
    }
    
    public synchronized void recordUpload(long durationMs) {
        uploadCount++;
        uploadTime += durationMs;
    }
    
    public synchronized void recordDownload(long durationMs) {
        downloadCount++;
        downloadTime += durationMs;
    }
    
    public synchronized void recordDelete(long durationMs) {
        deleteCount++;
        deleteTime += durationMs;
    }
    
    public synchronized void recordUpdate(long durationMs) {
        updateCount++;
        updateTime += durationMs;
    }
    
    public synchronized double getAverageUploadTime() {
        return uploadCount == 0 ? 0 : (double) uploadTime / uploadCount;
    }
    
    public synchronized double getAverageDownloadTime() {
        return downloadCount == 0 ? 0 : (double) downloadTime / downloadCount;
    }
    
    public synchronized double getAverageDeleteTime() {
        return deleteCount == 0 ? 0 : (double) deleteTime / deleteCount;
    }
    
    public synchronized double getAverageUpdateTime() {
        return updateCount == 0 ? 0 : (double) updateTime / updateCount;
    }
    
    public synchronized void logMetrics() {
        System.out.println("=== Performance Metrics ===");
        System.out.println("Uploads: " + uploadCount + ", Avg time: " + getAverageUploadTime() + " ms");
        System.out.println("Downloads: " + downloadCount + ", Avg time: " + getAverageDownloadTime() + " ms");
        System.out.println("Deletes: " + deleteCount + ", Avg time: " + getAverageDeleteTime() + " ms");
        System.out.println("Updates: " + updateCount + ", Avg time: " + getAverageUpdateTime() + " ms");
        System.out.println("===========================");
    }
}
