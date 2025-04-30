package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 苍镜月
 * @version 1.0
 * @implNote
 */

public class Stage implements Serializable {

    private final Map<String, String> addFiles;

    private final Map<String, String> removeFiles;

    public Stage() {
        addFiles = new HashMap<>();
        removeFiles = new HashMap<>();
    }

    public void addFile(String fileName, String hash) {
        addFiles.put(fileName, hash);
        Repository.saveStage(this);
    }

}
