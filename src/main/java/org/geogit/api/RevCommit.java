/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.util.Collections;
import java.util.List;

/**
 * A reference to a commit in the DAG.
 * 
 * @author groldan
 * 
 */
public class RevCommit extends RevObject {

    private ObjectId treeId;

    private List<ObjectId> parentIds;

    private String author;

    private String committer;

    private String message;

    private long timestamp;

    public RevCommit(final ObjectId id) {
        super(id);
    }

    @Override
    public TYPE getType() {
        return TYPE.COMMIT;
    }

    /**
     * @return the id of the tree this commit points to
     */
    public ObjectId getTreeId() {
        return treeId;
    }

    /**
     * @param treeId
     *            the treeId to set
     */
    public void setTreeId(ObjectId treeId) {
        this.treeId = treeId;
    }

    /**
     * @return the parentIds
     */
    @SuppressWarnings("unchecked")
    public List<ObjectId> getParentIds() {
        return parentIds == null ? Collections.EMPTY_LIST : Collections.unmodifiableList(parentIds);
    }

    /**
     * @param parentIds
     *            the parentIds to set
     */
    public void setParentIds(List<ObjectId> parentIds) {
        this.parentIds = parentIds;
    }

    /**
     * @return the author
     */
    public String getAuthor() {
        return author;
    }

    /**
     * @param author
     *            the author to set
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * @return the committer
     */
    public String getCommitter() {
        return committer;
    }

    /**
     * @param committer
     *            the committer to set
     */
    public void setCommitter(String committer) {
        this.committer = committer;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message
     *            the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Returns the commit time stamp in UTC milliseconds
     * 
     * @return the commit's time stamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp
     *            the timestamp to set
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String toString() {
        return "Commit[" + getId() + ", '" + message + "']";
    }
}
