package org.geogit.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.geogit.api.MutableTree;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevBlob;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.repository.DepthSearch;

/**
 * The Index (or Staging Area) object database.
 * <p>
 * This is a composite object database holding a reference to the actual repository object database
 * and a separate object database for the staging area itself.
 * <p>
 * Object look ups are first performed against the staging area database. If the object is not
 * found, then the look up is deferred to the actual repository database.
 * <p>
 * Object writes are always performed against the staging area object database.
 * <p>
 * The staing area database holds references to two root {@link RevTree trees}, one for the staged
 * objects and another one for the unstaged objects. When objects are added/changed/deleted to/from
 * the index, those modifications are written to the unstaged root tree. When objects are staged to
 * be committed, the unstaged objects are moved to the staged root tree.
 * <p>
 * A diff operation between the repository root tree and the index unstaged root tree results in the
 * list of unstaged objects.
 * <p>
 * A diff operation between the repository root tree and the index staged root tree results in the
 * list of staged objects.
 * 
 * @author groldan
 * 
 */
public class StagingDatabase implements ObjectDatabase {

    private static final String STAGED_TREE = "__staged_tree";

    private static final String UNSTAGED_TREE = "__unstaged_tree";

    /**
     * Holds references to the object ids of the staged and unstaged root trees
     */
    private final RefDatabase references;

    /**
     * The staging area object database, contains only differences between the index and the
     * repository
     */
    private final ObjectDatabase stagingDb;

    /**
     * The actual repository object database. Object look up misses on the {@link #stagingDb} are
     * deferred to this repository object store.
     */
    private final ObjectDatabase repoDb;

    /**
     * @param referenceDatabase
     *            the repository reference database, used to get the head re
     * @param repoDb
     * @param stagingDb
     */
    public StagingDatabase(final ObjectDatabase repoDb, final ObjectDatabase stagingDb) {
        this.repoDb = repoDb;
        this.stagingDb = stagingDb;
        this.references = new RefDatabase(stagingDb);
    }

    @Override
    public void create() {
        stagingDb.create();
        references.create();
        Ref stagedTreeRef = references.getRef(STAGED_TREE);
        if (stagedTreeRef == null) {
            reset();
        }
    }

    public void reset() {

        Ref head = references.getRef(Ref.HEAD);
        ObjectId commitId = head.getObjectId();
        ObjectId rootTreeId;
        if (commitId.isNull()) {
            rootTreeId = ObjectId.NULL;
        } else {
            rootTreeId = repoDb.getCommit(commitId).getTreeId();
        }

        Ref stagedTreeRef = new Ref(STAGED_TREE, rootTreeId, TYPE.TREE);
        references.put(stagedTreeRef);

        Ref unstagedTreeRef = references.getRef(UNSTAGED_TREE);
        stagedTreeRef = references.getRef(STAGED_TREE);
        unstagedTreeRef = new Ref(UNSTAGED_TREE, stagedTreeRef.getObjectId(), TYPE.TREE);
        references.put(unstagedTreeRef);
    }

    public MutableTree getStagedRoot() {
        return getTree(references.getRef(STAGED_TREE).getObjectId()).mutable();
    }

    public MutableTree getUnstagedRoot() {
        return getTree(references.getRef(UNSTAGED_TREE).getObjectId()).mutable();
    }

    public Ref getStagedRootRef() {
        return references.getRef(STAGED_TREE);
    }

    public Ref getUnstagedRootRef() {
        return references.getRef(UNSTAGED_TREE);
    }

    public ObjectDatabase getRepositoryDatabase() {
        return repoDb;
    }

    /**
     * Returns a the tree identified by {@code treeId}. Writes back to the index dabase, and looks
     * up first in the index db and if not found in the repo db.
     * 
     * @see org.geogit.storage.ObjectDatabase#getTree(org.geogit.api.ObjectId)
     * @see #getRepositoryTree(ObjectId)
     */
    @Override
    public RevTree getTree(final ObjectId treeId) {
        if (treeId.isNull()) {
            return newTree();
        }
        RevTree tree;
        try {
            tree = this.get(treeId, new RevTreeReader((ObjectDatabase) this));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return tree;
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#newTree()
     */
    @Override
    public MutableTree newTree() {
        return new RevSHA1Tree((ObjectDatabase) this).mutable();
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#close()
     */
    @Override
    public void close() {
        stagingDb.close();
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#exists(org.geogit.api.ObjectId)
     */
    @Override
    public boolean exists(ObjectId id) {
        return stagingDb.exists(id) || repoDb.exists(id);
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#getRaw(org.geogit.api.ObjectId)
     */
    @Override
    public InputStream getRaw(ObjectId id) throws IOException {
        if (stagingDb.exists(id)) {
            return stagingDb.getRaw(id);
        }
        return repoDb.getRaw(id);
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#get(org.geogit.api.ObjectId,
     *      org.geogit.storage.ObjectReader)
     */
    @Override
    public <T> T get(ObjectId id, ObjectReader<T> reader) throws IOException {
        if (stagingDb.exists(id)) {
            return stagingDb.get(id, reader);
        }
        return repoDb.get(id, reader);
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#getCached(org.geogit.api.ObjectId,
     *      org.geogit.storage.ObjectReader)
     */
    @Override
    public <T> T getCached(ObjectId id, ObjectReader<T> reader) throws IOException {
        if (stagingDb.exists(id)) {
            return stagingDb.getCached(id, reader);
        }
        return repoDb.getCached(id, reader);
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#put(org.geogit.storage.ObjectWriter)
     */
    @Override
    public <T> ObjectId put(ObjectWriter<T> writer) throws Exception {
        return stagingDb.put(writer);
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#put(org.geogit.api.ObjectId,
     *      org.geogit.storage.ObjectWriter)
     */
    @Override
    public boolean put(ObjectId id, ObjectWriter<?> writer) throws Exception {
        return stagingDb.put(id, writer);
    }

    @Override
    public ObjectInserter newObjectInserter() {
        return stagingDb.newObjectInserter();
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#getCommit(org.geogit.api.ObjectId)
     */
    @Override
    public RevCommit getCommit(final ObjectId commitId) {
        RevCommit commit;
        try {
            commit = this.getCached(commitId, new CommitReader());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return commit;
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#getTreeChild(org.geogit.api.RevTree,
     *      java.lang.String[])
     */
    @Override
    public Ref getTreeChild(RevTree root, String... path) {
        return getTreeChild(root, Arrays.asList(path));
    }

    @Override
    public Ref getTreeChild(RevTree root, List<String> path) {
        Ref treeRef = new DepthSearch(this).find(root, path);
        return treeRef;
    }

    public void setUnstagedRoot(ObjectId newRootId) {
        Ref ref = new Ref(UNSTAGED_TREE, newRootId, TYPE.TREE);
        references.put(ref);
    }

    public void setStagedRoot(ObjectId newRootId) {
        Ref ref = new Ref(STAGED_TREE, newRootId, TYPE.TREE);
        references.put(ref);
    }

    /**
     * Clears the unstaged tree
     */
    public void clearUnstaged() {
        // TODO: delete stale objects?
        Ref stagedTreeRef = new Ref(UNSTAGED_TREE, ObjectId.NULL, TYPE.TREE);
        references.put(stagedTreeRef);
    }

    /**
     * Clears the staged tree
     */
    public void clearStaged() {
        // TODO: delete stale objects?
        Ref stagedTreeRef = new Ref(STAGED_TREE, ObjectId.NULL, TYPE.TREE);
        references.put(stagedTreeRef);
    }

    @Override
    public MutableTree getOrCreateSubTree(RevTree parent, List<String> childPath) {
        return stagingDb.getOrCreateSubTree(parent, childPath);
    }

    @Override
    public ObjectId writeBack(MutableTree root, RevTree tree, List<String> pathToTree)
            throws Exception {
        return stagingDb.writeBack(root, tree, pathToTree);
    }

    @Override
    public boolean delete(final ObjectId objectId) {
        return stagingDb.delete(objectId);
    }

    @Override
    public List<ObjectId> lookUp(String partialId) {
        List<ObjectId> lookUp = stagingDb.lookUp(partialId);
        if (lookUp.isEmpty()) {
            lookUp = repoDb.lookUp(partialId);
        }
        return lookUp;
    }

    @Override
    public RevBlob getBlob(ObjectId objectId) {
        if (stagingDb.exists(objectId)) {
            return stagingDb.getBlob(objectId);
        }
        return repoDb.getBlob(objectId);
    }

}
