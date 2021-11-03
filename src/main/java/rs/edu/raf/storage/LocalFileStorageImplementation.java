package rs.edu.raf.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class LocalFileStorageImplementation implements FileStorage {

    private String downloadFolder = "/Download";
    // TODO: treba dodati polje koje drzi root direktorijum skladista, i onda sve putanje promeniti tako da su relativne u odnosu na root
    private List<User> users;
    private List<StorageModel> storageModelList = new ArrayList<>();
    private StorageModel currentStorage;

    private List<File> getFileList() {
        File directory = new File(currentStorage.getRootDirectory());
        File[] fileList = directory.listFiles();
        return Arrays.asList(fileList);
    }

    @Override
    public void createFolder(String path, String folderName) {

        if(folderName.contains("{") && folderName.contains("}")) {
            String filenameBase;
            int firstBrace = folderName.indexOf("{"), firstNum, secondNum;
            filenameBase = folderName.substring(0, firstBrace);
            System.out.println(filenameBase);
            folderName = folderName.replaceAll("[^0-9]+", " ");
            firstNum = Integer.parseInt(Arrays.asList(folderName.trim().split(" ")).get(0));
            secondNum = Integer.parseInt(Arrays.asList(folderName.trim().split(" ")).get(1));

            for (int i = firstNum; i <= secondNum; i++) {
                File folder = new File(getCurrentStorage().getRootDirectory() + "/" + path + "/" + filenameBase + i);
                folder.mkdir();
            }
        }
        else {
            String fullPath = getCurrentStorage().getRootDirectory() + "/" + path + "/" + folderName;
            File newFolder = new File(fullPath);
            newFolder.mkdir();
        }
    }

    @Override
    public void createFile(String path, String filename) {

        if(checkExtensions(filename)){
            System.out.println("Greska! Nije moguce cuvati fajl u ovoj ekstenziji.");
            return;
        }

        //File newFile = new File(currentStorage.getRootDirectory() + path + "/" + filename);
        //System.out.println(newFile.getPath());
        try {
            Files.createDirectories(Paths.get(currentStorage.getRootDirectory() + path));
            File newFile = new File(currentStorage.getRootDirectory() + path + "/" + filename);
            newFile.createNewFile();
            currentStorage.incrementCounter();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void createFolder(String folderName) {
        File newFolder = new File(getCurrentStorage().getRootDirectory() + "/" + folderName);
        newFolder.mkdir();
        currentStorage.incrementCounter();
    }

    @Override
    public void createFile(String filename) {

        if(checkExtensions(filename)){
            System.out.println("Greska! Nije moguce cuvati fajl u ovoj ekstenziji.");
            return;
        }

        File newFile = new File(currentStorage.getRootDirectory() + "/" + filename);
        try {
            newFile.createNewFile();
            currentStorage.incrementCounter();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(String path) {
        File file = new File(path);

        if(!file.exists()) {
            System.out.println("Greska! Navedeni fajl ne postoji.");
            return;
        }

        String type = file.isDirectory() ? "Folder" : "Fajl";
		boolean deleted = file.delete();

		if(deleted)
			System.out.println(type + " je uspesno obrisan.");
		else
            System.out.println("\nBrisanje nije uspelo. Proverite da li ste ispravno napisali naziv fajla i ekstenziju.");
    }

    @Override
    public void move(String source, String destination) {

        Path result = null;

        try {
            result = Files.move(Paths.get(source), Paths.get(destination));
        } catch (NoSuchFileException e1) {
            System.out.println("Greska! Navedeni fajl ne postoji.");
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(result != null)
			System.out.println("Fajl je uspesno premesten.");
		else
			System.out.println("Fajl nije premesten.");
    }

    @Override
    public void list() {
        List<File> fileList = getFileList();
        String type;

        System.out.println("\nLista fajlova i foldera u skladistu:");
        System.out.println("-----------------------------------\n");
        for (File file: fileList){
            type = file.isDirectory() ? "DIR" : "FILE";
            System.out.println(file.getName() + " --- " + file.length() / (1024 * 1024) + " MB " + " --- " + type);
        }
    }

    @Override
    public void list(String argument, Operations operation) {

        String type;
        List<File> fileList = getFileList();

        if (operation == Operations.FILTER_EXTENSION) {
            String extension = argument;
            System.out.println("\nLista fajlova sa datom ekstenzijom u skladistu:");
            System.out.println("------------------------------------------------\n");
            for (File file : fileList) {
                if (file.getName().endsWith(extension))
                    System.out.println(file.getPath());
            }
        } else if (operation == Operations.FILTER_FILENAME) {
            String filename = argument;
            System.out.println("\nLista fajlova ciji nazivi sadrze dati tekst:");
            System.out.println("----------------------------------------------\n");
            for (File file : fileList) {
                if (file.getName().contains(filename))
                    System.out.println(file.getPath());
            }
        } else if (operation == Operations.SORT_BY_NAME_ASC || operation == Operations.SORT_BY_NAME_DESC) {
            String order;
            if(operation == Operations.SORT_BY_NAME_ASC) {
                fileList.sort(new FileNameComparator());
                order = " rastuce ";
            }
            else {
                fileList.sort(new FileNameComparator().reversed());
                order = " opadajuce ";
            }

            System.out.println("\nLista fajlova i foldera sortirana" + order + "po nazivu:");
            System.out.println("-----------------------------------------------------\n");
            for (File file : fileList) {
                type = file.isDirectory() ? "DIR" : "FILE";
                System.out.println(file.getName() + " --- " + file.length() / (1024 * 1024) + " MB " + " --- " + type);
            }
        } else if (operation == Operations.SORT_BY_DATE_MODIFIED_ASC || operation == Operations.SORT_BY_DATE_MODIFIED_DESC) {
            String order;
            if(operation == Operations.SORT_BY_DATE_MODIFIED_ASC) {
                fileList.sort(new FileModifiedDateComparator());
                order = " rastuce ";
            }
            else {
                fileList.sort(new FileModifiedDateComparator().reversed());
                order = " opadajuce ";
            }
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

            System.out.println("\nLista fajlova i foldera sortirana" + order + "po datumu izmene:");
            System.out.println("-----------------------------------------------------\n");
            for (File file : fileList) {

                type = file.isDirectory() ? "DIR" : "FILE";
                System.out.println(file.getName() + " --- " + file.length() / (1024 * 1024) + " MB" + " --- " + type + " --- " + sdf.format(file.lastModified()));
            }
        // TODO: treba videti sta sa ovim, po onome sto sam citao na netu,
        //  Linux ne pise uopste ovu vrednost, tako da ja ne znam da li ovo moze ikako da se uradi a da radi svuda
        } else if (operation == Operations.SORT_BY_DATE_CREATED) {

        }
    }

    @Override
    public void get(String path) {
        move(currentStorage.getRootDirectory() + path, currentStorage.getDownloadFolder());
    }

    @Override
    public void initializeStorage(String path) {
        Boolean isStorage = false;
        Scanner scanner = new Scanner(System.in);
        File file = new File(path);
        File[] filesInRoot = file.listFiles();

        if(filesInRoot != null) {
            for (File f : filesInRoot) {
                if (f.getName().contains("users.json") || f.getName().contains("config.json")) {
                    isStorage = true;
                }
            }
        }

        if(isStorage){
            System.out.println("Direktorijum je vec skladiste. Unesite username i password kako biste se konektovali na skladiste.");
            System.out.println("Username:");
            String username = scanner.nextLine();
            System.out.println("Password:");
            String password = scanner.nextLine();

            User user = new User(username, password);

            // TODO: logovanje postojeceg usera u skladiste


            //this.currentStorage.checkUser(username, password);
//            User user = new User(username, password);
//            StorageModel storageModel = new StorageModel(user, path);
//            this.setCurrentStorage(storageModel);
        } else {
            System.out.println("Direktorijum nije skladiste. Da li zelite da kreirate novo skladiste? Unesite DA ili NE");
            String choice = scanner.nextLine();

            if(choice.equalsIgnoreCase("DA")){
                System.out.println("Unesite username i password kako biste kreirali skladiste.");
                System.out.println("Username:");
                String username = scanner.nextLine();
                System.out.println("Password:");
                String password = scanner.nextLine();

                User user = new User(username, password);
                StorageModel storageModel = new StorageModel(user, path);
                this.storageModelList.add(storageModel);
                setCurrentStorage(storageModel);
            } else {
                return;
            }

        }
    }

    public boolean checkExtensions(String filename){
        boolean found = false;
        for(String extension: currentStorage.getUnsupportedExtensions()){
            if(filename.contains(extension)){
                System.out.println("Nepodrzana ekstenzija!");
                found = true;
            }
        }
        return found;
    }

    public void setCurrentStorage(StorageModel currentStorage) {
        this.currentStorage = currentStorage;
    }

    public StorageModel getCurrentStorage() {
        return currentStorage;
    }

    public String getDownloadFolder() {
        return downloadFolder;
    }
}
