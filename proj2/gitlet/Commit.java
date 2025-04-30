package gitlet;

// TODO: any imports you need here

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Represents a gitlet commit object.
 *  does at a high level.
 *
 * @author TODO
 */
public class Commit implements Serializable {
    /**
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);

    /**
     * The message of this Commit.
     */
    private String message;

    /**
     * commit 创建时间
     */
    private Date creatTime;

    /**
     * 父1 commitId
     */
    private String parentId1;

    /**
     * 父2 commitId
     */
    private String parentId2;

    /**
     * 提交文件 k: path, v: blob hash value
     */
    private Map<String, String> tree;

    /**
     * 初始提交
     */
    public Commit(String message, String parentId) {
        this.message = message;
        // Unix 纪元时间
        this.creatTime = new Date(0L);
        tree = new HashMap<>();
        if (parentId != null) {
            // todo
        }
    }

    public Map<String, String> getTree() {
        return tree;
    }

    public String getCreatTime() {
        return DATE_FORMAT.format(this.creatTime);
    }

    public void setCreatTime(Date creatTime) {
        this.creatTime = creatTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
