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

    /**
     * 向暂存区添加新的文件
     * @param fileName 文件名
     * @param hash 哈希值
     */
    public void addFile(String fileName, String hash) {
        addFiles.put(fileName, hash);
        Repository.saveStage(this);
    }

    /**
     * 暂存区是否为空
     * @return 暂存区是否为空
     */
    public boolean isEmpty() {
        return addFiles.isEmpty() && removeFiles.isEmpty();
    }

    /**
     * 清空暂存区
     */
    public void clear() {
        addFiles.clear();
        removeFiles.clear();
        Repository.saveStage(this);
    }

    public Map<String, String> getAddFiles() {
        return addFiles;
    }

    public Map<String, String> getRemoveFiles() {
        return removeFiles;
    }
}
