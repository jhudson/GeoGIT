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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevTree;
import org.geogit.api.TreeVisitor;
import org.springframework.util.Assert;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterators;

public class RevSHA1Tree extends RevTree {

    /**
     * How many children to hold before splitting myself into subtrees
     */
    public static final int SPLIT_FACTOR = 64 * 1024;

    private static final int NORMALIZED_SIZE_LIMIT = 4 * 1024;

    private final int depth;

    private final ObjectDatabase db;

    private MessageDigest md;

    // aggregated number of leaf nodes (data entries)
    private BigInteger size;

    /**
     * If split == true, holds references to other trees, if split == false, holds references to
     * data elements
     */
    private final TreeMap<String, Ref> myEntries;

    private final TreeMap<Integer, Ref> mySubTrees;

    public RevSHA1Tree(final ObjectDatabase db) {
        this(null, db, 0);
    }

    RevSHA1Tree(final ObjectDatabase db, final int order) {
        this(null, db, order);
    }

    public RevSHA1Tree(final ObjectId id, final ObjectDatabase db, final int order) {
        super(id);
        this.db = db;
        this.depth = order;
        this.myEntries = new TreeMap<String, Ref>();
        this.mySubTrees = new TreeMap<Integer, Ref>();
    }

    /**
     * @return the number of elements in the tree, forces {@link #normalize()} if the tree has been
     *         modified since retrieved from the db
     */
    @SuppressWarnings("unchecked")
    @Override
    public BigInteger size() {
        if (size == null) {
            if (!isNormalized()) {
                normalize();
            } else {
                try {
                    this.size = computeSize(BigInteger.ZERO, Collections.EMPTY_SET);
                } catch (IOException e) {
                    Throwables.propagate(e);
                }
            }
        }
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
                                .get(subtreeId, new RevTreeReader(db, childDepth));
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

    /**
     * Adds or replaces an element in the tree with the given key.
     * <p>
     * <!-- Implementation detail: If the number of cached entries (entries held directly by this
     * tree) reaches {@link #SPLIT_FACTOR}, this tree will {@link #normalize()} itself.
     * 
     * -->
     * 
     * @param key
     *            non null
     * @param value
     *            non null
     */
    @Override
    public void put(final Ref ref) {
        Assert.notNull(ref, "ref can't be null");

        Ref oldCachedValue = myEntries.put(ref.getName(), ref);
        if (myEntries.size() >= SPLIT_FACTOR) {
            // hit the split factor modification tolerance, lets normalize
            normalize();
        } else {
            // still can handle more modifications before aut-splitting
            if (mySubTrees.isEmpty()) {
                // I'm not yet split into subtrees, can handle size safely
                if (this.size == null) {
                    this.size = BigInteger.ONE;
                } else if (oldCachedValue == null) {
                    // only increment size if it wasn't a replacement operation
                    this.size = this.size.add(BigInteger.ONE);
                }
            } else {
                // I'm not sure what my size is anymore, lets handle it lazily
                this.size = null;
            }
        }
    }

    @Override
    public Ref remove(final String key) {
        Assert.notNull(key, "key can't be null");
        final Integer bucket = computeBucket(key);
        if (null == mySubTrees.get(bucket)) {
            // we don't even have a subtree for this key's bucket, it's sure this tree doesn't
            // already hold a value for it
            Ref removed = myEntries.remove(key);
            if (removed != null) {
                this.size = this.size.subtract(BigInteger.ONE);
            }
            return removed;
        } else {
            Ref ref = this.get(key);
            // there's a subtree this key's bucket, we don't know if the subtree contains it at all
            // and it'd be too expensive to find out just now, use null value signaling the removal
            // of the entry. normalize() is gonna take care of removing it from the subtree
            // subsequently
            myEntries.put(key, null);
            size = null; // I'm not sure what my size is anymore
            if (myEntries.size() >= SPLIT_FACTOR) {
                normalize();
            }
            return ref;
        }
    }

    private Integer computeBucket(final String key) {
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
                    subTree = db.getCached(subtreeId, new RevTreeReader(db, this.depth + 1));
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

    /**
     * Splits the cached entries into subtrees and saves them, making sure the tree contains either
     * only entries or subtrees
     */
    @Override
    public void normalize() {
        if (isNormalized()) {
            return;
        }
        // System.err.println("spliting tree with order " + this.depth + " having "
        // + this.myEntries.size() + " entries....");
        if (myEntries.size() <= NORMALIZED_SIZE_LIMIT && mySubTrees.size() == 0) {
            size = BigInteger.valueOf(myEntries.size());
            return;
        }
        final int childOrder = this.depth + 1;
        try {
            // sort entries by the bucket they fall on
            Map<Integer, Set<String>> entriesByBucket = new TreeMap<Integer, Set<String>>();
            for (Object key : myEntries.keySet()) {
                Integer bucket = computeBucket((String) key);
                if (!entriesByBucket.containsKey(bucket)) {
                    entriesByBucket.put(bucket, new HashSet<String>());
                }
                entriesByBucket.get(bucket).add((String) key);
            }

            BigInteger size = BigInteger.ZERO;

            // ignore this subtrees for computing the size later as by that time their size has been
            // already added
            Set<Ref> ignoreForSizeComputation = new HashSet<Ref>();

            // for each bucket retrieve/create the bucket's subtree and set its entries
            Iterator<Map.Entry<Integer, Set<String>>> it = entriesByBucket.entrySet().iterator();

            Ref subtreeRef;
            ObjectId subtreeId;
            RevTree subtree;

            while (it.hasNext()) {
                Entry<Integer, Set<String>> e = it.next();
                Integer bucket = e.getKey();
                Set<String> keys = e.getValue();
                it.remove();
                subtreeRef = mySubTrees.get(bucket);
                if (subtreeRef == null) {
                    subtree = new RevSHA1Tree(db, childOrder);
                } else {
                    subtreeId = subtreeRef.getObjectId();
                    subtree = db.get(subtreeId, new RevTreeReader(db, childOrder));
                }
                for (String key : keys) {
                    Ref value = myEntries.remove(key);
                    if (value == null) {
                        subtree.remove(key);
                    } else {
                        subtree.put(value);
                    }
                }
                size = size.add(subtree.size());
                subtreeId = this.db.put(new RevTreeWriter(subtree));
                subtreeRef = new Ref("", subtreeId, TYPE.TREE);
                ignoreForSizeComputation.add(subtreeRef);
                mySubTrees.put(bucket, subtreeRef);
            }

            // compute the overall size
            this.size = computeSize(size, ignoreForSizeComputation);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // System.err.println("spliting complete.");
    }

    private BigInteger computeSize(final BigInteger initialSize,
            final Set<Ref> ignoreForSizeComputation) throws IOException {
        final int childOrder = this.depth + 1;
        ObjectId subtreeId;
        BigInteger size = initialSize;
        for (Ref ref : mySubTrees.values()) {
            if (ignoreForSizeComputation.contains(ref)) {
                continue;
            }
            subtreeId = ref.getObjectId();
            size = size.add(db.getCached(subtreeId, new RevTreeReader(db, childOrder)).size());
        }
        return size;
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
        Assert.isTrue(isNormalized(),
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
                    subtree = db.get(objectId, new RevTreeReader(db, depth));
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
