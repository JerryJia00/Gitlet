package gitlet;


import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeMap;

/**
 * Represents a gitlet commit object.
 * does at a high level.
 *
 * @author Jerry Jia
 */
public class Commit implements Serializable {
    /**
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /**
     * The message of this Commit.
     */
    private final String message;
    /**
     * The timestamp of this Commit at local time.
     */
    private final Date timestamp;
    /**
     * The parent of this commit
     */
    private Commit parent = null;
    private Commit parent2 = null;
    /**
     * The TreeMap data structure that manages files in a specific commit
     */
    private final TreeMap<String, File> files = new TreeMap<>();


    /**
     * Constructor of the initial commit
     */
    public Commit() {
        this.message = "initial commit";
        this.timestamp = new Date(0);
    }

    /**
     * Constructor of the rest of commits
     */
    public Commit(String msg) {
        this.message = msg;
        this.timestamp = new Date();
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        String pattern = "EEE MMM d HH:mm:ss yyyy Z";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        return simpleDateFormat.format(timestamp);
    }

    public Commit getParent() {
        return parent;
    }

    public void setParent(Commit parent) {
        this.parent = parent;
    }

    public Commit getParent2() {
        return parent2;
    }

    public void setParent2(Commit parent2) {
        this.parent2 = parent2;
    }

    public TreeMap<String, File> getFiles() {
        return this.files;
    }
}
