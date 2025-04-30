package gitlet;

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
            System.out.println("Please enter a command.");
            return;
        }
        String firstArg = args[0];

        switch(firstArg) {
            case "init":
                init();
                break;
            case "add":
                // TODO: handle the `add [filename]` command
                add(args);
                break;
            // TODO: FILL THE REST IN
            default:
                System.out.println("No command with that name exists.");
        }
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
        System.out.println("Incorrect operands.");
        System.exit(0);
    }

    private static void checkRepositoryExists() {
        // 不在初始化 gitlet 工作目录
        if (!Repository.GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }
}
