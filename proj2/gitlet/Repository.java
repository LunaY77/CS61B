package gitlet;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static gitlet.Utils.*;

/**
 * Represents a gitlet repository.
 * does at a high level.
 * <br>
 *
 * @author 苍镜月
 */
public class Repository {

    public static final RepositoryPath REPO_PATH = new RepositoryPath();

    public static void init() {
        // 文件夹已存在
        if (REPO_PATH.GITLET_DIR().exists()) {
            message("A Gitlet version-control system already exists in the current directory.");
            return;
        }
        // 创建.gitlet文件夹
        mkdir(REPO_PATH.GITLET_DIR());
        // 创建 obj 文件夹及其子文件夹
        mkdir(REPO_PATH.OBJECTS_DIR());
        mkdir(REPO_PATH.COMMITS_DIR());
        mkdir(REPO_PATH.BLOBS_DIR());
        // 创建 refs 文件夹及其子文件夹
        mkdir(REPO_PATH.REFS_DIR());
        mkdir(REPO_PATH.HEADS_DIR());
        mkdir(REPO_PATH.REMOTES_DIR());
        // 创建 Remote 对象
        initRemote();
        // 创建 stage 文件
        initStage();
        // 初始提交，初始化分支
        initialCommitAndBranch();
    }

    /**
     * 初始化 Remote 对象
     */
    private static void initRemote() {
        Remote remote = new Remote();
        REPO_PATH.saveRemote(remote);
    }

    /**
     * 初始化暂存区
     */
    private static void initStage() {
        Stage stage = new Stage();
        REPO_PATH.saveStage(stage);
    }

    /**
     * add 添加文件到暂存区
     *
     * @param fileName 文件名
     */
    public static void add(String fileName) {
        File file = join(REPO_PATH.CWD(), fileName);
        // 文件不存在
        if (!file.exists()) {
            errorAndExit("File does not exist.");
        }
        byte[] fileContent = readContents(file);
        String key = sha1(fileContent);
        // 读取暂存区
        Stage stage = REPO_PATH.getStage();
        // 如果有删除记录，删除 rm
        stage.cancelRemove(fileName);
        // 如果不存在 blob 且无同一 hash 的 blob，则添加 blob
        Commit curCommit = REPO_PATH.getCurrCommit();
        String blobKey = curCommit.getBlobKey(fileName);
        // 如果当前提交中没有该文件，或者文件内容已经改变，则添加
        if (!key.equals(blobKey)) {
            REPO_PATH.createAndSaveBlob(key, fileContent, fileName);
            stage.addFile(fileName, key);
        }
    }

    /**
     * commit 将暂存区文件提交
     *
     * @param message commit message
     */
    public static void commit(String message) {
        // 创建新的commit
        Commit commit = new Commit(message, REPO_PATH.getCurrCommit(), new Date());
        doCommit(commit);
    }

    /**
     * merge commit
     *
     * @param message     commit message
     * @param otherCommit 另一个 commit
     */
    public static void commit(String message, Commit otherCommit) {
        Commit commit = new Commit(message, REPO_PATH.getCurrCommit(), otherCommit, new Date());
        doCommit(commit);
    }

    /**
     * commit
     *
     * @param commit {@link Commit}
     */
    private static void doCommit(Commit commit) {
        Stage stage = REPO_PATH.getStage();
        // 添加暂存区文件
        Map<String, String> tree = commit.getTree();
        Map<String, String> addFiles = stage.getAddFiles();
        for (String filePath : addFiles.keySet()) {
            tree.put(filePath, addFiles.get(filePath));
        }
        // 删除暂存区中标记删除的文件
        for (String key : stage.getRemoveFiles()) {
            tree.remove(key);
        }
        // 保存 commit
        REPO_PATH.saveCommit(commit);
        // 更新 branch
        REPO_PATH.saveBranch(REPO_PATH.getCurrBranch(), commit.getKey());
        // 清空暂存区
        cleanStage();
    }

    /**
     * 初始提交 + 初始化分支
     */
    private static void initialCommitAndBranch() {
        // 初始提交
        Commit commit = Commit.initialCommit();
        REPO_PATH.saveCommit(commit);
        // 初始化分支
        REPO_PATH.saveBranchAndCheckout("master", commit.getKey());
    }


    /**
     * rm 删除暂存区的文件或者已提交的文件
     *
     * @param fileName 文件名
     */
    public static void rm(String fileName) {
        Stage stage = REPO_PATH.getStage();
        Commit commit = REPO_PATH.getCurrCommit();
        if (!stage.isAdded(fileName) && !commit.hasFile(fileName)) {
            errorAndExit("No reason to remove the file.");
        }
        if (stage.isAdded(fileName)) {
            stage.cancelAdd(fileName);
        }
        if (commit.hasFile(fileName)) {
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
        Commit commit = REPO_PATH.getCurrCommit();
        while (commit != null) {
            message("%s", commit);
            commit = REPO_PATH.getCommit(commit.getFirstParentKey());
        }
    }

    /**
     * global-log 全局日志
     */
    public static void globalLog() {
        List<String> commitKeys = plainFilenamesIn(REPO_PATH.COMMITS_DIR());
        for (String commitKey : commitKeys) {
            message("%s", REPO_PATH.getCommit(commitKey));
        }
    }

    /**
     * find 找到指定提交消息的 CommitId
     *
     * @param message 提交消息
     */
    public static void find(String message) {
        List<String> commitKeys = plainFilenamesIn(REPO_PATH.COMMITS_DIR());
        boolean exist = false;
        for (String commitKey : commitKeys) {
            if (REPO_PATH.getCommit(commitKey).getMessage().equals(message)) {
                message("%s", commitKey);
                exist = true;
            }
        }
        if (!exist) {
            errorAndExit("Found no commit with that message.");
        }
    }

    /**
     * status 当前分支状态
     */
    public static void status() {
        Stage stage = REPO_PATH.getStage();
        Commit commit = REPO_PATH.getCurrCommit();

        // === Branches ===
        String currBranch = REPO_PATH.getCurrBranch();
        List<String> branches = plainFilenamesIn(REPO_PATH.HEADS_DIR());
        message("=== Branches ===");
        for (String branch : branches) {
            if (branch.equals(currBranch)) {
                message("*%s", branch);
            } else {
                message("%s", branch);
            }
        }
        System.out.println();

        // === Staged Files ===
        message("=== Staged Files ===");
        Set<String> addFiles = stage.getAddFiles().keySet();
        addFiles.forEach(Utils::message);
        System.out.println();

        // === Removed Files ===
        message("=== Removed Files ===");
        Set<String> removeFiles = new HashSet<>(stage.getRemoveFiles());
        removeFiles.forEach(Utils::message);
        System.out.println();

        // === Modifications Not Staged For Commit ===
        message("=== Modifications Not Staged For Commit ===");
        Map<String, String> commitTree = commit.getTree();
        Set<String> trackedFiles = commitTree.keySet();
        // 文件路径基于字典序排序
        List<String> sortedFilePaths = trackedFiles.stream().sorted().collect(Collectors.toList());
        for (String filePath : sortedFilePaths) {
            File file = join(REPO_PATH.CWD(), filePath);
            if (file.exists()) {
                // 文件存在，基于sha1哈希值判断是否修改
                String contentKey = sha1(readContents(file));
                if (!Objects.equals(contentKey, commitTree.get(filePath))) {
                    message("%s (modified)", filePath);
                }
            } else {
                // 文件不存在，判断暂存区是否已添加删除，如果没有则打印
                if (!stage.isRemove(filePath)) {
                    message("%s (deleted)", filePath);
                }
            }
        }
        System.out.println();

        // === Untracked Files ===
        message("=== Untracked Files ===");
        List<String> allFiles = plainFilenamesIn(REPO_PATH.CWD());
        for (String file : allFiles) {
            if (!trackedFiles.contains(file)
                    && !addFiles.contains(file)
                    && !removeFiles.contains(file)) {
                message(file);
            }
        }
        System.out.println();
    }

    /**
     * checkout -- [file name]
     * checkout [commit id] -- [file name]
     *
     * @param commitId 提交 key
     * @param fileName 文件名
     */
    public static void checkoutCommit(String commitId, String fileName) {
        Commit commit = Optional.ofNullable(commitId)
                .map(REPO_PATH::getCommit)
                .orElseGet(REPO_PATH::getCurrCommit);
        String blobKey = commit.getBlobKey(fileName);
        Blob blob = REPO_PATH.getBlob(blobKey);
        if (blob == null) {
            errorAndExit("File does not exist in that commit.");
        }
        writeBlobToCWD(blob, fileName);
    }

    /**
     * 将 Blob 写入到工作目录
     *
     * @param blob     Blob
     * @param fileName
     */
    private static void writeBlobToCWD(Blob blob, String fileName) {
        File file = join(REPO_PATH.CWD(), fileName);
        writeContents(file, blob.getContent());
    }

    /**
     * checkout [branch name]
     *
     * @param branchName 分支名称
     */
    public static void checkoutBranch(String branchName) {
        String currBranch = REPO_PATH.getCurrBranch();
        // checkout 当前分支
        if (currBranch.equals(branchName)) {
            message("No need to checkout the current branch.");
            return;
        }
        // 找到对应的分支Head
        String commitKey = REPO_PATH.getBranchNotNull(branchName);
        Commit targetCommit = REPO_PATH.getCommit(commitKey);
        Commit currCommit = REPO_PATH.getCurrCommit();

        // checkout
        checkout(currCommit, targetCommit);
        REPO_PATH.saveBranchAndCheckout(branchName, targetCommit.getKey());
    }

    /**
     * checkout 从 from 分支 到 to 分支
     *
     * @param from base commit
     * @param to   target commit
     */
    private static void checkout(Commit from, Commit to) {
        checkUntrackedFiles(from, to);
        // 删除 from 存在的文件但是 to 不存在
        for (String fileName : from.getTree().keySet()) {
            if (!to.hasFile(fileName)) {
                join(REPO_PATH.CWD(), fileName).delete();
            }
        }
        // 目标分支存在
        for (String fileName : to.getTree().keySet()) {
            String blobKey = to.getBlobKey(fileName);
            Blob blob = REPO_PATH.getBlob(blobKey);
            if (blob == null) {
                errorAndExit("File does not exist in that commit.");
            }
            writeBlobToCWD(blob, fileName);
        }
        cleanStage();
    }

    /**
     * 如果工作目录中存在一个未跟踪的文件，当前分支不存在但目标分支中存在
     *
     * @param from base commit
     * @param to   target commit
     */
    private static void checkUntrackedFiles(Commit from, Commit to) {
        for (String fileName : to.getTree().keySet()) {
            if (!from.hasFile(fileName) && join(REPO_PATH.CWD(), fileName).exists()) {
                errorAndExit("There is an untracked file in the way; delete it, or add and commit it first.");
            }
        }
    }

    /**
     * 清空暂存区
     */
    private static void cleanStage() {
        REPO_PATH.getStage().clear();
    }

    /**
     * branch 创建新分支
     *
     * @param branchName 分支名
     */
    public static void branch(String branchName) {
        REPO_PATH.checkBranchExistsAndThrow(branchName);
        REPO_PATH.saveBranch(branchName, REPO_PATH.getCurrCommitId());
    }

    /**
     * 删除指定的分支
     *
     * @param branchName 分支名
     */
    public static void rmBranch(String branchName) {
        REPO_PATH.checkBranchNotExistsAndThrow(branchName);
        String currBranch = REPO_PATH.getCurrBranch();
        // 不能删除当前分支
        if (currBranch.equals(branchName)) {
            errorAndExit("Cannot remove the current branch.");
        }
        // delete
        join(REPO_PATH.HEADS_DIR(), branchName).delete();
    }

    /**
     * reset
     *
     * @param commitKey 分支 id
     */
    public static void reset(String commitKey) {
        Commit targetCommit = REPO_PATH.getCommit(commitKey);
        Commit currCommit = REPO_PATH.getCurrCommit();
        checkout(currCommit, targetCommit);
        // 依然保持在当前分支，只是 HEAD 可能指向其他分支所在的 commit
        REPO_PATH.saveBranch(REPO_PATH.getCurrBranch(), targetCommit.getKey());
    }

    /**
     * merge 合并分支
     * 总体思路：
     * 1. modified in target but not in base -> target (staged for addition)
     * 2. modified in base but not in target -> base (no need for stage)
     * 3.1. modified in base and target in same way -> same (no need for stage)
     * 3.2 modified in base and target in different way -> conflict
     * 4. not in split nor target but in base -> base (no need for stage)
     * 5. not in split nor base but in target -> target (staged for addition)
     * 6. unmodified in base but not present in target -> remove (staged for deletion)
     * 7. unmodified in target but not present in base -> remain remove (no need for stage)
     *
     * @param branchName 目标分支
     */
    public static void merge(String branchName) {
        // 暂存区是否为空
        if (!REPO_PATH.getStage().isEmpty()) {
            errorAndExit("You have uncommitted changes.");
        }
        // 是否和自己合并
        String currBranchName = REPO_PATH.getCurrBranch();
        if (currBranchName.equals(branchName)) {
            errorAndExit("Cannot merge a branch with itself.");
        }
        // 检查 branch 是否存在
        REPO_PATH.checkBranchNotExistsAndThrow(branchName);
        // 获取当前分支和目标分支
        Commit base = REPO_PATH.getCurrCommit();
        Commit target = REPO_PATH.getCommit(REPO_PATH.getBranchNotNull(branchName));

        checkUntrackedFiles(base, target);

        // 找到相交节点
        Commit splitPoint = REPO_PATH.findSplitPoint(base, target);
        // 1. 如果相交节点是目标节点，表示目标节点是当前节点的祖先
        if (Objects.equals(splitPoint, target)) {
            message("Given branch is an ancestor of the current branch.");
            return;
        }
        // 2. 如果相交节点是当前节点，表示当前节点是目标节点的祖先，则 checkout 到目标节点
        if (Objects.equals(splitPoint, base)) {
            message("Current branch fast-forwarded.");
            checkout(base, target);
            REPO_PATH.saveBranchAndCheckout(branchName, target.getKey());
            return;
        }
        // 3. 合并 merge
        mergeFiles(base, target, splitPoint);
        String commitMessage = String.format("Merged %s into %s.", branchName, currBranchName);
        commit(commitMessage, target);
    }

    /**
     * 合并文件
     *
     * @param base   当前分支
     * @param target 目标分支
     * @param split  公共父节点
     */
    private static void mergeFiles(Commit base, Commit target, Commit split) {
        Stage stage = REPO_PATH.getStage();
        Map<String, String> baseCommitTree = base.getTree();
        Map<String, String> targetCommitTree = target.getTree();
        Map<String, String> splitCommitTree = split.getTree();

        Set<String> files = new HashSet<>();
        files.addAll(baseCommitTree.keySet());
        files.addAll(targetCommitTree.keySet());

        for (String fileName : files) {
            String baseBlobKey = baseCommitTree.get(fileName);
            String targetBlobKey = targetCommitTree.get(fileName);
            String splitBlobKey = splitCommitTree.get(fileName);

            if (!Objects.equals(targetBlobKey, splitBlobKey) && Objects.equals(baseBlobKey, splitBlobKey)) {
                // 1. modified in target but not in base (staged for addition)
                // 5. not in split nor base but in target -> target (staged for addition)
                if (targetBlobKey != null) {
                    checkoutCommit(target.getKey(), fileName);
                    stage.addFile(fileName, targetBlobKey);
                }
                // 6. unmodified in base but not present in target -> remove (staged for deletion)
                else {
                    stage.removeFile(fileName);
                    File file = join(REPO_PATH.CWD(), fileName);
                    if (file.exists()) {
                        file.delete();
                    }
                }
            }
            // 2. modified in base but not in target (no need for stage)
            // 4. not in split nor target but in base -> base (no need for stage)
            // 7. unmodified in target but not present in base -> remain remove (no need for stage)
            if (Objects.equals(targetBlobKey, splitBlobKey) && !Objects.equals(baseBlobKey, splitBlobKey)) {
                continue;
            }
            // 3.1. modified in base and target in same way -> same (no need for stage)
            if (Objects.equals(targetBlobKey, baseBlobKey)) {
                continue;
            }
            // 3.2 modified in base and target in different way -> conflict
            if (!Objects.equals(targetBlobKey, splitBlobKey) && !Objects.equals(baseBlobKey, splitBlobKey)) {
                // 处理冲突
                message("Encountered a merge conflict.");
                byte[] content = ("<<<<<<< HEAD\n" +
                        (baseBlobKey == null ? "" : new String(readObject(join(REPO_PATH.BLOBS_DIR(), baseBlobKey), Blob.class).getContent(), StandardCharsets.UTF_8)) +
                        "=======\n" +
                        (targetBlobKey == null ? "" : new String(readObject(join(REPO_PATH.BLOBS_DIR(), targetBlobKey), Blob.class).getContent(), StandardCharsets.UTF_8)) +
                        ">>>>>>>\n").getBytes(StandardCharsets.UTF_8);
                String mergeBlobKey = sha1(content);
                REPO_PATH.createAndSaveBlob(mergeBlobKey, content, fileName);
                stage.addFile(fileName, mergeBlobKey);
                writeContents(join(REPO_PATH.CWD(), fileName), content);
            }
        }
    }

    /**
     * add-remote 添加远程仓库
     *
     * @param remoteName 远程仓库名
     * @param remotePath 远程仓库路径
     */
    public static void addRemote(String remoteName, String remotePath) {
        Remote remote = REPO_PATH.getRemote();
        remote.addRemote(remoteName, remotePath);
    }

    /**
     * rm-remote 删除远程仓库
     *
     * @param remoteName 远程仓库
     */
    public static void removeRemote(String remoteName) {
        Remote remote = REPO_PATH.getRemote();
        remote.removeRemote(remoteName);
    }

    /**
     * push 推送到远程仓库
     *
     * @param remoteName       远程仓库名
     * @param remoteBranchName 远程分支名
     */
    public static void push(String remoteName, String remoteBranchName) {
        Remote remote = REPO_PATH.getRemote();
        // 1. 检查远程仓库路径合法性
        remote.checkRemotePath(remoteName);
        // 2. 获取远程仓库对应分支
        RepositoryPath remoteRepositoryPath = remote.getRepositoryPath(remoteName);
        String remoteBranch = remoteRepositoryPath.getBranch(remoteBranchName);
        // 3. 如果分支不为空，需要判断 Head Commit 是否在本地历史中
        Commit currCommit = REPO_PATH.getCurrCommit();
        Map<String, Integer> localCommitMap = REPO_PATH.bfs(currCommit);
        Commit remoteCommit = remoteRepositoryPath.getCommit(remoteBranch);
        if (remoteCommit != null) {
            // Head Commit 不存在
            if (!localCommitMap.containsKey(remoteCommit.getKey())) {
                errorAndExit("Please pull down remote changes before pushing.");
            }
        }
        // 4. 向远程仓库复制 commit 和 blob
        localCommitMap.forEach((commitKey, layer) -> {
            Commit commit = REPO_PATH.getCommit(commitKey);
            List<Blob> blobs = commit.getTree().keySet()
                    .stream()
                    .map(fileName -> REPO_PATH.getBlob(commit.getBlobKey(fileName)))
                    .collect(Collectors.toList());
            remoteRepositoryPath.saveBlobs(blobs);
            remoteRepositoryPath.saveCommit(commit);
        });
        remoteRepositoryPath.saveBranch(remoteBranchName, currCommit.getKey());
    }

    /**
     * fetch
     *
     * @param remoteName       远程仓库名
     * @param remoteBranchName 远程分支名
     */
    public static void fetch(String remoteName, String remoteBranchName) {
        Remote remote = REPO_PATH.getRemote();
        // 1. 检查远程仓库路径合法性
        remote.checkRemotePath(remoteName);
        // 2. 获取远程仓库对应的分支
        RepositoryPath remoteRepositoryPath = remote.getRepositoryPath(remoteName);
        String remoteBranch = remoteRepositoryPath.getBranch(remoteBranchName);
        // 3. 判断分支是否存在
        if (remoteBranch == null) {
            errorAndExit("That remote does not have that branch.");
        }
        // 4. 向本地仓库复制 commit 和 blob
        Commit remoteCommit = remoteRepositoryPath.getCommit(remoteBranch);
        Map<String, Integer> remoteCommitMap = remoteRepositoryPath.bfs(remoteCommit);
        remoteCommitMap.forEach((commitKey, layer) -> {
            Commit commit = remoteRepositoryPath.getCommit(commitKey);
            List<Blob> blobs = commit.getTree().keySet()
                    .stream()
                    .map(fileName -> remoteRepositoryPath.getBlob(commit.getBlobKey(fileName)))
                    .collect(Collectors.toList());
            REPO_PATH.saveBlobs(blobs);
            REPO_PATH.saveCommit(commit);
        });
        REPO_PATH.saveRemoteBranch(remoteName, remoteBranchName, remoteCommit.getKey());
    }

    /**
     * pull = fetch + merge
     *
     * @param remoteName       远程仓库名
     * @param remoteBranchName 远程分支名
     */
    public static void pull(String remoteName, String remoteBranchName) {
        fetch(remoteName, remoteBranchName);
        merge(remoteName + "/" + remoteBranchName);
    }
}