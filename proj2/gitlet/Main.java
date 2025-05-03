package gitlet;

import static gitlet.Constant.GITLET_DIR;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        // 参数为空
        if (args.length == 0) {
            Utils.message("Please enter a command.");
            return;
        }
        String firstArg = args[0];

        switch(firstArg) {
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
            default:
                Utils.message("No command with that name exists.");
        }
    }

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
        if (Utils.isBlank(message)) {
            throw Utils.error("Please enter a commit message.");
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
            Utils.message("Incorrect operands.");
            System.exit(0);
        }
    }

    /**
     * 校验是否存在.gitlet目录
     */
    private static void checkRepositoryExists() {
        // 不在初始化 gitlet 工作目录
        if (!GITLET_DIR.exists()) {
            Utils.message("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }
}
