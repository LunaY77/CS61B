package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static gitlet.Utils.*;

/**
 * @author 苍镜月
 * @version 1.0
 * @implNote 仓库文件路径
 */

public class RepositoryPath implements Serializable {

    private static final String USER_DIR = System.getProperty("user.dir");

    private final File CWD;

    /**
     * 远程仓库路径
     *
     * @param remotePath 远程仓库路径
     */
    public RepositoryPath(String remotePath) {
        this.CWD = join(new File(USER_DIR), remotePath);
    }

    /**
     * 本地仓库路径
     */
    public RepositoryPath() {
        this.CWD = new File(USER_DIR);
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

    /**
     * 获取远程分支对象
     *
     * @return 远程分支对象
     */
    public Remote getRemote() {
        return readObject(REMOTE(), Remote.class);
    }

    /**
     * 获取当前暂存区信息
     *
     * @return 暂存区信息
     */
    public Stage getStage() {
        return readObject(STAGE(), Stage.class);
    }

    /**
     * 从 objects 文件夹下获取当前 Commit
     *
     * @return 当前 Commit
     */
    public Commit getCurrCommit() {
        String currCommitId = getCurrCommitId();
        return readObject(join(COMMITS_DIR(), currCommitId), Commit.class);
    }

    /**
     * 从 objects 文件夹下获取 Commit
     *
     * @param commitId commit key
     * @return Commit
     */
    public Commit getCommit(String commitId) {
        if (commitId == null) {
            return null;
        }
        List<String> matchingCommits = findMatchingCommits(commitId);
        // 如果文件不止一个 或者 文件不存在
        if (matchingCommits.size() != 1 || !join(COMMITS_DIR(), matchingCommits.get(0)).exists()) {
            errorAndExit("No commit with that id exists.");
        }
        return readObject(join(COMMITS_DIR(), matchingCommits.get(0)), Commit.class);
    }

    /**
     * 找到所有以 prefix 开头的 commit
     *
     * @param prefix 前缀
     * @return prefix 开头的 commit
     */
    public List<String> findMatchingCommits(String prefix) {
        List<String> commitKeys = plainFilenamesIn(COMMITS_DIR());
        List<String> res = new ArrayList<>();
        for (String commitKey : commitKeys) {
            if (commitKey.startsWith(prefix)) {
                res.add(commitKey);
            }
        }
        return res;
    }

    /**
     * 从 ref/heads 文件夹中获取当前 Commit ID
     *
     * @return 当前 Commit ID
     */
    public String getCurrCommitId() {
        String currBranch = getCurrBranch();
        return readContentsAsString(join(HEADS_DIR(), currBranch));
    }

    /**
     * 从 HEAD 文件中获取当前分支名字
     *
     * @return 当前分支名字
     */
    public String getCurrBranch() {
        return readContentsAsString(HEAD());
    }

    /**
     * 根据分支名获取分支
     *
     * @param branchName 分支名
     * @return Head Commit Key
     */
    public String getBranch(String branchName) {
        List<String> branches = plainFilenamesIn(HEADS_DIR());
        if (branches == null || branches.stream().noneMatch(b -> b.equals(branchName))) {
            errorAndExit("No such branch exists.");
        }
        return readContentsAsString(join(HEADS_DIR(), branchName));
    }

    /**
     * 如果分支名已存在则抛出异常
     *
     * @param branchName 分支名
     */
    public void checkBranchExistsAndThrow(String branchName) {
        if (join(HEADS_DIR(), branchName).exists()) {
            errorAndExit("A branch with that name already exists.");
        }
    }

    /**
     * 如果分支名不存在则抛出异常
     *
     * @param branchName 分支名
     */
    public void checkBranchNotExistsAndThrow(String branchName) {
        if (!join(HEADS_DIR(), branchName).exists()) {
            errorAndExit("A branch with that name does not exist.");
        }
    }

    /**
     * 创建并保存 Blob
     *
     * @param fileContent 文件内容
     * @param fileName    文件名
     */
    public void createAndSaveBlob(String key, byte[] fileContent, String fileName) {
        Blob blob = new Blob(key, fileContent, fileName);
        writeObject(join(BLOBS_DIR(), key), blob);
    }

    /**
     * 保存分支信息同时将头指针指向该分支
     *
     * @param branchName 分支名
     * @param commitKey  commitId
     */
    public void saveBranchAndCheckout(String branchName, String commitKey) {
        writeContents(HEAD(), branchName);
        writeContents(join(HEADS_DIR(), branchName), commitKey);
    }

    /**
     * 保存分支信息
     *
     * @param branchName 分支名
     * @param commitKey  commitId
     */
    public void saveBranch(String branchName, String commitKey) {
        writeContents(join(HEADS_DIR(), branchName), commitKey);
    }

    /**
     * 保存远程分支对象
     *
     * @param remote 远程分支对象
     */
    public void saveRemote(Remote remote) {
        writeObject(REMOTE(), remote);
    }

    /**
     * 保存 commit
     *
     * @param commit {@link Commit}
     */
    public void saveCommit(Commit commit) {
        writeObject(join(COMMITS_DIR(), commit.getKey()), commit);
    }

    /**
     * 保存 stage
     *
     * @param stage {@link Stage}
     */
    public void saveStage(Stage stage) {
        writeObject(STAGE(), stage);
    }
}