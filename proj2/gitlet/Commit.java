package gitlet;

// TODO: any imports you need here

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static gitlet.Utils.serialize;
import static gitlet.Utils.sha1;

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
    public Commit(String message, Commit parentCommit, Date createTime) {
        this.message = message;
        this.creatTime = createTime;
        tree = new HashMap<>();
        if (parentCommit != null) {
            this.parentId1 = parentCommit.getKey();
            tree.putAll(parentCommit.getTree());
        }
    }

    /**
     * 初始提交
     * @return commit
     */
    public static Commit initialCommit() {
        // 初始提交，Unix 纪元时间
        return new Commit("initial commit", null, new Date(0L));
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

    public String getKey() {
        return sha1(serialize(this));
    }

    public boolean hasFile(String fileName) {
        return this.tree.containsKey(fileName);
    }

    @Override
    public String toString() {
        return "Commit{" +
                "message='" + message + '\'' +
                ", creatTime=" + creatTime +
                ", parentId1='" + parentId1 + '\'' +
                ", parentId2='" + parentId2 + '\'' +
                ", tree=" + tree +
                '}';
    }
}
