package gitlet;

import java.io.File;

import static gitlet.Utils.join;

// TODO: any imports you need here

/**
 * Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 * @author TODO
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /**
     * The .gitlet directory.
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    /**
     * 暂存区
     */
    public static final File STAGE = join(GITLET_DIR, "stage");

    /* TODO: fill in the rest of this class. */

    public static void init() {
        // 文件夹已存在
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            return;
        }
        // 创建.gitlet文件夹
        GITLET_DIR.mkdir();
        // 创建 stage 文件
        Stage stage = new Stage();
        saveStage(stage);
    }

    public static void saveStage(Stage stage) {
        Utils.writeObject(STAGE, stage);
    }

    private static String hash(String fileName) {
        File file = join(CWD, fileName);
        if (!file.exists()) {
            throw Utils.error("File does not exist.");
        }
        return Utils.sha1(file);
    }

    public static void add(String fileName) {
        Stage stage = Utils.readObject(STAGE, Stage.class);
        stage.addFile(fileName, hash(fileName));
    }
}
