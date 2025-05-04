package gitlet;

import java.io.Serializable;

/**
 * @author 苍镜月
 * @version 1.0
 * @implNote
 */

public class Blob implements Serializable {

    private final String key;

    private final byte[] content;

    private String fileName;

    public Blob(String key, byte[] content, String fileName) {
        this.key = key;
        this.content = content;
        this.fileName = fileName;
    }

    public String getKey() {
        return key;
    }

    public byte[] getContent() {
        return content;
    }

    public String getFileName() {
        return fileName;
    }
}
