/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import java.io.File;
import javax.crypto.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
/**
 *
 * @author ntu-user
 */
public class FileChunking {
    
    public static void chunkFile(File inputFile, String outputDir, int numberChunks, String fileId) throws NoSuchAlgorithmException, FileNotFoundException, IOException, ClassNotFoundException, Exception{
           
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey secretKey = keyGen.generateKey();
        
        byte[] encryptedFileBytes = encryptFile(inputFile.getAbsolutePath(), secretKey);
        
        byte[] encryptedKey = encryptKey(secretKey);
        
        long fileLength = encryptedFileBytes.length;
        
        int chunkSize = (int) Math.ceil((double) fileLength / numberChunks);
        
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdir();
        }
        
        for(int i = 0; i < numberChunks; i++){
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, encryptedFileBytes.length);
            byte[] chunk = Arrays.copyOfRange(encryptedFileBytes, start, end);

            // Створення файлу для кожної частини
            String chunkName = UUID.randomUUID().toString();
            File chunkFile = new File(outputDir, chunkName);

            try (FileOutputStream fos = new FileOutputStream(chunkFile)) {
                fos.write(chunk);
            }

            
            DB db = new DB();
            db.addChunkMetaData(chunkName, fileId); 
        }
    }

        private static byte[] encryptFile(String inputFile, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] fileBytes = Files.readAllBytes(Paths.get(inputFile));
        return cipher.doFinal(fileBytes);
        }

        private static byte[] encryptKey(SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        PublicKey publicKey = keyPairGen.generateKeyPair().getPublic();
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(key.getEncoded());
        }
}
