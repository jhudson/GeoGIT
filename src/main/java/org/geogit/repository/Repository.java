/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevBlob;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject;
import org.geogit.api.RevTag;
import org.geogit.api.RevTree;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectInserter;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.RefDatabase;
import org.geogit.storage.RepositoryDatabase;
import org.geogit.storage.WrappedSerialisingFactory;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

/**
 * A repository is a collection of commits, each of which is an archive of what the project's
 * working tree looked like at a past date, whether on your machine or someone else's.
 * <p>
 * It also defines HEAD (see below), which identifies the branch or commit the current working tree
 * stemmed from. Lastly, it contains a set of branches and tags, to identify certain commits by
 * name.
 * </p>
 * 
 * @author Gabriel Roldan
 * @see WorkingTree
 */
public class Repository {

    private static final Logger LOGGER = Logging.getLogger(Repository.class);

    private final RepositoryDatabase repoDb;

    private final StagingArea index;

    private final WorkingTree workingTree;
    
    /**
     * This is stored here for the convenience of knowing where to load the configuration file from
     */
    private final File repositoryHome;
    
    public Repository(final RepositoryDatabase repoDb, File envHome) {
        Preconditions.checkNotNull(repoDb);
        this.repoDb = repoDb;
        // index = new Index(repoDb.getStagingDatabase());
        index = new Index(this, repoDb.getStagingDatabase());
        workingTree = new WorkingTree(this);
        this.repositoryHome = envHome;
    }

    public void create() {
        repoDb.create();
    }

    public RefDatabase getRefDatabase() {
        return repoDb.getReferenceDatabase();
    }

    public ObjectDatabase getObjectDatabase() {
        return repoDb.getObjectDatabase();
    }

    public StagingArea getIndex() {
        return index;
    }

    public void close() {
        repoDb.close();
    }

    public WorkingTree getWorkingTree() {
        return workingTree;
    }

    public InputStream getRawObject(final ObjectId oid) throws IOException {
        return getObjectDatabase().getRaw(oid);
    }

    @SuppressWarnings("unchecked")
    public <T extends RevObject> T resolve(final String revstr, Class<T> type) {
        Ref ref = getRef(revstr);
        if (ref != null) {
            RevObject parsed = parse(ref);
            if (!type.isAssignableFrom(parsed.getClass())) {
                return (T) parsed;
            }
        }

        // not a ref name, may be a partial object id?
        List<ObjectId> lookUp = getObjectDatabase().lookUp(revstr);
        if (!lookUp.isEmpty()) {
            for (ObjectId oid : lookUp) {
                try {
                    if (RevCommit.class.equals(type) || RevTag.class.equals(type)) {
                        return (T) getCommit(oid);
                    } else if (RevTree.class.equals(type)) {
                        return (T) getTree(oid);
                    } else if (RevBlob.class.equals(type)) {
                        return (T) getBlob(oid);
                    }
                } catch (Exception wrongType) {
                    // ignore
                }
            }
        }
        throw new NoSuchElementException();
    }

    /**
     * @param revstr
     *            an object reference expression
     * @return the object the resolved reference points to.
     * @throws NoSuchElementException
     *             if {@code revstr} dosn't resolve to any object
     * @throws IllegalArgumentException
     *             if {@code revstr} resolves to more than one object.
     */
    public RevObject resolve(final String revstr) {
        Ref ref = getRef(revstr);
        if (ref != null) {
            return parse(ref);
        }

        // not a ref name, may be a partial object id?
        List<ObjectId> lookUp = getObjectDatabase().lookUp(revstr);
        if (lookUp.size() == 1) {
            final ObjectId objectId = lookUp.get(0);
            try {
                return getCommit(objectId);
            } catch (Exception e) {
                try {
                    return getTree(objectId);
                } catch (Exception e2) {
                    return getBlob(objectId);
                }
            }
        }
        if (lookUp.size() > 1) {
            throw new IllegalArgumentException("revstr '" + revstr
                    + "' resolves to more than one object: " + lookUp);
        }
        throw new NoSuchElementException();
    }

    public RevObject getBlob(ObjectId objectId) {
        return getObjectDatabase().getBlob(objectId);
    }

    private RevObject parse(final Ref ref) {
        final ObjectDatabase objectDatabase = getObjectDatabase();
        switch (ref.getType()) {
        case BLOB:
            return getBlob(ref.getObjectId());
        case COMMIT:
        case TAG:
            return objectDatabase.getCommit(ref.getObjectId());
        case TREE:
            return objectDatabase.getTree(ref.getObjectId());
        default:
            throw new IllegalArgumentException("Unknown ref type: " + ref);
        }
    }

    public Ref getRef(final String revStr) {
        return getRefDatabase().getRef(revStr);
    }

    public Ref getHead() {
        return getRef(Ref.HEAD);
    }

    public synchronized Ref updateRef(final Ref ref) {
        boolean updated = getRefDatabase().put(ref);
        Preconditions.checkState(updated);
        Ref ref2 = getRef(ref.getName());
        Preconditions.checkState(ref.equals(ref2));
        return ref;
    }

    public boolean commitExists(final ObjectId id) {
        try {
            getObjectDatabase().getCached(id, WrappedSerialisingFactory.getInstance().createCommitReader());
//            getObjectDatabase().getCached(id, new BxmlCommitReader());
        } catch (IllegalArgumentException e) {
            return false;
        } catch (IOException e) {
            Throwables.propagate(e);
        }
        return true;
    }

    public RevCommit getCommit(final ObjectId commitId) {
        Preconditions.checkNotNull(commitId, "commitId");
        return getObjectDatabase().getCommit(commitId);
    }

    public RevTree getTree(final ObjectId treeId) {
        if (treeId.isNull()) {
            return newTree();
        }
        RevTree tree;
        try {
            tree = getObjectDatabase().getCached(treeId, WrappedSerialisingFactory.getInstance().createRevTreeReader(getObjectDatabase()));
//            tree = getObjectDatabase().getCached(treeId, new BxmlRevTreeReader(getObjectDatabase()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return tree;
    }

    public ObjectId getRootTreeId() {
        // find the root tree
        Ref head = getRef(Ref.HEAD);
        if (head == null) {
            throw new IllegalStateException("Repository has no HEAD");
        }

        final ObjectId headCommitId = head.getObjectId();
        if (headCommitId.isNull()) {
            return ObjectId.NULL;
        }
        final RevCommit lastCommit = getCommit(headCommitId);
        final ObjectId rootTreeId = lastCommit.getTreeId();
        return rootTreeId;
    }

    /**
     * @return the root tree for the current HEAD
     */
    public RevTree getHeadTree() {

        RevTree root;
        try {
            ObjectId rootTreeId = getRootTreeId();
            if (rootTreeId.isNull()) {
                return newTree();
            }
            root = getObjectDatabase().get(rootTreeId, WrappedSerialisingFactory.getInstance().createRevTreeReader(getObjectDatabase()));
//            root = getObjectDatabase().get(rootTreeId, new BxmlRevTreeReader(getObjectDatabase()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return root;
    }

    /**
     * @return an {@link ObjectInserter} to insert objects into the object database
     */
    public ObjectInserter newObjectInserter() {
        return getObjectDatabase().newObjectInserter();
    }

    public Feature getFeature(final FeatureType featureType, final String featureId,
            final ObjectId contentId) {
        ObjectReader<Feature> reader = WrappedSerialisingFactory.getInstance().createFeatureReader(featureType, featureId);
//        BxmlFeatureReader reader = new BxmlFeatureReader(featureType, featureId);
        Feature feature;
        try {
            feature = getObjectDatabase().get(contentId, reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return feature;
    }

    @Deprecated
    public void beginTransaction() {
        this.repoDb.beginTransaction();
    }

    @Deprecated
    public void commitTransaction() {
        this.repoDb.commitTransaction();
    }

    @Deprecated
    public void rollbackTransaction() {
        this.repoDb.rollbackTransaction();
    }

    /**
     * Creates and return a new, empty tree, that stores to this repository's {@link ObjectDatabase}
     */
    public RevTree newTree() {
        return getObjectDatabase().newTree();
    }

    public Ref getRootTreeChild(List<String> path) {
        RevTree root = getHeadTree();
        return getObjectDatabase().getTreeChild(root, path);
    }

    public Ref getRootTreeChild(String... path) {
        RevTree root = getHeadTree();
        return getObjectDatabase().getTreeChild(root, path);
    }
    
    /**
     * Add a new REF to the reference database, this is used mostly by a fetch to create new references to remote branches
     * @param ref
     */
    public void addRef(Ref ref) {
        this.repoDb.getReferenceDatabase().addRef(ref);
    }

    /**
     * Get this repositories home directory on disk
     * @return
     */
    public File getRepositoryHome() {
        return repositoryHome;
    }
}
