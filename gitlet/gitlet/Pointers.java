package gitlet;

//import org.antlr.v4.runtime.tree.Tree;

import java.io.Serializable;
import java.util.TreeMap;

public class Pointers implements Serializable {
    private final TreeMap<String, String> pointersMap;
    private String currentBranch = "master";

    public Pointers() {
        pointersMap = new TreeMap<>();
    }

    public String get(String p) {
        return pointersMap.get(p);
    }

    public String getCurrentBranch() {
        return this.currentBranch;
    }

    public void setCurrentBranch(String branchName) {
        this.currentBranch = branchName;
    }

    public TreeMap<String, String> getPointersMap() {
        return this.pointersMap;
    }

}
