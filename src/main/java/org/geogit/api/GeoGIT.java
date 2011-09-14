/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.util.List;

import org.geogit.api.config.Config;
import org.geogit.repository.Repository;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.type.Name;
import org.opengis.filter.expression.PropertyName;
import org.opengis.util.ProgressListener;

import com.google.common.base.Preconditions;

/**
 * A facade to Geo-GIT operations.
 * <p>
 * Represents the checkout of user's working tree and repository and provides the operations to work
 * on them.
 * </p>
 * 
 * @author groldan
 */
@SuppressWarnings("rawtypes")
public class GeoGIT {

    private final Repository repository;
    
    /**
     * The configuration object stores the git projects configuration
     */
    private Config config;

    public static final CommitStateResolver DEFAULT_COMMIT_RESOLVER = new PlatformResolver();

    private static CommitStateResolver commitStateResolver = DEFAULT_COMMIT_RESOLVER;
    
    public GeoGIT(final Repository repository) {
        Preconditions.checkNotNull(repository, "repository can't be null");
        this.repository = repository;
        this.config = new Config(getRepository());
    }

    public static CommitStateResolver getCommitStateResolver() {
        return commitStateResolver;
    }

    public static void setCommitStateResolver(CommitStateResolver resolver) {
        commitStateResolver = resolver == null ? new PlatformResolver() : resolver;
    }

    public Repository getRepository() {
        return repository;
    }

    public static CloneOp clone(final String url) {
        return null;//new CloneOp(url);
    }
    /**
     * Clone a FeatureType into a new working tree for the {@code user}'s repository.
     * <p>
     * If no repository already exists for the given user, a new one will be initialized.
     * </p>
     * 
     */
    // public void clone(final String user, final FeatureSource featureSource,
    // final ProgressListener progressListener) throws Exception {
    // final Name typeName = featureSource.getName();
    //
    // final Repository userRepo = repositoryBroker.get(user);
    // userRepo.init(typeName);
    //
    // final WorkingTree ftypeWorkingTree = workingTreeBroker.create(user, typeName);
    //
    // final FeatureCollection features = featureSource.getFeatures();
    //
    // ProgressListener addListener = new SubProgressListener(progressListener, 50f);
    // ProgressListener commitListener = new SubProgressListener(progressListener, 50f);
    //
    // progressListener.started();
    // String changeSetId = add(user, typeName, features, addListener);
    // List<String> changeSetIds = Collections.singletonList(changeSetId);
    // commit(user, user, changeSetIds, progressListener, commitListener);
    // progressListener.complete();
    // }

    /**
     * Add a transaction record to the index
     */
    public AddOp add() {
        return new AddOp(repository);
    }

    /**
     * Remove files from the working tree and from the index
     * 
     */
    public String rm(final String user, final Name typeName,
            final FeatureCollection affectedFeatures, final ProgressListener progressListener) {
        throw new UnsupportedOperationException();
    }

    /**
     * @return
     */
    public String update(final String user, final Name typeName,
            final List<PropertyName> changedProperties, final FeatureCollection affectedFeatures,
            final ProgressListener progressListener) {

        throw new UnsupportedOperationException();
    }

    /**
     * Record changes to the repository
     * 
     * @return commit id
     */
    public CommitOp commit() {
        return new CommitOp(repository, commitStateResolver);
    }

    /**
     * List, create or delete branches
     */
    public BranchCreateOp branchCreate() {
        return new BranchCreateOp(repository);
    }

    public BranchDeleteOp branchDelete() {
        return new BranchDeleteOp(repository);
    }

    /**
     * Check out a branch to the working tree
     */
    public CheckoutOp checkout() {
        return new CheckoutOp(repository);
    }

    /**
     * Show changes between commits, commit and working tree, etc
     */
    public DiffOp diff() {
        return new DiffOp(repository);
    }

    /**
     * Download objects and refs from another repository
     * @return new FetchOp
     */
    public FetchOp fetch() {
        return new FetchOp(repository, config.getRemotes());
    }

    /**
     * Create an empty working tree or reinitialize an existing one
     */
    public void init() {

    }

    /**
     * Show commit logs
     */
    public LogOp log() {
        return new LogOp(repository);
    }

    /**
     * Join two or more development histories together
     */
    public MergeOp merge() {
        return new MergeOp(repository);
    }

    /**
     * Fetch from and merge with another repository or a local branch
     */
    public void pull() {

    }

    /**
     * Update remote refs along with associated objects
     */
    public void push() {

    }

    /**
     * Forward-port local commits to the updated upstream head
     */
    public void rebase() {

    }

    /**
     * Reset current HEAD to the specified state
     */
    public void reset() {

    }

    /**
     * Show various types of objects by their unique id
     * 
     * @return
     */
    public ShowOp show() {
        return new ShowOp(repository);
    }

    /**
     * Show the working tree status
     */
    public void status() {

    }

    /**
     * Create, list, delete or verify a tag object
     */
    public void tag() {

    }

    /**
     * Return this GeoGit repositories config object, 
     * stores 
     *  CORE (TODO)
     *  REMOTES
     *  BRANCHS (TODO)
     * @return
     */
    public Config getConfig() {
       return this.config;
    }
    
    /**
     * Implementation of a "git remote add" command to add remotes to the configuraiton, which get updated after a fetch
     * @return
     */
    public RemoteAddOp remoteAddOp(){
        return new RemoteAddOp(getRepository(), this.config);
    }
}
