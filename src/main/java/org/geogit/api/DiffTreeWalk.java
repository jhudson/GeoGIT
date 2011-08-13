/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.geogit.api.DiffEntry.ChangeType;
import org.geogit.api.RevObject.TYPE;
import org.geogit.repository.DepthSearch;
import org.geogit.repository.Tuple;
import org.geogit.storage.ObjectDatabase;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;

public class DiffTreeWalk {

    private final ObjectDatabase objectDb;

    private final ObjectId fromCommit;

    private final ObjectId toCommit;

    private List<String> basePath;

    private Ref oldObject;

    private Ref newObject;

    private ObjectId idFilter;

    public DiffTreeWalk(final ObjectDatabase db, final ObjectId fromCommit, final ObjectId toCommit) {
        Preconditions.checkNotNull(db);
        Preconditions.checkNotNull(fromCommit);
        Preconditions.checkNotNull(toCommit);

        this.objectDb = db;
        this.fromCommit = fromCommit;
        this.toCommit = toCommit;
        this.basePath = Collections.emptyList();
    }

    public DiffTreeWalk(final ObjectDatabase db, final Ref oldObject, final Ref newObject) {
        Preconditions.checkNotNull(db);
        Preconditions.checkNotNull(oldObject);
        Preconditions.checkNotNull(newObject);

        this.objectDb = db;
        this.fromCommit = null;
        this.toCommit = null;
        this.oldObject = oldObject;
        this.newObject = newObject;
        this.basePath = Collections.emptyList();
    }

    public void setFilter(final String... path) {
        if (path == null || path.length == 0) {
            this.basePath = Collections.emptyList();
        } else {
            this.basePath = Arrays.asList(path);
        }
    }

    public void setFilter(ObjectId idFilter) {
        this.idFilter = idFilter;
    }

    public Iterator<DiffEntry> get() {
        if (fromCommit != null) {
            if (fromCommit.equals(toCommit)) {
                return Iterators.emptyIterator();
            }

            this.oldObject = getFilteredObject(fromCommit);
            this.newObject = getFilteredObject(toCommit);

            if (this.idFilter != null && (oldObject == null && newObject != null)
                    || (oldObject != null && newObject == null)) {
                // if uding idFilter then the id might have been found in a single tree, but might
                // be present in the other tree with a different id (accounting for a modification)
                if (this.basePath != null && this.basePath.size() > 0) {
                    // only if the id was found in any of the two trees, re-search based on path
                    this.oldObject = getFilteredObject(fromCommit);
                    this.newObject = getFilteredObject(toCommit);
                }
            }

            if (oldObject == null && newObject == null) {
                // filter didn't match anything
                return Iterators.emptyIterator();
            }
        }

        // easy, filter addressed a single blob
        if ((oldObject != null && oldObject.getType() == TYPE.BLOB)
                || (newObject != null && newObject.getType() == TYPE.BLOB)) {

            // but addressed object didn't change
            if (oldObject != null && newObject != null && oldObject.equals(newObject)) {
                return Iterators.emptyIterator();
            }
            // ok, found change between new and old version of the filter addressed object
            DiffEntry entry = DiffEntry.newInstance(fromCommit, toCommit, oldObject, newObject,
                    basePath);
            return Iterators.singletonIterator(entry);
        }

        // filter addressed a tree...
        final ObjectId oldTreeId = oldObject == null ? ObjectId.NULL : oldObject.getObjectId();
        final ObjectId newTreeId = newObject == null ? ObjectId.NULL : newObject.getObjectId();
        final RevTree oldTree = objectDb.getTree(oldTreeId);
        final RevTree newTree = objectDb.getTree(newTreeId);
        Preconditions.checkState(oldTree.isNormalized());
        Preconditions.checkState(newTree.isNormalized());

        return new TreeDiffEntryIterator(basePath, fromCommit, toCommit, oldTree, newTree, objectDb);

    }

    /**
     * @param commitId
     */
    private Ref getFilteredObject(final ObjectId commitId) {
        if (commitId.isNull()) {
            return new Ref("", ObjectId.NULL, TYPE.TREE);
        }
        final RevCommit commit = objectDb.getCommit(commitId);
        final ObjectId treeId = commit.getTreeId();

        Ref ref;

        final DepthSearch search = new DepthSearch(objectDb);
        if (!basePath.isEmpty()) {
            ref = search.find(treeId, basePath);
        } else if (idFilter != null) {
            RevTree tree = objectDb.getTree(treeId);
            Tuple<List<String>, Ref> found = search.find(tree, idFilter);
            if (found != null) {
                this.basePath = found.getFirst();
                ref = found.getMiddle();
            } else {
                ref = null;
            }
        } else {
            ref = new Ref("", treeId, TYPE.TREE);
        }
        return ref;
    }

    /**
     * @author groldan
     * 
     */
    private static abstract class AbstractDiffIterator extends AbstractIterator<DiffEntry> {

        protected final List<String> basePath;

        public AbstractDiffIterator(final List<String> basePath) {
            this.basePath = Collections.unmodifiableList(basePath);
        }

        protected List<String> childPath(final String name) {
            List<String> path = new ArrayList<String>(this.basePath.size() + 1);
            path.addAll(basePath);
            path.add(name);
            return path;
        }

    }

    /**
     * 
     * @author groldan
     */
    private static class AddRemoveAllTreeIterator extends AbstractDiffIterator {

        private final ObjectId oldCommit;

        private final ObjectId newCommit;

        private Iterator<?> treeIterator;

        private final DiffEntry.ChangeType changeType;

        private final ObjectDatabase objectDb;

        public AddRemoveAllTreeIterator(final DiffEntry.ChangeType changeType,
                final List<String> basePath, final ObjectId fromCommit, final ObjectId toCommit,
                final RevTree tree, final ObjectDatabase db) {
            this(changeType, basePath, fromCommit, toCommit, tree.iterator(null), db);
        }

        public AddRemoveAllTreeIterator(final DiffEntry.ChangeType changeType,
                final List<String> basePath, final ObjectId fromCommit, final ObjectId toCommit,
                final Iterator<Ref> treeIterator, final ObjectDatabase db) {
            super(basePath);
            this.oldCommit = fromCommit;
            this.newCommit = toCommit;
            this.treeIterator = treeIterator;
            this.changeType = changeType;
            this.objectDb = db;
        }

        @Override
        protected DiffEntry computeNext() {
            if (!treeIterator.hasNext()) {
                return endOfData();
            }

            final Object nextObj = treeIterator.next();
            if (nextObj instanceof DiffEntry) {
                return (DiffEntry) nextObj;
            }

            Preconditions.checkState(nextObj instanceof Ref);

            final Ref next = (Ref) nextObj;
            final List<String> childPath = childPath(next.getName());

            if (TYPE.TREE.equals(next.getType())) {
                RevTree tree = objectDb.getTree(next.getObjectId());
                Predicate<Ref> filter = null;// TODO: propagate filter?
                Iterator<?> childTreeIterator;
                childTreeIterator = new AddRemoveAllTreeIterator(this.changeType, childPath,
                        oldCommit, newCommit, tree, objectDb);
                this.treeIterator = Iterators.concat(childTreeIterator, this.treeIterator);
                return computeNext();
            }

            Preconditions.checkState(TYPE.BLOB.equals(next.getType()));

            Ref oldObject = null;
            Ref newObject = null;
            String name = next.getName();
            if (changeType == ChangeType.ADD) {
                newObject = next;
            } else {
                oldObject = next;
            }
            DiffEntry diffEntry;
            diffEntry = DiffEntry
                    .newInstance(oldCommit, newCommit, oldObject, newObject, childPath);
            return diffEntry;
        }

    }

    /**
     * Traverses the direct children iterators of both trees (fromTree and toTree) simultaneously.
     * If the current children is named the same for both iterators, finds out whether the two
     * children are changed. If the two elements of the current iteration are not the same, find out
     * whether it's an addition or a deletion.
     * 
     * @author groldan
     * 
     */
    private static class TreeDiffEntryIterator extends AbstractDiffIterator {

        private final ObjectId oldCommit;

        private final ObjectId newCommit;

        private final RevTree oldTree;

        private final RevTree newTree;

        private Iterator<DiffEntry> currSubTree;

        private RewindableIterator<Ref> oldEntries;

        private RewindableIterator<Ref> newEntries;

        private final ObjectDatabase objectDb;

        public TreeDiffEntryIterator(final List<String> basePath, final ObjectId fromCommit,
                final ObjectId toCommit, final RevTree fromTree, final RevTree toTree,
                final ObjectDatabase db) {
            super(basePath);
            this.oldCommit = fromCommit;
            this.newCommit = toCommit;
            this.oldTree = fromTree;
            this.newTree = toTree;
            this.objectDb = db;
            this.oldEntries = new RewindableIterator<Ref>(oldTree.iterator(null));
            this.newEntries = new RewindableIterator<Ref>(newTree.iterator(null));
        }

        @Override
        protected DiffEntry computeNext() {
            if (currSubTree != null && currSubTree.hasNext()) {
                return currSubTree.next();
            }
            if (!oldEntries.hasNext() && !newEntries.hasNext()) {
                return endOfData();
            }
            if (oldEntries.hasNext() && !newEntries.hasNext()) {
                currSubTree = new AddRemoveAllTreeIterator(ChangeType.DELETE, this.basePath,
                        this.oldCommit, this.newCommit, this.oldEntries, this.objectDb);
                return computeNext();
            }
            if (!oldEntries.hasNext() && newEntries.hasNext()) {
                currSubTree = new AddRemoveAllTreeIterator(ChangeType.ADD, basePath, oldCommit,
                        newCommit, newEntries, objectDb);
                return computeNext();
            }
            Preconditions.checkState(currSubTree == null || !currSubTree.hasNext());
            Preconditions.checkState(oldEntries.hasNext() && newEntries.hasNext());
            Ref nextOld = oldEntries.next();
            Ref nextNew = newEntries.next();

            while (nextOld.equals(nextNew)) {
                // no change, keep going, but avoid too much recursion
                if (oldEntries.hasNext() && newEntries.hasNext()) {
                    nextOld = oldEntries.next();
                    nextNew = newEntries.next();
                } else {
                    return computeNext();
                }
            }

            final String oldEntryName = nextOld.getName();
            final String newEntryName = nextNew.getName();

            final ChangeType changeType;
            final Ref oldRef, newRef;
            final RevObject.TYPE objectType;
            final List<String> childPath;

            if (oldEntryName.equals(newEntryName)) {
                // same child name, found a changed object
                childPath = childPath(oldEntryName);
                changeType = ChangeType.MODIFY;
                objectType = nextOld.getType();
                oldRef = nextOld;
                newRef = nextNew;

            } else {
                // not the same object (blob or tree), find out whether it's an addition or a
                // deletion. Uses the same ordering than RevTree's iteration order to perform the
                // comparison
                final int comparison = ObjectId.forString(oldEntryName).compareTo(
                        ObjectId.forString(newEntryName));
                Preconditions.checkState(comparison != 0,
                        "Comparison can't be 0 if reached this point!");

                if (comparison < 0) {
                    // something was deleted in oldVersion, return a delete diff from oldVersion and
                    // return the item to the "newVersion" iterator for the next round of
                    // pair-to-pair comparisons
                    newEntries.returnElement(nextNew);
                    changeType = ChangeType.DELETE;
                    childPath = childPath(oldEntryName);
                    objectType = nextOld.getType();
                    oldRef = nextOld;
                    newRef = null;
                } else {
                    // something was added in newVersion, return an "add diff" for newVersion and
                    // return the item to the "oldVersion" iterator for the next rounds of
                    // pair-to-pair comparisons
                    oldEntries.returnElement(nextOld);
                    changeType = ChangeType.ADD;
                    childPath = childPath(newEntryName);
                    objectType = nextNew.getType();
                    oldRef = null;
                    newRef = nextNew;
                }

            }

            if (RevObject.TYPE.BLOB.equals(objectType)) {
                DiffEntry singleChange = DiffEntry.newInstance(oldCommit, newCommit, oldRef,
                        newRef, childPath);
                return singleChange;
            }

            Preconditions.checkState(RevObject.TYPE.TREE.equals(objectType));

            Iterator<DiffEntry> changesIterator;

            switch (changeType) {
            case ADD:
            case DELETE: {
                ObjectId treeId = null == oldRef ? newRef.getObjectId() : oldRef.getObjectId();
                RevTree childTree = objectDb.getTree(treeId);
                changesIterator = new AddRemoveAllTreeIterator(changeType, childPath, oldCommit,
                        newCommit, childTree, objectDb);
                break;
            }
            case MODIFY: {
                Preconditions.checkState(RevObject.TYPE.TREE.equals(nextOld.getType()));
                Preconditions.checkState(RevObject.TYPE.TREE.equals(nextNew.getType()));
                RevTree oldChildTree = objectDb.getTree(oldRef.getObjectId());
                RevTree newChildTree = objectDb.getTree(newRef.getObjectId());
                changesIterator = new TreeDiffEntryIterator(childPath, oldCommit, newCommit,
                        oldChildTree, newChildTree, objectDb);
                break;
            }
            default:
                throw new IllegalStateException("Unrecognized change type: " + changeType);
            }
            if (this.currSubTree == null || !this.currSubTree.hasNext()) {
                this.currSubTree = changesIterator;
            } else {
                this.currSubTree = Iterators.concat(changesIterator, this.currSubTree);
            }
            return computeNext();
        }

    }

    private static class RewindableIterator<T> extends AbstractIterator<T> {

        private Iterator<T> subject;

        private LinkedList<T> returnQueue;

        public RewindableIterator(Iterator<T> subject) {
            this.subject = subject;
            this.returnQueue = new LinkedList<T>();
        }

        public void returnElement(T element) {
            this.returnQueue.offer(element);
        }

        @Override
        protected T computeNext() {
            T peak = returnQueue.poll();
            if (peak != null) {
                return peak;
            }
            if (!subject.hasNext()) {
                return endOfData();
            }
            return subject.next();
        }

    }

}
