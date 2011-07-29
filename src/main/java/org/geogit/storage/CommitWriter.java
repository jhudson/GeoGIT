/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import static org.geogit.storage.BLOBS.AUTHOR;
import static org.geogit.storage.BLOBS.COMMIT;
import static org.geogit.storage.BLOBS.COMMITTER;
import static org.geogit.storage.BLOBS.LONG;
import static org.geogit.storage.BLOBS.MESSAGE;
import static org.geogit.storage.BLOBS.NAMESPACE;
import static org.geogit.storage.BLOBS.PARENT_IDS;
import static org.geogit.storage.BLOBS.TIMESTAMP;
import static org.geogit.storage.BLOBS.TREE;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.gvsig.bxml.stream.BxmlOutputFactory;
import org.gvsig.bxml.stream.BxmlStreamWriter;

public final class CommitWriter implements ObjectWriter<RevCommit> {

    private final RevCommit commit;

    public CommitWriter(final RevCommit commit) {
        this.commit = commit;
    }

    /**
     * @see org.geogit.storage.ObjectWriter#write(java.io.OutputStream)
     */
    public void write(final OutputStream out) throws IOException {
        final BxmlOutputFactory factory = BLOBS.cachedOutputFactory;
        final BxmlStreamWriter w = factory.createSerializer(out);
        w.writeStartDocument();

        w.writeStartElement(COMMIT);
        w.writeDefaultNamespace(NAMESPACE);
        {
            w.writeStartElement(TREE);
            {
                BLOBS.writeObjectId(w, commit.getTreeId());
            }
            w.writeEndElement();

            w.writeStartElement(PARENT_IDS);
            List<ObjectId> parentIds = commit.getParentIds();
            for (int i = 0; parentIds != null && i < parentIds.size(); i++) {
                ObjectId parentId = parentIds.get(i);
                BLOBS.writeObjectId(w, parentId);
            }
            w.writeEndElement();

            BLOBS.writeString(w, AUTHOR, commit.getAuthor());
            BLOBS.writeString(w, COMMITTER, commit.getCommitter());
            BLOBS.writeString(w, MESSAGE, commit.getMessage());
            long timestamp = commit.getTimestamp();
            if (timestamp <= 0) {
                timestamp = System.currentTimeMillis();
            }
            w.writeStartElement(TIMESTAMP);
            {
                w.writeStartElement(LONG);
                w.writeValue(timestamp);
                w.writeEndElement();
            }
            w.writeEndElement();
        }
        w.writeEndElement();

        w.writeEndDocument();
        w.flush();
    }

}
