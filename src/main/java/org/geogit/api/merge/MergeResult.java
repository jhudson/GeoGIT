/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.merge;

import java.util.ArrayList;
import java.util.List;

import org.geogit.api.DiffEntry;

/**
 * A list of object ID's which are the content ID's of features which have been changed in this merge
 * 
 * @author jhudson
 * @since 1.2.0
 */
public class MergeResult {

    private List<DiffEntry> diffs;

    public MergeResult() {
        super();
        diffs = new ArrayList<DiffEntry>();
    }

    public List<DiffEntry> getDiffs() {
        return diffs;
    }

    public void addDiff(DiffEntry entry) {
        this.diffs.add(entry);
    }
}