/* Copyright (c) 2011-2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

/**
 * The fetch result shows how many updates were downloaded
 * @author jhudson
 *
 */
public class FetchResult {

    private int commits;
    private int trees;
    private int blobs;
    private int tags;
    private int branches;
    
    public FetchResult(){
    	this.commits = 0;
    	this.trees = 0;
    	this.blobs = 0;
    	this.tags = 0;
    	this.branches = 0;
    }
    
    public boolean newCommits(){
    	return commits > 0 || trees > 0 || blobs > 0;
    }
    
    public void addBranch(){
    	branches++;
    }

    public void addTag(){
    	tags++;
    }

    public void addCommit(){
    	commits++;
    }

    public void addTree(){
    	trees++;
    }

    public void addBlob(){
    	blobs++;
    }

	/**
	 * @return the commits
	 */
	public int getCommits() {
		return commits;
	}
	/**
	 * @param commits the commits to set
	 */
	public void setCommits(int commits) {
		this.commits = commits;
	}
	/**
	 * @return the trees
	 */
	public int getTrees() {
		return trees;
	}
	/**
	 * @param trees the trees to set
	 */
	public void setTrees(int trees) {
		this.trees = trees;
	}
	/**
	 * @return the blobs
	 */
	public int getBlobs() {
		return blobs;
	}
	/**
	 * @param blobs the blobs to set
	 */
	public void setBlobs(int blobs) {
		this.blobs = blobs;
	}
	
	/**
	 * @return the tags
	 */
	public int getTags() {
		return tags;
	}

	/**
	 * @param tags the tags to set
	 */
	public void setTags(int tags) {
		this.tags = tags;
	}

	/**
	 * @return the branches
	 */
	public int getBranches() {
		return branches;
	}

	/**
	 * @param branches the branches to set
	 */
	public void setBranches(int branches) {
		this.branches = branches;
	}

	public void merge(FetchResult result) {
		this.commits += result.getCommits();
		this.trees += result.getTrees();
		this.blobs += result.getBlobs();
		this.branches += result.getBranches();
		this.tags += result.getTags();
	}
}
