package gitlet;

import static gitlet.Utils.*;

/**
 * Driver class for Gitlet, a subset of the Git version-control system.
 *
 * @author 苍镜月
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {
        // 参数为空
        if (args.length == 0) {
            message("Please enter a command.");
            return;
        }
        String firstArg = args[0];

        switch (firstArg) {
            case "init":
                init(args);
                break;
            case "add":
                add(args);
                break;
            case "commit":
                commit(args);
                break;
            case "rm":
                rm(args);
                break;
            case "log":
                log(args);
                break;
            case "global-log":
                globalLog(args);
                break;
            case "find":
                find(args);
                break;
            case "status":
                status(args);
                break;
            case "checkout":
                checkout(args);
                break;
            case "branch":
                branch(args);
                break;
            case "rm-branch":
                rmBranch(args);
                break;
            case "reset":
                reset(args);
                break;
            case "merge":
                merge(args);
                break;
            case "add-remote":
                addRemote(args);
                break;
            case "rm-remote":
                removeRemote(args);
                break;
            case "push":
                push(args);
                break;
            case "fetch":
                fetch(args);
                break;
            case "pull":
                pull(args);
                break;
            default:
                message("No command with that name exists.");
        }
    }

    /**
     * pull
     */
    private static void pull(String[] args) {
        checkRepositoryExists();
        checkOperands(args, 3);
        Repository.pull(args[1], args[2]);
    }

    /**
     * fetch
     */
    private static void fetch(String[] args) {
        checkRepositoryExists();
        checkOperands(args, 3);
        Repository.fetch(args[1], args[2]);
    }

    /**
     * push
     */
    private static void push(String[] args) {
        checkRepositoryExists();
        checkOperands(args, 3);
        Repository.push(args[1], args[2]);
    }

    /**
     * rm-remote
     */
    private static void removeRemote(String[] args) {
        checkRepositoryExists();
        checkOperands(args, 2);
        Repository.removeRemote(args[1]);
    }

    /**
     * add-remote
     */
    private static void addRemote(String[] args) {
        checkRepositoryExists();
        checkOperands(args, 3);
        Repository.addRemote(args[1], args[2]);
    }

    /**
     * merge
     */
    private static void merge(String[] args) {
        checkRepositoryExists();
        checkOperands(args, 2);
        Repository.merge(args[1]);
    }

    /**
     * reset
     */
    private static void reset(String[] args) {
        checkRepositoryExists();
        checkOperands(args, 2);
        Repository.reset(args[1]);
    }

    /**
     * rm-branch
     */
    private static void rmBranch(String[] args) {
        checkRepositoryExists();
        checkOperands(args, 2);
        Repository.rmBranch(args[1]);
    }

    /**
     * branch
     */
    private static void branch(String[] args) {
        checkRepositoryExists();
        checkOperands(args, 2);
        Repository.branch(args[1]);
    }

    /**
     * checkout
     */
    private static void checkout(String[] args) {
        checkRepositoryExists();
        // checkout -- [file name]
        if (args.length == 3) {
            if (!args[1].equals("--")) {
                message("Incorrect operands.");
            }
            Repository.checkoutCommit(null, args[2]);
        }
        // checkout [commit id] -- [file name]
        else if (args.length == 4) {
            if (!args[2].equals("--")) {
                message("Incorrect operands.");
            }
            Repository.checkoutCommit(args[1], args[3]);
        }
        // checkout [branch name]
        else if (args.length == 2) {
            Repository.checkoutBranch(args[1]);
        } else {
            message("Incorrect operands.");
        }
    }

    /**
     * status
     */
    private static void status(String[] args) {
        checkRepositoryExists();
        checkOperands(args, 1);
        Repository.status();
    }

    /**
     * find
     *
     * @param args
     */
    private static void find(String[] args) {
        checkRepositoryExists();
        checkOperands(args, 2);
        Repository.find(args[1]);
    }

    /**
     * global args
     */
    private static void globalLog(String[] args) {
        checkRepositoryExists();
        checkOperands(args, 1);
        Repository.globalLog();
    }

    /**
     * log
     */
    private static void log(String[] args) {
        checkRepositoryExists();
        checkOperands(args, 1);
        Repository.log();
    }

    /**
     * rm
     */
    private static void rm(String[] args) {
        checkRepositoryExists();
        checkOperands(args, 2);
        Repository.rm(args[1]);
    }

    /**
     * commit
     */
    private static void commit(String[] args) {
        checkRepositoryExists();
        checkOperands(args, 2);
        String message = args[1];
        if (isBlank(message)) {
            errorAndExit("Please enter a commit message.");
        }
        // 暂存区中无文件
        Stage stage = Repository.REPO_PATH.getStage();
        if (stage.isEmpty()) {
            errorAndExit("No changes added to the commit.");
        }
        Repository.commit(message);
    }

    /**
     * add
     */
    private static void add(String[] args) {
        checkRepositoryExists();
        checkOperands(args, 2);
        Repository.add(args[1]);
    }

    /**
     * init
     */
    private static void init(String[] args) {
        checkOperands(args, 1);
        Repository.init();
    }

    /**
     * 命令格式错误
     */
    private static void checkOperands(String[] args, int expectedLength) {
        if (args.length != expectedLength) {
            message("Incorrect operands.");
            System.exit(0);
        }
    }

    /**
     * 校验是否存在.gitlet目录
     */
    private static void checkRepositoryExists() {
        // 不在初始化 gitlet 工作目录
        if (!Repository.REPO_PATH.GITLET_DIR().exists()) {
            message("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }
}