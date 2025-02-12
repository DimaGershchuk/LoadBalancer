/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 *
 * @author ntu-user
 */
public class FileModel {
    
    private SimpleStringProperty file_id;
    private SimpleStringProperty filename;
    private SimpleLongProperty file_length;
    private SimpleLongProperty crc32;
    private SimpleStringProperty file_path;

    public FileModel(String file_id, String filename, long file_length, long crc32, String file_path) {
        this.file_id = new SimpleStringProperty(file_id);
        this.filename = new SimpleStringProperty(filename);
        this.file_length = new SimpleLongProperty(file_length);
        this.crc32 = new SimpleLongProperty(crc32);
        this.file_path = new SimpleStringProperty(file_path);
    }

    public String getFileId() {
        return file_id.get();
    }

    public void setFileId(String file_id) {
        this.file_id.set(file_id);
    }

    public long getFileLength() {
        return file_length.get();
    }

    public void setFileLength(long file_length) {
        this.file_length.set(file_length);
    }

    public Long getCrc32() {
        return crc32.get();
    }

    public void setCrc32(long crc32) {
        this.crc32.set(crc32);
    }

    public String getFilename() {
        return filename.get();
    }

    public void setFilename(String filename) {
        this.filename.set(filename);
    }

    public String getFilePath() {
        return file_path.get();
    }

    public void setFilePath(String filePath) {
        this.file_path.set(filePath);
    }
}
