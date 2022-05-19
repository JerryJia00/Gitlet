package gitlet;


import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static gitlet.Utils.*;


/**
 * Represents a gitlet repository.
 * <p>
 * does at a high level.
 *
 * @author Jerry Jia
 */
public class Repository implements Serializable {
    /**
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /**
     * The .gitlet directory.
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /**
     * The staging area
     */
    public static final File STAGE_FILE = join(GITLET_DIR, "staging");

    public static final File POINTER_FILE = join(GITLET_DIR, "pointer");

    public static final File COMMITS_DIR = Utils.join(GITLET_DIR, "commits");

    public static final File BLOB_DIR = Utils.join(GITLET_DIR, "blobs");
    /**
     * The HEAD pointer to the current commit
     */

    /**
     * handles the init commit
     */
    public static void initRepo() throws IOException {
        if (GITLET_DIR.exists()) {
            String x = "A Gitlet version_control system already exists in the current directory.";
            System.out.println(x);
        } else {
            GITLET_DIR.mkdir();
            File blobDir = Utils.join(GITLET_DIR, "blobs");
            // Create Blobs DIR to store real files
            File commitsDir = Utils.join(GITLET_DIR, "commits");
            // Create Commits DIR to store the commits chain
            blobDir.mkdir();
            commitsDir.mkdir();
            /** Initial commit */
            Commit initialCommit = new Commit();
            String initialSha1 = Utils.sha1(serialize(initialCommit));
            File initialCommitFIle = join(commitsDir, initialSha1);
            // The name of the commit file is its hashcode
            writeObject(initialCommitFIle, initialCommit);
            /** Initiate pointer (head, master, branching) file */
            POINTER_FILE.createNewFile();
            Pointers pointer = new Pointers();
            pointer.getPointersMap().put("head", initialSha1);
            // Move the HEAD pointer to this current commit
            pointer.getPointersMap().put("master", initialSha1);
            // Move the Master pointer to this current commit
            writeObject(POINTER_FILE, pointer);
            /** Initiate staging area */
            STAGE_FILE.createNewFile();  // Creates the staging area file
            Staging stageArea = new Staging();
            // Initiate Staging Treemap,maps a filename to the address of the snap
            Utils.writeObject(STAGE_FILE, stageArea);
        }

    }

    /**
     * Add files to the staging area
     */
    public static void addFile(String filename) throws IOException {
        if (!fileExisted(CWD, filename)) {
            System.out.println("File does not exist.");
            return;
        }
        File file = Utils.join(CWD, filename);
        // Address to the real file working on
        Staging stageArea = Utils.readObject(STAGE_FILE, Staging.class);
        // Get the stageArea Class with (Treemap)
        String sha1 = getHash(file);
        Pointers pointer = readObject(POINTER_FILE, Pointers.class);
        File address1 = Utils.join(COMMITS_DIR, pointer.get("head"));
        Commit currentCommit = readObject(address1, Commit.class);
        TreeMap<String, File> currentFiles = currentCommit.getFiles();
        boolean differentInStage;
        boolean notCurrentVersion;
        if (stageArea.stagedAddition.containsKey(filename)) {
            differentInStage = !(getHash(stageArea.stagedAddition.get(filename)).equals(sha1));
        } else {
            differentInStage = true;
        }
        if (currentFiles.containsKey(filename)) {
            notCurrentVersion = !((getHash(currentFiles.get(filename))).equals(sha1));
        } else {
            notCurrentVersion = true;
        }
        boolean sameAndStaged = !(notCurrentVersion)
                && stageArea.stagedAddition.containsKey(filename);
        if (stageArea.stagedAddition.containsKey(filename)) {
            if (differentInStage && notCurrentVersion) {
                File blob = Utils.join(BLOB_DIR, sha1);
                // The address of the snap of file in CWD with hashcode name
                blob.createNewFile();
                Utils.writeContents(blob, readContentsAsString(file));
                stageArea.stagedAddition.put(filename, blob);
                // Maps the filename to it's snap in stageAddition
            } else if (sameAndStaged) {
                stageArea.stagedAddition.remove(filename);
                //System.out.println("sameAndStaged, so removed");
            }
        } else {
            if (notCurrentVersion) {
                File blob = Utils.join(BLOB_DIR, sha1);
                // The address of the snap of file in CWD with hashcode name
                blob.createNewFile();
                Utils.writeContents(blob, readContentsAsString(file));
                stageArea.stagedAddition.put(filename, blob);
                // Maps the filename to it's snap in stageAddition

            }
        }
        if (!notCurrentVersion) {
            stageArea.stagedRemoval.remove(filename);
        }
        Utils.writeObject(STAGE_FILE, stageArea);  // Save the Treemap to the file
    }

    public static void rm(String filename) {
        File file = Utils.join(CWD, filename);  // Address to the real file working on
        Staging stageArea = Utils.readObject(STAGE_FILE, Staging.class);
        // Get the stageArea Class with (Treemap)
        //String sha1 = getHash(stageArea.stagedAddition.get(filename));
        Pointers pointer = readObject(POINTER_FILE, Pointers.class);
        Commit currentCommit = readObject(
                Utils.join(COMMITS_DIR, pointer.get("head")), Commit.class);
        TreeMap<String, File> currentFiles = currentCommit.getFiles();
        if (!(stageArea.stagedAddition.containsKey(filename))
                && !(currentFiles.containsKey(filename))) {
            System.out.println("No reason to remove the file");
            return;
        }
        stageArea.stagedAddition.remove(filename);
        if (currentFiles.containsKey(filename)) {
            stageArea.stagedRemoval.put(filename, file);
            if (file.exists()) {
                file.delete();
            }
        }
        Utils.writeObject(STAGE_FILE, stageArea);
    }

    /**
     * This handles the commit method in gitlet
     */
    public static void commit(String msg, Commit mergeParent2) throws IOException {
        Commit currentCommit = new Commit(msg);
        Pointers pointer = readObject(POINTER_FILE, Pointers.class);
        File headAddress = Utils.join(COMMITS_DIR, pointer.getPointersMap().get("head"));
        Commit parentCommit = readObject(headAddress, Commit.class);
        /** Combine the files in parentFiles and staging area to commit for addition */
        currentCommit.setParent(parentCommit);  // Set this commit parent to the last commit
        if (mergeParent2 != null) {
            currentCommit.setParent2(mergeParent2);
        }
        TreeMap<String, File> parentFiles = parentCommit.getFiles();
        Staging stageArea = Utils.readObject(STAGE_FILE, Staging.class);
        // Get the private stageArea class
        if (stageArea.stagedAddition.size() == 0 && stageArea.stagedRemoval.size() == 0) {
            System.out.println("No changes added to the commit.");
            return;
        }
        currentCommit.getFiles().putAll(parentFiles);
        currentCommit.getFiles().putAll(stageArea.stagedAddition);
        /** stage for removal */
        for (Map.Entry<String, File> entry : stageArea.stagedRemoval.entrySet()) {
            currentCommit.getFiles().remove(entry.getKey());
        }
        String currentSha1 = Utils.sha1(serialize(currentCommit));
        // Get the hashcode of the current commit after done everything
        File currentCommitFIle = join(COMMITS_DIR, currentSha1);
        // The name of the commit file is its hashcode
        currentCommitFIle.createNewFile();
        // Create the commit file based on the address and name
        writeObject(currentCommitFIle, currentCommit);
        /** Move the pointers and clear stage */
        pointer.getPointersMap().put("head", currentSha1);
        // Move the HEAD pointer to this current commit
        pointer.getPointersMap().put(pointer.getCurrentBranch(), currentSha1);
        // Move the master pointer to the current commit
        writeObject(POINTER_FILE, pointer);
        stageArea.stagedAddition.clear(); // Clear the staging area
        stageArea.stagedRemoval.clear(); // Clear the removal area
        Utils.writeObject(STAGE_FILE, stageArea); // save the change
    }

    public static void branch(String branchName) {
        Pointers pointer = readObject(POINTER_FILE, Pointers.class);
        if (pointer.getPointersMap().containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
        } else {
            String headRef = pointer.getPointersMap().get("head");
            pointer.getPointersMap().put(branchName, headRef);
        }
        Utils.writeObject(POINTER_FILE, pointer);
    }

    public static void removeBranch(String branchName) {
        Pointers pointer = readObject(POINTER_FILE, Pointers.class);
        if (pointer.getCurrentBranch().equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
        } else if (pointer.getPointersMap().containsKey(branchName)) {
            pointer.getPointersMap().remove(branchName);
        } else {
            System.out.println("A branch with that name does not exist.");
        }
        Utils.writeObject(POINTER_FILE, pointer);
    }

    public static void status() {
        Pointers pointer = readObject(POINTER_FILE, Pointers.class);
        Commit currentCommit = headCommit();
        Staging stageArea = Utils.readObject(STAGE_FILE, Staging.class);
        // Get the stageArea Class with (Treemap)
        System.out.println("=== Branches ===");
        System.out.println("*" + pointer.getCurrentBranch());
        pointer.getPointersMap().remove(pointer.getCurrentBranch());
        for (Map.Entry<String, String> entry : pointer.getPointersMap().entrySet()) {
            if (!entry.getKey().equals("head")) {
                System.out.println(entry.getKey());
            }
        }
        TreeMap<String, Integer> modification = new TreeMap<>();
        for (Map.Entry<String, File> entry : currentCommit.getFiles().entrySet()) {
            // Tracked in the current commit
            if (fileExisted(CWD, entry.getKey())) {
                if (!getHash(entry.getValue()).equals(getHash(getFile(CWD, entry.getKey())))) {
                    // changed in the working directory
                    if (!stageArea.stagedAddition.containsKey(entry.getKey())) {
                        // but not staged
                        modification.put(entry.getKey() + " " + "(modified)", 1);
                    }
                }
            } else {  // deleted from the working directory.
                if (!stageArea.stagedRemoval.containsKey(entry.getKey())) {
                    modification.put(entry.getKey() + " " + "(deleted)", 1);
                }
            }
        }
        for (Map.Entry<String, File> entry : stageArea.stagedAddition.entrySet()) {
            // Staged for addition
            if (fileExisted(CWD, entry.getKey())) {
                if (!getHash(entry.getValue()).equals(getHash(getFile(CWD, entry.getKey())))) {
                    // but with different contents than in the working directory
                    modification.put(entry.getKey() + " " + "(modified)", 1);
                    stageArea.stagedAddition.remove(entry.getKey());
                }
            } else {  // deleted from the working directory.
                modification.put(entry.getKey() + " " + "(deleted)", 1);
                stageArea.stagedAddition.remove(entry.getKey());
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        for (Map.Entry<String, File> entry : stageArea.stagedAddition.entrySet()) {
            System.out.println(entry.getKey());
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (Map.Entry<String, File> entry : stageArea.stagedRemoval.entrySet()) {
            System.out.println(entry.getKey());
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");

        for (Map.Entry<String, Integer> entry : modification.entrySet()) {
            System.out.println(entry.getKey());
        }
        TreeMap<String, Integer> untrackedFiles = new TreeMap<>();
        for (String i : Objects.requireNonNull(plainFilenamesIn(CWD))) {
            // files present in the working directory
            if (!(currentCommit.getFiles().containsKey(i))
                    && !(stageArea.stagedAddition.containsKey(i))) {
                // neither staged for addition nor tracked
                untrackedFiles.put(i, 1);
            } else if (stageArea.stagedRemoval.containsKey(i)) {
                // that have been staged for removal, but then re-created without Gitlet’s knowledge
                untrackedFiles.put(i, 1);
            }
        }
        System.out.println();
        System.out.println("=== Untracked Files ===");
        for (Map.Entry<String, Integer> entry : untrackedFiles.entrySet()) {
            System.out.println(entry.getKey());
        }
        System.out.println();
    }

    public static void log() {
        Commit currentCommit = headCommit();
        while (currentCommit != null) {
            System.out.println("===");
            System.out.println("commit " + Utils.sha1(serialize(currentCommit)));
            if (currentCommit.getParent2() != null) {
                String id1 = sha1(serialize(currentCommit.getParent())).substring(0, 7);
                String id2 = sha1(serialize(currentCommit.getParent2())).substring(0, 7);
                System.out.println("Merge: " + id1 + " " + id2);
            }
            System.out.println("Date: " + currentCommit.getTimestamp());
            System.out.println(currentCommit.getMessage());
            System.out.println();
            currentCommit = currentCommit.getParent();
        }
    }

    public static void globalLog() {
        for (String i : Objects.requireNonNull(plainFilenamesIn(COMMITS_DIR))) {
            Commit currentCommit = Utils.readObject(join(COMMITS_DIR, i), Commit.class);
            System.out.println("===");
            System.out.println("commit " + i);
            System.out.println("Date: " + currentCommit.getTimestamp());
            System.out.println(currentCommit.getMessage());
            System.out.println();
        }
    }

    public static void find(String msg) {
        boolean exist = false;
        for (String i : Objects.requireNonNull(plainFilenamesIn(COMMITS_DIR))) {
            Commit currentCommit = Utils.readObject(join(COMMITS_DIR, i), Commit.class);
            if (currentCommit.getMessage().equals(msg)) {
                exist = true;
                System.out.println(i);
            }
        }
        if (!exist) {
            System.out.println("Found no commit with that message.");
        }
    }

    public static void checkoutCurrentCommit(String filename) {
        Commit headCommit = headCommit();
        checkoutInCommit(headCommit, filename);
    }

    public static void checkoutPrevCommit(String commitId, String filename) {
        if (commitId.length() < 40) {
            for (String i : Objects.requireNonNull(plainFilenamesIn(COMMITS_DIR))) {
                if (i.startsWith(commitId)) {
                    File commitAddress = Utils.join(COMMITS_DIR, i);
                    Commit thisCommit = Utils.readObject(commitAddress, Commit.class);
                    checkoutInCommit(thisCommit, filename);
                    return;
                }
            }
            System.out.println("No commit with that id exists.");
        } else {
            File commitAddress = Utils.join(COMMITS_DIR, commitId);
            if (commitAddress.exists()) {
                Commit thisCommit = Utils.readObject(commitAddress, Commit.class);
                checkoutInCommit(thisCommit, filename);
            } else {
                System.out.println("No commit with that id exists.");
            }
        }

    }

    public static void checkoutBranch(String branchName) throws IOException {
        Pointers pointer = readObject(POINTER_FILE, Pointers.class);
        if (!pointer.getPointersMap().containsKey(branchName)) {
            System.out.println("No such branch exists.");
        } else if (pointer.getCurrentBranch().equals(branchName)) {
            System.out.println("No need to checkout"
                    + " the current branch.");
        } else {
            Commit currentCommit = headCommit();
            Commit branchCommit = getCommitByHash(pointer.getPointersMap().get(branchName));
            String x = "There is an untracked file in the way;"
                    + " delete it, or add and commit it first.";
            for (Map.Entry<String, File> entry : branchCommit.getFiles().entrySet()) {
                if (!currentCommit.getFiles().containsKey(entry.getKey())) {
                    if (fileExisted(CWD, entry.getKey())) {
                        if (!(getHash(getFile(CWD, entry.getKey()))
                                .equals(getHash(entry.getValue())))) {
                            System.out.println(x);
                            return;
                        }
                    }
                }
            }
            for (Map.Entry<String, File> entry : branchCommit.getFiles().entrySet()) {
                if (fileExisted(CWD, entry.getKey())) {
                    File address = branchCommit.getFiles().get(entry.getKey());
                    Utils.writeContents(getFile(CWD, entry.getKey()),
                            readContentsAsString(address));
                } else {
                    writeNewFile(CWD, entry.getKey(),
                            branchCommit.getFiles().get(entry.getKey()));
                }
            }
            for (Map.Entry<String, File> entry : currentCommit.getFiles().entrySet()) {
                if (fileExisted(CWD, entry.getKey())
                        && !(branchCommit.getFiles().containsKey(entry.getKey()))) {
                    restrictedDelete(entry.getKey());
                }
            }
            pointer.setCurrentBranch(branchName);
            pointer.getPointersMap().put("head", Utils.sha1(serialize(branchCommit)));
            Utils.writeObject(POINTER_FILE, pointer);
            clearStaging();
        }

    }

    public static void reset(String commitId) {
        if (!commitExisted(commitId)) {
            System.out.println("No commit with that id exists.");
        } else if (untrackedAndOverwritten(commitId)) {
            System.out.println("There is an untracked file in the way;"
                    + " delete it, or add and commit it first.");
        } else {
            Pointers pointer = readObject(POINTER_FILE, Pointers.class);
            Commit givenCommit = null;
            if (commitId.length() < 40) {
                for (String i : Objects.requireNonNull(plainFilenamesIn(COMMITS_DIR))) {
                    if (i.startsWith(commitId)) {
                        File commitAddress = Utils.join(COMMITS_DIR, i);
                        givenCommit = Utils.readObject(commitAddress, Commit.class);
                    }
                }
            } else {
                givenCommit = readObject(getFile(COMMITS_DIR, commitId), Commit.class);
            }
            Commit currentCommit = headCommit();
            for (Map.Entry<String, File> entry : givenCommit.getFiles().entrySet()) {
                checkoutPrevCommit(commitId, entry.getKey());
            }
            for (Map.Entry<String, File> entry : currentCommit.getFiles().entrySet()) {
                if (!givenCommit.getFiles().containsKey(entry.getKey())) {
                    restrictedDelete(entry.getKey());
                }
            }
            clearStaging();
            pointer.getPointersMap().put("head", sha1(serialize(givenCommit)));
            pointer.getPointersMap().put(pointer.getCurrentBranch(),
                    sha1(serialize(givenCommit)));
            Utils.writeObject(POINTER_FILE, pointer);
        }
    }

    public static void merge(String branchName) throws IOException {
        Pointers pointer = readObject(POINTER_FILE, Pointers.class);
        boolean thereIsConflict = false;
        if (mergeCheck1(branchName)) {
            return;
        }
        String currComHash = pointer.getPointersMap().get(pointer.getCurrentBranch());
        String givenComHash = pointer.getPointersMap().get(branchName);
        Commit currentBranchCom = Utils.readObject(
                join(COMMITS_DIR, currComHash), Commit.class);
        Commit givenBranchCom = Utils.readObject(
                join(COMMITS_DIR, givenComHash), Commit.class);
        Commit splitPoint = splitPointFind(branchName);
        if (mergeUntracked(splitPoint, givenBranchCom)) {
            System.out.println("There is an untracked file in the way;"
                    + " delete it, or add and commit it first.");
            return;
        }
        if (sha1(serialize(splitPoint))
                .equals(pointer.getPointersMap().get(branchName))) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        } else if (sha1(serialize(splitPoint))
                .equals(pointer.getPointersMap().get(pointer.getCurrentBranch()))) {
            checkoutBranch(branchName);
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        if (mergeCurrentFile(splitPoint, currentBranchCom, givenBranchCom)) {
            thereIsConflict = true;
        }
        if (mergeGivenFile(splitPoint, currentBranchCom, givenBranchCom)) {
            thereIsConflict = true;
        }
        if (!(sha1(serialize(splitPoint)).equals(sha1(serialize(givenBranchCom))))
                || !(sha1(serialize(splitPoint)).equals(sha1(serialize(currentBranchCom))))) {
            commit("Merged " + branchName + " into " + pointer.getCurrentBranch()
                    + ".", givenBranchCom);
        }
        if (thereIsConflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    private static boolean mergeUntracked(Commit split, Commit given) {
        Pointers pointer = readObject(POINTER_FILE, Pointers.class);
        String currComHash = pointer.getPointersMap().get(pointer.getCurrentBranch());
        Commit currentBranchCom = Utils.readObject(join(COMMITS_DIR, currComHash), Commit.class);
        for (String i : Objects.requireNonNull(plainFilenamesIn(CWD))) {
            boolean inSplit = split.getFiles().containsKey(i);
            if (!inSplit && !(currentBranchCom.getFiles().containsKey(i))
                    && given.getFiles().containsKey(i)) {
                return true;
            }
        }
        return false;
    }

    private static Commit splitPointFind(String branchName) {
        Pointers pointer = readObject(POINTER_FILE, Pointers.class);
        String currComHash = pointer.getPointersMap().get(pointer.getCurrentBranch());
        String givenComHash = pointer.getPointersMap().get(branchName);
        Commit currentBranchCom = Utils.readObject(
                join(COMMITS_DIR, currComHash), Commit.class);
        Commit givenBranchCom = Utils.readObject(
                join(COMMITS_DIR, givenComHash), Commit.class);
        ArrayList<String> currentParents = new ArrayList<>();
        Set<String> givenBranchParents = new HashSet<>();
        bArray(currentBranchCom, currentParents);
        bSet(givenBranchCom, givenBranchParents);
        Commit splitPoint = currentBranchCom;
        for (String i : currentParents) {
            if (givenBranchParents.contains(i)) {
                splitPoint = Utils.readObject(join(COMMITS_DIR, i), Commit.class);
                break;
            }
        }
        return splitPoint;
    }

    private static boolean mergeGivenFile(Commit splitPoint,
                                          Commit currentBranchCom,
                                          Commit givenBranchCom) throws IOException {
        boolean result = false;
        for (Map.Entry<String, File> entry : givenBranchCom.getFiles().entrySet()) {
            boolean conflict = false;
            boolean inSplit = splitPoint.getFiles().containsKey(entry.getKey());
            boolean notModifiedInGiven = false;
            if (inSplit) {
                notModifiedInGiven = getHash(splitPoint.getFiles().get(entry.getKey()))
                        .equals(getHash(givenBranchCom.getFiles().get(entry.getKey())));
            }
            boolean notInCurrent = !(currentBranchCom.getFiles().containsKey(entry.getKey()));
            if (!inSplit && notInCurrent) {  // Number 5.
                checkoutPrevCommit(sha1(serialize(givenBranchCom)), entry.getKey());
                addFile(entry.getKey());
            }
            if (!notModifiedInGiven && !notInCurrent) {
                File splitAddress = splitPoint.getFiles().get(entry.getKey());
                File currentAddress = currentBranchCom.getFiles().get(entry.getKey());
                File givenAddress = givenBranchCom.getFiles().get(entry.getKey());
                if (splitAddress != null && currentAddress != null && givenAddress != null) {
                    boolean modifiedInCurr = !(getHash(splitAddress)
                            .equals(getHash(currentAddress)));
                    boolean differentFromOther = !(getHash(givenAddress)
                            .equals(getHash(currentAddress)));
                    if (modifiedInCurr && differentFromOther) {
                        conflict = true;
                    }
                }
            } else if (!notModifiedInGiven && notInCurrent && inSplit) {
                conflict = true;
            } else if (!inSplit) {
                File givenAddress = givenBranchCom.getFiles().get(entry.getKey());
                File currAddress = currentBranchCom.getFiles().get(entry.getKey());
                if (givenAddress != null && currAddress != null) {
                    if (!(getHash(givenAddress).equals(getHash(currAddress)))) {
                        conflict = true; // Not in split point and different from each other
                    }
                }
            }
            if (conflict) {
                result = true;
                File conflicted = Utils.join(CWD, entry.getKey());
                File currAddress = currentBranchCom.getFiles().get(entry.getKey());
                File givenAddress = givenBranchCom.getFiles().get(entry.getKey());
                if (currAddress == null) {
                    Utils.writeContents(conflicted, "<<<<<<< HEAD"
                            + '\n' + "=======" + '\n' + readContentsAsString(givenAddress)
                            + ">>>>>>>" + '\n');
                } else {
                    Utils.writeContents(conflicted, "<<<<<<< HEAD"
                            + '\n' + readContentsAsString(currAddress) + "======="
                            + '\n' + readContentsAsString(givenAddress) + ">>>>>>>"
                            + '\n');
                }
                addFile(entry.getKey());
            }
        }
        return result;
    }

    private static boolean mergeCurrentFile(Commit splitPoint,
                                            Commit currentBranchCom,
                                            Commit givenBranchCom) throws IOException {
        boolean result = false;
        for (Map.Entry<String, File> entry : currentBranchCom.getFiles().entrySet()) {
            boolean conflict = false;
            boolean inSplit = splitPoint.getFiles().containsKey(entry.getKey());
            boolean notModifiedInCurr = false;
            if (inSplit) {
                notModifiedInCurr = getHash(splitPoint.getFiles().get(entry.getKey()))
                        .equals(getHash(entry.getValue()));
            }
            boolean notInGiven = !(givenBranchCom.getFiles().containsKey(entry.getKey()));
            if (givenBranchCom.getFiles().containsKey(entry.getKey())) {
                File splitAddress = splitPoint.getFiles().get(entry.getKey());
                File givenAddress = givenBranchCom.getFiles().get(entry.getKey());
                boolean modifiedInGiven = false;
                if (splitAddress != null) {
                    modifiedInGiven = !(getHash(splitAddress).equals(getHash(givenAddress)));
                }
                if (inSplit && notModifiedInCurr && modifiedInGiven) { // Number 1.
                    Utils.writeContents(join(CWD, entry.getKey()),
                            readContentsAsString(givenAddress));
                    addFile(entry.getKey());
                }
            }
            if (inSplit && notModifiedInCurr
                    && !(givenBranchCom.getFiles().containsKey(entry.getKey()))) { // Number 6.
                rm(entry.getKey());
            }
            if (!notModifiedInCurr && notInGiven && inSplit) {
                conflict = true;
            }
            if (conflict) {
                result = true;
                File conflicted = Utils.join(CWD, entry.getKey());
                File currAddress = currentBranchCom.getFiles().get(entry.getKey());
                File givenAddress = givenBranchCom.getFiles().get(entry.getKey());
                if (givenAddress == null) {
                    Utils.writeContents(conflicted, "<<<<<<< HEAD" + '\n'
                            + readContentsAsString(currAddress) + "=======" + '\n'
                            + ">>>>>>>" + '\n');
                } else {
                    Utils.writeContents(conflicted, "<<<<<<< HEAD" + '\n'
                            + readContentsAsString(currAddress) + "======="
                            + '\n' + readContentsAsString(givenAddress) + ">>>>>>>"
                            + '\n');
                }
                addFile(entry.getKey());
            }
        }
        return result;
    }

    private static boolean mergeCheck1(String branchName) {
        Staging stageArea = Utils.readObject(STAGE_FILE, Staging.class);
        Pointers pointer = readObject(POINTER_FILE, Pointers.class);
        if (stageArea.stagedAddition.size() != 0
                || stageArea.stagedRemoval.size() != 0) {
            System.out.println("You have uncommitted changes.");
            return true;
        } else if (!(pointer.getPointersMap().containsKey(branchName))) {
            System.out.println("A branch with that name does not exist.");
            return true;
        } else if (branchName.equals(pointer.getCurrentBranch())) {
            System.out.println("Cannot merge a branch with itself.");
            return true;
        }
        return false;
    }

    /**
     * Get the commit that the head pointer points at
     */
    private static Commit headCommit() {
        return readObject(Utils.join(COMMITS_DIR,
                readObject(POINTER_FILE, Pointers.class).getPointersMap().get("head")),
                Commit.class);
    }

    /**
     * private static Commit parentCommit (Commit currentCommit) {
     * return
     * }
     */
    private static String getHash(File address) {
        return Utils.sha1(serialize(readContentsAsString(address)));
    }

    //private static String getHashObj (File address, Class c) {
    //return Utils.sha1(serialize(readObject(address, c)));
    //}

    private static void checkoutInCommit(Commit c, String filename) {
        if (c.getFiles().containsKey(filename)) {
            File cwdFile = Utils.join(CWD, filename);
            File commitFile = c.getFiles().get(filename);
            Utils.writeContents(cwdFile, Utils.readContentsAsString(commitFile));
        } else {
            System.out.println("File does not exist in that commit.");
        }
    }

    private static Commit getCommitByHash(String sha1) {
        return Utils.readObject(join(COMMITS_DIR, sha1), Commit.class);
    }

    private static boolean fileExisted(File address, String filename) {
        File file = Utils.join(address, filename);
        return file.exists();
    }

    private static File getFile(File address, String filename) {
        return Utils.join(address, filename);
    }

    private static void writeNewFile(File address, String filename, File fromFile)
            throws IOException {
        File file = Utils.join(address, filename);
        file.createNewFile();
        Utils.writeContents(file, Utils.readContentsAsString(fromFile));
    }

    private static boolean commitExisted(String commitId) {
        if (commitId.length() < 40) {
            for (String i : Objects.requireNonNull(plainFilenamesIn(COMMITS_DIR))) {
                if (i.startsWith(commitId)) {
                    return true;
                }
            }
        } else {
            File commitAddress = Utils.join(COMMITS_DIR, commitId);
            return commitAddress.exists();
        }
        return false;
    }

    private static boolean untrackedAndOverwritten(String targetId) {
        Commit currentCommit = headCommit();
        if (targetId.length() == 40) {
            Commit targetCommit = readObject(join(COMMITS_DIR, targetId), Commit.class);
            for (Map.Entry<String, File> entry : targetCommit.getFiles().entrySet()) {
                if (!currentCommit.getFiles().containsKey(entry.getKey())) {
                    if (fileExisted(CWD, entry.getKey())
                            && !(getHash(getFile(CWD, entry.getKey()))
                            .equals(getHash(entry.getValue())))) {
                        return true;
                    }
                }
            }
        } else if (targetId.length() < 40) {
            for (String i : Objects.requireNonNull(plainFilenamesIn(COMMITS_DIR))) {
                if (i.startsWith(targetId)) {
                    File commitAddress = Utils.join(COMMITS_DIR, i);
                    Commit thisCommit = Utils.readObject(commitAddress, Commit.class);
                    for (Map.Entry<String, File> entry : thisCommit.getFiles().entrySet()) {
                        if (!currentCommit.getFiles().containsKey(entry.getKey())) {
                            if (fileExisted(CWD, entry.getKey())
                                    && !(getHash(getFile(CWD, entry.getKey()))
                                    .equals(getHash(entry.getValue())))) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    private static void clearStaging() {
        Staging stageArea = Utils.readObject(STAGE_FILE, Staging.class);
        // Get the stageArea Class with (Treemap)
        stageArea.stagedAddition.clear(); // Clear the staging area
        stageArea.stagedRemoval.clear();
        Utils.writeObject(STAGE_FILE, stageArea); // save the change
    }

    private static void bArray(Commit currentCommit, ArrayList<String> currentParents) {
        ArrayDeque<Commit> fringe = new ArrayDeque<>();
        fringe.addLast(currentCommit);
        while (!fringe.isEmpty()) {
            Commit v = fringe.removeFirst();
            currentParents.add(sha1(serialize(v)));
            if (v.getParent() != null) {
                if (v.getParent2() == null) {
                    fringe.addLast(v.getParent());
                } else {
                    fringe.addLast(v.getParent());
                    fringe.addLast(v.getParent2());
                }
            }
        }
    }

    private static void bSet(Commit currentCommit, Set<String> givenParents) {
        ArrayDeque<Commit> fringe = new ArrayDeque<>();
        boolean first = true;
        fringe.addLast(currentCommit);
        while (!fringe.isEmpty()) {
            Commit v = fringe.removeFirst();
            givenParents.add(sha1(serialize(v)));
            if (v.getParent() != null) {
                if (v.getParent2() == null) {
                    fringe.addLast(v.getParent());
                } else {
                    fringe.addLast(v.getParent());
                    fringe.addLast(v.getParent2());
                }
            }
        }
    }

    private static class Staging implements Serializable {
        private final TreeMap<String, File> stagedAddition;
        private final TreeMap<String, File> stagedRemoval;

        Staging() {
            stagedAddition = new TreeMap<>();
            stagedRemoval = new TreeMap<>();
        }

        public void check() {
            System.out.println("Files in stage" + stagedAddition.keySet());
        }
    }
}




























