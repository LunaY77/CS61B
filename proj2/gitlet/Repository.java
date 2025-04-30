package gitlet;

import java.io.File;
import java.util.Objects;

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
        // 创建 stage 文件
        Stage stage = new Stage();
        saveStage(stage);
        // todo initial commit
        // todo master branch

    }

    public static void saveCommit(Commit commit) {
        writeObject(join(GITLET_DIR, sha1(commit)), commit);
    }

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

    public static void commit(Commit commit) {
        // todo
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
