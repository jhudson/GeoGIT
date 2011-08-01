/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.geogit.api.MutableTree;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.SpatialRef;
import org.geogit.storage.ObjectInserter;
import org.geogit.storage.ObjectWriter;
import org.geogit.storage.RevTreeWriter;
import org.geotools.util.logging.Logging;
import org.opengis.geometry.BoundingBox;

import com.google.common.base.Preconditions;

/**
 * The Index keeps track of the changes not yet committed to the repository.
 * <p>
 * Unlike other, similar tools you may have used, Git, I mean, GeoServer, does not commit changes
 * directly from the working tree into the repository. Instead, changes are first registered in
 * something called the index. Think of it as a way of "confirming" your changes, one by one, before
 * doing a commit (which records all your approved changes at once). Some find it helpful to call it
 * the "staging area", instead of the "index".
 * </p>
 * 
 * @author Gabriel Roldan
 * 
 */
public class Index {

    private static final Logger LOGGER = Logging.getLogger(Index.class);

    private Repository repository;

    private static final Comparator<List<String>> PATH_COMPARATOR = new Comparator<List<String>>() {
        public int compare(List<String> o1, List<String> o2) {
            int c = Integer.valueOf(o1.size()).compareTo(Integer.valueOf(o2.size()));
            if (c == 0) {
                for (int i = 0; i < o1.size(); i++) {
                    c = o1.get(i).compareTo(o2.get(i));
                    if (c != 0) {
                        return c;
                    }
                }
            }
            return c;
        }
    };

    // <parents, <featureId, blobId>>
    private TreeMap<List<String>, TreeMap<String, Entry>> staged;

    /**
     * 
     * Status of an entry in the index. Aka, status of a feature
     * 
     */
    public static enum Status {
        /**
         * Content is new and not staged to be committed
         */
        NEW_UNSTAGED,

        /**
         * Content is new and staged to be committed
         */
        NEW_STAGED,

        /**
         * Content exists, is modified, but is not staged to be committed
         */
        MODIFIED_UNSTAGED,

        /**
         * Content exists, is modified, and is staged to be committed
         */
        MODIFIED_STAGED,

        /**
         * Content was deleted, but is not staged to be committed
         */
        DELETED_UNSTAGED,

        /**
         * Content was deleted and staged to be committed
         */
        DELETED_STAGED
    }

    /*
     *
     */
    public static class Entry {

        private ObjectId blobId;

        private Status status;

        private BoundingBox bounds;

        public Entry(ObjectId blobId, BoundingBox bounds, Status status) {
            this.blobId = blobId;
            this.bounds = bounds;
            this.status = status;
        }

        public String toString() {
            return "[" + status + ":" + blobId.toString() + "]";
        }
    }

    public Index(final Repository repository) {
        this.repository = repository;
        this.staged = new TreeMap<List<String>, TreeMap<String, Entry>>(PATH_COMPARATOR);
    }

    public ObjectId created(final List<String> newTreePath) throws Exception {
        ObjectId treeChildId = repository.getTreeChildId(repository.getRootTree(), newTreePath);
        if (null != treeChildId) {
            throw new IllegalArgumentException("Tree exists at " + newTreePath);
        }
        ObjectWriter<?> emptyTree = new RevTreeWriter(repository.newTree());
        BoundingBox bounds = null;
        ObjectId newSubtreeId = inserted(emptyTree, bounds,
                newTreePath.toArray(new String[newTreePath.size()]));
        return newSubtreeId;
    }

    /**
     * Marks the object (tree or feature) addressed by {@code path} as an unstaged delete
     */
    public boolean deleted(final String... path) {
        Preconditions.checkNotNull(path);
        Preconditions.checkArgument(path.length > 0);

        final String fid = path[2];
        final String typeName = path[1];
        final String nsuri = path[0];

        final ObjectId typeTreeId = repository.getTreeChildId(nsuri, typeName);
        if (typeTreeId == null) {
            return false;
        }
        RevTree tree = repository.getTree(typeTreeId);
        Ref child = tree.get(fid);
        if (child == null) {
            return false;
        }

        TreeMap<String, Entry> fidMap = getFidMap(nsuri, typeName);
        fidMap.put(fid, new Entry(child.getObjectId(), null, Status.DELETED_UNSTAGED));
        return true;
    }

    /**
     * @param object
     * @param path
     * @return
     * @throws Exception
     */
    public ObjectId inserted(final ObjectWriter<?> object, final BoundingBox bounds,
            final String... path) throws Exception {
        Preconditions.checkNotNull(object);
        Preconditions.checkNotNull(path);

        final ObjectInserter objectInserter = repository.newObjectInserter();
        final ObjectId objectId = objectInserter.insert(object);
        final String featureId = path[path.length - 1];

        TreeMap<String, Entry> fidMap = getFidMap(path[0], path[1]);
        fidMap.put(featureId, new Entry(objectId, bounds, Status.NEW_UNSTAGED));

        return objectId;
    }

    /**
     * Stages the object addressed by patht o be added, if it's marked as an unstaged change. Does
     * nothing otherwise.
     * 
     * @param path
     */
    public void add(final String... path) {
        if (path == null) {
            // add all
            for (TreeMap<String, Entry> parentPaths : staged.values()) {
                setStaged(parentPaths.values());
            }
            return;
        } else if (path.length < 2) {
            throw new UnsupportedOperationException("not yet supported");
        }
        List<String> parent = Arrays.asList(path[0], path[1]);
        TreeMap<String, Entry> fidMap = staged.get(parent);
        if (path.length == 2) {
            setStaged(fidMap.values());
        } else {
            Entry entry = fidMap.get(path[2]);
            setStaged(entry);
        }
    }

    private void setStaged(Collection<Entry> entries) {
        if (entries == null) {
            return;
        }
        for (Entry e : entries) {
            setStaged(e);
        }
    }

    private void setStaged(Entry entry) {
        if (entry == null) {
            return;
        }
        switch (entry.status) {
        case NEW_UNSTAGED:
            entry.status = Status.NEW_STAGED;
            break;
        case MODIFIED_UNSTAGED:
            entry.status = Status.MODIFIED_STAGED;
            break;
        case DELETED_UNSTAGED:
            entry.status = Status.DELETED_STAGED;
            break;
        }
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
        TreeMap<String, Entry> oldMap = getFidMap(from.get(0), from.get(1));
        TreeMap<String, Entry> newMap = getFidMap(to.get(0), to.get(1));
        final String oldFid = from.get(2);
        final String newFid = to.get(2);

        Entry entry = oldMap.remove(oldFid);
        newMap.put(newFid, entry);
    }

    /**
     * Discards any staged change.
     * 
     * @REVISIT: should this be implemented through ResetOp (GeoGIT.reset()) instead?
     * @TODO: When we implement transaction management will be the time to discard any needed object
     *        inserted to the database too
     */
    public void reset() {
        this.staged.clear();
    }

    private synchronized TreeMap<String, Entry> getFidMap(final String nsuri, final String typeName) {
        final List<String> parents = Arrays.asList(nsuri, typeName);
        TreeMap<String, Entry> fidMap = staged.get(parents);

        if (fidMap == null) {
            fidMap = new TreeMap<String, Index.Entry>();
            staged.put(parents, fidMap);
        }
        return fidMap;
    }

    /**
     * @param objectInserter
     * @return non-null tuple, but possibly with null elements
     * @throws Exception
     */
    public Tuple<ObjectId, BoundingBox> writeTree(final ObjectInserter objectInserter)
            throws Exception {
        if (staged.size() == 0) {
            return new Tuple<ObjectId, BoundingBox>(null, null);
        }

        final MutableTree root;
        {
            RevTree currRoot = repository.getRootTree();
            root = currRoot == null ? repository.newTree() : currRoot.mutable();
        }

        BoundingBox bounds = null;

        Map<List<String>, MutableTree> updates = new TreeMap<List<String>, MutableTree>(
                PATH_COMPARATOR);
        final Set<List<String>> typeNames = new TreeSet<List<String>>(PATH_COMPARATOR);
        typeNames.addAll(staged.keySet());
        for (List<String> typeName : typeNames) {
            // TODO: make all this really n depth
            final String nsUri = typeName.get(0);
            final String localTypeName = typeName.get(1);

            MutableTree typeNameTree = updates.get(typeName);
            if (typeNameTree == null) {
                typeNameTree = findOrCreateTypeNameTree(root, objectInserter, nsUri, localTypeName);
                updates.put(typeName, typeNameTree);
            }

            // update the tree with the leaf entries
            TreeMap<String, Entry> fidMap = staged.remove(typeName);
            TreeSet<String> fids = new TreeSet<String>(fidMap.keySet());
            Ref ref = null;
            for (String fid : fids) {
                Entry entry = fidMap.remove(fid);
                if (entry.status == Status.DELETED_STAGED) {
                    ref = typeNameTree.remove(fid);
                } else if (entry.status == Status.NEW_STAGED
                        || entry.status == Status.MODIFIED_STAGED) {
                    ref = new SpatialRef(fid, entry.blobId, TYPE.BLOB, entry.bounds);
                    typeNameTree.put(ref);
                }
                if (ref != null && ref instanceof SpatialRef) {
                    bounds = SpatialOps.expandToInclude(bounds, ((SpatialRef) ref).getBounds());
                }
            }
        }

        Map<String, MutableTree> nsTrees = new HashMap<String, MutableTree>();

        for (Map.Entry<List<String>, MutableTree> typeTreeEntry : updates.entrySet()) {

            final List<String> typePath = typeTreeEntry.getKey();
            final String nsUri = typePath.get(0);
            final String typeName = typePath.get(1);

            final RevTree typeNameTree = typeTreeEntry.getValue();

            final ObjectId newTypeTreeId = objectInserter.insert(new RevTreeWriter(typeNameTree));
            MutableTree nsTree = nsTrees.get(nsUri);
            if (nsTree == null) {
                ObjectId nsTreeId = repository.getTreeChildId(root, nsUri);
                if (nsTreeId == null) {
                    nsTree = repository.newTree();
                } else {
                    nsTree = repository.getTree(nsTreeId).mutable();
                }
                nsTrees.put(nsUri, nsTree);
            }

            nsTree.put(new Ref(typeName, newTypeTreeId, TYPE.TREE));
        }

        for (Map.Entry<String, MutableTree> nste : nsTrees.entrySet()) {
            String nsUri = nste.getKey();
            RevTree nsTree = nste.getValue();
            ObjectId nsTreeId = objectInserter.insert(new RevTreeWriter(nsTree));
            root.put(new Ref(nsUri, nsTreeId, TYPE.TREE));
        }

        final ObjectId newRootId = objectInserter.insert(new RevTreeWriter(root));

        return new Tuple<ObjectId, BoundingBox>(newRootId, bounds);
    }

    private MutableTree findOrCreateTypeNameTree(RevTree root, ObjectInserter objectInserter,
            String nsUri, String typeName) throws Exception {

        ObjectId typeNameTreeId = repository.getTreeChildId(root, nsUri, typeName);
        RevTree typeNameTree;
        if (typeNameTreeId == null) {
            typeNameTree = repository.newTree();
        } else {
            typeNameTree = repository.getTree(typeNameTreeId);
        }
        return typeNameTree.mutable();
    }

}
