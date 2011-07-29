/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
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

    private ObjectId oldVersion;

    private ObjectId newVersion;

    private String[] pathFilter;

    public DiffOp(Repository repository) {
        super(repository);
    }

    /**
     * @param oldTreeId
     *            the oldVersion to set
     * @return
     */
    public DiffOp setOldVersion(ObjectId oldTreeId) {
        this.oldVersion = oldTreeId;
        return this;
    }

    /**
     * @param newTreeId
     *            the newVersion to set
     * @return
     */
    public DiffOp setNewVersion(ObjectId newTreeId) {
        this.newVersion = newTreeId;
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

    @Override
    public Iterator<DiffEntry> call() throws Exception {
        if (oldVersion == null) {
            throw new IllegalStateException("Old version not specified");
        }
        final Repository repo = getRepository();
        if (newVersion == null) {
            /*
             * new version not specified, assume head
             */
            Ref head = repo.getRef(Ref.HEAD);
            newVersion = head.getObjectId();
        }
        if (!oldVersion.isNull() && !repo.commitExists(oldVersion)) {
            throw new IllegalArgumentException("oldVersion commit set to diff op does not exist: "
                    + oldVersion.toString());
        }
        if (!newVersion.isNull() && !repo.commitExists(newVersion)) {
            throw new IllegalArgumentException("newVersion commit set to diff op does not exist: "
                    + newVersion.toString());
        }

        DiffTreeWalk diffReader = new DiffTreeWalk(repo, oldVersion, newVersion);

        diffReader.setFilter(this.pathFilter);

        Iterator<DiffEntry> iterator = diffReader.get();

        return iterator;
    }

}
