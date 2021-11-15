package rs.edu.raf.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import rs.edu.raf.storage.comparator.FileModifiedDateComparator;
import rs.edu.raf.storage.comparator.FileNameComparator;
import rs.edu.raf.storage.enums.Operations;
import rs.edu.raf.storage.enums.Privileges;
import rs.edu.raf.storage.exceptions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

// TODO: privilegija za neki folder

public class LocalFileStorageImplementation implements FileStorage {

    static {
        StorageManager.registerStorage(new LocalFileStorageImplementation());
    }

    private List<StorageModel> storageModelList = new ArrayList<>();
    private StorageModel currentStorage;

    @Override
    public void createFolder(String path, String ...folderNames) {

        // Provera privilegija:
        if(!currentStorage.getCurrentUser().getPrivileges().contains(Privileges.WRITE)){
            throw new InsufficientPrivilegesException();
        }
        for(String folderName: folderNames) {
            System.out.println("\nFOLDER NAMES: " + folderName);
            // Kreiranje pomocu {} patterna:
            if (folderName.contains("{") && folderName.contains("}")) {
                String folderNameBase;
                int firstBrace = folderName.indexOf("{"), firstNum, secondNum;
                folderNameBase = folderName.substring(0, firstBrace);
                System.out.println(folderNameBase);
                folderName = folderName.replaceAll("[^0-9]+", " ");
                firstNum = Integer.parseInt(Arrays.asList(folderName.trim().split(" ")).get(0));
                secondNum = Integer.parseInt(Arrays.asList(folderName.trim().split(" ")).get(1));

                for (int i = firstNum; i <= secondNum; i++) {
                    File folder = new File(currentStorage.getRootDirectory() + "/" + path + "/" + folderNameBase + i);
                    folder.mkdir();
                }
            } else {
                String fullPath = currentStorage.getRootDirectory() + "/" + path + "/" + folderName;
                File newFolder = new File(fullPath);
                newFolder.mkdir();
            }
        }
    }

    @Override
    public void createFile(String path, String ...filenames) throws InvalidExtensionException {

        String fullPath = currentStorage.getRootDirectory() + "/" + path;

        // Provera privilegija:
        if(!currentStorage.getCurrentUser().getPrivileges().contains(Privileges.WRITE)){
            throw new InsufficientPrivilegesException();
        }

        // Provera da li cemo prekoraciti broj fajlova u nekom folderu:
        // Prvo proverava da li u HashMap-u postoji folder u kojem se kreira novi fajl
        // Ako postoji, proverava da li (trenutni broj fajlova u tom folderu + 1) prekoracuje maksimalan definisani iz HashMap-a
        if(currentStorage.getMaxNumberOfFilesInDirectory().containsKey(currentStorage.getRootDirectory() + "/" + path)){
            int numberOfFiles = new File(currentStorage.getRootDirectory() + "/" + path).listFiles().length;
            if(numberOfFiles + 1 > currentStorage.getMaxNumberOfFilesInDirectory().get(fullPath))
                throw new FileLimitExceededException();
        }
        for(String filename: filenames) {
            // Provera da li prosledjeni fajl ima ekstenziju koja nije dozvoljena:
            if (checkExtensions(filename)) {
                throw new InvalidExtensionException();
            }


            // Kreiranje fajla u datom pathu
            // Metoda Files.createDirectories() kreira sve potrebne nadfoldere ako ne postoje
            // Npr. ako je prosledjeno /folder/folder1/folder2, napravice sva tri foldera ako ne postoje, a onda ce smestiti fajl u folder2
            try {
                System.out.println("NEW FILE: " + currentStorage.getRootDirectory() + path + "/" + filename);
                System.out.println("FULL PATH: " + fullPath);
                Files.createDirectories(Paths.get(fullPath));
                File newFile = new File(currentStorage.getRootDirectory() + "/" + path + "/" + filename);
                newFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void createFolder(String folderName) {

        // Provera privilegija:
        if(!currentStorage.getCurrentUser().getPrivileges().contains(Privileges.WRITE)){
            throw new InsufficientPrivilegesException();
        }
        if (folderName.contains("{") && folderName.contains("}")) {
            String folderNameBase;
            int firstBrace = folderName.indexOf("{"), firstNum, secondNum;
            folderNameBase = folderName.substring(0, firstBrace);
            System.out.println(folderNameBase);
            folderName = folderName.replaceAll("[^0-9]+", " ");
            firstNum = Integer.parseInt(Arrays.asList(folderName.trim().split(" ")).get(0));
            secondNum = Integer.parseInt(Arrays.asList(folderName.trim().split(" ")).get(1));

            for (int i = firstNum; i <= secondNum; i++) {
                File folder = new File(currentStorage.getRootDirectory() + "/" + folderNameBase + i);
                folder.mkdir();
            }
        }
        else {
            File newFolder = new File(getCurrentStorage().getRootDirectory() + "/" + folderName);
            newFolder.mkdir();
        }
    }


    @Override
    public void createFile(String filename) throws InvalidExtensionException {

        String fullPath = currentStorage.getRootDirectory() + "/" + filename;

        // Provera privilegija:
        if(!currentStorage.getCurrentUser().getPrivileges().contains(Privileges.WRITE)){
            throw new InsufficientPrivilegesException();
        }

        // Provera da li je dozvoljena ekstenzija fajla:
        if(checkExtensions(filename)){
            throw new InvalidExtensionException();
        }

        // Provera da li cemo prekoraciti broj fajlova u NEKOM FOLDERU:
        // Prvo proverava da li u HashMap-u postoji folder u kojem se kreira novi fajl
        // Ako postoji, proverava da li (trenutni broj fajlova u tom folderu + 1) prekoracuje maksimalan definisani iz HashMap-a
        if(currentStorage.getMaxNumberOfFilesInDirectory().containsKey(currentStorage.getRootDirectory())){
            int numberOfFiles = new File(currentStorage.getRootDirectory()).listFiles().length;
            if(numberOfFiles + 1 > currentStorage.getMaxNumberOfFilesInDirectory().get(currentStorage.getRootDirectory()))
                throw new FileLimitExceededException();
        }

        // Kreiranje fajla:
        File newFile = new File(fullPath);
        try {
            newFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(String ...paths) throws FileNotFoundException, InsufficientPrivilegesException, FileDeleteFailedException {

        // Provera da li trenutni korisnik skladista ima privilegiju za brisanje fajlova
        if(!getCurrentStorage().getCurrentUser().getPrivileges().contains(Privileges.DELETE)){
            throw new InsufficientPrivilegesException();
        }
        for(String path: paths) {
            File file = new File(currentStorage.getRootDirectory() + "/" + path);

            // Provera da li postoji fajl na prosledjenoj putanji:
            if (!file.exists()) {
                throw new FileNotFoundException();
            }

            // Brisanje fajla:
            String type = file.isDirectory() ? "Folder" : "Fajl";
            boolean deleted = file.delete();

            if (deleted) {
                currentStorage.setCurrentStorageSize(currentStorage.getCurrentStorageSize() - file.length());
                currentStorage.updateConfig();
            }
            else
                throw new FileDeleteFailedException();
        }
    }

    @Override
    public void get(String ...paths) {
        // Provera privilegija se vec desava u move metodi
        for(String path: paths) {
            move("Download", path);
            currentStorage.setCurrentStorageSize(currentStorage.getCurrentStorageSize() + new File(path).length());
            currentStorage.updateConfig();
        }
    }

    @Override
    public void move(String destination, String ...sources) {

        // Provera privilegija:
        if(!getCurrentStorage().getCurrentUser().getPrivileges().contains(Privileges.WRITE)){
            throw new InsufficientPrivilegesException();
        }

        // Provera prekoracenja broja fajlova u folderu:
        if(currentStorage.getMaxNumberOfFilesInDirectory().containsKey(destination)){
            int numberOfFiles = new File(destination).listFiles().length;
            if(numberOfFiles + 1 > currentStorage.getMaxNumberOfFilesInDirectory().get(destination))
                throw new FileLimitExceededException();
        }

        for(String source: sources) {

            source = currentStorage.getRootDirectory() + "/" + source;
            File sourceFile = new File(source);

            System.out.println(sourceFile.getPath());

            // Provera da li postoji fajl na prosledjenoj putanji:
            if(!sourceFile.exists()) {
                throw new FileNotFoundException();
            }

            // Provera da li je dozvoljena ekstenzija fajla:
            if (checkExtensions(source)) {
                throw new InvalidExtensionException();
            }

            // Provera prekoracenja velicine skladista u bajtovima:
            Path sourcePath = Paths.get(source);
            try {
                long size = Files.size(sourcePath);
                if (currentStorage.isStorageSizeLimitSet() && (currentStorage.getCurrentStorageSize() + size > currentStorage.getStorageSizeLimit())) {
                    throw new StorageSizeExceededException();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            Path result = null;

            try {
                System.out.println("SORS: " + source);
                System.out.println("DEST: " + currentStorage.getRootDirectory() + "/" + destination + "/" + Paths.get(source).getFileName());
                result = Files.move(Paths.get(source), Paths.get(currentStorage.getRootDirectory() + "/" + destination + "/" + Paths.get(source).getFileName()), StandardCopyOption.REPLACE_EXISTING);
                currentStorage.setCurrentStorageSize(currentStorage.getCurrentStorageSize() + Files.size(sourcePath));
                currentStorage.updateConfig();
            } catch (NoSuchFileException e1) {
                e1.printStackTrace();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (result != null)
                System.out.println("Fajl je uspesno premesten.");
            else
                System.out.println("Fajl nije premesten.");
        }
    }

    @Override
    public void put(String destination, String ...sources) throws FileAlreadyInStorageException {

        // Provera privilegija:
        if(!getCurrentStorage().getCurrentUser().getPrivileges().contains(Privileges.WRITE)){
            throw new InsufficientPrivilegesException();
        }

        // Provera prekoracenja broja fajlova u folderu:
        if(currentStorage.getMaxNumberOfFilesInDirectory().containsKey(destination)){
            int numberOfFiles = new File(destination).getParentFile().listFiles().length;
            if(numberOfFiles + 1 > currentStorage.getMaxNumberOfFilesInDirectory().get(destination))
                throw new FileLimitExceededException();
        }
        for(String source: sources) {

            Path sourcePath = Paths.get(source);

            //Provera da li se prosledjeni fajl koji stavljamo vec nalazi u skladistu - ako da, ispali exception FileAlreadyInStorageException
            if(source.contains(currentStorage.getRootDirectory())){
                throw new FileAlreadyInStorageException();
            }


            // Provera prekoracenja velicine skladista u bajtovima:

            try {
                long size = Files.size(sourcePath);
                if (currentStorage.getCurrentStorageSize() + size > currentStorage.getStorageSizeLimit()) {
                    throw new StorageSizeExceededException();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Provera da li je dozvoljena ekstenzija fajla:
            if (checkExtensions(source)) {
                throw new InvalidExtensionException();
            }


            Path result = null;

            try {
                result = Files.copy(sourcePath, Paths.get(currentStorage.getRootDirectory() + "/" + destination), StandardCopyOption.COPY_ATTRIBUTES);
                currentStorage.setCurrentStorageSize(currentStorage.getCurrentStorageSize() + Files.size(sourcePath));
                currentStorage.updateConfig();
            } catch (NoSuchFileException e1) {
                e1.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (result != null)
                System.out.println("Fajl je uspesno prekopiran.");
            else
                System.out.println("Fajl nije prekopiran.");
        }
    }

    // TODO: mozda vracati nesto iz funkcije list?
    @Override
    public void list(String path) {

        // Provera privilegija:
        if(!currentStorage.getCurrentUser().getPrivileges().contains(Privileges.READ)){
            throw new InsufficientPrivilegesException();
        }

        // Uzimanje fajl liste u root-u:
        List<File> fileList = getFileList(currentStorage.getRootDirectory() + "/" + path);
        String type;

        System.out.println("\nLista fajlova i foldera u skladistu:");
        System.out.println("-----------------------------------\n");
        for (File file: fileList){
            type = file.isDirectory() ? "DIR" : "FILE";
            System.out.println(file.getName() + " --- " + file.length() / (1024 * 1024) + " MB " + " --- " + type);
        }
    }

    // TODO: ne sme ispis!!!
    @Override
    public void list(String path, String argument, Operations operation) {

        // Provera privilegija:
        if(!currentStorage.getCurrentUser().getPrivileges().contains(Privileges.READ)){
            throw new InsufficientPrivilegesException();
        }

        String type;
        List<File> fileList = getFileList(currentStorage.getRootDirectory() + "/" + path);

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
            if(operation == Operations.SORT_BY_NAME_DESC) {
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
        }
    }

    @Override
    public void initializeStorage(String path) throws UserNotFoundException {

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
            List<User> users = new ArrayList<>();
            try {
                users = Arrays.asList(objectMapper.readValue(new File(path + "/users.json"), User[].class));
            } catch (IOException e) {
                e.printStackTrace();
            }

            boolean found = false;

            // Provera kredencijala - uporedjivanje prosledjenih username i password-a i procitanih iz users.json fajla
            for(User user: users) {
                if (username.equalsIgnoreCase(user.getUsername()) && password.equalsIgnoreCase(user.getPassword())) {
                    found = true;
                    try {
                        // Ako se podaci User-a match-uju, procitaj config, setuj trenutni storage i dodaj skladiste u listu skladista
                        StorageModel storageModel = objectMapper.readValue(new File(path + "/config.json"), StorageModel.class);
                        storageModelList.add(storageModel);
                        setCurrentStorage(storageModel);
                        currentStorage.setCurrentUser(user);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if(!found)
                throw new UserNotFoundException();
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
    public void limitNumberOfFiles(int numberOfFiles, String path) throws InsufficientPrivilegesException, FileNotFoundException {

        String fullPath = getCurrentStorage().getRootDirectory() + "/" + path;

        // Provera da li konfiguraciju vrsi superuser:
        if(!currentStorage.getSuperuser().equals(currentStorage.getCurrentUser())){
            throw new InsufficientPrivilegesException();
        }

        File directory = new File(getCurrentStorage().getRootDirectory() + "/" + path);

        // Provera da li postoji prosledjeni folder u trenutnom aktivnom skladistu
        if(!directory.exists()){
            throw new FileNotFoundException();
        }

        // Dodavanje novog para direktorijum-brFajlova u HashMap trenutnog skladista
        currentStorage.getMaxNumberOfFilesInDirectory().put(fullPath, Integer.valueOf(numberOfFiles));
        currentStorage.setMaxNumberOfFilesInDirectorySet(true);
        currentStorage.updateConfig();
    }

    @Override
    public void limitStorageSize(long l) throws InsufficientPrivilegesException {

        // Provera da li konfiguraciju vrsi superuser:
        if(!currentStorage.getSuperuser().equals(currentStorage.getCurrentUser())){
            throw new InsufficientPrivilegesException();
        }

        currentStorage.setStorageSizeLimit(l);
        currentStorage.setStorageSizeLimitSet(true);
        currentStorage.updateConfig();
    }

    @Override
    public void restrictExtension(String extension) throws InsufficientPrivilegesException {

        // Provera da li konfiguraciju vrsi superuser:
        if(!currentStorage.getSuperuser().equals(currentStorage.getCurrentUser())){
            throw new InsufficientPrivilegesException();
        }

        currentStorage.getUnsupportedExtensions().add(extension);
        currentStorage.updateConfig();
    }

    @Override
    public void addNewUser(User user, Set<Privileges> privilegesSet) {

        // Provera da li konfiguraciju vrsi superuser:
        if(!currentStorage.getSuperuser().equals(currentStorage.getCurrentUser())){
            throw new InsufficientPrivilegesException();
        }

        // Provera da li korisnik vec postoji:
        if(currentStorage.getUserList().contains(user)){
            throw new UserAlreadyExistsException();
        }

        user.setPrivileges(privilegesSet);
        currentStorage.getUserList().add(user);
        currentStorage.updateUsers();
        currentStorage.updateConfig();
    }

    @Override
    public void removeUser(User user) throws UserNotFoundException {

        // Provera da li konfiguraciju vrsi superuser:
        if(!currentStorage.getSuperuser().equals(currentStorage.getCurrentUser())){
            throw new InsufficientPrivilegesException();
        }

        // Pronalazenje korisnika sa datim username-om i password-om:
        User toRemove = null;
        boolean found = false;
        for(User u: currentStorage.getUserList()){
            if(u.getUsername().equalsIgnoreCase(user.getUsername()) && u.getPassword().equalsIgnoreCase(user.getPassword())){
                found = true;
                toRemove = u;
            }
        }

        // Ako postoji u listi, obrisi, ako ne, izbaci exception
        if(found) {
            currentStorage.getUserList().remove(toRemove);
            currentStorage.updateUsers();
            currentStorage.updateConfig();
        }
        else
            throw new UserNotFoundException();
    }

    // TODO: istestirati
    @Override
    public void login(User user) {

        User findUser = null;

        // Prodji kroz sve usere:
        for(User u: currentStorage.getUserList()){
            if(u.equals(user))
                findUser = u;
        }

        currentStorage.setCurrentUser(findUser);
        currentStorage.updateConfig();
        currentStorage.updateUsers();
    }

    // TODO: kod svih operacija sa skladistem uraditi proveru, ako je currentUser == null -> nijedan korisnik nije logovan, ne moze da se izvrsi
    @Override
    public void logout(User user) {
        User findUser = null;

        // Prodji kroz sve usere:
        for(User u: currentStorage.getUserList()){
            if(u.equals(user))
                findUser = u;
        }

        if(findUser == null)
            throw new UserNotFoundException();

        if(!currentStorage.getCurrentUser().equals(findUser))
            throw new UserLogoutException();

        currentStorage.setCurrentUser(null);
        currentStorage.updateUsers();
        currentStorage.updateConfig();


    }


    // Pomocna metoda za proveravanje ekstenzije prilikom dodavanja fajla u skladiste
    private boolean checkExtensions(String filename){
        boolean found = false;
        for(String extension: currentStorage.getUnsupportedExtensions()){
            if(filename.endsWith(extension))
                found = true;
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
