package rs.edu.raf.storage;

import java.io.File;
import java.util.Comparator;

public class FileModifiedDateComparator implements Comparator<File> {
    @Override
    public int compare(File o1, File o2) {
        return Long.compare(o1.lastModified(), o2.lastModified());
    }
}
