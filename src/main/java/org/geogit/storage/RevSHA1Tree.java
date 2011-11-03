/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.geogit.api.AbstractRevObject;
import org.geogit.api.MutableTree;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevTree;
import org.geogit.api.TreeVisitor;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterators;

public class RevSHA1Tree extends AbstractRevObject implements RevTree {

    /**
     * How many children to hold before splitting myself into subtrees
     */
    public static final int SPLIT_FACTOR = 64 * 1024;

    protected static final int NORMALIZED_SIZE_LIMIT = 4 * 1024;

    protected final int depth;

    protected final ObjectDatabase db;

    protected MessageDigest md;

    // aggregated number of leaf nodes (data entries)
    private final BigInteger size;

    /**
     * If split == true, holds references to other trees, if split == false, holds references to
     * data elements
     */
    protected final TreeMap<String, Ref> myEntries;

    protected final TreeMap<Integer, Ref> mySubTrees;

    public RevSHA1Tree(final ObjectDatabase db) {
        this(null, db, 0);
    }

    RevSHA1Tree(final ObjectDatabase db, final int order) {
        this(null, db, order);
    }

    public RevSHA1Tree(final ObjectId id, final ObjectDatabase db, final int order) {
        this(id, db, order, new TreeMap<String, Ref>(), new TreeMap<Integer, Ref>(),
                BigInteger.ZERO);
    }

    public RevSHA1Tree(final ObjectId id, final ObjectDatabase db, final int order,
            TreeMap<String, Ref> references, TreeMap<Integer, Ref> subTrees, final BigInteger size) {
        super(id, TYPE.TREE);
        this.db = db;
        this.depth = order;
        this.myEntries = references;
        this.mySubTrees = subTrees;
        this.size = size;
    }

    @Override
    public MutableTree mutable() {
        return new MutableRevSHA1Tree(this);
    }

    /**
     * @return the number of elements in the tree
     */
    @Override
    public BigInteger size() {
        return size;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void accept(TreeVisitor visitor) {
        // first visit all the cached entries
        accept(visitor, Collections.EMPTY_MAP);

        // and then the ones in the stored subtrees
        if (mySubTrees.size() > 0) {
            final int childDepth = this.depth + 1;
            try {
                Integer bucket;
                Ref subtreeRef;
                ObjectId subtreeId;
                RevSHA1Tree subtree;
                for (Map.Entry<Integer, Ref> e : mySubTrees.entrySet()) {
                    bucket = e.getKey();
                    subtreeRef = e.getValue();
                    subtreeId = subtreeRef.getObjectId();
                    if (visitor.visitSubTree(bucket, subtreeId)) {
                        subtree = (RevSHA1Tree) db
                                .get(subtreeId, 
                                WrappedSerialisingFactory.getInstance().createRevTreeReader(db, childDepth));
                        subtree.accept(visitor, myEntries);
                    }
                }
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
    }

    private void accept(final TreeVisitor visitor, final Map<String, Ref> ignore) {
        if (myEntries.size() > 0) {
            for (Map.Entry<String, Ref> e : myEntries.entrySet()) {
                String key = e.getKey();
                if (ignore.containsKey(key)) {
                    continue;
                }
                Ref value = e.getValue();
                if (!visitor.visitEntry(value)) {
                    return;
                }
            }
        }
    }

    void put(Integer bucket, ObjectId subtreeId) {
        mySubTrees.put(bucket, new Ref("", subtreeId, TYPE.TREE));
    }

    protected final Integer computeBucket(final String key) {
        byte[] hashedKey = hashKey(key);
        // int ch1 = hashedKey[2 * this.order] & 0xFF;
        // int ch2 = hashedKey[2 * this.order + 1] & 0xFF;
        // int b = (ch1 << 8) + (ch2 << 0);
        // final Integer bucket = Integer.valueOf(b);
        final Integer bucket = Integer.valueOf(hashedKey[this.depth] & 0xFF);

        return bucket;
    }

    /**
     * Gets an entry by key, this is potentially slow.
     * 
     * @param key
     * @return
     * @see ObjectDatabase#getCached(ObjectId, org.geogit.storage.ObjectPersister)
     */
    @Override
    public Ref get(final String key) {
        Ref value = null;
        if (myEntries.containsKey(key)) {
            value = myEntries.get(key);
            if (value == null) {
                // key is marked as removed
                return null;
            }
        }
        if (value == null) {
            final Integer bucket = computeBucket(key);
            final Ref subTreeRef = mySubTrees.get(bucket);
            if (subTreeRef == null) {
                value = null;
            } else {
                RevTree subTree;
                try {
                    ObjectId subtreeId = subTreeRef.getObjectId();
                    subTree = db.getCached(subtreeId, 
                    		WrappedSerialisingFactory.getInstance().createRevTreeReader(db, this.depth + 1));
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
                value = subTree.get(key);
            }
        }
        return value;
    }

    private synchronized byte[] hashKey(final String key) {
        if (md == null) {
            try {
                md = MessageDigest.getInstance("SHA1");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            md.reset();
            return md.digest(key.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isNormalized() {
        boolean normalized = (myEntries.size() <= NORMALIZED_SIZE_LIMIT && mySubTrees.isEmpty())
                || (myEntries.isEmpty() && mySubTrees.isEmpty())
                || (myEntries.isEmpty() && !mySubTrees.isEmpty());
        return normalized;
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append("[size: ")
                .append(this.myEntries.size()).append(", order: ").append(this.depth)
                .append(", subtrees: ").append(this.mySubTrees.size()).append(']').toString();
    }

    /**
     * Returns an iterator over this tree children
     * 
     * @see org.geogit.api.RevTree#iterator(com.google.common.base.Predicate)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Iterator<Ref> iterator(Predicate<Ref> filter) {
        Preconditions
                .checkState(isNormalized(),
                        "iterator() should only be called on a normalized tree to account for element deletions");
        if (filter == null) {
            filter = Predicates.alwaysTrue();
        }

        if (myEntries.isEmpty() && mySubTrees.isEmpty()) {
            return Collections.EMPTY_SET.iterator();
        }
        if (!mySubTrees.isEmpty()) {
            Iterator<Ref>[] iterators = new Iterator[mySubTrees.size()];
            int i = 0;
            for (Ref subtreeRef : mySubTrees.values()) {
                iterators[i] = new LazySubtreeIterator(this.db, subtreeRef.getObjectId(),
                        this.depth + 1, filter);
                i++;
            }
            return Iterators.concat(iterators);
        }

        // we have only content entries, return them in our internal order
        Map<ObjectId, Ref> sorted = new TreeMap<ObjectId, Ref>();
        for (Ref ref : myEntries.values()) {
            if (filter.apply(ref)) {
                sorted.put(ObjectId.forString(ref.getName()), ref);
            }
        }
        return sorted.values().iterator();
    }

    private static class LazySubtreeIterator implements Iterator<Ref> {

        private final ObjectDatabase db;

        private final ObjectId objectId;

        private final int depth;

        private final Predicate<Ref> filter;

        private Iterator<Ref> subject;

        public LazySubtreeIterator(ObjectDatabase db, ObjectId objectId, int depth,
                Predicate<Ref> filter) {
            this.db = db;
            this.objectId = objectId;
            this.depth = depth;
            this.filter = filter;
        }

        public boolean hasNext() {
            if (subject == null) {
                RevTree subtree;
                try {
                    subtree = db.get(objectId, 
                    		WrappedSerialisingFactory.getInstance().createRevTreeReader(db, depth));
                    subject = subtree.iterator(filter);
                } catch (IOException e) {
                    Throwables.propagate(e);
                }
            }
            return subject.hasNext();
        }

        public Ref next() {
            return subject.next();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
