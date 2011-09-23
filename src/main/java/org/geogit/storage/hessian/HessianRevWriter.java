/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.storage.hessian;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.collections.map.LRUMap;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.SpatialRef;
import org.geotools.referencing.CRS;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.caucho.hessian.io.Hessian2Output;
import com.google.common.base.Throwables;

public class HessianRevWriter {

    @SuppressWarnings("unchecked")
    private static Map<CoordinateReferenceSystem, String> crsIdCache = Collections
            .synchronizedMap(new LRUMap(3));

    public HessianRevWriter() {
        super();
    }

    /**
     * Writes out an ObjectId as an array of bytes.
     * 
     * @param hout
     * @param id
     * @throws IOException
     */
    protected void writeObjectId(Hessian2Output hout, ObjectId id) throws IOException {
        if (id == null) {
            hout.writeBytes(new byte[0]);
        } else {
            hout.writeBytes(id.getRawValue());
        }
    }

    protected void writeRef(Hessian2Output hout, Ref ref) throws IOException {
        BoundingBox bounds = null;
        if (ref instanceof SpatialRef) {
            bounds = ((SpatialRef) ref).getBounds();
        }
        hout.writeInt(HessianRevReader.Node.REF.getValue());
        hout.writeInt(ref.getType().value());
        hout.writeString(ref.getName());
        writeObjectId(hout, ref.getObjectId());
        writeBBox(hout, bounds);
    }

    /**
     * Writes a BoundingBox to the provided output stream. The bounding box is encoded as four
     * double values, in the following order: - minx or westing - maxx or easting - miny or southing
     * - maxy or northing
     * 
     * A null bounding box is written as a single double NaN value.
     * 
     * @param hout
     * @param bbox
     * @throws IOException
     */
    private void writeBBox(Hessian2Output hout, BoundingBox bbox) throws IOException {
        if (bbox == null) {
            hout.writeDouble(Double.NaN);
            return;
        }
        CoordinateReferenceSystem crs = bbox.getCoordinateReferenceSystem();
        String epsgCode;
        if (crs == null) {
            epsgCode = "";
        } else {
            epsgCode = lookupIdentifier(crs);
        }

        hout.writeDouble(bbox.getMinX());
        hout.writeDouble(bbox.getMaxX());
        hout.writeDouble(bbox.getMinY());
        hout.writeDouble(bbox.getMaxY());

        hout.writeString(epsgCode);
    }

    private String lookupIdentifier(CoordinateReferenceSystem crs) {
        String epsgCode = crsIdCache.get(crs);
        if (epsgCode == null) {
            try {
                epsgCode = CRS.toSRS(crs);
            } catch (Exception e) {
                Throwables.propagate(e);
            }
            if (epsgCode == null) {
                throw new IllegalArgumentException("Can't find EPSG code for CRS " + crs.toWKT());
            }
            crsIdCache.put(crs, epsgCode);
        }
        return epsgCode;
    }

}