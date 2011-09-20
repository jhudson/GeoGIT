package org.geogit.storage.hessian;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.collections.map.LRUMap;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.SpatialRef;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.caucho.hessian.io.Hessian2Input;
import com.google.common.base.Throwables;

public class HessianRevReader {
	public enum Node {
		REF(0),
		TREE(1),
		END(2);
		
		private int value;
		
		Node(int value) {
			this.value = value;
		}
		
		public int getValue() {
			return this.value;
		}
		
		public static Node fromValue(int value) {
			for(Node n : Node.values()) {
				if(value == n.getValue())
					return n;
			}
			return null;
		}
	}

    @SuppressWarnings("unchecked")
    private static Map<String, CoordinateReferenceSystem> crsCache = Collections
            .synchronizedMap(new LRUMap(3));

	public HessianRevReader() {
		super();
	}
	
	protected ObjectId readObjectId(Hessian2Input hin) 
			throws IOException {
		byte[] bytes = hin.readBytes();
		ObjectId id = new ObjectId(bytes);
		return id;
	}
	
	protected Ref readRef(Hessian2Input hin) 
			throws IOException {
		TYPE type = TYPE.valueOf(hin.readInt());
		String name = hin.readString();
		ObjectId id = readObjectId(hin);
		BoundingBox bbox = readBBox(hin);
		
		Ref ref;
		if(bbox == null) {
			ref = new Ref(name, id, type);
		} else {
			ref = new SpatialRef(name, id, type, bbox);
		}
		
		return ref;
	}
	
	protected BoundingBox readBBox(Hessian2Input hin) 
			throws IOException {
		double minx = hin.readDouble();
		if(Double.isNaN(minx))
			return null;
		
		double maxx = hin.readDouble();
		double miny = hin.readDouble();
		double maxy = hin.readDouble();
		
		String epsgCode = hin.readString();
		CoordinateReferenceSystem crs = null;
		if(epsgCode != null && epsgCode.length() > 0)
			crs = lookupCrs(epsgCode);
		
		BoundingBox bbox = new ReferencedEnvelope(minx, maxx, miny, maxy, crs);
		return bbox;
	}
	
    private static CoordinateReferenceSystem lookupCrs(final String epsgCode) {
        CoordinateReferenceSystem crs = crsCache.get(epsgCode);
        if (crs == null) {
            try {
                crs = CRS.decode(epsgCode, false);
                crsCache.put(epsgCode, crs);
            } catch (Exception e) {
                Throwables.propagate(e);
            }
        }
        return crs;
    }
}
