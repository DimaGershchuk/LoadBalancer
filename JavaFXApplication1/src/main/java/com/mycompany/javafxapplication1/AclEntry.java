/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

/**
 *
 * @author ntu-user
 */
public class AclEntry {
    private final String fileId;
    private final int userId;
    private final boolean canRead;
    private final boolean canWrite;

    public AclEntry(String fileId, int userId, boolean canRead, boolean canWrite) {
        this.fileId = fileId;
        this.userId = userId;
        this.canRead = canRead;
        this.canWrite = canWrite;
    }

    // Геттери
    public String getFileId() {
        return fileId;
    }

    public int getUserId() {
        return userId;
    }

    public boolean isCanRead() {
        return canRead;
    }

    public boolean isCanWrite() {
        return canWrite;
    }
    
    @Override
    public String toString() {
        return "AclEntry [fileId=" + fileId + ", userId=" + userId + ", canRead=" + canRead + ", canWrite=" + canWrite + "]";
    }
}
