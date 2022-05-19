package gitlet;

import java.io.IOException;

/**
 * Driver class for Gitlet, a subset of the Git version-control system.
 *
 * @author Jerry Jia
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        String firstArg = args[0];
        if (!firstArg.equals("init") && !(Utils.join(Repository.CWD, ".gitlet").exists())) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }
        switch (firstArg) {
            case "init":
                if (args.length != 1) {
                    System.out.println("Incorrect operands.");
                    return;
                }
                Repository.initRepo();
                break;
            case "add":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    return;
                }
                String fileName = args[1];
                Repository.addFile(fileName);
                break;
            case "commit":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    return;
                }
                String msg = args[1];
                if (msg == null || msg.equals("")) {
                    System.out.println("Please enter a commit message");
                    return;
                } else {
                    Repository.commit(msg, null);
                }
                break;
            case "log":
                if (args.length != 1) {
                    System.out.println("Incorrect operands.");
                    return;
                }
                Repository.log();
                break;
            case "checkout":
                if (args.length > 4 || (args.length == 1)) {
                    System.out.println("Incorrect operands.");
                    return;
                }
                if (args.length == 4) {
                    if (!args[2].equals("--")) {
                        System.out.println("Incorrect operands.");
                        return;
                    }
                    Repository.checkoutPrevCommit(args[1], args[3]);
                } else if (args.length == 3) {
                    if (!args[1].equals("--")) {
                        System.out.println("Incorrect operands.");
                        return;
                    }
                    Repository.checkoutCurrentCommit(args[2]);
                } else {
                    Repository.checkoutBranch(args[1]);
                }
                break;
            case "rm":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    return;
                }
                fileName = args[1];
                Repository.rm(fileName);
                break;
            case "find":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    return;
                }
                String commitMessage = args[1];
                Repository.find(commitMessage);
                break;
            case "global-log":
                if (args.length != 1) {
                    System.out.println("Incorrect operands.");
                    return;
                }
                Repository.globalLog();
                break;
            case "branch":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    return;
                }
                String branchName = args[1];
                Repository.branch(branchName);
                break;
            case "status":
                if (args.length != 1) {
                    System.out.println("Incorrect operands.");
                    return;
                }
                Repository.status();
                break;
            case "rm-branch":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    return;
                }
                branchName = args[1];
                Repository.removeBranch(branchName);
                break;
            case "reset":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    return;
                }
                String commitId = args[1];
                Repository.reset(commitId);
                break;
            case "merge":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    return;
                }
                branchName = args[1];
                Repository.merge(branchName);
                break;
            default:
                System.out.println("No command with that name exists.");
        }
    }
}
