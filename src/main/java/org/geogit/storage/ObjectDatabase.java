/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.geogit.api.MutableTree;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevBlob;
import org.geogit.api.RevCommit;
import org.geogit.api.RevTree;

public interface ObjectDatabase {

    public abstract void close();

    public abstract void create();

    public abstract boolean exists(final ObjectId id);

    /**
     * @param id
     * @return
     * @throws IOException
     * @throws IllegalArgumentException
     *             if an object with such id does not exist
     */
    public abstract InputStream getRaw(final ObjectId id) throws IOException;

    public List<ObjectId> lookUp(final String partialId);

    /**
     * @param <T>
     * @param id
     * @param reader
     * @return
     * @throws IOException
     * @throws IllegalArgumentException
     *             if an object with such id does not exist
     */
    public abstract <T> T get(final ObjectId id, final ObjectReader<T> reader) throws IOException;

    /**
     * Returns a possibly cached version of the object identified by the given {@code id}.
     * <p>
     * The returned object is meant to be immutable. If any modification is to be made the calling
     * code is in charge of cloning the returned object so that it doesn't affect the cached
     * version.
     * </p>
     * 
     * @param <T>
     *            the type of object returned
     * @param id
     *            the id of the object to return from the cache, or to look up in the database and
     *            cache afterwards.
     * @param reader
     *            the reader to use in the case of a cache miss.
     * @return the cached version of the required object.
     * @throws IOException
     */
    public abstract <T> T getCached(final ObjectId id, final ObjectReader<T> reader)
            throws IOException;

    /**
     * 
     */
    public abstract <T> ObjectId put(final ObjectWriter<T> writer) throws Exception;

    /**
     * @param id
     * @param writer
     * @return {@code true} if the object was inserted and it didn't exist previously, {@code false}
     *         if the object was inserted and it replaced an already existing object with the same
     *         key.
     * @throws Exception
     */
    public abstract boolean put(final ObjectId id, final ObjectWriter<?> writer) throws Exception;

    /**
     * @param root
     * @param tree
     * @param pathToTree
     * @return the id of the saved state of the modified root
     * @throws Exception
     */
    public ObjectId writeBack(MutableTree root, final RevTree tree, final List<String> pathToTree)
            throws Exception;

    public abstract ObjectInserter newObjectInserter();

    public RevBlob getBlob(ObjectId objectId);

    public RevCommit getCommit(final ObjectId commitId);

    public RevTree getTree(final ObjectId treeId);

    /**
     * If a child tree of {@code parent} addressed by the given {@code childPath} exists, returns
     * it's mutable copy, otherwise just returns a new mutable tree without any modification to
     * root.
     * 
     * @throws IllegalArgumentException
     *             if an reference exists for {@code childPath} but is not of type {@code TREE}
     */
    public MutableTree getOrCreateSubTree(final RevTree parent, List<String> childPath);

    /**
     * Creates and return a new, empty tree, that stores to this {@link ObjectDatabase}
     */
    public MutableTree newTree();

    public Ref getTreeChild(RevTree root, String... path);

    public Ref getTreeChild(RevTree root, List<String> path);

    public boolean delete(ObjectId objectId);

}