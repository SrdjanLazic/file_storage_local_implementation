package rs.edu.raf.storage;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StorageModel {

    private File usersJSON;
    private File configJSON;
    private File storageFolder;
    private String downloadFolder;
    private String rootDirectory;
    private User superuser;
    private User currentUser;
    private long storageSize;
    private int maxNumberOfFiles;
    private int currNumberOfFiles;
    private List<User> userList = new ArrayList<>();
    private List<String> unsupportedExtensions = new ArrayList<>();
    private Map<File, Integer> maxNumberOfFilesInDirectory = new HashMap<>();

    public StorageModel(User user, String path) {

        // Provera da li folder na prosledjenoj adresi vec postoji:
        File storageFolder = new File(path);
        if(!storageFolder.exists())
            storageFolder.mkdir();

        // Inicijalizacija parametara:
        this.storageFolder = storageFolder;
        this.currNumberOfFiles = 0;
        this.rootDirectory = path;
        this.superuser = user;
        this.currentUser = user;
        this.userList.add(user);

        // Kreiranje download foldera:
        File downloadFolder = new File(getRootDirectory() + "/Download");
        if(!downloadFolder.exists()) {
            downloadFolder.mkdir();
            this.downloadFolder = getRootDirectory() + "/Download";
        }

        // Dodavanje config.json i users.json fajlova u root:
        this.usersJSON = new File(rootDirectory + "/users.json"); // TODO: kako da append-ujemo vise usera? Uvek mi overwrite-uje
        this.configJSON = new File(rootDirectory + "/config.json");
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(usersJSON, user);
            mapper.writeValue(configJSON, this);
            currNumberOfFiles += 2; // inkrementiramo trenutni broj fajlova u skladistu
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getRootDirectory() {
        return rootDirectory;
    }

    public long getStorageSize() {
        return storageSize;
    }

    public List<String> getUnsupportedExtensions() {
        return unsupportedExtensions;
    }

    public int getMaxNumberOfFiles() {
        return maxNumberOfFiles;
    }

    public void setStorageSize(long storageSize) {
        this.storageSize = storageSize;
    }

    public void setMaxNumberOfFiles(int maxNumberOfFiles) {
        this.maxNumberOfFiles = maxNumberOfFiles;
    }

    public void setUnsupportedExtensions(List<String> unsupportedExtensions) {
        this.unsupportedExtensions = unsupportedExtensions;
    }

    public Map<File, Integer> getMaxNumberOfFilesInDirectory() {
        return maxNumberOfFilesInDirectory;
    }

    public void setMaxNumberOfFilesInDirectory(Map<File, Integer> maxNumberOfFilesInDirectory) {
        this.maxNumberOfFilesInDirectory = maxNumberOfFilesInDirectory;
    }

    public List<User> getUserList() {
        return userList;
    }

    public void setUserList(List<User> userList) {
        this.userList = userList;
    }

    public int getCurrNumberOfFiles() {
        return currNumberOfFiles;
    }

    public void setCurrNumberOfFiles(int currNumberOfFiles) {
        this.currNumberOfFiles = currNumberOfFiles;
    }

    public void incrementCounter(){
        this.currNumberOfFiles++;
    }

    public String getDownloadFolder() {
        return downloadFolder;
    }

    public void setDownloadFolder(String downloadFolder) {
        this.downloadFolder = downloadFolder;
    }

    public void setRootDirectory(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public File getStorageFolder() {
        return storageFolder;
    }

    public void setStorageFolder(File storageFolder) {
        this.storageFolder = storageFolder;
    }
}

