/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.util.LinkedList;
import java.util.List;

import org.geogit.api.RevObject.TYPE;
import org.geogit.repository.CommitBuilder;
import org.geogit.repository.Index;
import org.geogit.repository.Repository;
import org.geogit.repository.Tuple;
import org.geogit.storage.CommitWriter;
import org.geogit.storage.ObjectInserter;
import org.opengis.geometry.BoundingBox;

public class CommitOp extends AbstractGeoGitOp<RevCommit> {

    private String author;

    private String committer;

    private String message;

    private Long timeStamp;

    /**
     * This commit's parents. Will be the current HEAD, but when we support merges it should include
     * the equivalent to git's .git/MERGE_HEAD
     */
    private List<ObjectId> parents = new LinkedList<ObjectId>();

    // like the -a option in git commit
    private boolean all;

    public CommitOp(final Repository repository) {
        super(repository);
    }

    public CommitOp setAuthor(final String author) {
        this.author = author;
        return this;
    }

    public CommitOp setCommitter(final String committer) {
        this.committer = committer;
        return this;
    }

    public CommitOp setMessage(final String message) {
        this.message = message;
        return this;
    }

    public CommitOp setTimestamp(final Long timestamp) {
        this.timeStamp = timestamp;
        return this;
    }

    public CommitOp setAll(boolean all) {
        this.all = all;
        return this;
    }

    /**
     * @return the commit just applied
     * @see org.geogit.api.AbstractGeoGitOp#call()
     * @throws NothingToCommitException
     */
    public RevCommit call() throws Exception {
        // TODO: check repository is in a state that allows committing

        final Repository repository = getRepository();
        GeoGIT ggit = new GeoGIT(repository);
        if (all) {
            ggit.add().addPattern(".").setUpdateOnly(true).call();
        }
        final Ref currHead = repository.getRef(Ref.HEAD);
        if (currHead == null) {
            throw new IllegalStateException("Repository has no HEAD, can't commit");
        }

        final ObjectId currHeadCommitId = currHead.getObjectId();
        parents.add(currHeadCommitId);

        final Index index = repository.getIndex();
        final ObjectInserter objectInserter = repository.newObjectInserter();
        Tuple<ObjectId, BoundingBox> result = index.writeTree(objectInserter);
        final ObjectId treeId = result.getFirst();
        final BoundingBox affectedArea = result.getLast();

        if (treeId == null) {
            throw new NothingToCommitException("Nothing to commit after " + currHeadCommitId);
        }
        final ObjectId commitId;
        {
            CommitBuilder cb = new CommitBuilder();
            cb.setAuthor(author);
            cb.setCommitter(committer);
            cb.setMessage(message);
            cb.setParentIds(parents);
            cb.setTreeId(treeId);
            if (timeStamp != null) {
                cb.setTimestamp(timeStamp.longValue());
            }
            // cb.setBounds(bounds);
            commitId = objectInserter.insert(new CommitWriter(cb.build(ObjectId.NULL)));
        }
        final RevCommit commit = repository.getCommit(commitId);
        // set the HEAD pointing to the new commit
        final Ref newHead = repository.updateRef(new Ref(Ref.HEAD, commitId, TYPE.COMMIT));
        LOGGER.fine("New head: " + newHead);

        // System.err.println("OLD Commit:\n\t");
        // if (currHeadCommitId.isNull()) {
        // System.err.println(ObjectId.NULL);
        // } else {
        // BLOBS.print(repository.getRawObject(currHeadCommitId), System.err);
        // }
        // System.err.println("NEW Commit:\n\t");
        // BLOBS.print(repository.getRawObject(commitId), System.err);

        return commit;
    }
}
