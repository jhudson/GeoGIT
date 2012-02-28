/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import java.util.Iterator;
import java.util.List;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevTree;
import org.geogit.storage.ObjectWriter;
import org.geogit.storage.StagingDatabase;
import org.opengis.geometry.BoundingBox;
import org.opengis.util.ProgressListener;

public interface StagingArea {

    public StagingDatabase getDatabase();

    /**
     * @see #created(List)
     */
    public abstract void created(final String... newTreePath) throws Exception;

    /**
     * Creates an empty unstaged tree at the given path
     * 
     * @param newTreePath
     * @throws Exception
     *             if an error happens writing the new tree
     * @throws IllegalArgumentException
     *             if a tree or blob already exists at the given path
     */
    public abstract void created(final List<String> newTreePath) throws Exception;

    /**
     * Marks the object (tree or feature) addressed by {@code path} as an unstaged delete.
     * 
     * @param path
     * @return
     * @throws Exception
     */
    public abstract boolean deleted(final String... path) throws Exception;

    /**
     * Inserts an object into de index database and marks it as unstaged.
     * 
     * @param blob
     *            the writer for the object to be inserted.
     * @param path
     *            the path from the repository root to the name of the object to be inserted.
     * @return the reference to the newly inserted object.
     * @throws Exception
     */
    public abstract Ref inserted(final ObjectWriter<?> blob, final BoundingBox bounds,
            final String... path) throws Exception;

    /**
     * Inserts the given objects into the index database and marks them as unstaged.
     * 
     * @param objects
     *            list of blobs to be batch inserted as unstaged, as [Object writer, bounds, path]
     * @return list of inserted blob references,or the empty list of the process was cancelled by
     *         the listener
     * @throws Exception
     */
    public abstract List<Ref> inserted(
            final Iterator<Triplet<ObjectWriter<?>, BoundingBox, List<String>>> objects,
            final ProgressListener progress, final Integer size) throws Exception;

    /**
     * Stages the object addressed by path to be added, if it's marked as an unstaged change. Does
     * nothing otherwise.
     * <p>
     * To stage changes not yet staged, a diff tree walk is performed using the current staged
     * {@link RevTree} as the old object and the current unstaged {@link RevTree} as the new object.
     * Then all the differences are traversed and the staged tree is updated with the changes
     * reported by the diff walk (neat).
     * </p>
     * 
     * @param path
     * @param progressListener
     * @throws Exception
     */
    public abstract void stage(ProgressListener progress, final String... path) throws Exception;

    /**
     * Marks an object rename (in practice, it's used to change the feature id of a Feature once it
     * was committed and the DataStore generated FID is obtained)
     * 
     * @param from
     *            old path to featureId
     * @param to
     *            new path to featureId
     */
    public abstract void renamed(final List<String> from, final List<String> to);

    /**
     * Discards any staged change.
     * 
     * @REVISIT: should this be implemented through ResetOp (GeoGIT.reset()) instead?
     * @TODO: When we implement transaction management will be the time to discard any needed object
     *        inserted to the database too
     */
    public abstract void reset();

    public Tuple<ObjectId, BoundingBox> writeTree(final Ref targetRef) throws Exception;

    /**
     * Updates the repository target HEAD tree given by {@code targetRootRef} with the staged
     * changes in this index.
     * 
     * @param targetRef
     *            reference to either a commit or a tree that's the root of the head to be updated
     * @param objectInserter
     * @return non-null tuple, but possibly with null elements, containing the id of the new top
     *         level tree created on the repository after applying the staged changes, and the
     *         aggregated bounds of the changes, if any.
     * @throws Exception
     */
    public Tuple<ObjectId, BoundingBox> writeTree(final Ref targetRef,
            final ProgressListener progress) throws Exception;

}