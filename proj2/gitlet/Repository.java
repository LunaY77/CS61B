package gitlet;

import java.io.File;
import java.util.Date;

import static gitlet.Constant.*;
import static gitlet.Utils.*;

/**
 * Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *  <br>
 * .gitlet
 * ├── objects
 *     ├── commit
 *     └── blob
 * └── refs
 *     ├── branch
 * └── stage
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
        // 创建 obj 文件夹
        mkdir(OBJECTS_DIR);
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
        writeObject(join(OBJECTS_DIR, commit.getKey()), commit);
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
        if (blobKey == null || key.equals(curCommit.getTree().get(blobKey))) {
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
        writeObject(join(OBJECTS_DIR, key), blob);
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
        for (String key : stage.getRemoveFiles().keySet()) {
            commit.getTree().remove(key);
        }
        // 保存 commit
        saveCommit(commit);
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
        writeContents(HEAD, "master");
        writeContents(join(HEADS_DIR, "master"), commit.getKey());
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
        return readObject(join(OBJECTS_DIR, currCommitId), Commit.class);
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
}
