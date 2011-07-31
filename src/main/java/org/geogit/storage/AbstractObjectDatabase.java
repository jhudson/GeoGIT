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
import java.util.Map;

import org.apache.commons.collections.map.LRUMap;
import org.geogit.api.MutableTree;
import org.geogit.api.ObjectId;
import org.springframework.util.Assert;

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
     * @see org.geogit.storage.ObjectDatabase#get(org.geogit.api.ObjectId,
     *      org.geogit.storage.ObjectReader)
     */
    public <T> T get(final ObjectId id, final ObjectReader<T> reader) throws IOException {
        Assert.notNull(id, "id");
        Assert.notNull(reader, "reader");

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
        Assert.notNull(id, "id");
        Assert.notNull(reader, "reader");

        T object = (T) cache.get(id);
        if (object == null) {
            object = get(id, reader);
            if (object != null) {
                Assert.isTrue(!(object instanceof MutableTree));
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

}