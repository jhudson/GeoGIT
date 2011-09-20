/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.storage.bxml;

import static org.geogit.storage.bxml.BLOBS.REF;
import static org.geogit.storage.bxml.BLOBS.WHERE;

import java.io.IOException;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.SpatialRef;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;
import org.opengis.geometry.BoundingBox;

class BxmlRefReader {

    public Ref read(final BxmlStreamReader r) throws IOException {
        TYPE type;
        String refName;
        ObjectId refObjectId;
        BoundingBox where = null;

        r.require(EventType.START_ELEMENT, REF.getNamespaceURI(), REF.getLocalPart());

        int typeCode = Integer.parseInt(r.getAttributeValue(0));
        r.nextTag();
        refName = BLOBS.parseString(r);
        r.nextTag();
        refObjectId = BLOBS.parseObjectId(r);
        r.nextTag();
        if (EventType.START_ELEMENT.equals(r.getEventType())) {
            r.require(EventType.START_ELEMENT, WHERE.getNamespaceURI(), WHERE.getLocalPart());
            where = BLOBS.parseWhere(r);
            r.nextTag();
        }
        r.require(EventType.END_ELEMENT, REF.getNamespaceURI(), REF.getLocalPart());

        type = TYPE.valueOf(typeCode);

        Ref ref;
        if (where == null) {
            ref = new Ref(refName, refObjectId, type);
        } else {
            ref = new SpatialRef(refName, refObjectId, type, where);
        }
        return ref;
    }

}
