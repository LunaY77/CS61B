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

    public Blob(byte[] content) {
        this.key = Utils.sha1(content);
        this.content = content;
    }

    public Blob(String key, byte[] content) {
        this.key = key;
        this.content = content;
    }

    public String getKey() {
        return key;
    }

    public byte[] getContent() {
        return content;
    }
}
