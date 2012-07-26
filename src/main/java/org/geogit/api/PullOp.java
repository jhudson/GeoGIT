/* Copyright (c) 2011-2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import org.geogit.api.merge.MergeResult;
import org.geogit.repository.Repository;

/**
 * 
 * As in git this is a concatenation of a fetch and a merge operation.
 * 
 * 1. GeoGit.FetchOp
 * 2. GeoGit.MergeOp
 * 
 * @author jhudson
 * @since 1.2.0
 */
public class PullOp extends AbstractGeoGitOp<MergeResult> {

	private String branchName;

	public PullOp(Repository repository) {
		super(repository);
		branchName = Ref.REMOTES_PREFIX + Ref.ORIGIN + Ref.MASTER; /*default is origin*/
	}

	/**
	 * Set the name of the branch you wish to pull - defaults to "remotes/origin/master"
	 * 
	 * TODO: think about this method - naming aside - should it really include the 
	 * Ref.REMOTES_PREFIX or should it have smarts to add it only if its missing?
	 * 
	 * @param branchName
	 * @return PullOp this
	 */
	public PullOp setRepository(final String branchName) {
		this.branchName = Ref.REMOTES_PREFIX + branchName;
		return this;
	}

	@Override
	public MergeResult call() throws Exception {
		GeoGIT gg = new GeoGIT(getRepository());
		FetchResult result = gg.fetch().call();
		
		/*
		 * if there were no changes in the fetch operation don't try to merge - nothing to do 
		 */
		if (!result.newCommits()) {
			return new MergeResult();
		}
		/*
		 * merge the branch over to the named branch - defaults to origin/master 
		 */
		Ref branch = getRepository().getRef(branchName);
		return gg.merge().include(branch).call();
	}
}
