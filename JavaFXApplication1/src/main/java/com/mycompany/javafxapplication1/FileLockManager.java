/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author ntu-user
 */
public class FileLockManager {
    
     private static final ConcurrentHashMap<String, ReentrantLock> fileLocks = new ConcurrentHashMap<>();

    public static void lockFile(String fileId) {
        fileLocks.putIfAbsent(fileId, new ReentrantLock());
        ReentrantLock lock = fileLocks.get(fileId);
        
        System.out.println("Locking file: " + fileId);
        lock.lock();
    }

    public static void unlockFile(String fileId) {
        ReentrantLock lock = fileLocks.get(fileId);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            System.out.println("Unlocking file: " + fileId);

            if (!lock.isLocked()) {
                fileLocks.remove(fileId);
            }
        }
    }

    public static boolean isFileLocked(String fileId) {
        ReentrantLock lock = fileLocks.get(fileId);
        return lock != null && lock.isLocked();
    }
}
