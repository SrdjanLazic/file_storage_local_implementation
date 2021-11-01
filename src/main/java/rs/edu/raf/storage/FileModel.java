package rs.edu.raf.storage;

import java.io.File;

public class FileModel {

    private FolderModel parentFolder;
    private File file;

    public FileModel(File file){
        this.file = file;
    }

    public FolderModel getParentFolder() {
        return parentFolder;
    }

    public File getCurrentFile() {
        return file;
    }
}
