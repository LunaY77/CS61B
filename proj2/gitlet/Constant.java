package gitlet;

import java.io.File;

import static gitlet.Utils.join;

/**
 * @author 苍镜月
 * @version 1.0
 * @implNote
 */

public interface Constant {
    /*
      List all instance variables of the Repository class here with a useful
      comment above them describing what that variable represents and how that
      variable is used. We've provided two examples for you.
     */

    /**
     * The current working directory.
     */
    File CWD = new File(System.getProperty("user.dir"));

    /**
     * The .gitlet directory.
     */
    File GITLET_DIR = join(CWD, ".gitlet");

    /**
     * 暂存区
     */
    File STAGE = join(GITLET_DIR, "stage");

    /**
     * 初始提交消息
     */
    String INITIAL_COMMIT = "initial commit";

    /**
     * 存放 commit 和 blob 文件夹
     */
    File OBJECTS_DIR = join(GITLET_DIR, "objects");

    /**
     * 存放 commit 的文件夹
     */
    File COMMITS_DIR = join(OBJECTS_DIR, "commits");

    /**
     * 存放 blob 的文件夹
     */
    File BLOBS_DIR = join(OBJECTS_DIR, "blobs");

    /**
     * 引用文件夹
     */
    File REFS_DIR = join(GITLET_DIR, "refs");

    /**
     * 头指针文件夹(branch)
     */
    File HEADS_DIR = join(REFS_DIR, "heads");

    /**
     * 当前头指针
     */
    File HEAD = join(GITLET_DIR, "HEAD");

    /**
     * REMOTE 对象
     */
    File REMOTE = join(GITLET_DIR, "REMOTE");

    /**
     * 远程分支文件夹(remotes)
     */
    File REMOTES_DIR = join(REFS_DIR, "remotes");

    /**
     * 远程分支头部
     */
    File FETCH_HEAD = join(GITLET_DIR, "FETCH_HEAD");
}
