/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */
package com.mycompany.javafxapplication1;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 *
 * @author ntu-user
 */
public class DB {
    private String fileName = "jdbc:sqlite:comp20081.db";
    private int timeout = 30;
    private String dataBaseName = "COMP20081";
    private String dataBaseTableName = "users";
    private Connection connection = null;
    private Random random = new SecureRandom();
    private String characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private int iterations = 10000;
    private int keylength = 256;
    private String saltValue;
    
    /**
     * @brief constructor - generates the salt if it doesn't exists or load it from the file .salt
     */
    DB() {
        try {
            File fp = new File(".salt");
            if (!fp.exists()) {
                saltValue = this.getSaltvalue(30);
                FileWriter myWriter = new FileWriter(fp);
                myWriter.write(saltValue);
                myWriter.close();
            } else {
                Scanner myReader = new Scanner(fp);
                while (myReader.hasNextLine()) {
                    saltValue = myReader.nextLine();
                }
            }
            connection = initConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
     private Connection initConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection(fileName);
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
        
    
    
    /**
     * @brief create a new table
     * @param tableName name of type String
     */
    public void createTable(String tableName) throws ClassNotFoundException {
        try {
            
            if (connection == null) {
            connection = initConnection();
                }
        
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            statement.executeUpdate("create table if not exists " + tableName + "(id integer primary key autoincrement, name string, password string, role string)");

        } catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }

    /**
     * @brief delete table
     * @param tableName of type String
     */
   public void delTable(String tableName) throws ClassNotFoundException {
        try {
            if (connection == null) {
            connection = initConnection();
                }
            
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            statement.executeUpdate("drop table if exists " + tableName);
        } catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
    }

    /**
     * @brief add data to the database method
     * @param user name of type String
     * @param password of type String
     */
        public void addDataToDB(String user, String password, String role) throws InvalidKeySpecException, ClassNotFoundException {
        try {
            
            if (connection == null) {
            connection = initConnection();
                }
            
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            statement.executeUpdate("insert into " + dataBaseTableName + " (name, password, role) values('" + user + "','" + generateSecurePassword(password) + "', '"+ role +"')");
        } catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
            } 
            
        }
    }

    /**
     * @brief get data from the Database method
     * @retunr results as ResultSet
     */
    public ObservableList<User> getUserData() throws ClassNotFoundException {
        ObservableList<User> result = FXCollections.observableArrayList();
        try {
            
            if (connection == null) {
            connection = initConnection();
                }

            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            try(ResultSet rs = statement.executeQuery("select * from " + this.dataBaseTableName)){
            while (rs.next()) {
                result.add(new User(rs.getInt("id"), rs.getString("name"),rs.getString("password"), rs.getString("role")));
            }
            
        }} catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } 
        return result;
    }
    
    
    public String getUserRole(String username) {
     
        String role = null;
        String query = "SELECT role FROM Users WHERE name = ?";

        try (Connection connection = DriverManager.getConnection(fileName);
             PreparedStatement pstmt = connection.prepareStatement(query)) {

            pstmt.setString(1, username); 
            try(ResultSet rs = pstmt.executeQuery()){

            if (rs.next()) {  
                role = rs.getString("role");
            } else {
                System.out.println("User not found.");
            } }
        } catch (SQLException e) {
            System.err.println("Error during sql query" + e.getMessage());
            e.printStackTrace();
        }
        return role;
    }
    
    public ObservableList<User> getUsersWithRoleUser() throws ClassNotFoundException, SQLException {
        
    ObservableList<User> result = FXCollections.observableArrayList();
    
    try {
        
        if (connection == null) {
            connection = initConnection();
                }

        var statement = connection.createStatement();
        statement.setQueryTimeout(timeout);

        try(ResultSet rs = statement.executeQuery("SELECT * FROM " + this.dataBaseTableName + " WHERE role = 'user'")){

        while (rs.next()) {
            result.add(new User(rs.getInt("id"), rs.getString("name"), rs.getString("password"), rs.getString("role")));
            }
         }
            } catch (SQLException ex) {
        Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
    
        }
    
        return result;
    }
    
    public int getUserId(String username) throws SQLException, ClassNotFoundException {
        
        if (connection == null) {
                connection = initConnection();
                    }

        String query = "SELECT id FROM users WHERE name = ?";
        try(PreparedStatement stmt = connection.prepareStatement(query)){
        stmt.setString(1, username);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return rs.getInt("id");
        }
        }
        return -1; 
}

    
    
    public boolean updateUser(String originalUsername, String newUsername, String newPassword, String newRole) throws InvalidKeySpecException {
        String query = "UPDATE Users SET name = ?, password = ?, role = ? WHERE name = ?";
        try (Connection connection = DriverManager.getConnection(fileName);
             PreparedStatement pstmt = connection.prepareStatement(query)) {

            pstmt.setString(1, newUsername);
            pstmt.setString(2, generateSecurePassword(newPassword));
            pstmt.setString(3, newRole);
            pstmt.setString(4, originalUsername);

            int rowsUpdated = pstmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
   
    }
    
    public boolean deleteUser(String username){
        
        if (connection == null) {
            connection = initConnection();
                }
        
        String sql = "DELETE FROM Users WHERE name = ?";
        try(Connection connection = DriverManager.getConnection(fileName);
            PreparedStatement pstmt = connection.prepareStatement(sql)){
            pstmt.setString(1, username);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        }  catch(SQLException ex){  
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    /**
     * @brief decode password method
     * @param user name as type String
     * @param pass plain password of type String
     * @return true if the credentials are valid, otherwise false
     */
    public boolean validateUser(String user, String pass) throws InvalidKeySpecException, ClassNotFoundException {
        Boolean flag = false;
        try {
            
            if (connection == null) {
            connection = initConnection();
                }

            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            try(ResultSet rs = statement.executeQuery("select name, password from " + this.dataBaseTableName)){
            String inPass = generateSecurePassword(pass);
            // Let's iterate through the java ResultSet
            while (rs.next()) {
                if (user.equals(rs.getString("name")) && rs.getString("password").equals(inPass)) {
                    flag = true;
                    break;
                }
            }
            }
        } catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } 

        return flag;
    }

    private String getSaltvalue(int length) {
        StringBuilder finalval = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            finalval.append(characters.charAt(random.nextInt(characters.length())));
        }

        return new String(finalval);
    }

    /* Method to generate the hash value */
    private byte[] hash(char[] password, byte[] salt) throws InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keylength);
        Arrays.fill(password, Character.MIN_VALUE);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new AssertionError("Error while hashing a password: " + e.getMessage(), e);
        } finally {
            spec.clearPassword();
        }
    }

    public String generateSecurePassword(String password) throws InvalidKeySpecException {
        String finalval = null;

        byte[] securePassword = hash(password.toCharArray(), saltValue.getBytes());

        finalval = Base64.getEncoder().encodeToString(securePassword);

        return finalval;
    }

    /**
     * @brief get table name
     * @return table name as String
     */
    public String getTableName() {
        return this.dataBaseTableName;
    }

    /**
     * @brief print a message on screen method
     * @param message of type String
     */
    public void log(String message) {
        System.out.println(message);

    }
    
    public ObservableList<FileModel> getUserFiles(int userId) throws ClassNotFoundException, SQLException {
        
    ObservableList<FileModel> result = FXCollections.observableArrayList();
    
    try{

        if (connection == null) {
            connection = initConnection();
                }

        String query = "SELECT f.file_id, f.filename, f.file_length, f.crc32, f.file_path FROM files f JOIN acl a ON f.file_id = a.file_id WHERE a.user_id = ? AND a.can_read = 1";
        
        try(PreparedStatement statement = connection.prepareStatement(query)){
        statement.setInt(1, userId);

        try(ResultSet rs = statement.executeQuery()){
        
        while (rs.next()) {
            result.add(new FileModel(rs.getString("file_id"), rs.getString("filename"), rs.getLong("file_length"), rs.getInt("crc32"), rs.getString("file_path")));
        }
        }
        }
    } catch(SQLException e){
         System.err.println(e.getMessage());
    }
    return result;
    }
    
    
    public String addFileToUser(String filename, long fileLength, int crc32, String filePath, int userId) throws ClassNotFoundException, SQLException{
        
    String insertFileQuery = "INSERT INTO files (file_id, filename, file_length, crc32, file_path) VALUES (?, ?, ?, ?, ?)";
    String insertAclQuery = "INSERT INTO acl (file_id, user_id, can_read, can_write) VALUES (?, ?, 1, 1)";
    
    String generatedFileId = java.util.UUID.randomUUID().toString(); // Генерація унікального ID для file_id

    try {
      
        if (connection == null) {
            connection = initConnection();
                }

        try(PreparedStatement fileStmt = connection.prepareStatement(insertFileQuery)){
        

        fileStmt.setString(1, generatedFileId);
        fileStmt.setString(2, filename);
        fileStmt.setLong(3, fileLength);
        fileStmt.setInt(4, crc32);
        fileStmt.setString(5, filePath);
        fileStmt.executeUpdate();

        try(PreparedStatement aclStmt = connection.prepareStatement(insertAclQuery)){
        aclStmt.setString(1, generatedFileId);
        aclStmt.setInt(2, userId);
        aclStmt.executeUpdate();

        return generatedFileId; 
        }
        }
    } catch (SQLException e) {
        e.printStackTrace();
        throw e;
    }
    } 
    
    public void updateFileForUser(String fileID, String newFileName, long newFileLenght, String newCrc32, String newFilePath) throws ClassNotFoundException, SQLException{
        String updateFileQuery = "UPDATE files SET filename = ?, file_length = ?, crc32 = ?, file_path = ? WHERE file_id = ?";
        
        try {
            
            if (connection == null) {
            connection = initConnection();
                }

            try(PreparedStatement fileStm = connection.prepareStatement(updateFileQuery)){

            fileStm.setString(1, newFileName);
            fileStm.setLong(2, newFileLenght);
            fileStm.setString(3, newCrc32);
            fileStm.setString(4, newFilePath);
            fileStm.setString(5, fileID);
            fileStm.executeUpdate();
            }
            
    } catch (SQLException e) {
        e.printStackTrace();
    }
    
    }
    
    public void deleteFileForUser(String file_id, int userId) throws ClassNotFoundException{
        String deleteFileQuery = "DELETE FROM files WHERE file_id = ?";
        String deleteAclQuery = "DELETE FROM acl WHERE file_id = ? AND user_id = ?";
        String deleteChunksQuery = "DELETE FROM chunks WHERE file_id = ?";
        
        try {
            
            if (connection == null) {
            connection = initConnection();
                }

            try(PreparedStatement aclStmt = connection.prepareStatement(deleteAclQuery)){
            
            
            aclStmt.setString(1, file_id);
            aclStmt.setInt(2, userId);
            aclStmt.executeUpdate();

            try(PreparedStatement fileStmt = connection.prepareStatement(deleteFileQuery)){
            fileStmt.setString(1, file_id);
            fileStmt.executeUpdate();
            
            try(PreparedStatement chunkStmt = connection.prepareStatement(deleteChunksQuery)){
            chunkStmt.setString(1, file_id);
            chunkStmt.executeUpdate();
            
                    }
                }
            }
            
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void addChunkMetaData(String chunkId, String fileId) throws ClassNotFoundException{
        String sql = "INSERT INTO chunks (chunk_id, file_id) VALUES (?, ?)";
        
        try {
            
            if (connection == null) {
            connection = initConnection();
                }
            
            try(PreparedStatement pstmt = connection.prepareStatement(sql)){
            pstmt.setString(1, chunkId);
            pstmt.setString(2, fileId);
            pstmt.executeUpdate();
            
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void addPermissionToFile(String fileId, int userId, boolean canRead, boolean canWrite) throws ClassNotFoundException, SQLException{
        
        String checkSql = "SELECT * FROM acl WHERE file_id = ? AND user_id = ?";
        String insertSql = "INSERT INTO acl (file_id, user_id, can_read, can_write) VALUES (?, ?, ?, ?)";
        String updateSql = "UPDATE acl SET can_read = ?, can_write = ? WHERE file_id = ? AND user_id = ?";
        
        
        try {
      
            if (connection == null) {
            connection = initConnection();
                }
            
           try(PreparedStatement pstmtSelect = connection.prepareStatement(checkSql)){
               
            pstmtSelect.setString(1, fileId);
            pstmtSelect.setInt(2, userId);
            try(ResultSet rs = pstmtSelect.executeQuery()){
            
            if(rs.next()){
               
                try (PreparedStatement pstmtUpdate = connection.prepareStatement(updateSql)){
                pstmtUpdate.setBoolean(1, canRead);
                pstmtUpdate.setBoolean(2, canWrite);
                pstmtUpdate.setString(3, fileId);
                pstmtUpdate.setInt(4, userId);
                pstmtUpdate.executeUpdate();
            }
            } else {
                    
                try (PreparedStatement pstmtInsert = connection.prepareStatement(insertSql)){
                pstmtInsert.setString(1, fileId);
                pstmtInsert.setInt(2, userId);
                pstmtInsert.setBoolean(3, canRead);
                pstmtInsert.setBoolean(4, canWrite);
                pstmtInsert.executeUpdate();
                }
            }
            }
           }
            
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }
    
    public boolean checkPermissionForUser(String fileId, int userId) throws ClassNotFoundException{
        String sql = "SELECT can_write FROM acl WHERE file_id = ? AND user_id = ?";
        try {

                if (connection == null) {
                connection = initConnection();
                    }
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, fileId);
            pstmt.setInt(2, userId);

             ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("can_write"); 
            } else {
                return false; 
            }

            }catch (SQLException e) {
                    e.printStackTrace();
                }
            return false;
    }
    
    public List<String> getChunksForFile(String fileId) throws SQLException, ClassNotFoundException {
            
        List<String> chunks = new ArrayList<>();
        String query = "SELECT chunk_id FROM chunks WHERE file_id = ?";

            if (connection == null) {
                connection = initConnection();
            }

            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, fileId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                    String chunkId = rs.getString("chunk_id");
                    System.out.println("Retrieved chunk: " + chunkId);  
                    chunks.add(chunkId);
                }
            }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            return chunks;
        }
    }


