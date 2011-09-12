package org.geogit.storage.hessian;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;

import org.geogit.storage.ObjectWriter;
import org.geotools.referencing.CRS;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.caucho.hessian.io.Hessian2Output;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.OutStream;
import com.vividsolutions.jts.io.WKBWriter;

public class HessianFeatureWriter implements ObjectWriter<Feature> {
	Feature feat;
	
	
	public HessianFeatureWriter(final Feature feature) {
		this.feat = feature;
	}
	
	public void write(final OutputStream out) throws IOException {
		Hessian2Output hout = new Hessian2Output(out);
		try {
			hout.startMessage();
			hout.writeInt(BlobType.FEATURE.getValue());
			Collection<Property> props = feat.getProperties();

			hout.writeString(feat.getType().getName().getURI());
			hout.writeInt(props.size());
			for(Property p : props) {
				writeProperty(hout, p);
			}
			hout.completeMessage();
		} finally {
			hout.flush();
		}
	}
	
	private void writeProperty(final Hessian2Output out, Property prop) 
			throws IOException {
		Object value = prop.getValue();
		EntityType type = EntityType.determineType(value);
		out.writeObject(type);
		switch(type) {
		case STRING:
		case BOOLEAN:
		case BYTE:
		case DOUBLE:
		case FLOAT:
		case INT:
		case LONG:
			out.writeObject(value);
			break;
		case BYTE_ARRAY:
			byte[] bytes = (byte[])value;
			out.writeBytes(bytes);
			break;
		case BOOLEAN_ARRAY:
			boolean[] bools = (boolean[])value;
			out.writeInt(bools.length);
			for(boolean bool : bools) {
				out.writeBoolean(bool);
			}
			break;
		case CHAR_ARRAY:
			String chars = new String((char[])value);
			out.writeString(chars);
			break;
		case DOUBLE_ARRAY:
			out.writeNull();
			break;
		case FLOAT_ARRAY:
			out.writeNull();
			break;
		case INT_ARRAY:
			out.writeNull();
			break;
		case LONG_ARRAY:
			out.writeNull();
			break;
		case BIGDECIMAL:
			String bdString = ((BigDecimal)value).toEngineeringString();
			out.writeString(bdString);
			break;
		case BIGINT:
			byte[] bigBytes = ((BigInteger)value).toByteArray();
			out.writeBytes(bigBytes);
			break;
		case GEOMETRY:
			Geometry geom = (Geometry)value;
			String srs;
			if(geom.getUserData() instanceof CoordinateReferenceSystem) {
				srs = CRS.toSRS((CoordinateReferenceSystem)geom.getUserData());
			} else {
				srs = "urn.ogc.def.crs.EPSG::4326";
			}
			out.writeString(srs);
			out.writeByteBufferStart();
			/*
			 * The output streaming cleverness is modelled on Gabriel Roldans approach of ensuring that 
			 * we are streaming the wkb definition of the geometry instead of producing a complete byte[].
			 * 
			 * The bugs and typos are my own.
			 */
			final OutStream wkbOut = new OutStream() {
				
				public void write(byte[] buf, int len) throws IOException {
					out.writeByteBufferPart(buf, 0, len);
				}
			};
			
			WKBWriter wkbwriter = new WKBWriter();
			wkbwriter.write(geom, wkbOut);
			out.writeByteBufferEnd(new byte[0], 0, 0);
			
			break;
		case NULL:
			out.writeNull();
			break;
		case UNKNOWN_SERIALISABLE:
			out.writeObject(value);
			break;
		case UNKNOWN:
			out.writeString(value.getClass().getName());
			out.writeString(value.toString());
			break;
		}
	}
}
