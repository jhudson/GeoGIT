/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import java.util.LinkedList;
import java.util.List;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.TreeVisitor;
import org.geogit.storage.ObjectDatabase;

public class DepthSearch {

    private final ObjectDatabase objectDb;

    public DepthSearch(final ObjectDatabase db) {
        this.objectDb = db;
    }

    public Ref find(final ObjectId treeId, final List<String> path) {
        RevTree tree = objectDb.getTree(treeId);
        if (tree == null) {
            return null;
        }
        return find(tree, path);
    }

    public Ref find(final RevTree tree, final List<String> path) {
        if (path.size() == 1) {
            return tree.get(path.get(0));
        }
        final String childName = path.get(0);
        final Ref childTreeRef = tree.get(childName);
        if (childTreeRef == null) {
            return null;
        }
        final RevTree childTree = objectDb.getTree(childTreeRef.getObjectId());
        final List<String> subpath = path.subList(1, path.size());
        return find(childTree, subpath);
    }

    /**
     * @param tree
     * @param childId
     * @return the tuple of tree path/object reference if found, or {@code null} if not.
     */
    public Tuple<List<String>, Ref> find(final RevTree tree, final ObjectId childId) {
        final Ref[] target = new Ref[1];
        final List<String> path = new LinkedList<String>();

        class IdFindedVisitor implements TreeVisitor {

            @Override
            public boolean visitEntry(Ref ref) {
                if (childId.equals(ref.getObjectId())) {
                    target[0] = ref;
                    path.add(ref.getName());
                    return false;// end walk
                }
                if (TYPE.TREE.equals(ref.getType())) {
                    final int idx = path.size();
                    path.add(ref.getName());
                    objectDb.getTree(ref.getObjectId()).accept(this);
                    if (target[0] == null) {
                        path.remove(idx);
                    } else {
                        // found, stop walk
                        return false;
                    }
                }
                return true;// continue
            }

            @Override
            public boolean visitSubTree(int bucket, ObjectId treeId) {
                return true;
            }
        }

        tree.accept(new IdFindedVisitor());
        if (target[0] == null) {
            return null;
        }
        return new Tuple<List<String>, Ref>(path, target[0]);
    }
}
