package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;

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
    public File getCwd() {
        return CWD;
    }

    /**
     * The .gitlet directory.
     */
    public File getGitletDir() {
        return join(CWD, ".gitlet");
    }

    /**
     * 暂存区
     */
    public File getStageFile() {
        return join(getGitletDir(), "stage");
    }

    /**
     * 存放 commit 和 blob 文件夹
     */
    public File getObjectsDir() {
        return join(getGitletDir(), "objects");
    }

    /**
     * 存放 commit 的文件夹
     */
    public File getCommitsDir() {
        return join(getObjectsDir(), "commits");
    }

    /**
     * 存放 blob 的文件夹
     */
    public File getBlobsDir() {
        return join(getObjectsDir(), "blobs");
    }

    /**
     * 引用文件夹
     */
    public File getRefsDir() {
        return join(getGitletDir(), "refs");
    }

    /**
     * 头指针文件夹(branch)
     */
    public File getHeadsDir() {
        return join(getRefsDir(), "heads");
    }

    /**
     * 当前头指针
     */
    public File getHead() {
        return join(getGitletDir(), "HEAD");
    }

    /**
     * REMOTE 对象
     */
    public File getRemoteFile() {
        return join(getGitletDir(), "REMOTE");
    }

    /**
     * 远程分支文件夹(remotes)
     */
    public File getRemotesDir() {
        return join(getRefsDir(), "remotes");
    }

    /**
     * 远程分支头部
     */
    public File getFetchHead() {
        return join(getGitletDir(), "FETCH_HEAD");
    }

    /**
     * 获取远程分支对象
     *
     * @return 远程分支对象
     */
    public Remote getRemote() {
        return readObject(getRemoteFile(), Remote.class);
    }

    /**
     * 获取当前暂存区信息
     *
     * @return 暂存区信息
     */
    public Stage getStage() {
        return readObject(getStageFile(), Stage.class);
    }

    /**
     * 从 objects 文件夹下获取当前 Commit
     *
     * @return 当前 Commit
     */
    public Commit getCurrCommit() {
        String currCommitId = getCurrCommitId();
        return readObject(join(getCommitsDir(), currCommitId), Commit.class);
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
        if (matchingCommits.size() != 1 
                || !join(getCommitsDir(), matchingCommits.get(0)).exists()) {
            errorAndExit("No commit with that id exists.");
        }
        return readObject(join(getCommitsDir(), matchingCommits.get(0)), Commit.class);
    }

    /**
     * 找到所有以 prefix 开头的 commit
     *
     * @param prefix 前缀
     * @return prefix 开头的 commit
     */
    public List<String> findMatchingCommits(String prefix) {
        List<String> commitKeys = plainFilenamesIn(getCommitsDir());
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
        String[] split = currBranch.split("/");
        if (split.length == 1) {
            return readContentsAsString(join(getHeadsDir(), currBranch));
        } else {
            return readContentsAsString(join(getRemotesDir(), split[0], split[1]));
        }
    }

    /**
     * 从 HEAD 文件中获取当前分支名字
     *
     * @return 当前分支名字
     */
    public String getCurrBranch() {
        return readContentsAsString(getHead());
    }

    /**
     * 根据分支名获取分支(不允许为空)
     *
     * @param branchName 分支名
     * @return Head Commit Key
     */
    public String getBranchNotNull(String branchName) {
        String[] split = branchName.split("/");
        // 本地分支
        if (split.length == 1) {
            List<String> branches = plainFilenamesIn(getHeadsDir());
            if (branches == null || branches.stream().noneMatch(b -> b.equals(branchName))) {
                errorAndExit("No such branch exists.");
            }
            return readContentsAsString(join(getHeadsDir(), branchName));
        // 远程分支
        } else {
            return getRemoteBranchNotNull(split[0], split[1]);
        }
    }

    /**
     * 根据远程仓库名和远程分支名获取分支(不允许为空)
     *
     * @param remoteName       远程仓库名
     * @param remoteBranchName 远程分支名
     * @return Head Commit Key
     */
    private String getRemoteBranchNotNull(String remoteName, String remoteBranchName) {
        File remoteRepoDir = join(getRemotesDir(), remoteName);
        if (!remoteRepoDir.exists()) {
            errorAndExit("No such branch exists.");
        }
        List<String> remoteBranches = plainFilenamesIn(remoteRepoDir);
        if (remoteBranches == null 
                || remoteBranches.stream().noneMatch(b -> b.equals(remoteBranchName))) {
            errorAndExit("No such branch exists.");
        }
        return readContentsAsString(join(remoteRepoDir, remoteBranchName));
    }

    /**
     * 根据分支名获取分支(允许为空)
     *
     * @param branchName 分支名
     * @return Head Commit Key
     */
    public String getBranch(String branchName) {
        File branchFile = join(getHeadsDir(), branchName);
        if (!branchFile.exists()) {
            return null;
        }
        return readContentsAsString(branchFile);
    }

    /**
     * 如果分支名已存在则抛出异常
     *
     * @param branchName 分支名
     */
    public void checkBranchExistsAndThrow(String branchName) {
        if (join(getHeadsDir(), branchName).exists()) {
            errorAndExit("A branch with that name already exists.");
        }
    }

    /**
     * 如果分支名不存在则抛出异常
     *
     * @param branchName 分支名
     */
    public void checkBranchNotExistsAndThrow(String branchName) {
        String[] split = branchName.split("/");
        if (split.length == 1) {
            if (!join(getHeadsDir(), branchName).exists()) {
                errorAndExit("A branch with that name does not exist.");
            }
        } else {
            if (!join(getRemotesDir(), split[0], split[1]).exists()) {
                errorAndExit("A branch with that name does not exist.");
            }
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
        writeObject(join(getBlobsDir(), key), blob);
    }

    /**
     * 保存分支信息同时将头指针指向该分支
     *
     * @param branchName 分支名
     * @param commitKey  commitId
     */
    public void saveBranchAndCheckout(String branchName, String commitKey) {
        writeContents(getHead(), branchName);
        saveBranch(branchName, commitKey);
    }

    /**
     * 保存分支信息
     *
     * @param branchName 分支名
     * @param commitKey  commitId
     */
    public void saveBranch(String branchName, String commitKey) {
        String[] split = branchName.split("/");
        // 本地分支
        if (split.length == 1) {
            writeContents(join(getHeadsDir(), branchName), commitKey);
        // 远程分支
        } else {
            saveRemoteBranch(split[0], split[1], commitKey);
        }
    }

    /**
     * 保存远程分支对象
     *
     * @param remote 远程分支对象
     */
    public void saveRemote(Remote remote) {
        writeObject(getRemoteFile(), remote);
    }

    /**
     * 保存 commit
     *
     * @param commit {@link Commit}
     */
    public void saveCommit(Commit commit) {
        writeObject(join(getCommitsDir(), commit.getKey()), commit);
    }

    /**
     * 保存 stage
     *
     * @param stage {@link Stage}
     */
    public void saveStage(Stage stage) {
        writeObject(getStageFile(), stage);
    }

    /**
     * 找到公共父节点
     *
     * @param base   base commit
     * @param target target commit
     * @return 公共父节点
     */
    public Commit findSplitPoint(Commit base, Commit target) {
        Map<String, Integer> baseAncestorLayerMap = bfs(base);
        Map<String, Integer> targetAncestorLayerMap = bfs(target);

        String commitId = null;
        int layer = Integer.MAX_VALUE;
        for (String targetKey : targetAncestorLayerMap.keySet()) {
            if (baseAncestorLayerMap.containsKey(targetKey) 
                    && layer > baseAncestorLayerMap.get(targetKey)) {
                commitId = targetKey;
                layer = baseAncestorLayerMap.get(targetKey);
            }
        }
        return getCommit(commitId);
    }

    public Map<String, Integer> bfs(Commit base) {
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

    /**
     * 批量保存 blob
     *
     * @param blobs blobs
     */
    public void saveBlobs(List<Blob> blobs) {
        blobs.forEach(b -> writeObject(join(getBlobsDir(), b.getKey()), b));
    }

    /**
     * 保存远程分支
     *
     * @param remoteName
     * @param remoteBranchName
     * @param key
     */
    public void saveRemoteBranch(String remoteName, String remoteBranchName, String key) {
        File remoteNameDir = join(getRemotesDir(), remoteName);
        if (!remoteNameDir.exists()) {
            mkdir(remoteNameDir);
        }
        writeContents(join(remoteNameDir, remoteBranchName), key);
    }

    /**
     * 获取 blob
     *
     * @param blobKey blob 哈希值
     * @return blob
     */
    public Blob getBlob(String blobKey) {
        if (blobKey == null) {
            return null;
        }
        return readObject(join(getBlobsDir(), blobKey), Blob.class);
    }
}
