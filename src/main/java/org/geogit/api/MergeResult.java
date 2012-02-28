/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.util.ArrayList;
import java.util.List;

/**
 * A list of object ID's which are the content ID's of features which have been changed in this merge
 * 
 * @author jhudson
 * @since 1.2.0
 */
public class MergeResult {

    List<ObjectId> merged;

    public MergeResult() {
        super();

        merged = new ArrayList<ObjectId>();
    }

    public List<ObjectId> getMerged() {
        return merged;
    }

    public void addMerged(ObjectId merged) {
        this.merged.add(merged);
    }
}