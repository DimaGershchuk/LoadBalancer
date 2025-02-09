/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
/** 
 *
 * @author ntu-user
 */
public class LocalDB {
    
    private String sqliteUrl = "jdbc:sqlite:comp20081.db";
    private Connection connection;

    public LocalDB() {
        try {
            connection = DriverManager.getConnection(sqliteUrl);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    // Метод для оновлення локальної таблиці users
    public void syncUsers(List<User> remoteUsers) throws SQLException {
        String deleteQuery = "DELETE FROM users";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(deleteQuery);
        }
        String insertQuery = "INSERT INTO users (id, name, password, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertQuery)) {
            for (User user : remoteUsers) {
                pstmt.setInt(1, user.getId());
                pstmt.setString(2, user.getUser());
                pstmt.setString(3, user.getPass());
                pstmt.setString(4, user.getRole());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    // Аналогічні методи можна реалізувати для local_files та local_acl
    public void syncFiles(List<FileModel> remoteFiles) throws SQLException {
        String deleteQuery = "DELETE FROM local_files";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(deleteQuery);
        }
        String insertQuery = "INSERT INTO local_files (file_id, filename, file_path, file_size, crc32) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertQuery)) {
            for (FileModel file : remoteFiles) {
                pstmt.setString(1, file.getFileId());
                pstmt.setString(2, file.getFilename());
                pstmt.setString(3, file.getFilePath());
                pstmt.setLong(4, file.getFileLength());
                pstmt.setInt(5, file.getCrc32());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }
    
    public void syncAcl(List<AclEntry> remoteAclEntries) throws SQLException {
        String deleteQuery = "DELETE FROM local_acl";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(deleteQuery);
        }
        String insertQuery = "INSERT INTO local_acl (file_id, user_id, can_read, can_write) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertQuery)) {
            for (AclEntry entry : remoteAclEntries) {
                pstmt.setString(1, entry.getFileId());
                pstmt.setInt(2, entry.getUserId());
                pstmt.setBoolean(3, entry.isCanRead());
                pstmt.setBoolean(4, entry.isCanWrite());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }
}
