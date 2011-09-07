package org.geogit.api.config;

/**
 * Object representing a branch as supplied in the config file
 * 
 * @author jhudson
 */
public class BranchConfigObject {
    private final String name;
    private final String remote;
    private final String merge;
    public BranchConfigObject( String name, String remote, String merge ) {
        super();
        this.name = name;
        this.remote = remote;
        this.merge = merge;
    }
    public String getName() {
        return name;
    }
    public String getRemote() {
        return remote;
    }
    public String getMerge() {
        return merge;
    }
    @Override
    public String toString() {
        return "BranchConfigObject [name=" + name + ", remote=" + remote + ", merge=" + merge + "]";
    }
}
