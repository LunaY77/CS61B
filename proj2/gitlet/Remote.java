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
     * key: 远程分支名
     * value: 远程分支路径
     */
    private Map<String, String> remoteMap;

    public Remote() {
        remoteMap = new HashMap<>();
    }

    /**
     * 添加远程分支信息
     *
     * @param remoteName 远程分支名
     * @param remotePath 远程分支路径
     */
    public void addRemote(String remoteName, String remotePath) {
        checkRemoteNameExistsAndThrow(remoteName);
        remoteMap.put(remoteName, remotePath);
        Repository.saveRemote(this);
    }

    /**
     * 删除远程分支信息
     *
     * @param remoteName 远程分支名
     */
    public void removeRemote(String remoteName) {
        checkRemoteNameNotExistsAndThrow(remoteName);
        remoteMap.remove(remoteName);
        Repository.saveRemote(this);
    }

    /**
     * 如果远程分支名字存在则抛出异常
     *
     * @param remoteName 远程分支名
     */
    private void checkRemoteNameExistsAndThrow(String remoteName) {
        if (remoteMap.containsKey(remoteName)) {
            errorAndExit("A remote with that name already exists.");
        }
    }

    /**
     * 如果远程分支名不存在则抛出异常
     *
     * @param remoteName 远程分支名
     */
    private void checkRemoteNameNotExistsAndThrow(String remoteName) {
        if (!remoteMap.containsKey(remoteName)) {
            errorAndExit("A remote with that name does not exist.");
        }
    }
}
