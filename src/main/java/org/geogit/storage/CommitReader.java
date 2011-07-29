/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import static org.geogit.storage.BLOBS.AUTHOR;
import static org.geogit.storage.BLOBS.COMMIT;
import static org.geogit.storage.BLOBS.COMMITTER;
import static org.geogit.storage.BLOBS.MESSAGE;
import static org.geogit.storage.BLOBS.PARENT_IDS;
import static org.geogit.storage.BLOBS.TIMESTAMP;
import static org.geogit.storage.BLOBS.TREE;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.repository.CommitBuilder;
import org.gvsig.bxml.stream.BxmlInputFactory;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;

public final class CommitReader implements ObjectReader<RevCommit> {

    /**
     * @see org.geogit.storage.ObjectReader#read(org.geogit.api.ObjectId, java.io.InputStream)
     */
    public RevCommit read(final ObjectId id, final InputStream raw) throws IOException {
        final BxmlInputFactory inputFactory = BLOBS.cachedInputFactory;
        final BxmlStreamReader r = inputFactory.createScanner(raw);
        CommitBuilder builder = new CommitBuilder();

        r.nextTag();
        try {
            r.require(EventType.START_ELEMENT, COMMIT.getNamespaceURI(), COMMIT.getLocalPart());
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("Object is not a commit: "
                    + r.getElementName().getLocalPart(), e);
        }

        EventType event;
        QName name;
        while ((event = r.next()) != EventType.END_DOCUMENT) {
            if (EventType.START_ELEMENT != event) {
                continue;
            }
            name = r.getElementName();
            if (COMMIT.equals(name)) {
                continue;
            }
            if (TREE.equals(name)) {
                r.nextTag();
                ObjectId treeId = BLOBS.parseObjectId(r);
                builder.setTreeId(treeId);
            }
            if (PARENT_IDS.equals(name)) {
                event = r.nextTag();
                name = r.getElementName();
                List<ObjectId> parentIds = new ArrayList<ObjectId>(2);
                while (!PARENT_IDS.equals(name)) {
                    ObjectId parentId = BLOBS.parseObjectId(r);
                    parentIds.add(parentId);
                    event = r.nextTag();
                    name = r.getElementName();
                }
                builder.setParentIds(parentIds);
            }
            if (AUTHOR.equals(name)) {
                event = r.nextTag();
                builder.setAuthor(BLOBS.parseString(r));
            }
            if (COMMITTER.equals(name)) {
                event = r.nextTag();
                builder.setCommitter(BLOBS.parseString(r));
            }
            if (MESSAGE.equals(name)) {
                event = r.nextTag();
                builder.setMessage(BLOBS.parseString(r));
            }
            if (TIMESTAMP.equals(name)) {
                event = r.nextTag();
                builder.setTimestamp(BLOBS.parseLong(r));
            }
        }
        return builder.build(id);
    }

}
