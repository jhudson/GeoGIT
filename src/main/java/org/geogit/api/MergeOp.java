/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.geogit.api.DiffEntry.ChangeType;
import org.geogit.api.RevObject.TYPE;
import org.geogit.repository.CommitBuilder;
import org.geogit.repository.Repository;
import org.geogit.storage.ObjectInserter;
import org.geogit.storage.WrappedSerialisingFactory;

import com.google.common.collect.Iterators;

/**
 * Very simple merge; to push the HEAD up to the current remotes head.
 * 
 * This will rebase if there are no new commits on this repo. Otherwise
 * it will create a new commit head and set the parents the old head and
 * the branch head.
 * 
 * @author jhudson
 * @since 1.2.0
 */
public class MergeOp extends AbstractGeoGitOp<MergeResult> {

    private Ref branch;
    private String comment;
    
    public MergeOp(Repository repository) {
        super(repository);
        this.comment = "";
    }

    public MergeOp include(final Ref branch) {
        this.branch = branch;
        return this;
    }

    public MergeOp setComment(final String comment){
        this.comment = comment;
        return this;
    }

    public MergeResult call() throws Exception {
        MergeResult mergeResult = new MergeResult();

        if (branch == null){
            return mergeResult;
        }

        /**
         * Grab old head
         */
        RevCommit oldHead;
        if (!ObjectId.NULL.equals(getRepository().getHead().getObjectId())) {
             oldHead = getRepository().getCommit(getRepository().getHead().getObjectId());
        } else {
            rebase();
            return mergeResult;
        }

        /**
         * Work out if this is a rebase or a merge
         */
        LogOp l = new LogOp(getRepository());
        Iterator<RevCommit> s = l.setSince(oldHead.getId()).call();

        if (Iterators.contains(s, oldHead)){ /*rebase*/
            rebase();
        } else { /*merge - new commit head and add parents of both branches*/

            /**
             * New head
             */
            final ObjectId commitId;
            final RevCommit branchHead;
            {
                CommitBuilder cb = new CommitBuilder();
                
                /**
                 * Grab branch head parents
                 */
                branchHead = getRepository().getCommit(branch.getObjectId());

                /**
                 * Merge the trees
                 */
                ObjectId treeId = mergeTrees(oldHead.getId(), branchHead.getId());

                /**
                 * add the parents
                 */
                List<ObjectId> parents = Arrays.asList(oldHead.getId(), branchHead.getId());
                cb.setParentIds(parents);
                cb.setTreeId(treeId);
                cb.setMessage(this.comment);

                /**
                 * insert the new commit
                 */
                ObjectInserter objectInserter = getRepository().newObjectInserter();
                commitId = objectInserter.insert(WrappedSerialisingFactory.getInstance().createCommitWriter(cb.build(ObjectId.NULL)));
            }

            /**
             * Update the head
             */
            getRepository().getRefDatabase().put(new Ref(Ref.HEAD, commitId, TYPE.COMMIT));

            /**
             * diff the changes
             */
            DiffOp diffOp = new DiffOp(getRepository());
            Iterator<DiffEntry> diffs = diffOp.setNewVersion(oldHead.getId()).setOldVersion(branchHead.getId()).call();

            while (diffs.hasNext()) {
                DiffEntry diff = diffs.next();
                if (diff.getType()==ChangeType.MODIFY){
                    mergeResult.addMerged(diff.getNewObjectId());
                }
            }

            LOGGER.info("Merged master -> " + branch.getName());
            LOGGER.info(" " + commitId.printSmallId());
        }

        return mergeResult;
    }

    /**
     * Merge the two trees together so the new commit has a reference to the actual features
     * 
     * TODO: is this actually needed? GIT uses its history to traverse its commits to create
     * its checkout - since the parents of this new commit HEAD have the trees should this BE
     * objectId.NULL... not sure?
     * 
     * @param oldHead
     * @param branchHead
     * @return ObjectId of the new tree created and inserted into the DB
     * @throws Exception 
     */
    private ObjectId mergeTrees(ObjectId oldHead, ObjectId branchHead) throws Exception {
        return ObjectId.NULL;
    }

    /**
     * Point the HEAD at the current remote branch head - is this a rebaseOp?
     * @throws Exception 
     */
    private void rebase() throws Exception {
        RebaseOp rebaseOp = new RebaseOp(getRepository());
        rebaseOp.include(branch).call();
    }
}