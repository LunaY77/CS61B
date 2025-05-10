package gitlet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 苍镜月
 * @version 1.0
 * @implNote
 */

public class Stage implements Serializable {

    private final Map<String, String> addFiles;

    private final List<String> removeFiles;

    public Stage() {
        addFiles = new HashMap<>();
        removeFiles = new ArrayList<>();
    }

    /**
     * 向暂存区添加新的文件
     *
     * @param fileName 文件名
     * @param hash     哈希值
     */
    public void addFile(String fileName, String hash) {
        addFiles.put(fileName, hash);
        Repository.saveStage(this);
    }

    /**
     * 删除暂存区中的添加
     *
     * @param fileName 文件名
     */
    public void cancelAdd(String fileName) {
        addFiles.remove(fileName);
        Repository.saveStage(this);
    }

    /**
     * 撤销暂存区的删除
     *
     * @param fileName 文件名
     */
    public void cancelRemove(String fileName) {
        removeFiles.remove(fileName);
        Repository.saveStage(this);
    }

    /**
     * 文件是否添加到暂存区
     *
     * @param fileName 文件名
     * @return 是否添加
     */
    public boolean isAdded(String fileName) {
        return addFiles.containsKey(fileName);
    }

    /**
     * 文件是否在暂存区中删除
     *
     * @param fileName 文件名
     * @return 是否删除
     */
    public boolean isRemove(String fileName) {
        return removeFiles.contains(fileName);
    }

    /**
     * 暂存区是否为空
     *
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

    public List<String> getRemoveFiles() {
        return removeFiles;
    }

    /**
     * 标记删除
     *
     * @param fileName 文件名
     */
    public void removeFile(String fileName) {
        removeFiles.add(fileName);
        Repository.saveStage(this);
    }

    @Override
    public String toString() {
        return "Stage{" +
                "addFiles=" + addFiles +
                ", removeFiles=" + removeFiles +
                '}';
    }
}
