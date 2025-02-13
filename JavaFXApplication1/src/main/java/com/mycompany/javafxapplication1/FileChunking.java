/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author ntu-user
 */
public class FileChunking {
    
    private LoadBalancer loadBalancer;
    private String zipPassword;

    public FileChunking(LoadBalancer loadBalancer, String zipPassword) {
        this.loadBalancer = loadBalancer;
        this.zipPassword = zipPassword;
    }

    public List<String> chunkFile(File inputFile, String outputDir, int numberChunks, String fileId) throws Exception {
        
        DB db = new DB();
        List<String> chunkNames = new ArrayList<>();

        String zipFilePath = outputDir + File.separator + fileId + ".zip";
        createEncryptedZip(inputFile, zipFilePath, zipPassword);

        byte[] zipBytes = Files.readAllBytes(Paths.get(zipFilePath));
        long fileLength = zipBytes.length;
        int chunkSize = (int) Math.ceil((double) fileLength / numberChunks);

        File chunksDir = new File(outputDir);
        if (!chunksDir.exists()) {
            chunksDir.mkdirs();
        }

        for (int i = 0; i < numberChunks; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, zipBytes.length);
            byte[] chunk = Arrays.copyOfRange(zipBytes, start, end);

            String chunkName = UUID.randomUUID().toString();
            File chunkFile = new File(chunksDir, chunkName);
            try (FileOutputStream fos = new FileOutputStream(chunkFile)) {
                fos.write(chunk);
            }

            Container selectedContainer = loadBalancer.roundRobin();
            String containerId = selectedContainer.getId();
            db.addChunkMetaData(chunkName, fileId, containerId, i);
            chunkNames.add(chunkName);
        }
        System.out.println("Completed chunking for file: " + fileId);
        return chunkNames;
    }

    private void createEncryptedZip(File inputFile, String zipFilePath, String password) throws ZipException {
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setEncryptFiles(true);
        zipParameters.setCompressionLevel(CompressionLevel.MAXIMUM);
        zipParameters.setEncryptionMethod(EncryptionMethod.AES);
        ZipFile zipFile = new ZipFile(zipFilePath, password.toCharArray());
        zipFile.addFile(inputFile, zipParameters);
        System.out.println("Encrypted zip file created: " + zipFilePath);
    }
    
    public String getZipPassword() {
        return zipPassword;
    }
}
