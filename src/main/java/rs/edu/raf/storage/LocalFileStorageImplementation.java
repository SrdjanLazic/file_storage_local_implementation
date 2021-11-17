package rs.edu.raf.storage;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import rs.edu.raf.storage.comparator.FileModifiedDateComparator;
import rs.edu.raf.storage.comparator.FileNameComparator;
import rs.edu.raf.storage.enums.Operations;
import rs.edu.raf.storage.enums.Privileges;
import rs.edu.raf.storage.exceptions.*;
import rs.edu.raf.storage.storage_management.FileStorage;
import rs.edu.raf.storage.storage_management.StorageManager;
import rs.edu.raf.storage.user_management.User;


import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class LocalFileStorageImplementation implements FileStorage {

    static {
        StorageManager.registerStorage(new LocalFileStorageImplementation());
    }

    private final List<StorageModel> storageModelList = new ArrayList<>();
    private StorageModel currentStorage;

    @Override
    public void createFolder(String path, String ...folderNames) throws InsufficientPrivilegesException, FileNotFoundException {

        String fullPath = currentStorage.getRootDirectory() + "/" + path;

        // Provera privilegija na nivou foldera:
        if (currentStorage.getCurrentUser().getFolderPrivileges().containsKey(fullPath)) {
            if(!currentStorage.getCurrentUser().getFolderPrivileges().get(fullPath).contains(Privileges.CREATE))
                throw new InsufficientPrivilegesException("Greska! Folder nema potrebne privilegije.");
        }

        // Provera da li je trenutni korisnik null
        if(currentStorage.getCurrentUser() == null)
            throw new CurrentUserIsNullException();

        // Provera privilegija:
        if(!currentStorage.getCurrentUser().getPrivileges().contains(Privileges.CREATE)){
            throw new InsufficientPrivilegesException();
        }

        // Provera da li postoje prosledjeni path
        if (!new File(currentStorage.getRootDirectory() + "/" + path).exists()) {
            throw new FileNotFoundException();
        }

        for(String folderName: folderNames) {

            // Kreiranje pomocu {} patterna:
            if (folderName.contains("{") && folderName.contains("}")) {
                String folderNameBase;
                int firstBrace = folderName.indexOf("{"), firstNum, secondNum;
                folderNameBase = folderName.substring(0, firstBrace);
                folderName = folderName.replaceAll("[^0-9]+", " ");
                firstNum = Integer.parseInt(Arrays.asList(folderName.trim().split(" ")).get(0));
                secondNum = Integer.parseInt(Arrays.asList(folderName.trim().split(" ")).get(1));

                for (int i = firstNum; i <= secondNum; i++) {
                    File folder = new File(currentStorage.getRootDirectory() + "/" + path + "/" + folderNameBase + i);
                    folder.mkdir();
                }
            } else {
                fullPath = currentStorage.getRootDirectory() + "/" + path + "/" + folderName;
                File newFolder = new File(fullPath);
                newFolder.mkdir();
            }
        }
    }

    @Override
    public void createFile(String path, String ...filenames) throws InvalidExtensionException, InsufficientPrivilegesException, FileLimitExceededException, FileNotFoundException {

        String fullPath = currentStorage.getRootDirectory() + "/" + path;
        File destinationFolder = new File(fullPath);

        // Provera privilegija na nivou foldera:
        if (currentStorage.getCurrentUser().getFolderPrivileges().containsKey(fullPath)) {
            if(!currentStorage.getCurrentUser().getFolderPrivileges().get(fullPath).contains(Privileges.CREATE))
                throw new InsufficientPrivilegesException("Greska! Folder nema potrebne privilegije.");
        }

        // Provera da li je trenutni korisnik null
        if(currentStorage.getCurrentUser() == null)
            throw new CurrentUserIsNullException();

        // Provera privilegija:
        if(!currentStorage.getCurrentUser().getPrivileges().contains(Privileges.CREATE)){
            throw new InsufficientPrivilegesException();
        }

        // Provera li postoji putanja na koju smestamo fajl
        if(!new File(fullPath).exists())
            throw new FileNotFoundException();

        // Provera da li cemo prekoraciti broj fajlova u nekom folderu:
        // Prvo proverava da li u HashMap-u postoji folder u kojem se kreira novi fajl
        // Ako postoji, proverava da li (trenutni broj fajlova u tom folderu + 1) prekoracuje maksimalan definisani iz HashMap-a
        if(currentStorage.getMaxNumberOfFilesInDirectory().containsKey(fullPath)){
            int numberOfFiles = destinationFolder.listFiles().length;
            if(numberOfFiles + filenames.length > currentStorage.getMaxNumberOfFilesInDirectory().get(fullPath))
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

        // Provera da li je trenutni korisnik null
        if(currentStorage.getCurrentUser() == null)
            throw new CurrentUserIsNullException();

        // Provera privilegija:
        if(!currentStorage.getCurrentUser().getPrivileges().contains(Privileges.CREATE)){
            throw new InsufficientPrivilegesException();
        }

        // Provera privilegija na nivou foldera:
        if (currentStorage.getCurrentUser().getFolderPrivileges().containsKey(currentStorage.getRootDirectory())) {
            if(!currentStorage.getCurrentUser().getFolderPrivileges().get(currentStorage.getRootDirectory()).contains(Privileges.CREATE))
                throw new InsufficientPrivilegesException("Greska! Folder nema potrebne privilegije.");
        }

        if (folderName.contains("{") && folderName.contains("}")) {
            String folderNameBase;
            int firstBrace = folderName.indexOf("{"), firstNum, secondNum;
            folderNameBase = folderName.substring(0, firstBrace);
            folderName = folderName.replaceAll("[^0-9]+", " ");
            firstNum = Integer.parseInt(Arrays.asList(folderName.trim().split(" ")).get(0));
            secondNum = Integer.parseInt(Arrays.asList(folderName.trim().split(" ")).get(1));

            for (int i = firstNum; i <= secondNum; i++) {
                File folder = new File(currentStorage.getRootDirectory() + "/" + folderNameBase + i);
                folder.mkdir();
            }
        } else {
            String fullPath = currentStorage.getRootDirectory() + "/" + folderName;
            File newFolder = new File(fullPath);
            newFolder.mkdir();
        }

    }


    @Override
    public void createFile(String filename) throws InvalidExtensionException, InsufficientPrivilegesException, FileLimitExceededException {

        String fullPath = currentStorage.getRootDirectory() + "/" + filename;
        File destinationFolder = currentStorage.getStorageFolder();

        // Provera da li je trenutni korisnik null
        if(currentStorage.getCurrentUser() == null)
            throw new CurrentUserIsNullException();

        // Provera privilegija:
        if(!currentStorage.getCurrentUser().getPrivileges().contains(Privileges.CREATE)){
            throw new InsufficientPrivilegesException();
        }

        // Provera privilegija na nivou foldera:
        if (currentStorage.getCurrentUser().getFolderPrivileges().containsKey(currentStorage.getRootDirectory())) {
            if(!currentStorage.getCurrentUser().getFolderPrivileges().get(currentStorage.getRootDirectory()).contains(Privileges.CREATE))
                throw new InsufficientPrivilegesException("Greska! Folder nema potrebne privilegije.");
        }

        // Provera da li je dozvoljena ekstenzija fajla:
        if(checkExtensions(filename)){
            throw new InvalidExtensionException();
        }

        // Provera da li cemo prekoraciti broj fajlova u NEKOM FOLDERU:
        // Prvo proverava da li u HashMap-u postoji folder u kojem se kreira novi fajl
        // Ako postoji, proverava da li (trenutni broj fajlova u tom folderu + 1) prekoracuje maksimalan definisani iz HashMap-a
        if(currentStorage.getMaxNumberOfFilesInDirectory().containsKey(currentStorage.getRootDirectory())){
            int numberOfFiles = destinationFolder.listFiles().length;
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

        // Provera da li je trenutni korisnik null
        if(currentStorage.getCurrentUser() == null)
            throw new CurrentUserIsNullException();

        // Provera da li trenutni korisnik skladista ima privilegiju za brisanje fajlova
        if(!getCurrentStorage().getCurrentUser().getPrivileges().contains(Privileges.DELETE)){
            throw new InsufficientPrivilegesException();
        }

        for(String path: paths) {
            String fullPath = currentStorage.getRootDirectory() + "/" + path;
            String pathWithoutFile = fullPath.substring(0, fullPath.lastIndexOf('/'));
            File file = new File(fullPath);

            // Provera privilegija na nivou foldera:
            if (currentStorage.getCurrentUser().getFolderPrivileges().containsKey(pathWithoutFile)) {
                if(!currentStorage.getCurrentUser().getFolderPrivileges().get(pathWithoutFile).contains(Privileges.DELETE))
                    throw new InsufficientPrivilegesException("Greska! Folder nema potrebne privilegije.");
            }

            // Provera da li postoji fajl na prosledjenoj putanji:
            if (!file.exists()) {
                throw new FileNotFoundException();
            }

            if(file.isDirectory()){
                for(File f : file.listFiles()){
                    delete(f.getPath().replace("\\","/").replace(currentStorage.getRootDirectory(),""));
                }
                file.delete();
            } else {
                boolean deleted = file.delete();

                if (deleted) {
                currentStorage.setCurrentStorageSize(currentStorage.getCurrentStorageSize() - file.length());
                currentStorage.updateConfig();
                }
                else
                    throw new FileDeleteFailedException();
            }
//            // Brisanje fajla:
//            boolean deleted = file.delete();
//
//            if (deleted) {
//                currentStorage.setCurrentStorageSize(currentStorage.getCurrentStorageSize() - file.length());
//                currentStorage.updateConfig();
//            }
//            else
//                throw new FileDeleteFailedException();
        }
    }

    @Override
    public void get(String ...paths) {

        // Provera da li je trenutni korisnik null
        if(currentStorage.getCurrentUser() == null)
            throw new CurrentUserIsNullException();

        // Provera privilegija:
        if(!getCurrentStorage().getCurrentUser().getPrivileges().contains(Privileges.DOWNLOAD)){
            throw new InsufficientPrivilegesException();
        }

        // Provera privilegija na nivou foldera:
        if (currentStorage.getCurrentUser().getFolderPrivileges().containsKey(currentStorage.getDownloadFolder())) {
            if(!currentStorage.getCurrentUser().getFolderPrivileges().get(currentStorage.getDownloadFolder()).contains(Privileges.DOWNLOAD))
                throw new InsufficientPrivilegesException("Greska! Folder nema potrebne privilegije.");
        }

        for(String path: paths) {
            move("Download", path);
            currentStorage.setCurrentStorageSize(currentStorage.getCurrentStorageSize() + new File(path).length());
            currentStorage.updateConfig();
        }
    }

    @Override
    public void move(String destination, String ...sources) throws InsufficientPrivilegesException, OperationFailedException, FileLimitExceededException, FileNotFoundException, StorageSizeExceededException, InvalidExtensionException{

        String fullPath = currentStorage.getRootDirectory() + "/" + destination;
        File destinationFolder = new File(fullPath);

        // Provera da li je trenutni korisnik null
        if(currentStorage.getCurrentUser() == null)
            throw new CurrentUserIsNullException();

        // Provera privilegija:
        if(!getCurrentStorage().getCurrentUser().getPrivileges().contains(Privileges.CREATE)){
            throw new InsufficientPrivilegesException();
        }

        // Provera privilegija na nivou foldera:
        if (currentStorage.getCurrentUser().getFolderPrivileges().containsKey(fullPath)) {
            if(!currentStorage.getCurrentUser().getFolderPrivileges().get(fullPath).contains(Privileges.CREATE))
                throw new InsufficientPrivilegesException("Greska! Folder nema potrebne privilegije.");
        }

        // Provera da li postoji destination:
        if(!destinationFolder.exists())
            throw new FileNotFoundException();

        // Provera prekoracenja broja fajlova u folderu:
        if(currentStorage.getMaxNumberOfFilesInDirectory().containsKey(fullPath)){
            int numberOfFiles = destinationFolder.listFiles().length;
            if(numberOfFiles + sources.length > currentStorage.getMaxNumberOfFilesInDirectory().get(fullPath))
                throw new FileLimitExceededException();
        }

        for(String source: sources) {

            source = currentStorage.getRootDirectory() + "/" + source;
            System.out.println(source);
            File sourceFile = new File(source);

            // Provera da li postoji fajl na prosledjenoj putanji:
            if(!sourceFile.exists()) {
                throw new FileNotFoundException();
            }

            Path result = null;

            try {
                String resultingPath = currentStorage.getRootDirectory() + "/" + destination + "/" + Paths.get(source).getFileName();
                result = Files.move(Paths.get(source), Paths.get(resultingPath), StandardCopyOption.REPLACE_EXISTING);
            } catch (NoSuchFileException e1) {
                e1.printStackTrace();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(result == null)
                throw new OperationFailedException();
        }
    }

    @Override
    public void put(String destination, String ...sources) throws FileAlreadyInStorageException, OperationFailedException, FileNotFoundException, InsufficientPrivilegesException, FileLimitExceededException, InvalidExtensionException, StorageSizeExceededException {

        String fullPath = currentStorage.getRootDirectory() + "/" + destination;
        File destinationFolder = new File(fullPath);

        // Provera da li je trenutni korisnik null
        if(currentStorage.getCurrentUser() == null)
            throw new CurrentUserIsNullException();

        // Provera privilegija:
        if(!getCurrentStorage().getCurrentUser().getPrivileges().contains(Privileges.CREATE)){
            throw new InsufficientPrivilegesException();
        }

        // Provera privilegija na nivou foldera:
        if (currentStorage.getCurrentUser().getFolderPrivileges().containsKey(fullPath)) {
            if(!currentStorage.getCurrentUser().getFolderPrivileges().get(fullPath).contains(Privileges.CREATE))
                throw new InsufficientPrivilegesException("Greska! Folder nema potrebne privilegije.");
        }

        // Provera da li postoji destinacija
        if(!destinationFolder.exists())
            throw new FileNotFoundException();

        for(String source: sources) {
            // Provera prekoracenja broja fajlova u folderu:
            if(currentStorage.getMaxNumberOfFilesInDirectory().containsKey(fullPath)){
                int numberOfFiles = destinationFolder.listFiles().length;
                System.out.println(numberOfFiles);
                if(numberOfFiles + sources.length > currentStorage.getMaxNumberOfFilesInDirectory().get(fullPath))
                    throw new FileLimitExceededException();

            }

            // Provera da li postoji source fajl
            if(!new File(source).exists())
                throw new FileNotFoundException();

            Path sourcePath = Paths.get(source);

            //Provera da li se prosledjeni fajl koji stavljamo vec nalazi u skladistu - ako da, ispali exception FileAlreadyInStorageException
            if(source.contains(currentStorage.getRootDirectory())){
                throw new FileAlreadyInStorageException();
            }

            // Provera prekoracenja velicine skladista u bajtovima:
            try {
                long size = Files.size(sourcePath);
                if (currentStorage.isStorageSizeLimitSet() && (currentStorage.getCurrentStorageSize() + size > currentStorage.getStorageSizeLimit())) {
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
                result = Files.copy(sourcePath, Paths.get(currentStorage.getRootDirectory() + "/" + destination + "/" + sourcePath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                currentStorage.setCurrentStorageSize(currentStorage.getCurrentStorageSize() + Files.size(sourcePath));
                currentStorage.updateConfig();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            if(result == null)
                throw new OperationFailedException();
        }
    }

    @Override
    public Collection<String> list(String path, boolean searchSubdirectories) throws InsufficientPrivilegesException, FileNotFoundException {
        String destinationPath;
        if(path.equalsIgnoreCase("root"))
            destinationPath = currentStorage.getRootDirectory();
        else
            destinationPath = currentStorage.getRootDirectory() + "/" + path;

        Collection<String> toReturn = new ArrayList<>();

        // Provera da li je trenutni korisnik null
        if(currentStorage.getCurrentUser() == null)
            throw new CurrentUserIsNullException();

        // Provera privilegija:
        if(!currentStorage.getCurrentUser().getPrivileges().contains(Privileges.VIEW)){
            throw new InsufficientPrivilegesException();
        }

        // Provera privilegija na nivou foldera:
        if (currentStorage.getCurrentUser().getFolderPrivileges().containsKey(destinationPath)) {
            if(!currentStorage.getCurrentUser().getFolderPrivileges().get(destinationPath).contains(Privileges.VIEW))
                throw new InsufficientPrivilegesException("Greska! Folder nema potrebne privilegije.");
        }

        // Provera da li putanja postoji:
        if(!new File(destinationPath).exists())
            throw new FileNotFoundException();

        // Uzimanje fajl liste u root-u:
        List<File> fileList = getFileList(destinationPath);

        if(searchSubdirectories) {
            for (File file : fileList) {
                if(file.isFile()){
                    toReturn.add(file.getName() + " --- " + file.getPath() + " --- " + file.length() / 1024 + " KB" + " --- " + "FILE");
                } else {
                    String fullPath = file.getPath();
                    fullPath = fullPath.replace("\\", "/").replace(currentStorage.getRootDirectory(), "");
                    toReturn.add(file.getName() + " --- " + file.getPath() + " --- " + file.length() / 1024 + " KB" + " --- " + "DIR");
                    toReturn = listRecursive(fullPath, true, toReturn);
                }
            }
        } else {
            for (File file : fileList) {
                if(file.isFile()){
                    toReturn.add(file.getName() + " --- " + file.getPath() + " --- " + file.length() / 1024 + " KB" + " --- " + "FILE");
                } else {
                    toReturn.add(file.getName() + " --- " + file.getPath() + " --- " + file.length() / 1024 + " KB" + " --- " + "DIR");
                }
            }
        }
        return toReturn;
    }


    // TODO: ne sme ispis!!!
    @Override
    public Collection<String> list(String path, String argument, Operations operation, boolean searchSubdirectories) throws InsufficientPrivilegesException, FileNotFoundException {

        String destinationPath;
        if(path.equalsIgnoreCase("root"))
            destinationPath = currentStorage.getRootDirectory();
        else
            destinationPath = currentStorage.getRootDirectory() + "/" + path;

        Collection<String> toReturn = new ArrayList<>();

        // Provera da li je trenutni korisnik null
        if(currentStorage.getCurrentUser() == null)
            throw new CurrentUserIsNullException();

        // Provera privilegija:
        if(!currentStorage.getCurrentUser().getPrivileges().contains(Privileges.VIEW)){
            throw new InsufficientPrivilegesException();
        }

        // Provera privilegija na nivou foldera:
        if (currentStorage.getCurrentUser().getFolderPrivileges().containsKey(destinationPath)) {
            if(!currentStorage.getCurrentUser().getFolderPrivileges().get(destinationPath).contains(Privileges.VIEW))
                throw new InsufficientPrivilegesException("Greska! Folder nema potrebne privilegije.");
        }

        // Provera da li postoji fajl:
        if(!new File(destinationPath).exists())
            throw new FileNotFoundException();

        List<File> fileList = getFileList(destinationPath);

        if (operation == Operations.FILTER_EXTENSION) {
            String extension = argument;

            if(searchSubdirectories) {
                for (File file : fileList) {
                    if(file.isFile()) {
                        if (file.getName().endsWith(extension))
                            toReturn.add(file.getName() + " --- " + file.getPath() + " --- " + file.length() / 1024 + " KB" + " --- " + "FILE");
                    } else {
                        String fullPath = file.getPath();
                        fullPath = fullPath.replace("\\", "/").replace(currentStorage.getRootDirectory(), "");
                        toReturn = listRecursive(fullPath, extension, Operations.FILTER_EXTENSION, true, toReturn);
                    }
                }
            } else {
                for (File file : fileList) {
                    if(file.isFile()){
                        if (file.getName().endsWith(extension))
                            toReturn.add(file.getName() + " --- " + file.getPath() + " --- " + file.length() / 1024 + " KB" + " --- " + "FILE");
                    }  //toReturn.add(file.getName() + " --- " + file.getPath() + " --- " + file.length() / 1024 + " KB" + " --- " + "DIR");

                }
            }
        } else if (operation == Operations.FILTER_FILENAME) {
            String filename = argument;
            if(searchSubdirectories) {
                for (File file : fileList) {
                    if(file.isFile()) {
                        if (file.getName().contains(filename))
                            toReturn.add(file.getName() + " --- " + file.getPath() + " --- " + file.length() / 1024 + " KB" + " --- " + "FILE");
                    } else {
                        String fullPath = file.getPath();
                        fullPath = fullPath.replace("\\", "/").replace(currentStorage.getRootDirectory(), "");
                        toReturn = listRecursive(fullPath, filename, Operations.FILTER_FILENAME, true, toReturn);
                    }
                }
            } else {
                for (File file : fileList) {
                    if(file.isFile()){
                        if (file.getName().contains(filename))
                            toReturn.add(file.getName() + " --- " + file.getPath() + " --- " + file.length() / 1024 + " KB" + " --- " + "FILE");
                    }
                }
            }
        } else if (operation == Operations.SORT_BY_NAME_ASC || operation == Operations.SORT_BY_NAME_DESC) {
            if(searchSubdirectories) {
                for (File file : fileList) {
                    if(file.isFile()) {
                        toReturn.add(file.getPath());
                    } else {
                        String fullPath = file.getPath();
                        fullPath = fullPath.replace("\\", "/").replace(currentStorage.getRootDirectory(), "");
                        toReturn.add(file.getPath());
                        toReturn = listRecursive(fullPath, null, operation, true, toReturn);
                    }
                }
            } else {
                for (File file : fileList) {
                    if(file.isFile()){
                        toReturn.add(file.getPath());
                    } else {
                        toReturn.add(file.getPath());
                    }
                }
            }
            List<File> outputFiles = new ArrayList<>();
            for(String outputPath: toReturn){
                outputFiles.add(new File(outputPath));
            }
            if(operation == Operations.SORT_BY_NAME_ASC)
                outputFiles.sort(new FileNameComparator());
            else
                outputFiles.sort(new FileNameComparator().reversed());
            toReturn.clear();
            for(File f : outputFiles){
                if(f.isFile())
                    toReturn.add(f.getName() + " --- " + f.getPath() + " --- " + f.length() / 1024 + " KB" + " --- " + "FILE");
                else
                    toReturn.add(f.getName() + " --- " + f.getPath() + " --- " + f.length() / 1024 + " KB" + " --- " + "DIR");
            }
        } else if (operation == Operations.SORT_BY_DATE_MODIFIED_ASC || operation == Operations.SORT_BY_DATE_MODIFIED_DESC) {
            if(searchSubdirectories) {
                for (File file : fileList) {
                    if(file.isFile()) {
                        toReturn.add(file.getPath());
                    } else {
                        String fullPath = file.getPath();
                        fullPath = fullPath.replace("\\", "/").replace(currentStorage.getRootDirectory(), "");
                        toReturn.add(file.getPath());
                        toReturn = listRecursive(fullPath, null, operation, true, toReturn);
                    }
                }
            } else {
                for (File file : fileList) {
                    if(file.isFile()){
                        toReturn.add(file.getPath());
                    } else {
                        toReturn.add(file.getPath());
                    }
                }
            }
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            List<File> outputFiles = new ArrayList<>();
            for(String outputPath: toReturn){
                outputFiles.add(new File(outputPath));
            }
            if(operation == Operations.SORT_BY_DATE_MODIFIED_ASC)
                outputFiles.sort(new FileModifiedDateComparator());
            else
                outputFiles.sort(new FileModifiedDateComparator().reversed());
            toReturn.clear();
            for(File f : outputFiles){

                if(f.isFile())
                    toReturn.add(f.getName() + " --- " + f.getPath() + " --- " + f.length() / 1024 + " KB" + " --- " + "FILE" + " --- " + sdf.format(f.lastModified()));
                else
                    toReturn.add(f.getName() + " --- " + f.getPath() + " --- " + f.length() / 1024 + " KB" + " --- " + "DIR" + " --- " + sdf.format(f.lastModified()));
            }
        }
        return toReturn;
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

        // TODO: ispis ne bi trebalo da bude ovde!
        // Ako jeste skladiste, procitaj user i config fajlove
        if(isStorage){
            System.out.println("Direktorijum je vec skladiste. Unesite username i password kako biste se konektovali na skladiste.");
            System.out.println("Username:");
            String username = scanner.nextLine();
            System.out.println("Password:");
            String password = scanner.nextLine();

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
            List<User> users = new ArrayList<>();
            try {
                users = Arrays.asList(objectMapper.readValue(new File(path + "/users.json"), User[].class));
            } catch (IOException e) {
                e.printStackTrace();
            }

            boolean found = false;

            // TODO: provera kredencijala preko equals za Usera
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
            // TODO: Sve println nekako izbaciti
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
                // TODO: videti sta ovde
                System.out.println("Skladiste nije kreirano.");
            }
        }
    }

    @Override
    public void limitNumberOfFiles(int numberOfFiles, String path) throws InsufficientPrivilegesException, FileNotFoundException {

        String fullPath = getCurrentStorage().getRootDirectory() + "/" + path;

        // Provera da li je trenutni korisnik null
        if(currentStorage.getCurrentUser() == null)
            throw new CurrentUserIsNullException();

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
        currentStorage.getMaxNumberOfFilesInDirectory().put(fullPath, numberOfFiles);
        currentStorage.setMaxNumberOfFilesInDirectorySet(true);
        currentStorage.updateConfig();
    }

    @Override
    public void limitStorageSize(long l) throws InsufficientPrivilegesException {

        // Provera da li je trenutni korisnik null
        if(currentStorage.getCurrentUser() == null)
            throw new CurrentUserIsNullException();

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

        // Provera da li je trenutni korisnik null
        if(currentStorage.getCurrentUser() == null)
            throw new CurrentUserIsNullException();

        // Provera da li konfiguraciju vrsi superuser:
        if(!currentStorage.getSuperuser().equals(currentStorage.getCurrentUser())){
            throw new InsufficientPrivilegesException();
        }

        currentStorage.getUnsupportedExtensions().add(extension);
        currentStorage.updateConfig();
    }

    @Override
    public void addNewUser(String username, String password, Set<Privileges> privilegesSet) {

        User user = new User(username, password);

        // Provera da li je trenutni korisnik null
        if(currentStorage.getCurrentUser() == null)
            throw new CurrentUserIsNullException();

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
    public void removeUser(String username) throws UserNotFoundException {

        // Provera da li konfiguraciju vrsi superuser:
        if(!currentStorage.getSuperuser().equals(currentStorage.getCurrentUser())){
            throw new InsufficientPrivilegesException();
        }

        // Pronalazenje korisnika sa datim username-om i password-om:
        User toRemove = null;
        boolean found = false;
        for(User u: currentStorage.getUserList()){
            if(u.getUsername().equalsIgnoreCase(username)){
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

    @Override
    public void login(String username, String password) throws UserNotFoundException, UserAlreadyLoggedInException{

        User user = new User(username, password);
        User findUser = null;

        if(!(currentStorage.getCurrentUser() == null))
            throw new CurrentUserIsNullException();

        // Prodji kroz sve usere:
        for(User u: currentStorage.getUserList()){
            if(u.equals(user))
                findUser = u;
        }

        if(findUser == null)
            throw new UserNotFoundException();

        if(!(currentStorage.getCurrentUser() == null) && currentStorage.getCurrentUser().equals(findUser))
            throw new UserAlreadyLoggedInException();

        currentStorage.setCurrentUser(findUser);
        currentStorage.updateConfig();
        currentStorage.updateUsers();
    }

    // TODO: kod svih operacija sa skladistem uraditi proveru, ako je currentUser == null -> nijedan korisnik nije logovan, ne moze da se izvrsi
    @Override
    public void logout() throws UserNotFoundException, UserLogoutException{

        if(currentStorage.getCurrentUser() == null)
            throw new CurrentUserIsNullException();

        currentStorage.setCurrentUser(null);
        currentStorage.updateUsers();
        currentStorage.updateConfig();
    }

    @Override
    public void setFolderPrivileges(String username, String path, Set<Privileges> privileges) {
        String fullPath = currentStorage.getRootDirectory() + "/" + path;

        // Provera da li je trenutni korisnik null
        if(currentStorage.getCurrentUser() == null)
            throw new CurrentUserIsNullException();

        // Provera da li konfiguraciju vrsi superuser:
        if(!currentStorage.getSuperuser().equals(currentStorage.getCurrentUser())){
            throw new InsufficientPrivilegesException();
        }

        User user = null;
        for(User u : currentStorage.getUserList()){
            if(u.getUsername().equalsIgnoreCase(username))
                user = u;
        }

        if(user == null)
            throw new UserNotFoundException();

        Set<Privileges> privilegesToAdd = new HashSet<>();

        if(privileges.contains(Privileges.DELETE))
            privilegesToAdd.addAll(List.of(Privileges.DELETE, Privileges.CREATE, Privileges.DOWNLOAD, Privileges.VIEW));
        else if(privileges.contains(Privileges.CREATE))
            privilegesToAdd.addAll(List.of(Privileges.CREATE, Privileges.DOWNLOAD, Privileges.VIEW));
        else if(privileges.contains(Privileges.DOWNLOAD))
            privilegesToAdd.addAll(List.of(Privileges.DOWNLOAD, Privileges.VIEW));
        else if(privileges.contains(Privileges.VIEW))
            privilegesToAdd.add((Privileges.VIEW));

        user.getFolderPrivileges().put(fullPath, privilegesToAdd);
    }


    // Pomocna metoda za proveravanje ekstenzije prilikom dodavanja fajla u skladiste
    private boolean checkExtensions(String filename){
        boolean found = false;
        for(String extension: currentStorage.getUnsupportedExtensions()){
            if (filename.endsWith(extension)) {
                found = true;
                break;
            }
        }
        return found;
    }

    private Collection<String> listRecursive(String path, boolean searchSubdirectories, Collection<String> toReturn) throws InsufficientPrivilegesException, FileNotFoundException {

        String destinationPath = currentStorage.getRootDirectory() + "/" + path;

        // Provera privilegija:
        if(!currentStorage.getCurrentUser().getPrivileges().contains(Privileges.VIEW)){
            throw new InsufficientPrivilegesException();
        }

        // Provera privilegija foldera:


        // Provera da li postoji prosledjeni path
        if(!new File(destinationPath).exists())
            throw new FileNotFoundException();

        // Uzimanje fajl liste u root-u:
        List<File> fileList = getFileList(currentStorage.getRootDirectory() + "/" + path);

        if(searchSubdirectories) {
            for (File file : fileList) {
                if(file.isFile()){
                    toReturn.add(file.getName() + " --- " + file.getPath() + " --- " + file.length() / 1024 + " KB" + " --- " + "FILE");
                } else {
                    String fullPath = file.getPath();
                    fullPath = fullPath.replace("\\", "/").replace(currentStorage.getRootDirectory(), "");
                    toReturn = listRecursive(fullPath, true, toReturn);
                }
            }
        } else {
            for (File file : fileList) {
                if(file.isFile())
                    toReturn.add(file.getName() + " --- " + file.getPath() + " --- " + file.length() / 1024 + " KB" + " --- " + "FILE");
                else
                    toReturn.add(file.getName() + " --- " + file.getPath() + " --- " + file.length() / 1024 + " KB" + " --- " + "DIR");
            }
        }
        return toReturn;
    }

    private Collection<String> listRecursive(String path, String argument, Operations operation, boolean searchSubdirectories, Collection<String> toReturn) throws InsufficientPrivilegesException, FileNotFoundException {

        String destinationPath = currentStorage.getRootDirectory() + "/" + path;

        // Provera privilegija:
        if(!currentStorage.getCurrentUser().getPrivileges().contains(Privileges.VIEW)){
            throw new InsufficientPrivilegesException();
        }

        // Provera da li postoji fajl:
        if(!new File(destinationPath).exists())
            throw new FileNotFoundException();

        List<File> fileList = getFileList(currentStorage.getRootDirectory() + "/" + path);

        if (operation == Operations.FILTER_EXTENSION) {
            String extension = argument;

            if(searchSubdirectories) {
                for (File file : fileList) {
                    if(file.isFile()) {
                        if (file.getName().endsWith(extension))
                            toReturn.add(file.getName() + " --- " + file.getPath() + " --- " + file.length() / 1024 + " KB" + " --- " + "FILE");
                    } else {
                        String fullPath = file.getPath();
                        fullPath = fullPath.replace("\\", "/").replace(currentStorage.getRootDirectory(), "");
                        //toReturn.add(file.getName() + " --- " + file.getPath() + " --- " + file.length() / 1024 + " KB" + " --- " + "DIR");
                        toReturn = listRecursive(fullPath, extension, Operations.FILTER_EXTENSION, true, toReturn);
                    }
                }
            } else {
                for (File file : fileList) {
                    if(file.isFile()){
                        if (file.getName().endsWith(extension))
                            toReturn.add(file.getName() + " --- " + file.getPath() + " --- " + file.length() / 1024 + " KB" + " --- " + "FILE");
                    } else {
                        //toReturn.add(file.getName() + " --- " + file.getPath() + " --- " + file.length() / 1024 + " KB" + " --- " + "DIR");
                    }
                }
            }
        } else if (operation == Operations.FILTER_FILENAME) {
            String filename = argument;

            if(searchSubdirectories) {
                for (File file : fileList) {
                    if(file.isFile()) {
                        if (file.getName().contains(filename))
                            toReturn.add(file.getName() + " --- " + file.getPath() + " --- " + file.length() / 1024 + " KB" + " --- " + "FILE");
                    } else {
                        String fullPath = file.getPath();
                        fullPath = fullPath.replace("\\", "/").replace(currentStorage.getRootDirectory(), "");
                        toReturn = listRecursive(fullPath, filename, Operations.FILTER_FILENAME, true, toReturn);
                    }
                }
            } else {
                for (File file : fileList) {
                    if(file.isFile()){
                        if (file.getName().contains(filename))
                            toReturn.add(file.getName() + " --- " + file.getPath() + " --- " + file.length() / 1024 + " KB" + " --- " + "FILE");
                    }
                }
            }
        } else if (operation == Operations.SORT_BY_NAME_ASC || operation == Operations.SORT_BY_NAME_DESC) {

            if(searchSubdirectories) {
                for (File file : fileList) {
                    if(file.isFile()) {
                        toReturn.add(file.getPath());
                    } else {
                        String fullPath = file.getPath();
                        fullPath = fullPath.replace("\\", "/").replace(currentStorage.getRootDirectory(), "");
                        toReturn.add(file.getPath());
                        toReturn = listRecursive(fullPath, null, operation, true, toReturn);
                    }
                }
            } else {
                for (File file : fileList) {
                    if(file.isFile()){
                        toReturn.add(file.getPath());
                    } else {

                        toReturn.add(file.getPath());
                    }
                }
            }
        } else if (operation == Operations.SORT_BY_DATE_MODIFIED_ASC || operation == Operations.SORT_BY_DATE_MODIFIED_DESC) {

            if(operation == Operations.SORT_BY_DATE_MODIFIED_ASC) {
                fileList.sort(new FileModifiedDateComparator());
            }
            else {
                fileList.sort(new FileModifiedDateComparator().reversed());
            }

            if(searchSubdirectories) {
                for (File file : fileList) {
                    if(file.isFile()) {
                        toReturn.add(file.getPath());
                    } else {
                        if(operation == Operations.SORT_BY_DATE_MODIFIED_DESC) {
                            fileList = getFileList(file.getPath());
                            fileList.sort(new FileModifiedDateComparator());
                        }
                        else {
                            fileList = getFileList(file.getPath());
                            fileList.sort(new FileModifiedDateComparator().reversed());
                        }
                        String fullPath = file.getPath();
                        fullPath = fullPath.replace("\\", "/").replace(currentStorage.getRootDirectory(), "");
                        toReturn.add(file.getPath());
                        toReturn = listRecursive(fullPath, null, operation, true, toReturn);
                    }
                }
            } else {
                for (File file : fileList) {
                    if(file.isFile()){
                        toReturn.add(file.getPath());
                    } else {
                        if(operation == Operations.SORT_BY_DATE_MODIFIED_DESC) {
                            fileList = getFileList(file.getPath());
                            fileList.sort(new FileModifiedDateComparator());
                        }
                        else {
                            fileList = getFileList(file.getPath());
                            fileList.sort(new FileModifiedDateComparator().reversed());
                        }
                        toReturn.add(file.getPath());
                    }
                }
            }
        }

        return toReturn;
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
