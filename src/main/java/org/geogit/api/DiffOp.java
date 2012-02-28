/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.util.Iterator;
import java.util.List;

import org.geogit.repository.Repository;

/**
 * Perform a diff between trees pointed out by two commits
 * 
 */
public class DiffOp extends AbstractGeoGitOp<Iterator<DiffEntry>> {

    private ObjectId oldCommit;

    private ObjectId newCommit;

    private String[] pathFilter;

    private ObjectId idFilter;

    public DiffOp(Repository repository) {
        super(repository);
    }

    /**
     * @param commitId
     *            the oldVersion to set
     * @return
     */
    public DiffOp setOldVersion(ObjectId commitId) {
        this.oldCommit = commitId;
        return this;
    }

    /**
     * @param commitId
     *            the newVersion to set
     * @return
     */
    public DiffOp setNewVersion(ObjectId commitId) {
        this.newCommit = commitId;
        return this;
    }

    public DiffOp setFilter(String... pathFilter) {
        this.pathFilter = pathFilter;
        return this;
    }

    public DiffOp setFilter(List<String> pathFilter) {
        this.pathFilter = pathFilter.toArray(new String[pathFilter.size()]);
        return this;
    }

    /**
     * Filter on diffs that affect the given blob id
     * 
     * @param blobId
     * @return
     */
    public DiffOp setFilter(final ObjectId blobId) {
        this.idFilter = blobId;
        return this;
    }

    @Override
    public Iterator<DiffEntry> call() throws Exception {
        if (oldCommit == null) {
            throw new IllegalStateException("Old version not specified");
        }
        final Repository repo = getRepository();
        if (newCommit == null) {
            /*
             * new version not specified, assume head
             */
            Ref head = repo.getRef(Ref.HEAD);
            newCommit = head.getObjectId();
        }
        if (!oldCommit.isNull() && !repo.commitExists(oldCommit)) {
            throw new IllegalArgumentException("oldVersion commit set to diff op does not exist: "
                    + oldCommit.toString());
        }
        if (!newCommit.isNull() && !repo.commitExists(newCommit)) {
            throw new IllegalArgumentException("newVersion commit set to diff op does not exist: "
                    + newCommit.toString());
        }

        DiffTreeWalk diffReader = new DiffTreeWalk(repo.getObjectDatabase(), oldCommit, newCommit);

        if (pathFilter != null) {
            diffReader.setFilter(pathFilter);
        }
        if (idFilter != null) {
            diffReader.setFilter(idFilter);
        }

        Iterator<DiffEntry> iterator = diffReader.get();

        return iterator;
    }

}
