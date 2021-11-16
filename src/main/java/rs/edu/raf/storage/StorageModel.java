package rs.edu.raf.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import rs.edu.raf.storage.enums.Privileges;
import rs.edu.raf.storage.user_management.User;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class StorageModel {

    //TODO: privilegije za foldere
    private String usersJSON;
    private String configJSON;
    private File storageFolder;
    private String downloadFolder;
    private String rootDirectory;
    private User superuser;
    private User currentUser;
    private long storageSizeLimit;
    private boolean storageSizeLimitSet = false;
    private boolean maxNumberOfFilesInDirectorySet = false;
    private long currentStorageSize;
    private List<User> userList = new ArrayList<>();
    private List<String> unsupportedExtensions = new ArrayList<>();
    private Map<String, Integer> maxNumberOfFilesInDirectory = new HashMap<>();
    // TODO:
    // D:/storage/folder1 : {DELETE, CREATE, VIEW}
    // folder2 : {VIEW}
    // if(!currentStorage.getFolderPrivileges.get(folderName).contains(Privileges.VIEW))
        // throw new InsufficientPrivilegesException();
    private Map<String, Set<Privileges>> folderPrivileges = new HashMap<>();
    private ObjectMapper mapper = new ObjectMapper();

    public StorageModel(){

    }

    public StorageModel(User user, String path) {

        // Provera da li folder na prosledjenoj adresi vec postoji:
        File storageFolder = new File(path);
        if(!storageFolder.exists())
            storageFolder.mkdir();

        // Inicijalizacija parametara:
        this.storageFolder = storageFolder;
        this.rootDirectory = path;
        this.superuser = user;
        this.currentUser = user;
        this.userList.add(user);
        this.usersJSON = rootDirectory + "/users.json";
        this.configJSON = rootDirectory + "/config.json";

        // Kreiranje download foldera:
        File downloadFolder = new File(getRootDirectory() + "/Download");

        if(!downloadFolder.exists()) {
            downloadFolder.mkdir();
            this.downloadFolder = getRootDirectory() + "/Download";
        }


        try {
            mapper.writeValue(new File(usersJSON), user);
            mapper.writeValue(new File(configJSON), this);
            updateConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getRootDirectory() {
        return rootDirectory;
    }

    public long getStorageSizeLimit() {
        return storageSizeLimit;
    }

    public List<String> getUnsupportedExtensions() {
        return unsupportedExtensions;
    }

    public void setStorageSizeLimit(long storageSizeLimit) {
        this.storageSizeLimit = storageSizeLimit;
    }

    public void setUnsupportedExtensions(List<String> unsupportedExtensions) {
        this.unsupportedExtensions = unsupportedExtensions;
    }

    public Map<String, Integer> getMaxNumberOfFilesInDirectory() {
        return maxNumberOfFilesInDirectory;
    }

    public void setMaxNumberOfFilesInDirectory(Map<String, Integer> maxNumberOfFilesInDirectory) {
        this.maxNumberOfFilesInDirectory = maxNumberOfFilesInDirectory;
    }

    public List<User> getUserList() {
        return userList;
    }

    public void setUserList(List<User> userList) {
        this.userList = userList;
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

    public User getSuperuser() {
        return superuser;
    }

    public void setSuperuser(User superuser) {
        this.superuser = superuser;
    }

    public void updateConfig(){
        try {
            mapper.writeValue(new File(configJSON), this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateUsers(){
        try {
            mapper.writeValue(new File(usersJSON), userList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public boolean isMaxNumberOfFilesInDirectorySet() {
        return maxNumberOfFilesInDirectorySet;
    }

    public void setMaxNumberOfFilesInDirectorySet(boolean maxNumberOfFilesInDirectorySet) {
        this.maxNumberOfFilesInDirectorySet = maxNumberOfFilesInDirectorySet;
    }

    @Override
    public String toString() {
        return "StorageModel{" +
                "usersJSON=" + usersJSON +
                ", configJSON=" + configJSON +
                ", storageFolder=" + storageFolder +
                ", downloadFolder='" + downloadFolder + '\'' +
                ", rootDirectory='" + rootDirectory + '\'' +
                ", superuser=" + superuser +
                ", currentUser=" + currentUser +
                ", storageSize=" + storageSizeLimit +
                ", userList=" + userList +
                ", unsupportedExtensions=" + unsupportedExtensions +
                ", maxNumberOfFilesInDirectory=" + maxNumberOfFilesInDirectory +
                ", mapper=" + mapper +
                '}';
    }

    public String getUsersJSON() {
        return usersJSON;
    }

    public void setUsersJSON(String usersJSON) {
        this.usersJSON = usersJSON;
    }

    public String getConfigJSON() {
        return configJSON;
    }

    public void setConfigJSON(String configJSON) {
        this.configJSON = configJSON;
    }

    public long getCurrentStorageSize() {
        return currentStorageSize;
    }

    public void setCurrentStorageSize(long currentStorageSize) {
        this.currentStorageSize = currentStorageSize;
    }

    public boolean isStorageSizeLimitSet() {
        return storageSizeLimitSet;
    }

    public void setStorageSizeLimitSet(boolean storageSizeLimitSet) {
        this.storageSizeLimitSet = storageSizeLimitSet;
    }

    public Map<String, Set<Privileges>> getFolderPrivileges() {
        return folderPrivileges;
    }

    public void setFolderPrivileges(Map<String, Set<Privileges>> folderPrivileges) {
        this.folderPrivileges = folderPrivileges;
    }
}

