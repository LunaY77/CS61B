package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static gitlet.Utils.errorAndExit;

/**
 * @author 苍镜月
 * @version 1.0
 * @implNote Remote对象，保存 remote 信息，在原生 git 中 remote 信息存储在 config
 */

public class Remote implements Serializable {

    /**
     * remote 信息
     * key: 远程仓库名
     * value: 远程仓库路径
     */
    private Map<String, RepositoryPath> remoteMap;

    public Remote() {
        remoteMap = new HashMap<>();
    }

    /**
     * 添加远程分支信息
     *
     * @param remoteName 远程仓库名
     * @param remotePath 远程仓库路径
     */
    public void addRemote(String remoteName, String remotePath) {
        checkRemoteNameExistsAndThrow(remoteName);
        // 去掉/.gitlet后缀
        String actualRemotePath = remotePath.substring(0, remotePath.length() - 8);
        remoteMap.put(remoteName, new RepositoryPath(actualRemotePath));
        Repository.REPO_PATH.saveRemote(this);
    }

    /**
     * 删除远程仓库信息
     *
     * @param remoteName 远程仓库名
     */
    public void removeRemote(String remoteName) {
        checkRemoteNameNotExistsAndThrow(remoteName);
        remoteMap.remove(remoteName);
        Repository.REPO_PATH.saveRemote(this);
    }

    /**
     * 如果远程仓库名字存在则抛出异常
     *
     * @param remoteName 远程仓库名
     */
    private void checkRemoteNameExistsAndThrow(String remoteName) {
        if (remoteMap.containsKey(remoteName)) {
            errorAndExit("A remote with that name already exists.");
        }
    }

    /**
     * 如果远程仓库名不存在则抛出异常
     *
     * @param remoteName 远程仓库名
     */
    private void checkRemoteNameNotExistsAndThrow(String remoteName) {
        if (!remoteMap.containsKey(remoteName)) {
            errorAndExit("A remote with that name does not exist.");
        }
    }

    /**
     * 校验远程仓库路径合法性
     *
     * @param remoteName 远程仓库名
     */
    public void checkRemotePath(String remoteName) {
        // 校验仓库名字是否存在
        checkRemoteNameNotExistsAndThrow(remoteName);
        RepositoryPath remoteRepositoryPath = remoteMap.get(remoteName);
        // 校验.gitlet文件夹是否存在
        if (!remoteRepositoryPath.GITLET_DIR().exists()) {
            errorAndExit("Remote directory not found.");
        }
    }

    /**
     * 获取远程仓库路径
     *
     * @param remoteName 远程仓库名
     * @return 远程仓库路径
     */
    public RepositoryPath getRepositoryPath(String remoteName) {
        return remoteMap.get(remoteName);
    }
}
