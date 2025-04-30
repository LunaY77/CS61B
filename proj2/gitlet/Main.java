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
                init();
                break;
            case "add":
                add(args);
                break;
            case "commit":
                commit(args);
                break;
            // TODO: FILL THE REST IN
            default:
                Utils.message("No command with that name exists.");
        }
    }

    private static void commit(String[] args) {
        checkRepositoryExists();
        if (args.length != 2) {
            errorOperands();
        }
        String message = args[1];
        if (Utils.isBlank(message)) {
            throw Utils.error("Please enter a commit message.");
        }
//        Repository.commit(message);
    }

    private static void add(String[] args) {
        checkRepositoryExists();
        if (args.length != 2) {
            errorOperands();
        }
        Repository.add(args[1]);
    }

    private static void init() {
        Repository.init();
    }

    private static void errorOperands() {
        Utils.message("Incorrect operands.");
        System.exit(0);
    }

    private static void checkRepositoryExists() {
        // 不在初始化 gitlet 工作目录
        if (!GITLET_DIR.exists()) {
            Utils.message("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }
}
