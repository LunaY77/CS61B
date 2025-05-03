package gitlet;

import java.io.File;
import java.util.Date;
import java.util.List;

import static gitlet.Constant.*;
import static gitlet.Utils.*;

/**
 * Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *  <br>
 * .gitlet
 * ├── objects
 *     ├── commits
 *     └── blobs
 * └── refs
 *     ├── branch
 * └── stage
 * └── HEAD
 * @author 苍镜月
 */
public class Repository {

    public static void init() {
        // 文件夹已存在
        if (GITLET_DIR.exists()) {
            message("A Gitlet version-control system already exists in the current directory.");
            return;
        }
        // 创建.gitlet文件夹
        mkdir(GITLET_DIR);
        // 创建 obj 文件夹及其子文件夹
        mkdir(OBJECTS_DIR);
        mkdir(COMMITS_DIR);
        mkdir(BLOBS_DIR);
        // 创建 refs 文件夹及其子文件夹
        mkdir(REFS_DIR);
        mkdir(HEADS_DIR);
        // 创建 stage 文件
        initStage();
        // 初始提交，初始化分支
        initialCommitAndBranch();
    }

    /**
     * 初始化暂存区
     */
    private static void initStage() {
        Stage stage = new Stage();
        saveStage(stage);
    }

    /**
     * 保存 commit
     * @param commit {@link Commit}
     */
    public static void saveCommit(Commit commit) {
        writeObject(join(COMMITS_DIR, commit.getKey()), commit);
    }

    /**
     * 保存 stage
     * @param stage {@link Stage}
     */
    public static void saveStage(Stage stage) {
        writeObject(STAGE, stage);
    }

    /**
     * add 添加文件到暂存区
     * @param fileName 文件名
     */
    public static void add(String fileName) {
        File file = join(CWD, fileName);
        // 文件不存在
        if (!file.exists()) {
            throw error("File does not exist.");
        }
        byte[] fileContent = readContents(file);
        String key = sha1(fileContent);
        // 读取暂存区
        Stage stage = getStage();
        // 如果有删除记录，删除 rm
        stage.getRemoveFiles().remove(fileName);
        // 如果不存在 blob 且无同一 hash 的 blob，则添加 blob
        Commit curCommit = getCurrCommit();
        String blobKey = curCommit.getTree().get(fileName);
        if (blobKey == null || !key.equals(curCommit.getTree().get(fileName))) {
            createAndSaveBlob(key, fileContent);
            stage.addFile(fileName, key);
        }
    }

    /**
     * 创建并保存 Blob
     * @param fileContent 文件内容
     */
    private static void createAndSaveBlob(String key, byte[] fileContent) {
        Blob blob = new Blob(key, fileContent);
        writeObject(join(BLOBS_DIR, key), blob);
    }

    /**
     * commit 将暂存区文件提交
     * @param message commit message
     */
    public static void commit(String message) {
        // 暂存区中无文件
        Stage stage = getStage();
        if (stage.isEmpty()) {
            throw error("No changes added to the commit.");
        }
        // 创建新的commit
        Commit commit = new Commit(message, getCurrCommit(), new Date());
        // 添加暂存区文件
        commit.getTree().putAll(stage.getAddFiles());
        // 删除暂存区中标记删除的文件
        for (String key : stage.getRemoveFiles()) {
            commit.getTree().remove(key);
        }
        // 保存 commit
        saveCommit(commit);
        // 更新 branch
        saveBranch("master", commit.getKey());
        // 清空暂存区
        stage.clear();
    }

    /**
     * 初始提交 + 初始化分支
     */
    private static void initialCommitAndBranch() {
        // 初始提交
        Commit commit = Commit.initialCommit();
        saveCommit(commit);
        // 初始化分支
        saveBranch("master", commit.getKey());
    }

    public static void saveBranch(String branchName, String commitKey) {
        writeContents(HEAD, branchName);
        writeContents(join(HEADS_DIR, branchName), commitKey);
    }

    /**
     * 获取当前暂存区信息
     * @return 暂存区信息
     */
    private static Stage getStage() {
        return readObject(STAGE, Stage.class);
    }

    /**
     * 从 objects 文件夹下获取当前 Commit
     * @return 当前 Commit
     */
    private static Commit getCurrCommit() {
        String currCommitId = getCurrCommitId();
        return readObject(join(COMMITS_DIR, currCommitId), Commit.class);
    }

    /**
     * 从 objects 文件夹下获取 Commit
     * @param commitId commit key
     * @return Commit
     */
    private static Commit getCommit(String commitId) {
        if (commitId == null) {
            return null;
        }
        return readObject(join(COMMITS_DIR, commitId), Commit.class);
    }

    /**
     * 从 ref/heads 文件夹中获取当前 Commit ID
     * @return 当前 Commit ID
     */
    private static String getCurrCommitId() {
        String currBranch = getCurrBranch();
        return readContentsAsString(join(HEADS_DIR, currBranch));
    }

    /**
     * 从 HEAD 文件中获取当前分支名字
     * @return 当前分支名字
     */
    private static String getCurrBranch() {
        return readContentsAsString(HEAD);
    }

    /**
     * rm 删除暂存区的文件或者已提交的文件
     * @param fileName 文件名
     */
    public static void rm(String fileName) {
        Stage stage = getStage();
        Commit commit = getCurrCommit();
        if (!stage.isAdded(fileName) && !commit.hasFile(fileName)) {
            throw error("No reason to remove the file.");
        }
        if (stage.isAdded(fileName)) {
            stage.cancelAdd(fileName);
        }
        else if (commit.hasFile(fileName)) {
            stage.removeFile(fileName);

            File file = new File(fileName);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    /**
     * log 日志，从当前提交开始到初始提交
     */
    public static void log() {
        Commit commit = getCurrCommit();
        while (commit != null) {
            message("%s", commit);
            commit = getCommit(commit.getFirstParentKey());
        }
    }

    /**
     * global-log 全局日志
     */
    public static void globalLog() {
        List<String> commitKeys = plainFilenamesIn(COMMITS_DIR);
        for (String commitKey : commitKeys) {
            message("%s", getCommit(commitKey));
        }
    }

    /**
     * find 找到指定提交消息的 CommitId
     * @param message 提交消息
     */
    public static void find(String message) {
        List<String> commitKeys = plainFilenamesIn(COMMITS_DIR);
        boolean exist = false;
        for (String commitKey : commitKeys) {
            if (getCommit(commitKey).getMessage().equals(message)) {
                message("%s", commitKey);
                exist = true;
            }
        }
        if (!exist) {
            throw error("Found no commit with that message.");
        }
    }
}
