/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.google.gson.Gson;
import javafx.collections.ObservableList;
/**
 *
 * @author ntu-user
 */
public class DatabaseSynchronizer {
    private final DB remoteDB;     
    private final LocalDB localDB;  

    public DatabaseSynchronizer(DB remoteDB, LocalDB localDB) {
        this.remoteDB = remoteDB;
        this.localDB = localDB;
    }
    
    
    public void run() {
        try {
            ObservableList<User> remoteUsers = remoteDB.getUserData();
            ObservableList<FileModel> remoteFiles = remoteDB.getFiles(); 
            List<AclEntry> remoteAclEntries = remoteDB.getAclEntries();  

            localDB.syncUsers(remoteUsers);
            localDB.syncFiles(remoteFiles);
            localDB.syncAcl(remoteAclEntries);

            System.out.println("Database synchronization completed at " + System.currentTimeMillis());
            
        } catch (SQLException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }
}
