/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.storage.bxml;

import static org.geogit.storage.bxml.BLOBS.REF;
import static org.geogit.storage.bxml.BLOBS.STRING;

import java.io.IOException;

import org.geogit.api.Ref;
import org.geogit.api.SpatialRef;
import org.gvsig.bxml.stream.BxmlStreamWriter;
import org.opengis.geometry.BoundingBox;

class BxmlRefWriter {

    /**
     * <pre>
     * <code>
     *  &lt;ref type="COMMIT|TREE|BLOB|TAG"&gt;
     *    &lt;string&gt;refName&lt;/string&gt;
     *    &lt;objectid&gt;ref object id&lt;/objectid&gt;
     *    &lt;/where epsg="intEpsgCode"&gt; x1 y1 [x2 x2] &lt;/where&gt;
     *  &lt;/ref&gt;
     * </code>
     * </pre>
     * 
     * @param writer
     * @param ref
     * @throws IOException
     */
    public void write(final BxmlStreamWriter writer, final Ref ref) throws IOException {
        BoundingBox bounds = null;
        if (ref instanceof SpatialRef) {
            bounds = ((SpatialRef) ref).getBounds();
        }
        writer.writeStartElement(REF);

        writer.writeStartAttribute("", "type");
        writer.writeValue(ref.getType().value());
        writer.writeEndAttributes();
        {
            writer.writeStartElement(STRING);
            writer.writeValue(ref.getName());
            writer.writeEndElement();

            BLOBS.writeObjectId(writer, ref.getObjectId());

            if (bounds != null) {
                BLOBS.writeWhere(writer, bounds);
            }
        }
        writer.writeEndElement();
    }

}
