package org.geogit.repository.remote.payload;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geogit.api.Ref;
import org.geogit.api.RevBlob;
import org.geogit.api.RevCommit;
import org.geogit.api.RevTag;
import org.geogit.api.RevTree;

/**
 * Abstract payload (or packfile as is in GIT protocol).
 * @author johnathonhudson
 *
 */
public abstract class AbstractPayload implements IPayload {

	/**
	 * The branch updates from the remote
	 */
	Map<String, Ref> branchUpdates;
	
	/**
	 * commit updates from the remote
	 */
	List<RevCommit> commitUpdates;
	
	/**
	 * tree updates from the remote
	 */
	List<RevTree> treeUpdates;
	
	/**
	 * blob updates from the remote
	 */
	List<RevBlob> blobUpdates;

	/**
	 * tag updates from the remote
	 */
	List<RevTag> tagUpdates;
	
	public AbstractPayload(){
		this.branchUpdates = new HashMap<String, Ref>();
		this.commitUpdates = new ArrayList<RevCommit>();
		this.treeUpdates = new ArrayList<RevTree>();
		this.blobUpdates = new ArrayList<RevBlob>();
		this.tagUpdates = new ArrayList<RevTag>();
	}
	
	/**
	 * Add a branch 
	 * @param branchName
	 * @param branchRef
	 */
	public void addBranches(String branchName, Ref branchRef) {
		this.branchUpdates.put(branchName, branchRef);
	}

	/**
	 * Add new commits to the commits list
	 * @param commits
	 */
	public void addCommits(RevCommit ... commits) {
		this.commitUpdates.addAll(Arrays.asList(commits));
	}
	
	/**
	 * Add new Tree Refs to the tree store
	 */
	public void addTrees(RevTree ... trees) {
		this.treeUpdates.addAll(Arrays.asList(trees));
	}
	
	/**
	 * Add new blobs to the blob store
	 * @param blobs
	 */
	public void addBlobs(RevBlob ... blobs) {
		this.blobUpdates.addAll(Arrays.asList(blobs));
	}
	
	/**
	 * Add new tags to the tag store
	 * @param tags
	 */
	public void addTags(RevTag ... tags) {
		this.tagUpdates.addAll(Arrays.asList(tags));
	}

	public Map<String, Ref> getBranchUpdates() {
		return branchUpdates;
	}

	public List<RevCommit> getCommitUpdates() {
		return commitUpdates;
	}

	public List<RevTree> getTreeUpdates() {
		return treeUpdates;
	}

	public List<RevBlob> getBlobUpdates() {
		return blobUpdates;
	}

	public List<RevTag> getTagUpdates() {
		return tagUpdates;
	}
}