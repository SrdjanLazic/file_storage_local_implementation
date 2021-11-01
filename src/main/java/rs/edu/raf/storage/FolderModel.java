package rs.edu.raf.storage;

import java.io.File;
import java.util.List;

public class FolderModel {

    private List<FileModel> files;
    private List<FolderModel> folders;
    private FolderModel parentFolder;
    private StorageModel root;
    private File file;
    // TODO: Parent za folder - moze biti i skladiste i folder, nekako ishendlovati to


    public FolderModel(File file){
        this.file = file;
    }

    public List<FileModel> getFiles() {
        return files;
    }

    public FolderModel getParentFolder() {
        return parentFolder;
    }

    public StorageModel getRoot() {
        return root;
    }

    public File getCurrentFolder() {
        return file;
    }
}
