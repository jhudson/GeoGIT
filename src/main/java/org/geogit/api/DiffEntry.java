/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.util.Collections;
import java.util.List;

import org.geogit.repository.SpatialOps;
import org.opengis.geometry.BoundingBox;

import com.google.common.base.Preconditions;

public class DiffEntry {

    public static enum ChangeType {
        /**
         * Add a new Feature
         */
        ADD,

        /**
         * Modify an existing Feature
         */
        MODIFY,

        /**
         * Delete an existing Feature
         */
        DELETE
    }

    private final Ref oldObject;

    private final Ref newObject;

    private final ChangeType type;

    /**
     * Path to object. Basically a three step path name made of
     * {@code [namespace, FeatureType name, Feature ID]}
     */
    private final List<String> path;

    private final ObjectId oldCommitId;

    private final ObjectId newCommitId;

    private final BoundingBox where;

    public DiffEntry(ChangeType type, ObjectId oldCommitId, ObjectId newCommitId, Ref oldObject,
            Ref newObject, BoundingBox where, List<String> path) {
        this.type = type;
        this.oldCommitId = oldCommitId;
        this.newCommitId = newCommitId;
        this.oldObject = oldObject;
        this.newObject = newObject;
        this.where = where;
        this.path = Collections.unmodifiableList(path);
    }

    /**
     * @return the id of the old version id of the object, or {@link ObjectId#NULL} if
     *         {@link #getType()} is {@code ADD}
     */
    public ObjectId getOldObjectId() {
        return oldObject == null ? ObjectId.NULL : oldObject.getObjectId();
    }

    /**
     * @return the old object, or {@code null} if {@link #getType()} is {@code ADD}
     */
    public Ref getOldObject() {
        return oldObject;
    }

    /**
     * @return the id of the new version id of the object, or {@link ObjectId#NULL} if
     *         {@link #getType()} is {@code DELETE}
     */
    public ObjectId getNewObjectId() {
        return newObject == null ? ObjectId.NULL : newObject.getObjectId();
    }

    /**
     * @return the id of the new version of the object, or {@code null} if {@link #getType()} is
     *         {@code DELETE}
     */
    public Ref getNewObject() {
        return newObject;
    }

    public ObjectId getOldCommitId() {
        return oldCommitId;
    }

    public ObjectId getNewCommitId() {
        return newCommitId;
    }

    /**
     * @return the type of change
     */
    public ChangeType getType() {
        return type;
    }

    /**
     * @return the affected geographic region of the change, may be {@code null}
     */
    public BoundingBox getWhere() {
        return where;
    }

    /**
     * @return Path to object. Basically a three step path name made of
     *         {@code [<namespace>, <FeatureType name>, <Feature ID>]}
     */
    public List<String> getPath() {
        return path;
    }

    public String toString() {
        return new StringBuilder(getType().toString()).append(' ').append(getPath()).toString();
    }

    public static DiffEntry newInstance(final ObjectId fromCommit, final ObjectId toCommit,
            final Ref oldObject, final Ref newObject, final List<String> path) {

        Preconditions.checkArgument(oldObject != null || newObject != null);

        if (oldObject != null && oldObject.equals(newObject)) {
            throw new IllegalArgumentException(
                    "Trying to create a DiffEntry for the same object id, means the object didn't change: "
                            + oldObject.toString());
        }

        BoundingBox bounds = SpatialOps.aggregatedBounds(oldObject, newObject);

        ChangeType type;

        if (oldObject == null || oldObject.getObjectId().isNull()) {
            type = ChangeType.ADD;
        } else if (newObject == null || newObject.getObjectId().isNull()) {
            type = ChangeType.DELETE;
        } else {
            type = ChangeType.MODIFY;
        }

        DiffEntry entry = new DiffEntry(type, fromCommit, toCommit, oldObject, newObject, bounds,
                path);
        return entry;
    }
}
