/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.api;

import org.geogit.api.RevObject.TYPE;
import org.opengis.geometry.BoundingBox;

public class SpatialRef extends Ref {

    private BoundingBox bounds;

    public SpatialRef(String name, ObjectId oid, TYPE type, BoundingBox bounds) {
        super(name, oid, type);
        this.bounds = bounds;
    }

    public BoundingBox getBounds() {
        return bounds;
    }
}
