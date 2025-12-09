package util;

import java.io.File;

public class FileManager {

    public static boolean ensureFolder(String path) {
        File folder = new File(path);
        if (!folder.exists()) {
            return folder.mkdirs();
        }
        return true;
    }
}
