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
 * @author jhudson
 * @since 1.2.0
 */
public class PullOp extends AbstractGeoGitOp<MergeResult> {

	private String branchName;

	public PullOp(Repository repository) {
		super(repository);
		branchName = Ref.REMOTES_PREFIX + Ref.ORIGIN + Ref.MASTER; /*default is origin*/
	}

	public PullOp setRepository(final String branchName) {
		this.branchName = Ref.REMOTES_PREFIX + branchName;
		return this;
	}

	@Override
	public MergeResult call() throws Exception {
		GeoGIT gg = new GeoGIT(getRepository());
		FetchResult result = gg.fetch().call();
		if (!result.newCommits()) {
			return new MergeResult();
		}
		Ref branch = getRepository().getRef(branchName);
		return gg.merge().include(branch).call();
	}
}
