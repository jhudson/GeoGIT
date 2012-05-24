/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.merge;

import java.util.Iterator;
import java.util.List;

import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.repository.Repository;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;

public class MergeUtils {

	/**
	 * Find the commit - start looking from the provided commit object
	 * @param branchTip
	 * @param repository
	 * @return
	 */
    public static RevCommit findBranchCommitSplit(final RevCommit branchTip, final Repository repository){
    	for (ObjectId parentId : branchTip.getParentIds()){
    		if (MergeUtils.onBranch(repository.getHead().getObjectId(), parentId, repository)){
    			return branchTip;
    		} else {
    			if (!ObjectId.NULL.equals(parentId)){
    				RevCommit nextBranchTip = repository.getCommit(parentId);
    					return findBranchCommitSplit(nextBranchTip, repository);
    			}
    		}
    	}
    	return null;
    }
    
    /**
     * Is the commit on the master branch?
     * @param parentId
     * @param repository
     * @return true if the objectid is found on the branch
     */
    public static boolean onBranch(final ObjectId branchHeadId, final ObjectId parentId, final Repository repository){
    	Iterator<RevCommit> linearHistory = new LinearHistoryIterator(branchHeadId, repository);
   	 	while (linearHistory.hasNext()){
   	 		RevCommit c = linearHistory.next();
   		 	if (c.getParentIds().contains(parentId)){
   		 		return true;
   		 	}
   	 	}
   	 	return false;
    }

    private static class LinearHistoryIterator extends AbstractIterator<RevCommit> {
        private ObjectId nextCommitId;
        private final Repository repo;

        public LinearHistoryIterator(final ObjectId tip, final Repository repo) {
            this.nextCommitId = tip;
            this.repo = repo;
        }

        @Override
        protected RevCommit computeNext() {
            if (nextCommitId.isNull()) {
                return endOfData();
            }
            final RevCommit commit = repo.getCommit(nextCommitId);
            List<ObjectId> parentIds = commit.getParentIds();
            Preconditions.checkNotNull(parentIds);
            Preconditions.checkState(parentIds.size() > 0);
            nextCommitId = commit.getParentIds().get(0);

            return commit;
        }

    }
}
