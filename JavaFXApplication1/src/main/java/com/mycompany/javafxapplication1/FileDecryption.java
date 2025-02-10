/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author ntu-user
 */
public class FileDecryption {
    
    public static byte[] combineChunks(List<String> chunkFilePaths) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (String path : chunkFilePaths) {
            byte[] chunkBytes = Files.readAllBytes(Paths.get(path));
            baos.write(chunkBytes);
        }
        return baos.toByteArray();
    }
    /**
     * Розшифровує зашифрований AES ключ за допомогою RSA приватного ключа.
     *
     * @param encryptedAesKey зашифрований AES ключ (у вигляді байтів)
     * @param rsaPrivateKey   RSA приватний ключ
     * @return об’єкт SecretKey, який використовується для AES розшифрування
     * @throws Exception
     */
    public static SecretKey decryptAesKey(byte[] encryptedAesKey, PrivateKey rsaPrivateKey) throws Exception {
        Cipher rsaCipher = Cipher.getInstance("RSA");
        rsaCipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
        byte[] aesKeyBytes = rsaCipher.doFinal(encryptedAesKey);
        return new SecretKeySpec(aesKeyBytes, "AES");
    }

    /**
     * Розшифровує зашифровані дані файлу за допомогою AES ключа.
     *
     * @param encryptedFileBytes зашифровані дані файлу
     * @param aesKey             AES ключ для розшифрування
     * @return розшифровані байти файлу
     * @throws Exception
     */
    public static byte[] decryptFile(byte[] encryptedFileBytes, SecretKey aesKey) throws Exception {
        Cipher aesCipher = Cipher.getInstance("AES");
        aesCipher.init(Cipher.DECRYPT_MODE, aesKey);
        return aesCipher.doFinal(encryptedFileBytes);
    }

    /**

     * @param chunkFilePaths  список шляхів до файлів-чанків
     * @param fileId          унікальний ідентифікатор файлу
     * @param db              об’єкт бази даних для отримання зашифрованого ключа
     * @param rsaPrivateKey   RSA приватний ключ для розшифрування AES ключа
     * @param outputFilePath  шлях, куди буде збережено розшифрований файл
     * @throws Exception
     */
    
    public static void downloadAndDecryptFile(List<String> chunkFilePaths, String fileId, DB db, PrivateKey rsaPrivateKey, String outputFilePath) throws Exception {
        // 1. Об’єднуємо всі чанки в один зашифрований файл
        byte[] encryptedFileBytes = combineChunks(chunkFilePaths);

        // 2. Отримуємо зашифрований AES ключ з бази даних (збережений у таблиці encryption_keys)
        String encryptedKeyBase64 = db.getEncryptionKey(fileId);
        if (encryptedKeyBase64 == null) {
            throw new Exception("Encrypted key not found for file: " + fileId);
        }
        byte[] encryptedAesKey = Base64.getDecoder().decode(encryptedKeyBase64);

        // 3. Розшифровуємо AES ключ за допомогою RSA приватного ключа
        SecretKey aesKey = decryptAesKey(encryptedAesKey, rsaPrivateKey);

        // 4. Розшифровуємо файл за допомогою отриманого AES ключа
        byte[] decryptedFileBytes = decryptFile(encryptedFileBytes, aesKey);

        // 5. Записуємо розшифрований файл
        try (FileOutputStream fos = new FileOutputStream(new File(outputFilePath))) {
            fos.write(decryptedFileBytes);
        }
        System.out.println("File decrypted successfully. Saved to: " + outputFilePath);
    }


    public static KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        return keyPairGen.generateKeyPair();
    }
}
