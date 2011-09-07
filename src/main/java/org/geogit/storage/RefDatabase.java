/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.geogit.api.MutableTree;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;

/**
 * Database of repository {@link Ref references}
 * <p>
 * It uses the {@link ObjectDatabase} to store the references in a {@link RevTree} under the
 * {@code ".geogit/refs"} key.
 * </p>
 * 
 */
public class RefDatabase {

    private static final String REFS_TREE_KEY = ".geogit/refs";

    private static final ObjectId REFS_TREE_ID = ObjectId.forString(REFS_TREE_KEY);

    private ObjectDatabase db;

    public RefDatabase(final ObjectDatabase db) {
        this.db = db;
    }

    public void create() {
        final String headRefName = Ref.HEAD;
        condCreate(headRefName, TYPE.COMMIT);
        final String master = Ref.MASTER;
        condCreate(master, TYPE.COMMIT);
    }
    
    /**
     * Add a new tracked remote to the reference database
     * @param refName the name of the remote to track, no need to add the prefix, "remotes/".
     */
    public void addRef(final Ref ref){
        condCreate(ref.getName(), TYPE.REMOTE);
    }

    private void condCreate(final String refName, TYPE type) {
        RevTree refsTree = getRefsTree();

        Ref child = refsTree.get(refName);
        if (null == child) {
            put(new Ref(refName, ObjectId.NULL, type));
        }
    }

    private RevTree getRefsTree() {
        RevTree refsTree;
        try {
            if (db.exists(REFS_TREE_ID)) {
                refsTree = db.get(REFS_TREE_ID, new RevTreeReader(db));
            } else {
                refsTree = new RevSHA1Tree(db);
                db.put(REFS_TREE_ID, new RevTreeWriter(refsTree));
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return refsTree;
    }

    public void close() {
        //
    }

    public Ref getRef(final String name) {
        Preconditions.checkNotNull(name, "Ref name can't be null");
        RevTree refsTree = getRefsTree();
        Ref child = refsTree.get(name);
        if (child == null) {
            return null;
        }
        return child;
    }

    public List<Ref> getRefs(final String prefix) {
        Preconditions.checkNotNull(prefix, "Ref prefix can't be null");
        List<Ref> refs = new LinkedList<Ref>();
        RevTree refsTree = getRefsTree();

        Iterator<Ref> iterator = refsTree.iterator(new Predicate<Ref>() {
            public boolean apply(Ref input) {
                return input.getName().startsWith(prefix);
            }
        });

        Iterators.addAll(refs, iterator);
        return refs;
    }

    public List<Ref> getRefsPontingTo(final ObjectId oid) {
        Preconditions.checkNotNull(oid);
        List<Ref> refs = new LinkedList<Ref>();
        RevTree refsTree = getRefsTree();
        throw new UnsupportedOperationException(
                "waiting for tree walking implementation to reliable implement this method");
        // return refs;
    }

    /**
     * @param ref
     * @return {@code true} if the ref was inserted, {@code false} if it already existed and pointed
     *         to the same object
     */
    public boolean put(final Ref ref) {
        Preconditions.checkNotNull(ref);
        Preconditions.checkNotNull(ref.getName());
        Preconditions.checkNotNull(ref.getObjectId());

        RevTree refsTree = getRefsTree();
        Ref oldTarget = refsTree.get(ref.getName());
        if (oldTarget != null && oldTarget.equals(ref)) {
            return false;
        }
        refsTree = refsTree.mutable();
        ((MutableTree) refsTree).put(ref);
        try {
            db.put(REFS_TREE_ID, new RevTreeWriter(refsTree));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return true;
    }
}
