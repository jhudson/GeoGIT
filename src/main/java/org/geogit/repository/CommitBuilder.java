/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import java.util.List;

import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.springframework.util.Assert;

public final class CommitBuilder {

    private ObjectId treeId;

    private List<ObjectId> parentIds;

    private String author;

    private String committer;

    private String message;

    private long timestamp;

    public CommitBuilder() {
    }

    /**
     * @return the treeId
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
    public List<ObjectId> getParentIds() {
        return parentIds;
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
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp
     *            timestamp, in UTC, of the commit. Let it blank for the builder to auto-set it at
     *            {@link #build()} time
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public RevCommit build(final ObjectId id) {
        Assert.notNull(id, "Id can't be null");

        if (treeId == null) {
            throw new IllegalStateException("No tree id set");
        }
        RevCommit commit = new RevCommit(id);
        commit.setAuthor(author);
        commit.setCommitter(committer);
        commit.setMessage(message);
        commit.setParentIds(parentIds);
        commit.setTimestamp(getTimestamp());
        commit.setTreeId(treeId);

        return commit;
    }
}
