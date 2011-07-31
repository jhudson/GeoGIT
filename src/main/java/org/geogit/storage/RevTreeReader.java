/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import static org.geogit.storage.BLOBS.BUCKET;
import static org.geogit.storage.BLOBS.REF;
import static org.geogit.storage.BLOBS.TREE;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.TreeMap;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.gvsig.bxml.stream.BxmlInputFactory;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;

public class RevTreeReader implements ObjectReader<RevTree> {

    private ObjectDatabase objectDb;

    private int order;

    private final RefReader refReader;

    public RevTreeReader(ObjectDatabase objectDb) {
        this(objectDb, 0);
    }

    public RevTreeReader(ObjectDatabase objectDb, int order) {
        this.objectDb = objectDb;
        this.order = order;
        this.refReader = new RefReader();
    }

    /**
     * @throws IOException
     * @see org.geogit.storage.ObjectReader#read(org.geogit.api.ObjectId, java.io.InputStream)
     */
    public RevSHA1Tree read(final ObjectId id, final InputStream rawData) throws IOException {
        final BxmlInputFactory inputFactory = BLOBS.cachedInputFactory;
        final BxmlStreamReader r = inputFactory.createScanner(rawData);
        r.nextTag();
        try {
            r.require(EventType.START_ELEMENT, TREE.getNamespaceURI(), TREE.getLocalPart());
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        BigInteger size = BigInteger.valueOf(Long.valueOf(r.getAttributeValue(null, "size")));

        TreeMap<String, Ref> references = new TreeMap<String, Ref>();
        TreeMap<Integer, Ref> subtrees = new TreeMap<Integer, Ref>();

        EventType event;
        while ((event = r.next()) != EventType.END_DOCUMENT) {
            if (EventType.START_ELEMENT.equals(event)) {
                if (REF.equals(r.getElementName())) {
                    Ref entryRef = parseEntry(r);
                    references.put(entryRef.getName(), entryRef);
                } else if (TREE.equals(r.getElementName())) {
                    parseAndSetSubTree(r, subtrees);
                }
            }
        }
        RevSHA1Tree tree = new RevSHA1Tree(id, objectDb, order, references, subtrees, size);
        return tree;
    }

    private void parseAndSetSubTree(BxmlStreamReader r, TreeMap<Integer, Ref> subtrees)
            throws IOException {
        int bucket;
        ObjectId subtreeId;

        r.require(EventType.START_ELEMENT, TREE.getNamespaceURI(), TREE.getLocalPart());
        r.nextTag();
        r.require(EventType.START_ELEMENT, BUCKET.getNamespaceURI(), BUCKET.getLocalPart());
        r.next();
        r.require(EventType.VALUE_INT, null, null);
        bucket = r.getIntValue();
        r.nextTag();
        r.require(EventType.END_ELEMENT, BUCKET.getNamespaceURI(), BUCKET.getLocalPart());

        r.nextTag();
        subtreeId = BLOBS.parseObjectId(r);
        r.nextTag();
        r.require(EventType.END_ELEMENT, TREE.getNamespaceURI(), TREE.getLocalPart());

        subtrees.put(Integer.valueOf(bucket), new Ref("", subtreeId, TYPE.TREE));
    }

    private Ref parseEntry(BxmlStreamReader r) throws IOException {
        return refReader.read(r);
    }
}
