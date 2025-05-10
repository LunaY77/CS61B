package gitlet;

// TODO: any imports you need here

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Constant.BLOBS_DIR;
import static gitlet.Utils.*;

/**
 * Represents a gitlet commit object.
 * does at a high level.
 *
 * @author 苍镜月
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
     * commitKey
     */
    private String commitKey;

    public Commit(String message, Commit parentCommit, Date createTime) {
        this.message = message;
        this.creatTime = createTime;
        tree = new HashMap<>();
        if (parentCommit != null) {
            this.parentId1 = parentCommit.getKey();
            Map<String, String> parentTree = parentCommit.getTree();
            for (String filePath : parentTree.keySet()) {
                tree.put(filePath, parentTree.get(filePath));
            }
        }
    }

    public Commit(String message, Commit currCommit, Commit otherCommit, Date date) {
        this(message, currCommit, date);
        this.parentId2 = otherCommit.getKey();
    }

    /**
     * 初始提交
     *
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
        if (commitKey == null) {
            commitKey = sha1(serialize(this));
        }
        return commitKey;
    }

    public boolean hasFile(String fileName) {
        return this.tree.containsKey(fileName);
    }

    public String getBlobKey(String fileName) {
        return this.tree.get(fileName);
    }

    public boolean isMerge() {
        return parentId1 != null && parentId2 != null;
    }

    public String getFirstParentKey() {
        return parentId1;
    }

    public String getSecondParentKey() {
        return parentId2;
    }

    @Override
    public String toString() {
        return isMerge() ? printMerge() : print();
    }

    private String print() {
        return "===\n" +
                "commit " + getKey() + "\n" +
                "Date: " + getCreatTime() + "\n" +
                getMessage() + "\n";
    }

    private String printMerge() {
        return "===\n" +
                "commit " + getKey() + "\n" +
                "Merge: " + getFirstParentKey().substring(0, 7) + " " + getSecondParentKey().substring(0, 7) + "\n" +
                "Date: " + getCreatTime() + "\n" +
                getMessage() + "\n";
    }

    public Blob getBlob(String fileName) {
        String blobKey = getBlobKey(fileName);
        if (blobKey == null) return null;
        return readObject(join(BLOBS_DIR, blobKey), Blob.class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Commit commit = (Commit) o;
        return Objects.equals(message, commit.message) && Objects.equals(creatTime, commit.creatTime) && Objects.equals(parentId1, commit.parentId1) && Objects.equals(parentId2, commit.parentId2) && Objects.equals(tree, commit.tree) && Objects.equals(commitKey, commit.commitKey);
    }
}