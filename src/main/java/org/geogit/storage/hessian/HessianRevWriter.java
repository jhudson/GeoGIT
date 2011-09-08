package org.geogit.storage.hessian;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.collections.map.LRUMap;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevTree;
import org.geogit.api.SpatialRef;
import org.geogit.storage.ObjectWriter;
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
	
	protected void writeObjectId(Hessian2Output hout, ObjectId id)
			throws IOException {
				if(id == null) {
					hout.writeBytes(new byte[0]);
				} else {
					hout.writeBytes(id.getRawValue());
				}
			}

	protected void writeRef(Hessian2Output hout, Ref ref) throws IOException {
		BoundingBox bounds = null;
		if(ref instanceof SpatialRef) {
			bounds = ((SpatialRef)ref).getBounds();
		}
		hout.writeInt(HessianRevReader.Node.REF.getValue());
		hout.writeInt(ref.getType().value());
		hout.writeString(ref.getName());
		writeObjectId(hout, ref.getObjectId());
		writeBBox(hout, bounds);
	}

	private void writeBBox(Hessian2Output hout, BoundingBox bbox) throws IOException {
		if(bbox == null) {
			hout.writeDouble(Double.NaN);
			return;
		}
		CoordinateReferenceSystem crs = bbox.getCoordinateReferenceSystem();
		String epsgCode;
		if(crs == null) {
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