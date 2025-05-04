package gitlet;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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
        if (blobKey == null || !key.equals(curCommit.getBlobKey(fileName))) {
            createAndSaveBlob(key, fileContent, fileName);
            stage.addFile(fileName, key);
        }
    }

    /**
     * 创建并保存 Blob
     *
     * @param fileContent 文件内容
     * @param fileName 文件名
     */
    private static void createAndSaveBlob(String key, byte[] fileContent, String fileName) {
        Blob blob = new Blob(key, fileContent, fileName);
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
        saveBranch(getCurrBranch(), commit.getKey());
        // 清空暂存区
        cleanStage();
    }

    /**
     * 初始提交 + 初始化分支
     */
    private static void initialCommitAndBranch() {
        // 初始提交
        Commit commit = Commit.initialCommit();
        saveCommit(commit);
        // 初始化分支
        saveBranchAndCheckout("master", commit.getKey());
    }

    /**
     * 保存分支信息同时将头指针指向该分支
     * @param branchName 分支名
     * @param commitKey commitId
     */
    public static void saveBranchAndCheckout(String branchName, String commitKey) {
        writeContents(HEAD, branchName);
        writeContents(join(HEADS_DIR, branchName), commitKey);
    }

    /**
     * 保存分支信息
     * @param branchName 分支名
     * @param commitKey commitId
     */
    private static void saveBranch(String branchName, String commitKey) {
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
        List<String> matchingCommits = findMatchingCommits(commitId);
        // 如果文件不止一个 或者 文件不存在
        if (matchingCommits.size() != 1 || !join(COMMITS_DIR, matchingCommits.get(0)).exists()) {
            throw error("No commit with that id exists.");
        }
        return readObject(join(COMMITS_DIR, commitId), Commit.class);
    }

    private static List<String> findMatchingCommits(String prefix) {
        List<String> commitKeys = plainFilenamesIn(COMMITS_DIR);
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
     * 根据分支名获取分支
     * @param branchName 分支名
     * @return Head Commit Key
     */
    private static String getBranch(String branchName) {
        List<String> branches = plainFilenamesIn(HEADS_DIR);
        if (branches == null || branches.stream().noneMatch(b -> b.equals(branchName))) {
            throw error("No such branch exists.");
        }
        return readContentsAsString(join(HEADS_DIR, branchName));
    }

    /**
     * 如果分支名已存在则抛出异常
     * @param branchName 分支名
     */
    private static void checkBranchExistsAndThrow(String branchName) {
        if (join(HEADS_DIR, branchName).exists()) {
            throw error("A branch with that name already exists.");
        }
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

    /**
     * status 当前分支状态
     */
    public static void status() {
        Stage stage = getStage();

        // Branches
        String currBranch = getCurrBranch();
        List<String> branches = plainFilenamesIn(HEADS_DIR);
        message("%s", "=== Branches ===");
        for (String branch : branches) {
            if (branch.equals(currBranch)) {
                message("*%s", branch);
            } else {
                message("%s", branch);
            }
        }
        System.out.println();

        // Staged Files
        message("%s", "=== Staged Files ===");
        for (String filePath : stage.getAddFiles().keySet()) {
            message("%s", filePath);
        }
        System.out.println();

        // Removed Files
        message("%s", "=== Removed Files ===");
        stage.getRemoveFiles().forEach(System.out::println);
        System.out.println();

        // todo === Modifications Not Staged For Commit ===
        message("%s", "=== Modifications Not Staged For Commit ===");
        System.out.println();

        // todo === Untracked Files ===
        message("%s", "=== Untracked Files ===");
        System.out.println();
    }

    /**
     * checkout -- [file name]
     * checkout [commit id] -- [file name]
     * @param commitId 提交 key
     * @param fileName 文件名
     */
    public static void checkoutCommit(String commitId, String fileName) {
        Commit commit = Optional.ofNullable(commitId)
                .map(Repository::getCommit)
                .orElseGet(Repository::getCurrCommit);
        Blob blob = commit.getBlob(fileName);
        if (blob == null) {
            throw error("File does not exist in that commit.");
        }
        writeBlobToCWD(blob);
    }

    /**
     * 将 Blob 写入到工作目录
     * @param blob Blob
     */
    private static void writeBlobToCWD(Blob blob) {
        File file = join(CWD, blob.getFileName());
        writeContents(file, new String(blob.getContent(), StandardCharsets.UTF_8));
    }

    /**
     * checkout [branch name]
     * @param branchName 分支名称
     */
    public static void checkoutBranch(String branchName) {
        String currBranch = getCurrBranch();
        // checkout 当前分支
        if (currBranch.equals(branchName)) {
            message("%s", "No need to checkout the current branch.");
            return;
        }
        // 找到对应的分支Head
        String commitKey = getBranch(branchName);
        Commit targetCommit = getCommit(commitKey);
        Commit currCommit = getCurrCommit();

        // 如果工作目录中存在一个未跟踪的文件，且目标分支中也存在
        for (String fileName : targetCommit.getTree().keySet()) {
            if (!currCommit.hasFile(fileName) && join(CWD, fileName).exists()) {
                throw error("There is an untracked file in the way; delete it, or add and commit it first.");
            }
        }

        // checkout
        saveBranchAndCheckout(branchName, commitKey);
        checkout(currCommit, targetCommit);
    }

    /**
     * checkout 从 from 分支 到 to 分支
     * @param from from commit
     * @param to to commit
     */
    private static void checkout(Commit from, Commit to) {
        // 删除 from 存在的文件但是 to 不存在
        for (String fileName : from.getTree().keySet()) {
            if (!to.hasFile(fileName)) {
                join(CWD, fileName).delete();
            }
        }
        for (String fileName : to.getTree().keySet()) {
            Blob blob = to.getBlob(fileName);
            if (blob == null) {
                throw error("File does not exist in that commit.");
            }
            writeBlobToCWD(blob);
        }
        cleanStage();
    }

    /**
     * 清空暂存区
     */
    private static void cleanStage() {
        getStage().clear();
    }

    /**
     * branch 创建新分支
     * @param branchName 分支名
     */
    public static void branch(String branchName) {
        checkBranchExistsAndThrow(branchName);
        saveBranch(branchName, getCurrCommitId());
    }
}
