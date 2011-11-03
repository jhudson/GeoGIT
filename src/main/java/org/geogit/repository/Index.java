package org.geogit.repository;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.geogit.api.DiffEntry;
import org.geogit.api.DiffEntry.ChangeType;
import org.geogit.api.MutableTree;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.SpatialRef;
import org.geogit.api.TreeVisitor;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectWriter;
import org.geogit.storage.RawObjectWriter;
import org.geogit.storage.StagingDatabase;
import org.geogit.storage.WrappedSerialisingFactory;
import org.geotools.util.NullProgressListener;
import org.opengis.geometry.BoundingBox;
import org.opengis.util.ProgressListener;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterators;

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
public class Index implements StagingArea {

    private final Repository repository;

    private final StagingDatabase indexDatabase;

    public Index(Repository repository, StagingDatabase indexDatabase) {
        this.repository = repository;
        this.indexDatabase = indexDatabase;
    }

    @Override
    public StagingDatabase getDatabase() {
        return indexDatabase;
    }

    @Override
    public void created(String... newTreePath) throws Exception {
        Preconditions.checkNotNull(newTreePath);
        created(Arrays.asList(newTreePath));
    }

    @Override
    public void created(final List<String> newTreePath) throws Exception {
        checkValidPath(newTreePath);

        {
            DiffEntry staged = indexDatabase.findStaged(newTreePath);
            if (staged != null) {
                if (!ChangeType.DELETE.equals(staged.getType())) {
                    throw new IllegalArgumentException("Tree exists at " + newTreePath);
                }
            } else {
                if (repository.getRootTreeChild(newTreePath) != null) {
                    throw new IllegalArgumentException("Tree exists at " + newTreePath);
                }
            }
        }

        final String nodeId = newTreePath.get(newTreePath.size() - 1);
        MutableTree emptyTree = indexDatabase.getObjectDatabase().newTree();
        ObjectWriter<RevTree> treeWriter;
        treeWriter = WrappedSerialisingFactory.getInstance().createRevTreeWriter(emptyTree);
        ObjectId emptyTreeId = indexDatabase.getObjectDatabase().put(treeWriter);
        Ref newTreeRef = new Ref(nodeId, emptyTreeId, TYPE.TREE);

        DiffEntry entry = DiffEntry.newInstance(null, newTreeRef, newTreePath);
        indexDatabase.putUnstaged(entry);
    }

    @Override
    public boolean deleted(String... path) throws Exception {
        Preconditions.checkNotNull(path);
        final List<String> searchPath = Arrays.asList(path);
        checkValidPath(searchPath);

        DiffEntry unstagedEntry;
        DiffEntry stagedEntry;

        unstagedEntry = indexDatabase.findUnstaged(searchPath);
        if (unstagedEntry != null) {
            Ref oldObject = null;
            switch (unstagedEntry.getType()) {
            case DELETE:
                // delete already unstaged
                return true;
            case ADD:
                oldObject = unstagedEntry.getNewObject();
                break;
            case MODIFY:
                oldObject = unstagedEntry.getOldObject();
                break;
            default:
                throw new IllegalStateException();
            }
            DiffEntry diffEntry = DiffEntry.newInstance(oldObject, null, searchPath);
            indexDatabase.putUnstaged(diffEntry);
            return true;
        } else if (null != (stagedEntry = indexDatabase.findStaged(searchPath))) {
            Ref oldObject;
            switch (stagedEntry.getType()) {
            case DELETE:
                return false;
            case ADD:
                oldObject = stagedEntry.getNewObject();
                break;
            case MODIFY:
                oldObject = stagedEntry.getOldObject();
            default:
                throw new IllegalStateException();
            }
            DiffEntry diffEntry = DiffEntry.newInstance(oldObject, null, searchPath);
            indexDatabase.putUnstaged(diffEntry);
            return true;
        }

        Ref existingOrStaged = repository.getRootTreeChild(path);
        if (existingOrStaged != null) {
            final Ref oldObject = existingOrStaged;
            final Ref newObject = null;
            DiffEntry deleteEntry = DiffEntry.newInstance(oldObject, newObject, searchPath);
            indexDatabase.putUnstaged(deleteEntry);
            return true;
        }
        return false;
    }

    @Override
    public Ref inserted(ObjectWriter<?> blob, BoundingBox bounds, String... path) throws Exception {

        Preconditions.checkNotNull(blob);
        Preconditions.checkNotNull(path);

        Triplet<ObjectWriter<?>, BoundingBox, List<String>> tuple;
        tuple = new Triplet<ObjectWriter<?>, BoundingBox, List<String>>(blob, bounds,
                Arrays.asList(path));

        List<Ref> inserted = inserted(Iterators.singletonIterator(tuple),
                new NullProgressListener(), null);
        return inserted.get(0);
    }

    @Override
    public List<Ref> inserted(
            final Iterator<Triplet<ObjectWriter<?>, BoundingBox, List<String>>> objects,//
            final ProgressListener progress,//
            final Integer size) throws Exception {

        Preconditions.checkNotNull(objects);
        Preconditions.checkNotNull(progress);
        Preconditions.checkArgument(size == null || size.intValue() > 0);

        List<Ref> inserted = new LinkedList<Ref>();
        Triplet<ObjectWriter<?>, BoundingBox, List<String>> triplet;
        int count = 0;

        while (objects.hasNext()) {
            count++;
            if (progress.isCanceled()) {
                return Collections.emptyList();
            }
            if (size != null) {
                progress.progress((float) (count * 100) / size.intValue());
            }

            triplet = objects.next();
            ObjectWriter<?> object = triplet.getFirst();
            BoundingBox bounds = triplet.getMiddle();
            List<String> path = triplet.getLast();

            final String nodeId = path.get(path.size() - 1);

            ObjectId objectId = indexDatabase.getObjectDatabase().put(object);
            Ref objectRef;
            if (bounds == null) {
                objectRef = new Ref(nodeId, objectId, TYPE.BLOB);
            } else {
                objectRef = new SpatialRef(nodeId, objectId, TYPE.BLOB, bounds);
            }
            inserted.add(objectRef);
            DiffEntry diffEntry = DiffEntry.newInstance(null, null, null, objectRef, path);
            indexDatabase.putUnstaged(diffEntry);
        }
        progress.complete();
        return inserted;
    }

    @Override
    public void stage(final ProgressListener progress, final String... path) throws Exception {
        List<String> path2 = path == null ? null : Arrays.asList(path);
        final int numChanges = indexDatabase.countUnstaged(path2);
        int i = 0;
        progress.started();
        Iterator<DiffEntry> unstaged = indexDatabase.getUnstaged(path2);
        while (unstaged.hasNext()) {
            i++;
            progress.progress((float) (i * 100) / numChanges);

            DiffEntry entry = unstaged.next();
            indexDatabase.stage(entry);
        }
        progress.complete();
    }

    @Override
    public void renamed(final List<String> fromPath, final List<String> toPath) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void reset() {
        int unstagedClearedCount = indexDatabase.removeUnStaged(null);
        int stagedClearedCount = indexDatabase.removeStaged(null);
    }

    @Override
    public Tuple<ObjectId, BoundingBox> writeTree(Ref targetRef) throws Exception {
        return writeTree(targetRef, new NullProgressListener());
    }

    @Override
    public Tuple<ObjectId, BoundingBox> writeTree(final Ref targetRef,
            final ProgressListener progress) throws Exception {

        Preconditions.checkNotNull(targetRef, "null targetRef");
        Preconditions.checkNotNull(progress,
                "null ProgressListener. Use new NullProgressListener() instead");

        final ObjectDatabase repositoryDatabase = repository.getObjectDatabase();

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

        final ObjectId toTreeId = targetRootTreeRef.getObjectId();
        final RevTree oldRoot = repositoryDatabase.getTree(toTreeId);

        List<String> pathFilter = null;
        final int numChanges = indexDatabase.countStaged(pathFilter);
        if (numChanges == 0) {
            return new Tuple<ObjectId, BoundingBox>(toTreeId, null);
        }
        if (progress.isCanceled()) {
            return null;
        }

        Iterator<DiffEntry> staged = indexDatabase.getStaged(pathFilter);

        Map<List<String>, MutableTree> changedTrees = new HashMap<List<String>, MutableTree>();

        DiffEntry diffEntry;
        int i = 0;
        while (staged.hasNext()) {
            progress.progress((float) (++i * 100) / numChanges);
            if (progress.isCanceled()) {
                return null;
            }

            diffEntry = staged.next();

            final List<String> entryPath = diffEntry.getPath();

            final List<String> entryParentPath = entryPath.subList(0, entryPath.size() - 1);
            MutableTree parentTree = changedTrees.get(entryParentPath);
            if (parentTree == null) {
                parentTree = repositoryDatabase.getOrCreateSubTree(oldRoot, entryParentPath);
                changedTrees.put(entryParentPath, parentTree);
            }

            final Ref oldObject = diffEntry.getOldObject();
            final Ref newObject = diffEntry.getNewObject();
            final ChangeType type = diffEntry.getType();
            switch (type) {
            case ADD:
            case MODIFY:
                parentTree.put(newObject);
                deepMove(newObject, indexDatabase, repositoryDatabase);
                break;
            case DELETE:
                parentTree.remove(oldObject.getName());
                break;
            default:
                throw new IllegalStateException("Unknown change type " + type + " for diff "
                        + diffEntry);
            }
        }

        if (progress.isCanceled()) {
            return null;
        }
        // now write back all changed trees
        ObjectId newTargetRootId = toTreeId;
        for (Map.Entry<List<String>, MutableTree> e : changedTrees.entrySet()) {
            List<String> treePath = e.getKey();
            MutableTree tree = e.getValue();
            RevTree newRoot = repositoryDatabase.getTree(newTargetRootId);
            newTargetRootId = repositoryDatabase.writeBack(newRoot.mutable(), tree, treePath);
        }

        indexDatabase.removeStaged(pathFilter);

        progress.complete();
        BoundingBox bounds = null;
        return new Tuple<ObjectId, BoundingBox>(newTargetRootId, bounds);
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

}
