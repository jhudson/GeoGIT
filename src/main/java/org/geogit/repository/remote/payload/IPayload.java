/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.repository.remote.payload;

import java.util.List;
import java.util.Map;

import org.geogit.api.Ref;
import org.geogit.api.RevBlob;
import org.geogit.api.RevCommit;
import org.geogit.api.RevTag;
import org.geogit.api.RevTree;

public interface IPayload {
	public void addBranches(String branchName, Ref branchRef);
	public void addCommits(RevCommit ... commits);
	public void addTrees(RevTree ... trees);
	public void addBlobs(RevBlob ... blobs);
	public void addTags(RevTag ... tags);
	public Map<String, Ref> getBranchUpdates();
	public List<RevCommit> getCommitUpdates();
	public List<RevTree> getTreeUpdates();
	public List<RevBlob> getBlobUpdates();
	public List<RevTag> getTagUpdates();
}