/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geogit.api.DiffEntry;
import org.geogit.api.DiffEntry.ChangeType;
import org.geogit.api.DiffTreeWalk;
import org.geogit.api.MutableTree;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.SpatialRef;
import org.geogit.api.TreeVisitor;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectInserter;
import org.geogit.storage.ObjectWriter;
import org.geogit.storage.RawObjectWriter;
import org.geogit.storage.StagingDatabase;
import org.geotools.util.NullProgressListener;
import org.opengis.geometry.BoundingBox;
import org.opengis.util.ProgressListener;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

/**
 * The Index keeps track of the changes not yet committed to the repository.
 * <p>
 * The Index uses an {@link StagingDatabase object database} as storage for the staged and unstaged
 * changes. This allows for really large operations not to eat up too much heap, and also works
 * better and allows for easier implementation of operations that need to manipulate the index.
 * <p>
 * The Index database is a composite of its own ObjectDatabase and the repository's. Object look ups
 * against the index first search on the index db, and if not found defer to the repository object
 * db.
 * <p>
 * The index holds references to two trees of its own, one for the staged changes and one for the
 * unstaged ones. Modifications to the working tree shall update the Index unstaged changes tree
 * through the {@link #inserted(Iterator) inserted} and {@link #deleted(String...) deleted} methods
 * (an object update is just another insert as far as GeoGit is concerned).
 * <p>
 * Marking unstaged changes to be committed is made through the {@link #stage(String...)} method.
 * <p>
 * Internally, finding out what changes are unstaged is a matter of comparing (through a diff tree
 * walk) the unstaged changes tree and the staged changes tree. And finding out what changes are
 * staged to be committed is performed through a diff tree walk comparing the staged changes tree
 * and the repository's head tree (or any other repository tree reference given to
 * {@link #writeTree(Ref)}).
 * <p>
 * When staged changes are to be committed to the repository, the {@link #writeTree(Ref)} method
 * shall be called with a reference to the repository root tree that the staged changes tree is to
 * be compared against (usually the HEAD tree ref).
 * 
 * @author Gabriel Roldan
 * 
 */
public class Index {

    private StagingDatabase indexDatabase;

    public Index(final StagingDatabase indexDatabase) {
        this.indexDatabase = indexDatabase;
    }

    public StagingDatabase getDatabase() {
        return indexDatabase;
    }

    public RevTree getUnstaged() {
        return indexDatabase.getUnstagedRoot();
    }

    public RevTree getStaged() {
        return indexDatabase.getStagedRoot();
    }

    /**
     * @see #created(List)
     */
    public synchronized void created(final String... newTreePath) throws Exception {
        Preconditions.checkNotNull(newTreePath);
        created(Arrays.asList(newTreePath));
    }

    /**
     * Creates an empty unstaged tree at the given path
     * 
     * @param newTreePath
     * @throws Exception
     *             if an error happens writing the new tree
     * @throws IllegalArgumentException
     *             if a tree or blob already exists at the given path
     */
    public synchronized void created(final List<String> newTreePath) throws Exception {
        checkValidPath(newTreePath);

        Ref treeChildId = indexDatabase.getTreeChild(indexDatabase.getUnstagedRoot(), newTreePath);

        if (null != treeChildId) {
            throw new IllegalArgumentException("Tree exists at " + newTreePath);
        }

        MutableTree newTree = indexDatabase.newTree();
        writeBack(newTree, newTreePath, false);
    }

    /**
     * Marks the object (tree or feature) addressed by {@code path} as an unstaged delete.
     * 
     * @param path
     * @return
     * @throws Exception
     */
    public synchronized boolean deleted(final String... path) throws Exception {
        Preconditions.checkNotNull(path);
        Preconditions.checkArgument(path.length > 0);

        MutableTree unstagedRoot = indexDatabase.getUnstagedRoot();

        final String leafName = path[path.length - 1];
        final List<String> parentPath;
        RevTree parent = null;
        if (path.length == 1) {
            parent = unstagedRoot;
            parentPath = Collections.emptyList();
        } else {
            parentPath = Arrays.asList(path).subList(0, path.length - 1);
            Ref parentRef = indexDatabase.getTreeChild(unstagedRoot, parentPath);
            if (parentRef != null) {
                parent = indexDatabase.getTree(parentRef.getObjectId());
            }
        }

        Ref removed = null;
        if (parent != null) {
            parent = parent.mutable();
            removed = ((MutableTree) parent).remove(leafName);
            writeBack(parent, parentPath, false);
        }
        return removed != null;
    }

    /**
     * @param tree
     * @param path
     * @param staged
     *            whether to save the tree and its ancestors to the staged root (true) or the
     *            unstaged one (false)
     * @return new root id, either for the staged tree or the unstaged one, depending on the
     *         {@code staged} param
     * @throws Exception
     */
    private ObjectId writeBack(final RevTree tree, final List<String> path, final boolean staged)
            throws Exception {
        MutableTree root;
        if (staged) {
            root = indexDatabase.getStagedRoot();
        } else {
            root = indexDatabase.getUnstagedRoot();
        }
        ObjectId newRootId = indexDatabase.writeBack(root, tree, path);
        if (staged) {
            indexDatabase.setStagedRoot(newRootId);
        } else {
            indexDatabase.setUnstagedRoot(newRootId);
        }
        return newRootId;
    }

    /**
     * Inserts an object into de index database and marks it as unstaged.
     * 
     * @param blob
     *            the writer for the object to be inserted.
     * @param path
     *            the path from the repository root to the name of the object to be inserted.
     * @return the reference to the newly inserted object.
     * @throws Exception
     */
    public Ref inserted(final ObjectWriter<?> blob, final BoundingBox bounds, final String... path)
            throws Exception {

        Preconditions.checkNotNull(blob);
        Preconditions.checkNotNull(path);

        Tuple<ObjectWriter<?>, BoundingBox, List<String>> tuple;
        tuple = new Tuple<ObjectWriter<?>, BoundingBox, List<String>>(blob, bounds,
                Arrays.asList(path));

        List<Ref> inserted = inserted(Collections.singleton(tuple).iterator(),
                new NullProgressListener(), null);

        return inserted.get(0);
    }

    /**
     * Inserts the given objects into the index database and marks them as unstaged.
     * 
     * @param objects
     *            list of blobs to be batch inserted as unstaged, as [Object writer, bounds, path]
     * @return list of inserted blob references
     * @throws Exception
     */
    public synchronized List<Ref> inserted(
            final Iterator<Tuple<ObjectWriter<?>, BoundingBox, List<String>>> objects,
            final ProgressListener progress, final Integer size) throws Exception {

        Preconditions.checkNotNull(objects);
        Preconditions.checkNotNull(progress);
        Preconditions.checkArgument(size == null || size.intValue() > 0);

        final MutableTree currentUnstagedRoot = indexDatabase.getUnstagedRoot();
        final ObjectInserter objectInserter = indexDatabase.newObjectInserter();

        Tuple<ObjectWriter<?>, BoundingBox, List<String>> next;
        ObjectWriter<?> object;
        BoundingBox bounds;
        List<String> path;

        List<Ref> inserts = new ArrayList<Ref>();

        Map<List<String>, MutableTree> changedTrees = new HashMap<List<String>, MutableTree>();

        int count = 0;
        // first insert all the objects and hold the modified leaf trees
        while (objects.hasNext()) {
            count++;
            if (size != null) {
                progress.progress((float) (count * 100) / size.intValue());
            }

            next = objects.next();
            object = next.getFirst();
            bounds = next.getMiddle();
            path = next.getLast();
            checkValidInsert(object, path);

            final String nodeId = path.get(path.size() - 1);
            final ObjectId blobId = objectInserter.insert(object);
            Ref blobRef;
            if (bounds == null || bounds.isEmpty()) {
                blobRef = new Ref(nodeId, blobId, TYPE.BLOB);
            } else {
                blobRef = new SpatialRef(nodeId, blobId, TYPE.BLOB, bounds);
            }

            inserts.add(blobRef);

            final List<String> blobParentPath = path.subList(0, path.size() - 1);
            MutableTree parentTree = changedTrees.get(blobParentPath);
            if (parentTree == null) {
                parentTree = indexDatabase.getOrCreateSubTree(currentUnstagedRoot, blobParentPath);
                changedTrees.put(blobParentPath, parentTree);
            }

            parentTree.put(blobRef);
        }

        // now write back all changed trees
        for (Map.Entry<List<String>, MutableTree> e : changedTrees.entrySet()) {
            List<String> treePath = e.getKey();
            MutableTree tree = e.getValue();
            writeBack(tree, treePath, false);
        }

        return inserts;
    }

    private void checkValidInsert(ObjectWriter<?> object, List<String> path) {
        checkValidPath(path);
        if (object == null) {
            throw new IllegalArgumentException("Object is null for path: " + path);
        }
    }

    private void checkValidPath(List<String> path) {
        if (path == null || path.size() == 0) {
            throw new IllegalArgumentException("null path");
        }
        if (path == null || path.size() == 0) {
            throw new IllegalArgumentException("empty path");
        }
        for (int i = 0; i < path.size(); i++) {
            if (path.get(i) == null) {
                throw new IllegalArgumentException("null path element at index " + i + ": " + path);
            }
        }
    }

    /**
     * Stages the object addressed by path to be added, if it's marked as an unstaged change. Does
     * nothing otherwise.
     * <p>
     * To stage changes not yet staged, a diff tree walk is performed using the current staged
     * {@link RevTree} as the old object and the current unstaged {@link RevTree} as the new object.
     * Then all the differences are traversed and the staged tree is updated with the changes
     * reported by the diff walk (neat).
     * </p>
     * 
     * @param path
     * @throws Exception
     */
    public synchronized void stage(final String... path) throws Exception {
        if (path == null) {
            // add all
            final ObjectId newStagedRootId = stageAll();
            // this is the add all operation, so set the unstaged root too
            indexDatabase.setUnstagedRoot(newStagedRootId);

            return;
        }
        checkValidPath(Arrays.asList(path));
        throw new UnsupportedOperationException("partial staging not yet implemented");
    }

    private ObjectId stageAll() throws Exception {
        final Ref stagedRoot = indexDatabase.getStagedRootRef();
        final Ref unstagedRoot = indexDatabase.getUnstagedRootRef();

        final ObjectId fromTreeId = unstagedRoot.getObjectId();
        final StagingDatabase fromDb = indexDatabase;
        final ObjectId toTreeId = stagedRoot.getObjectId();
        final StagingDatabase targetDb = indexDatabase;
        final boolean copyContents = false;
        final ObjectId newRootTreeId = copyTreeDiffs(fromTreeId, fromDb, toTreeId, targetDb,
                copyContents);

        indexDatabase.setStagedRoot(newRootTreeId);

        return newRootTreeId;

    }

    private ObjectId copyTreeDiffs(final ObjectId fromTreeId, final ObjectDatabase fromDb,
            final ObjectId toTreeId, final ObjectDatabase targetDb, final boolean moveContents)
            throws Exception {

        if (fromTreeId.equals(toTreeId)) {
            return fromTreeId;
        }

        Map<List<String>, MutableTree> changedTrees = new HashMap<List<String>, MutableTree>();

        final Ref newTreeRef = new Ref("", fromTreeId, TYPE.TREE);
        final Ref oldTreeRef = new Ref("", toTreeId, TYPE.TREE);

        final RevTree targetRoot = targetDb.getTree(toTreeId);

        DiffTreeWalk diffTreeWalk = new DiffTreeWalk(fromDb, oldTreeRef, newTreeRef);
        Iterator<DiffEntry> diffs = diffTreeWalk.get();
        DiffEntry diff;
        while (diffs.hasNext()) {
            diff = diffs.next();

            List<String> path = diff.getPath();

            final List<String> blobParentPath = path.subList(0, path.size() - 1);
            MutableTree parentTree = changedTrees.get(blobParentPath);
            if (parentTree == null) {
                parentTree = targetDb.getOrCreateSubTree(targetRoot, blobParentPath);
                changedTrees.put(blobParentPath, parentTree);
            }

            final ChangeType type = diff.getType();
            final Ref oldObject = diff.getOldObject();
            final Ref newObject = diff.getNewObject();
            switch (type) {
            case ADD:
            case MODIFY:
                parentTree.put(newObject);
                if (moveContents) {
                    deepMove(newObject, fromDb, targetDb);
                }
                break;
            case DELETE:
                parentTree.remove(oldObject.getName());
                break;
            default:
                throw new IllegalStateException("Unknown change type " + type + " for diff " + diff);
            }
        }

        // now write back all changed trees
        ObjectId newTargetRootId = toTreeId;
        for (Map.Entry<List<String>, MutableTree> e : changedTrees.entrySet()) {
            List<String> treePath = e.getKey();
            MutableTree tree = e.getValue();
            RevTree newRoot = targetDb.getTree(newTargetRootId);
            newTargetRootId = targetDb.writeBack(newRoot.mutable(), tree, treePath);
        }
        return newTargetRootId;
    }

    /**
     * Marks an object rename (in practice, it's used to change the feature id of a Feature once it
     * was committed and the DataStore generated FID is obtained)
     * 
     * @param from
     *            old path to featureId
     * @param to
     *            new path to featureId
     */
    public void renamed(final List<String> from, final List<String> to) {
        // TreeMap<String, Entry> oldMap = getFidMap(from.get(0), from.get(1));
        // TreeMap<String, Entry> newMap = getFidMap(to.get(0), to.get(1));
        // final String oldFid = from.get(2);
        // final String newFid = to.get(2);
        //
        // Entry entry = oldMap.remove(oldFid);
        // newMap.put(newFid, entry);
    }

    /**
     * Discards any staged change.
     * 
     * @REVISIT: should this be implemented through ResetOp (GeoGIT.reset()) instead?
     * @TODO: When we implement transaction management will be the time to discard any needed object
     *        inserted to the database too
     */
    public void reset() {
        indexDatabase.reset();
    }

    /**
     * Updates the repository target HEAD tree given by {@code targetRootRef} with the staged
     * changes in this index.
     * 
     * @param targetRef
     *            reference to either a commit or a tree that's the root of the head to be updated
     * @param objectInserter
     * @return non-null tuple, but possibly with null elements, containing the id of the new top
     *         level tree created on the repository after applying the staged changes, and the
     *         aggregated bounds of the changes, if any.
     * @throws Exception
     */
    public Tuple<ObjectId, BoundingBox, ?> writeTree(final Ref targetRef) throws Exception {
        Preconditions.checkNotNull(targetRef);

        final ObjectDatabase repositoryDatabase = indexDatabase.getRepositoryDatabase();

        // resolve target ref to the target root tree id
        final Ref targetRootTreeRef;
        if (TYPE.TREE.equals(targetRef.getType())) {
            targetRootTreeRef = targetRef;
        } else if (TYPE.COMMIT.equals(targetRef.getType())) {
            if (targetRef.getObjectId().isNull()) {
                targetRootTreeRef = new Ref(targetRef.getName(), ObjectId.NULL, TYPE.TREE);
            } else {
                RevCommit commit = repositoryDatabase.getCommit(targetRef.getObjectId());
                ObjectId targetTeeId = commit.getTreeId();
                targetRootTreeRef = new Ref(targetRef.getName(), targetTeeId, TYPE.TREE);
            }
        } else {
            throw new IllegalStateException("target ref is not a commit nor a tree");
        }

        final ObjectId fromTreeId = indexDatabase.getStagedRootRef().getObjectId();
        final ObjectDatabase fromDb = indexDatabase;
        final ObjectId toTreeId = targetRootTreeRef.getObjectId();
        final ObjectDatabase targetDb = repositoryDatabase;
        final boolean copyContents = true;

        final ObjectId newRepoTreeId = copyTreeDiffs(fromTreeId, fromDb, toTreeId, targetDb,
                copyContents);

        indexDatabase.setStagedRoot(newRepoTreeId);

        BoundingBox bounds = null;
        return new Tuple<ObjectId, BoundingBox, Object>(newRepoTreeId, bounds);
    }

    /**
     * Transfers the object referenced by {@code objectRef} from the given object database to the
     * given objectInserter as well as any child object if {@code objectRef} references a tree.
     * 
     * @param newObject
     * @param repositoryObjectInserter
     * @throws Exception
     */
    private void deepMove(final Ref objectRef, final ObjectDatabase from, final ObjectDatabase to)
            throws Exception {

        final InputStream raw = from.getRaw(objectRef.getObjectId());
        final ObjectId insertedId;
        try {
            insertedId = to.put(new RawObjectWriter(raw));
            from.delete(objectRef.getObjectId());

            Preconditions.checkState(objectRef.getObjectId().equals(insertedId));
            Preconditions.checkState(to.exists(insertedId));

        } finally {
            raw.close();
        }

        if (TYPE.TREE.equals(objectRef.getType())) {
            RevTree tree = from.getTree(objectRef.getObjectId());
            tree.accept(new TreeVisitor() {

                @Override
                public boolean visitEntry(final Ref ref) {
                    try {
                        deepMove(ref, from, to);
                    } catch (Exception e) {
                        Throwables.propagate(e);
                    }
                    return true;
                }

                @Override
                public boolean visitSubTree(int bucket, ObjectId treeId) {
                    return true;
                }
            });
        }
    }
}
