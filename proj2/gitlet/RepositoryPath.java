package gitlet;

import java.io.File;

import static gitlet.Utils.join;

/**
 * @author 苍镜月
 * @version 1.0
 * @implNote 仓库文件路径
 */

public class RepositoryPath {

    private final File CWD;

    public RepositoryPath(String cwd) {
        this.CWD = new File(cwd);
    }

    /**
     * The current working directory.
     */
    public File CWD() {
        return CWD;
    }

    /**
     * The .gitlet directory.
     */
    public File GITLET_DIR() {
        return join(CWD, ".gitlet");
    }

    /**
     * 暂存区
     */
    public File STAGE() {
        return join(GITLET_DIR(), "stage");
    }

    /**
     * 存放 commit 和 blob 文件夹
     */
    public File OBJECTS_DIR() {
        return join(GITLET_DIR(), "objects");
    }

    /**
     * 存放 commit 的文件夹
     */
    public File COMMITS_DIR() {
        return join(OBJECTS_DIR(), "commits");
    }

    /**
     * 存放 blob 的文件夹
     */
    public File BLOBS_DIR() {
        return join(OBJECTS_DIR(), "blobs");
    }

    /**
     * 引用文件夹
     */
    public File REFS_DIR() {
        return join(GITLET_DIR(), "refs");
    }

    /**
     * 头指针文件夹(branch)
     */
    public File HEADS_DIR() {
        return join(REFS_DIR(), "heads");
    }

    /**
     * 当前头指针
     */
    public File HEAD() {
        return join(GITLET_DIR(), "HEAD");
    }

    /**
     * REMOTE 对象
     */
    public File REMOTE() {
        return join(GITLET_DIR(), "REMOTE");
    }

    /**
     * 远程分支文件夹(remotes)
     */
    public File REMOTES_DIR() {
        return join(REFS_DIR(), "remotes");
    }

    /**
     * 远程分支头部
     */
    public File FETCH_HEAD() {
        return join(GITLET_DIR(), "FETCH_HEAD");
    }
}