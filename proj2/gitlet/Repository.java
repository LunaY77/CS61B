package gitlet;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static gitlet.Constant.*;
import static gitlet.Utils.*;

/**
 * Represents a gitlet repository.
 * does at a high level.
 * <br>
 * .gitlet
 * ├── objects
 * ├── commits
 * └── blobs
 * └── refs
 * ├── branch
 * └── stage
 * └── HEAD
 *
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
     *
     * @param commit {@link Commit}
     */
    public static void saveCommit(Commit commit) {
        writeObject(join(COMMITS_DIR, commit.getKey()), commit);
    }

    /**
     * 保存 stage
     *
     * @param stage {@link Stage}
     */
    public static void saveStage(Stage stage) {
        writeObject(STAGE, stage);
    }

    /**
     * add 添加文件到暂存区
     *
     * @param fileName 文件名
     */
    public static void add(String fileName) {
        File file = join(CWD, fileName);
        // 文件不存在
        if (!file.exists()) {
            errorAndExit("File does not exist.");
        }
        byte[] fileContent = readContents(file);
        String key = sha1(fileContent);
        // 读取暂存区
        Stage stage = getStage();
        // 如果有删除记录，删除 rm
        stage.cancelRemove(fileName);
        // 如果不存在 blob 且无同一 hash 的 blob，则添加 blob
        Commit curCommit = getCurrCommit();
        String blobKey = curCommit.getBlobKey(fileName);
        // 如果当前提交中没有该文件，或者文件内容已经改变，则添加
        if (!key.equals(blobKey)) {
            createAndSaveBlob(key, fileContent, fileName);
            stage.addFile(fileName, key);
        }
    }

    /**
     * 创建并保存 Blob
     *
     * @param fileContent 文件内容
     * @param fileName    文件名
     */
    private static void createAndSaveBlob(String key, byte[] fileContent, String fileName) {
        Blob blob = new Blob(key, fileContent, fileName);
        writeObject(join(BLOBS_DIR, key), blob);
    }

    /**
     * commit 将暂存区文件提交
     *
     * @param message commit message
     */
    public static void commit(String message) {
        // 创建新的commit
        Commit commit = new Commit(message, getCurrCommit(), new Date());
        doCommit(commit);
    }

    /**
     * merge commit
     *
     * @param message     commit message
     * @param otherCommit 另一个 commit
     */
    public static void commit(String message, Commit otherCommit) {
        Commit commit = new Commit(message, getCurrCommit(), otherCommit, new Date());
        doCommit(commit);
    }

    /**
     * commit
     *
     * @param commit {@link Commit}
     */
    private static void doCommit(Commit commit) {
        Stage stage = getStage();
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
     *
     * @param branchName 分支名
     * @param commitKey  commitId
     */
    public static void saveBranchAndCheckout(String branchName, String commitKey) {
        writeContents(HEAD, branchName);
        writeContents(join(HEADS_DIR, branchName), commitKey);
    }

    /**
     * 保存分支信息
     *
     * @param branchName 分支名
     * @param commitKey  commitId
     */
    private static void saveBranch(String branchName, String commitKey) {
        writeContents(join(HEADS_DIR, branchName), commitKey);
    }

    /**
     * 获取当前暂存区信息
     *
     * @return 暂存区信息
     */
    public static Stage getStage() {
        return readObject(STAGE, Stage.class);
    }

    /**
     * 从 objects 文件夹下获取当前 Commit
     *
     * @return 当前 Commit
     */
    private static Commit getCurrCommit() {
        String currCommitId = getCurrCommitId();
        return readObject(join(COMMITS_DIR, currCommitId), Commit.class);
    }

    /**
     * 从 objects 文件夹下获取 Commit
     *
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
            errorAndExit("No commit with that id exists.");
        }
        return readObject(join(COMMITS_DIR, matchingCommits.get(0)), Commit.class);
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
     *
     * @return 当前 Commit ID
     */
    private static String getCurrCommitId() {
        String currBranch = getCurrBranch();
        return readContentsAsString(join(HEADS_DIR, currBranch));
    }

    /**
     * 从 HEAD 文件中获取当前分支名字
     *
     * @return 当前分支名字
     */
    private static String getCurrBranch() {
        return readContentsAsString(HEAD);
    }

    /**
     * 根据分支名获取分支
     *
     * @param branchName 分支名
     * @return Head Commit Key
     */
    private static String getBranch(String branchName) {
        List<String> branches = plainFilenamesIn(HEADS_DIR);
        if (branches == null || branches.stream().noneMatch(b -> b.equals(branchName))) {
            errorAndExit("No such branch exists.");
        }
        return readContentsAsString(join(HEADS_DIR, branchName));
    }

    /**
     * 如果分支名已存在则抛出异常
     *
     * @param branchName 分支名
     */
    private static void checkBranchExistsAndThrow(String branchName) {
        if (join(HEADS_DIR, branchName).exists()) {
            errorAndExit("A branch with that name already exists.");
        }
    }

    /**
     * 如果分支名不存在则抛出异常
     *
     * @param branchName 分支名
     */
    private static void checkBranchNotExistsAndThrow(String branchName) {
        if (!join(HEADS_DIR, branchName).exists()) {
            errorAndExit("A branch with that name does not exist.");
        }
    }

    /**
     * rm 删除暂存区的文件或者已提交的文件
     *
     * @param fileName 文件名
     */
    public static void rm(String fileName) {
        Stage stage = getStage();
        Commit commit = getCurrCommit();
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
     *
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
            errorAndExit("Found no commit with that message.");
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
        message("=== Branches ===");
        for (String branch : branches) {
            if (branch.equals(currBranch)) {
                message("*%s", branch);
            } else {
                message("%s", branch);
            }
        }
        System.out.println();

        // Staged Files
        message("=== Staged Files ===");
        for (String filePath : stage.getAddFiles().keySet()) {
            message("%s", filePath);
        }
        System.out.println();

        // Removed Files
        message("=== Removed Files ===");
        stage.getRemoveFiles().forEach(System.out::println);
        System.out.println();

        // todo === Modifications Not Staged For Commit ===
        message("=== Modifications Not Staged For Commit ===");
        System.out.println();

        // todo === Untracked Files ===
        message("=== Untracked Files ===");
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
                .map(Repository::getCommit)
                .orElseGet(Repository::getCurrCommit);
        Blob blob = commit.getBlob(fileName);
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
        File file = join(CWD, fileName);
        writeContents(file, blob.getContent());
    }

    /**
     * checkout [branch name]
     *
     * @param branchName 分支名称
     */
    public static void checkoutBranch(String branchName) {
        String currBranch = getCurrBranch();
        // checkout 当前分支
        if (currBranch.equals(branchName)) {
            message("No need to checkout the current branch.");
            return;
        }
        // 找到对应的分支Head
        String commitKey = getBranch(branchName);
        Commit targetCommit = getCommit(commitKey);
        Commit currCommit = getCurrCommit();

        // checkout
        checkout(currCommit, targetCommit);
        saveBranchAndCheckout(branchName, targetCommit.getKey());
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
                join(CWD, fileName).delete();
            }
        }
        // 目标分支存在
        for (String fileName : to.getTree().keySet()) {
            Blob blob = to.getBlob(fileName);
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
            if (!from.hasFile(fileName) && join(CWD, fileName).exists()) {
                errorAndExit("There is an untracked file in the way; delete it, or add and commit it first.");
            }
        }
    }

    /**
     * 清空暂存区
     */
    private static void cleanStage() {
        getStage().clear();
    }

    /**
     * branch 创建新分支
     *
     * @param branchName 分支名
     */
    public static void branch(String branchName) {
        checkBranchExistsAndThrow(branchName);
        saveBranch(branchName, getCurrCommitId());
    }

    /**
     * 删除指定的分支
     *
     * @param branchName 分支名
     */
    public static void rmBranch(String branchName) {
        checkBranchNotExistsAndThrow(branchName);
        String currBranch = getCurrBranch();
        // 不能删除当前分支
        if (currBranch.equals(branchName)) {
            errorAndExit("Cannot remove the current branch.");
        }
        // delete
        join(HEADS_DIR, branchName).delete();
    }

    /**
     * reset
     *
     * @param commitKey 分支 id
     */
    public static void reset(String commitKey) {
        Commit targetCommit = Repository.getCommit(commitKey);
        Commit currCommit = getCurrCommit();
        checkout(currCommit, targetCommit);
        // 依然保持在当前分支，只是 HEAD 可能指向其他分支所在的 commit
        saveBranch(getCurrBranch(), targetCommit.getKey());
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
        if (!getStage().isEmpty()) {
            errorAndExit("You have uncommitted changes.");
        }
        // 是否和自己合并
        String currBranchName = getCurrBranch();
        if (currBranchName.equals(branchName)) {
            errorAndExit("Cannot merge a branch with itself.");
        }
        // 检查 branch 是否存在
        checkBranchNotExistsAndThrow(branchName);
        // 获取当前分支和目标分支
        Commit base = getCurrCommit();
        Commit target = getCommit(getBranch(branchName));

        checkUntrackedFiles(base, target);

        // 找到相交节点
        Commit splitPoint = findSplitPoint(base, target);
        // 1. 如果相交节点是目标节点，表示目标节点是当前节点的祖先
        if (Objects.equals(splitPoint, target)) {
            message("Given branch is an ancestor of the current branch.");
            return;
        }
        // 2. 如果相交节点是当前节点，表示当前节点是目标节点的祖先，则 checkout 到目标节点
        if (Objects.equals(splitPoint, base)) {
            message("Current branch fast-forwarded.");
            checkout(base, target);
            saveBranchAndCheckout(branchName, target.getKey());
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
        Stage stage = getStage();
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
                    File file = join(CWD, fileName);
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
                        (baseBlobKey == null ? "" : new String(readObject(join(BLOBS_DIR, baseBlobKey), Blob.class).getContent(), StandardCharsets.UTF_8)) +
                        "=======\n" +
                        (targetBlobKey == null ? "" : new String(readObject(join(BLOBS_DIR, targetBlobKey), Blob.class).getContent(), StandardCharsets.UTF_8)) +
                        ">>>>>>>\n").getBytes(StandardCharsets.UTF_8);
                String mergeBlobKey = sha1(content);
                createAndSaveBlob(mergeBlobKey, content, fileName);
                stage.addFile(fileName, mergeBlobKey);
                writeContents(join(CWD, fileName), content);
            }
        }
    }

    /**
     * 找到公共父节点
     *
     * @param base   base commit
     * @param target target commit
     * @return 公共父节点
     */
    private static Commit findSplitPoint(Commit base, Commit target) {
        Map<String, Integer> baseAncestorLayerMap = bfs(base);
        Map<String, Integer> targetAncestorLayerMap = bfs(target);

        String commitId = null;
        int layer = Integer.MAX_VALUE;
        for (String targetKey : targetAncestorLayerMap.keySet()) {
            if (baseAncestorLayerMap.containsKey(targetKey) && layer > baseAncestorLayerMap.get(targetKey)) {
                commitId = targetKey;
                layer = baseAncestorLayerMap.get(targetKey);
            }
        }
        return getCommit(commitId);
    }

    private static Map<String, Integer> bfs(Commit base) {
        Map<String, Integer> map = new HashMap<>();
        Queue<Pair> q = new LinkedList<>();
        q.add(new Pair(base.getKey(), 0));

        while (!q.isEmpty()) {
            Pair cur = q.poll();
            String key = cur.key;
            int layer = cur.layer;
            map.put(key, layer);
            Commit commit = getCommit(key);
            if (commit.getFirstParentKey() != null) {
                q.add(new Pair(commit.getFirstParentKey(), layer + 1));
            }
            if (commit.getSecondParentKey() != null) {
                q.add(new Pair(commit.getSecondParentKey(), layer + 1));
            }
        }
        return map;
    }

    private static class Pair {
        String key;
        Integer layer;

        Pair(String key, Integer layer) {
            this.key = key;
            this.layer = layer;
        }
    }
}