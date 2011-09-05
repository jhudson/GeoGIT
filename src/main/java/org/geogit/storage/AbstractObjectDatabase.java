/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.LRUMap;
import org.geogit.api.MutableTree;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevBlob;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.repository.DepthSearch;
import org.geogit.storage.bxml.BxmlCommitReader;
import org.geogit.storage.bxml.BxmlRevTreeReader;
import org.geogit.storage.bxml.BxmlRevTreeWriter;

import com.google.common.base.Preconditions;
import com.ning.compress.lzf.LZFInputStream;
import com.ning.compress.lzf.LZFOutputStream;

public abstract class AbstractObjectDatabase implements ObjectDatabase {

    protected Map<ObjectId, Object> cache;

    @SuppressWarnings("unchecked")
    public AbstractObjectDatabase() {
        // TODO: use an external cache
        final long maxMemMegs = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        final int maxSize;
        if (maxMemMegs <= 64) {
            maxSize = 16;
        } else if (maxMemMegs <= 100) {
            maxSize = 64;
        } else if (maxMemMegs <= 200) {
            maxSize = 128;
        } else {
            maxSize = 256;
        }
        cache = new LRUMap(maxSize);
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#lookUp(java.lang.String)
     */
    @Override
    public final List<ObjectId> lookUp(final String partialId) {
        Preconditions.checkNotNull(partialId);

        byte[] raw = ObjectId.toRaw(partialId);

        return lookUpInternal(raw);
    }

    protected abstract List<ObjectId> lookUpInternal(byte[] raw);

    /**
     * @see org.geogit.storage.ObjectDatabase#get(org.geogit.api.ObjectId,
     *      org.geogit.storage.ObjectReader)
     */
    @Override
    public <T> T get(final ObjectId id, final ObjectReader<T> reader) throws IOException {
        Preconditions.checkNotNull(id, "id");
        Preconditions.checkNotNull(reader, "reader");

        T object;
        InputStream raw = getRaw(id);
        try {
            object = reader.read(id, raw);
        } finally {
            raw.close();
        }
        return object;
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#getCached(org.geogit.api.ObjectId,
     *      org.geogit.storage.ObjectReader)
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCached(final ObjectId id, final ObjectReader<T> reader) throws IOException {
        Preconditions.checkNotNull(id, "id");
        Preconditions.checkNotNull(reader, "reader");

        T object = (T) cache.get(id);
        if (object == null) {
            object = get(id, reader);
            if (object != null) {
                assert !(object instanceof MutableTree);
                cache.put(id, object);
            }
        }
        return object;
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#getRaw(org.geogit.api.ObjectId)
     */
    @Override
    public final InputStream getRaw(final ObjectId id) throws IOException {
        InputStream in = getRawInternal(id);
        return new LZFInputStream(in);
    }

    protected abstract InputStream getRawInternal(ObjectId id) throws IOException;

    /**
     * @see org.geogit.storage.ObjectDatabase#put(org.geogit.storage.ObjectWriter)
     */
    @Override
    public final <T> ObjectId put(final ObjectWriter<T> writer) throws Exception {
        MessageDigest sha1;
        sha1 = MessageDigest.getInstance("SHA1");

        ByteArrayOutputStream rawOut = new ByteArrayOutputStream();

        DigestOutputStream keyGenOut = new DigestOutputStream(rawOut, sha1);
        // GZIPOutputStream cOut = new GZIPOutputStream(keyGenOut);
        LZFOutputStream cOut = new LZFOutputStream(keyGenOut);

        try {
            writer.write(cOut);
        } finally {
            // cOut.finish();
            cOut.flush();
            cOut.close();
            keyGenOut.flush();
            keyGenOut.close();
            rawOut.flush();
            rawOut.close();
        }

        final byte[] rawData = rawOut.toByteArray();
        final byte[] rawKey = keyGenOut.getMessageDigest().digest();
        final ObjectId id = new ObjectId(rawKey);
        putInternal(id, rawData, false);
        return id;
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#put(org.geogit.api.ObjectId,
     *      org.geogit.storage.ObjectWriter)
     */
    @Override
    public final boolean put(final ObjectId id, final ObjectWriter<?> writer) throws Exception {
        ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        // GZIPOutputStream cOut = new GZIPOutputStream(rawOut);
        LZFOutputStream cOut = new LZFOutputStream(rawOut);
        try {
            // writer.write(cOut);
            writer.write(cOut);
        } finally {
            // cOut.finish();
            cOut.flush();
            cOut.close();
            rawOut.flush();
            rawOut.close();
        }
        final byte[] rawData = rawOut.toByteArray();
        return putInternal(id, rawData, true);
    }

    /**
     * @param id
     * @param rawData
     * @param override
     *            if {@code true} an a record with the given id already exists, it shall be
     *            overriden. If {@code false} and a record with the given id already exists, it
     *            shall not be overriden.
     * @return
     * @throws IOException
     */
    protected abstract boolean putInternal(ObjectId id, byte[] rawData, final boolean override)
            throws IOException;

    /**
     * @see org.geogit.storage.ObjectDatabase#newObjectInserter()
     */
    @Override
    public ObjectInserter newObjectInserter() {
        return new ObjectInserter(this);
    }

    @Override
    public RevBlob getBlob(ObjectId objectId) {
        try {
            return get(objectId, new BlobReader());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#getCommit(org.geogit.api.ObjectId)
     */
    @Override
    public RevCommit getCommit(final ObjectId commitId) {
        RevCommit commit;
        try {
            commit = this.getCached(commitId, new BxmlCommitReader());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return commit;
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#newTree()
     */
    @Override
    public MutableTree newTree() {
        return new RevSHA1Tree(this).mutable();
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#getTree(org.geogit.api.ObjectId)
     */
    @Override
    public RevTree getTree(final ObjectId treeId) {
        if (treeId.isNull()) {
            return newTree();
        }
        RevTree tree;
        try {
            tree = this.getCached(treeId, new BxmlRevTreeReader(this));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return tree;
    }

    /**
     * If a child tree of {@code parent} addressed by the given {@code childPath} exists, returns
     * it's mutable copy, otherwise just returns a new mutable tree without any modification to
     * root.
     * 
     * @throws IllegalArgumentException
     *             if an reference exists for {@code childPath} but is not of type {@code TREE}
     */
    @Override
    public MutableTree getOrCreateSubTree(final RevTree parent, List<String> childPath) {
        Ref treeChildRef = getTreeChild(parent, childPath);
        if (treeChildRef == null) {
            return newTree();
        }
        if (!TYPE.TREE.equals(treeChildRef.getType())) {
            throw new IllegalArgumentException("Object exsits as child of tree " + parent.getId()
                    + " but is not a tree: " + treeChildRef);
        }
        return getTree(treeChildRef.getObjectId()).mutable();
    }

    /**
     * @param root
     * @param tree
     * @param pathToTree
     * @return the id of the saved state of the modified root
     * @throws Exception
     */
    @Override
    public ObjectId writeBack(MutableTree root, final RevTree tree, final List<String> pathToTree)
            throws Exception {

        final ObjectId treeId = put(new BxmlRevTreeWriter(tree));
        final String treeName = pathToTree.get(pathToTree.size() - 1);

        if (pathToTree.size() == 1) {
            root.put(new Ref(treeName, treeId, TYPE.TREE));
            ObjectId newRootId = put(new BxmlRevTreeWriter(root));
            return newRootId;
        }
        final List<String> parentPath = pathToTree.subList(0, pathToTree.size() - 1);
        Ref parentRef = getTreeChild(root, parentPath);
        MutableTree parent;
        if (parentRef == null) {
            parent = newTree();
        } else {
            ObjectId parentId = parentRef.getObjectId();
            parent = getTree(parentId).mutable();
        }
        parent.put(new Ref(treeName, treeId, TYPE.TREE));
        return writeBack(root, parent, parentPath);
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#getTreeChild(org.geogit.api.RevTree,
     *      java.lang.String[])
     */
    @Override
    public Ref getTreeChild(RevTree root, String... path) {
        return getTreeChild(root, Arrays.asList(path));
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#getTreeChild(org.geogit.api.RevTree, java.util.List)
     */
    @Override
    public Ref getTreeChild(RevTree root, List<String> path) {
        Ref treeRef = new DepthSearch(this).find(root, path);
        return treeRef;
    }
}
