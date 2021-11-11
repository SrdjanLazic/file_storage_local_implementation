package rs.edu.raf.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import rs.edu.raf.storage.comparator.FileModifiedDateComparator;
import rs.edu.raf.storage.comparator.FileNameComparator;
import rs.edu.raf.storage.enums.Operations;
import rs.edu.raf.storage.enums.Privileges;
import rs.edu.raf.storage.exceptions.FileNotFoundException;
import rs.edu.raf.storage.exceptions.InsufficientPrivilegesException;
import rs.edu.raf.storage.exceptions.InvalidExtensionException;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class LocalFileStorageImplementation implements FileStorage {

    static {
        StorageManager.registerStorage(new LocalFileStorageImplementation());
    }

    // TODO: Exceptioni svuda za greske!

    private List<StorageModel> storageModelList = new ArrayList<>();
    private StorageModel currentStorage;

    @Override
    public void createFolder(String path, String folderName) {

        if(!currentStorage.getCurrentUser().getPrivileges().contains(Privileges.WRITE)){
            throw new InsufficientPrivilegesException();
        }

        // Kreiranje pomocu {} patterna:
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
    public void createFile(String path, String filename) throws InvalidExtensionException {

        // Provera privilegija:
        if(!currentStorage.getCurrentUser().getPrivileges().contains(Privileges.WRITE)){
            throw new InsufficientPrivilegesException();
        }

        // Provera da li prosledjeni fajl ima ekstenziju koja nije dozvoljena:
        if(checkExtensions(filename)){
            throw new InvalidExtensionException();
        }

        // Kreiranje fajla u datom pathu
        // Metoda Files.createDirectories() kraira sve potrebne nadfoldere ako ne postoje
        // Npr. ako je prosledjeno /folder/folder1/folder2, napravice sva tri foldera ako ne postoje, a onda ce smestiti fajl u folder2
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

        // Provera privilegija:
        if(!currentStorage.getCurrentUser().getPrivileges().contains(Privileges.WRITE)){
            throw new InsufficientPrivilegesException();
        }

        File newFolder = new File(getCurrentStorage().getRootDirectory() + "/" + folderName);
        newFolder.mkdir();
    }

    @Override
    public void createFile(String filename) throws InvalidExtensionException {

        // Provera privilegija:
        if(!currentStorage.getCurrentUser().getPrivileges().contains(Privileges.WRITE)){
            throw new InsufficientPrivilegesException();
        }

        // Provera da li je dozvoljena ekstenzija fajla:
        if(checkExtensions(filename)){
            throw new InvalidExtensionException();
        }

        // Kreiranje fajla:
        File newFile = new File(currentStorage.getRootDirectory() + "/" + filename);
        try {
            newFile.createNewFile();
            currentStorage.incrementCounter(); // inkrementiranje counter-a za broj fajlova
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(String path) throws FileNotFoundException, InsufficientPrivilegesException {

        // Provera da li trenutni korisnik skladista ima privilegiju za brisanje fajlova
        if(!getCurrentStorage().getCurrentUser().getPrivileges().contains(Privileges.DELETE)){
            throw new InsufficientPrivilegesException();
        }

        File file = new File(path);

        // Provera da li postoji fajl na prosledjenoj putanji:
        if(!file.exists()) {
            throw new FileNotFoundException();
        }

        // Brisanje fajla:
        String type = file.isDirectory() ? "Folder" : "Fajl";
		boolean deleted = file.delete();

		if(deleted)
			System.out.println(type + " je uspesno obrisan.");
		else
            System.out.println("\nBrisanje nije uspelo. Proverite da li ste ispravno napisali naziv fajla i ekstenziju.");
    }

    @Override
    public void move(String source, String destination) {

        // Provera privilegija:
        if(!getCurrentStorage().getCurrentUser().getPrivileges().contains(Privileges.WRITE)){
            throw new InsufficientPrivilegesException();
        }

        Path result = null;

        try {
            result = Files.move(Paths.get(source), Paths.get(destination));
        } catch (NoSuchFileException e1) {
            System.out.println("Greska! Navedeni fajl ne postoji."); // TODO throw new ...
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(result != null)
			System.out.println("Fajl je uspesno premesten.");
		else
			System.out.println("Fajl nije premesten.");
    }

    // TODO: list nad path-om? Preraditi getFileList() metodu za to, tako da prima kao argument path gde izlistavamo fajlove

    @Override
    public void put(String sources, String destination) {
        Path result = null;

        try {
            result = Files.copy(Paths.get(sources), Paths.get(destination), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        } catch (NoSuchFileException e1) {
            System.out.println("Greska! Navedeni fajl ne postoji.");
            return; // TODO: throw new ...
        }catch (IOException e) {
            e.printStackTrace();
        }

        if(result != null)
            System.out.println("Fajl je uspesno prekopiran.");
        else
            System.out.println("Fajl nije prekopiran.");
    }

    @Override
    public void list() {

        // Provera privilegija:

        // Uzimanje fajl liste u root-u:
        List<File> fileList = getFileList(currentStorage.getRootDirectory());
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

        // Provera privilegija:
        if(!currentStorage.getCurrentUser().getPrivileges().contains(Privileges.READ)){
            throw new InsufficientPrivilegesException();
        }

        String type;
        List<File> fileList = getFileList(currentStorage.getRootDirectory());

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

        // Provera privilegija se vec desava u move metodi
        move(currentStorage.getRootDirectory() + path, currentStorage.getDownloadFolder());
    }

    @Override
    public void initializeStorage(String path) {

        boolean isStorage = false;
        Scanner scanner = new Scanner(System.in);
        File file = new File(path);
        File[] filesInRoot = file.listFiles();

        // Provera da li je prosledjena putanja vec skladiste:
        if(filesInRoot != null) {
            for (File f : filesInRoot) {
                if (f.getName().contains("users.json") || f.getName().contains("config.json")) {
                    isStorage = true;
                }
            }
        }

        // Ako jeste skladiste, procitaj user i config fajlove
        if(isStorage){
            System.out.println("Direktorijum je vec skladiste. Unesite username i password kako biste se konektovali na skladiste.");
            System.out.println("Username:");
            String username = scanner.nextLine();
            System.out.println("Password:");
            String password = scanner.nextLine();

            ObjectMapper objectMapper = new ObjectMapper();
            User user = null;
            try {
                user = objectMapper.readValue(new File(path + "/users.json"), User.class);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Provera kredencijala - uporedjivanje prosledjenih username i password-a i procitanih iz users.json fajla
            if(username.equalsIgnoreCase(user.getUsername()) && password.equalsIgnoreCase(user.getPassword())){
                try {
                    // Ako se podaci User-a match-uju, procitaj config, setuj trenutni storage i dodaj skladiste u listu skladista
                    StorageModel storageModel = objectMapper.readValue(new File(path + "/config.json"), StorageModel.class);
                    this.storageModelList.add(storageModel);
                    setCurrentStorage(storageModel);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Neispravan username ili password!"); // TODO: new incorrect password exception
                return;
            }
            // Pravimo novo skladiste, prilikom kreiranja User-u koji ga je kreirao dodeljujemo sve privilegije
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
                user.setPrivileges(Set.of(Privileges.values()));
                StorageModel storageModel = new StorageModel(user, path);
                this.storageModelList.add(storageModel);
                setCurrentStorage(storageModel);
            } else {
                System.out.println("Skladiste nije kreirano.");
                return;
            }

        }
    }

    @Override
    public void limitNumberOfFiles(int i, String path) throws InsufficientPrivilegesException, FileNotFoundException {

        // Provera da li konfiguraciju vrsi superuser:
        if(currentStorage.getSuperuser() != currentStorage.getCurrentUser()){
            throw new InsufficientPrivilegesException();
        }

        File directory = new File(getCurrentStorage().getRootDirectory() + "/" + path);

        // Provera da li postoji prosledjeni folder u trenutnom aktivnom skladistu
        if(!directory.exists()){
            throw new FileNotFoundException();
        }

        // Dodavanje novog para direktorijum-brFajlova u HashMap trenutnog skladista
        currentStorage.getMaxNumberOfFilesInDirectory().put(directory, Integer.valueOf(i));

    }

    @Override
    public void limitStorageSize(long l) throws InsufficientPrivilegesException {

        // Provera da li konfiguraciju vrsi superuser:
        if(currentStorage.getSuperuser() != currentStorage.getCurrentUser()){
            throw new InsufficientPrivilegesException();
        }

        currentStorage.setStorageSize(l);
    }

    @Override
    public void restrictExtension(String s) throws InsufficientPrivilegesException {

        // Provera da li konfiguraciju vrsi superuser:
        if(currentStorage.getCurrentUser() != currentStorage.getSuperuser()){
            throw new InsufficientPrivilegesException();
        }

        currentStorage.getUnsupportedExtensions().add(s);
    }

    @Override
    public void addNewUser(User abstractUser, Set<Privileges> set) {

        // Provera da li konfiguraciju vrsi superuser:
        if(currentStorage.getCurrentUser() != currentStorage.getSuperuser()){
            throw new InsufficientPrivilegesException();
        }

        currentStorage.getUserList().add(abstractUser);
    }

    @Override
    public void disconnectUser(User abstractUser) {

    }



    // Pomocna metoda za proveravanje ekstenzije prilikom dodavanja fajla u skladiste
    private boolean checkExtensions(String filename){
        boolean found = false;
        for(String extension: currentStorage.getUnsupportedExtensions()){
            if(filename.contains(extension)){
                found = true;
            }
        }
        return found;
    }

    // Listanje fajlova u root direktorijumu:
    private List<File> getFileList(String path) {
        File directory = new File(path);
        File[] fileList = directory.listFiles();
        return Arrays.asList(fileList);
    }

    public void setCurrentStorage(StorageModel currentStorage) {
        this.currentStorage = currentStorage;
    }

    public StorageModel getCurrentStorage() {
        return currentStorage;
    }

}
