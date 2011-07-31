/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.geogit.api.MutableTree;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.springframework.util.Assert;

import com.google.common.base.Throwables;

class MutableRevSHA1Tree extends RevSHA1Tree implements MutableTree {

    private BigInteger mutableSize;

    /**
     * Copy constructor
     */
    public MutableRevSHA1Tree(final RevSHA1Tree copy) {
        super(copy.getId(), copy.db, copy.depth);
        this.mutableSize = copy.size();
        super.myEntries.putAll(copy.myEntries);
        super.mySubTrees.putAll(copy.mySubTrees);
    }

    MutableRevSHA1Tree(ObjectDatabase db, int childOrder) {
        super(db, childOrder);
    }

    @Override
    public MutableTree mutable() {
        return this;
    }

    /**
     * @return the number of elements in the tree, forces {@link #normalize()} if the tree has been
     *         modified since retrieved from the db
     */
    @SuppressWarnings("unchecked")
    @Override
    public BigInteger size() {
        if (mutableSize == null) {
            if (!isNormalized()) {
                normalize();
            } else {
                try {
                    this.mutableSize = computeSize(BigInteger.ZERO, Collections.EMPTY_SET);
                } catch (IOException e) {
                    Throwables.propagate(e);
                }
            }
        }
        return mutableSize;
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
                if (this.mutableSize == null) {
                    this.mutableSize = BigInteger.ONE;
                } else if (oldCachedValue == null) {
                    // only increment size if it wasn't a replacement operation
                    this.mutableSize = this.mutableSize.add(BigInteger.ONE);
                }
            } else {
                // I'm not sure what my size is anymore, lets handle it lazily
                this.mutableSize = null;
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
                this.mutableSize = size().subtract(BigInteger.ONE);
            }
            return removed;
        } else {
            Ref ref = this.get(key);
            // there's a subtree this key's bucket, we don't know if the subtree contains it at all
            // and it'd be too expensive to find out just now, use null value signaling the removal
            // of the entry. normalize() is gonna take care of removing it from the subtree
            // subsequently
            myEntries.put(key, null);
            mutableSize = null; // I'm not sure what my size is anymore
            if (myEntries.size() >= SPLIT_FACTOR) {
                normalize();
            }
            return ref;
        }
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
            mutableSize = BigInteger.valueOf(myEntries.size());
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
            MutableTree subtree;

            while (it.hasNext()) {
                Entry<Integer, Set<String>> e = it.next();
                Integer bucket = e.getKey();
                Set<String> keys = e.getValue();
                it.remove();
                subtreeRef = mySubTrees.get(bucket);
                if (subtreeRef == null) {
                    subtree = new MutableRevSHA1Tree(db, childOrder);
                } else {
                    subtreeId = subtreeRef.getObjectId();
                    subtree = db.get(subtreeId, new RevTreeReader(db, childOrder)).mutable();
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
            this.mutableSize = computeSize(size, ignoreForSizeComputation);
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
}
