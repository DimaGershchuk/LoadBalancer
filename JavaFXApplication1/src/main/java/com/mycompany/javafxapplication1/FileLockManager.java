/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author ntu-user
 */
public class FileLockManager {
    
    private static final ReentrantLock lock = new ReentrantLock();

    public static void lockFile(String fileId) {
        System.out.println("Locking file: " + fileId);
        lock.lock();
    }

    public static void unlockFile(String fileId) {
        System.out.println("Unlocking file: " + fileId);
        lock.unlock();
    }

    public static boolean isFileLocked(String fileId) {
        return lock.isLocked();
    }
}
