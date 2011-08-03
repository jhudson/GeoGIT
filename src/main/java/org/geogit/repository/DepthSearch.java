/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import java.util.List;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevTree;
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
}
