package org.geogit.storage.hessian;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.geogit.api.ObjectId;
import org.geogit.storage.ObjectReader;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureImpl;
import org.geotools.filter.identity.FeatureIdImpl;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;

import com.caucho.hessian.io.Hessian2Input;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequenceFactory;
import com.vividsolutions.jts.io.InStream;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;

public class HessianFeatureReader implements ObjectReader<Feature> {

	private static final FilterFactory2 FILTER_FAC = CommonFactoryFinder.getFilterFactory2(null);
	
	private FeatureType featureType;
	private String featureId;
	private GeometryFactory geometryFactory;
	public HessianFeatureReader(final FeatureType featureType, final String featureId) {
		this.featureType = featureType;
		this.featureId = featureId;
	}
	
	public void setGeometryFactory(GeometryFactory geometryFactory) {
		this.geometryFactory = geometryFactory;
	}
	public Feature read(ObjectId id, InputStream rawData) throws IOException,
			IllegalArgumentException {
		Hessian2Input in = new Hessian2Input(rawData);
		in.startMessage();
		List<Object>values = new ArrayList<Object>();
		String typeString = in.readString();
		int attrCount = in.readInt();
		for(int i = 0; i < attrCount; i++) {
			Object obj = readValue(in);
			values.add(obj);
		}
		in.completeMessage();
		FeatureId fid = FILTER_FAC.resourceId(featureId, id.toString());
		SimpleFeature feat = new SimpleFeatureImpl(values, (SimpleFeatureType)featureType, fid);
		return feat;
	}
	
	private Object readValue(final Hessian2Input in) throws IOException {
		Object obj = in.readObject();
		if(!(obj instanceof EntityType)) 
			throw new IOException("Illegal format in data stream");
		EntityType type = (EntityType)obj;
		switch(type) {
		case STRING:
		case BOOLEAN:
		case BYTE:
		case DOUBLE:
		case FLOAT:
		case INT:
		case LONG:
			Object obje = in.readObject();
			return obje;
		case BYTE_ARRAY:
			return in.readBytes();
		case BOOLEAN_ARRAY:
			int boolLength = in.readInt();
			boolean[] bools = new boolean[boolLength];
			for(int i = 0; i < boolLength; i++) {
				bools[i] = in.readBoolean();
			}
			return bools;
		case CHAR_ARRAY:
			String charstring = in.readString();
			return charstring.toCharArray();
		case DOUBLE_ARRAY:
			in.readNull();
			break;
		case FLOAT_ARRAY:
			in.readNull();
			break;
		case INT_ARRAY:
			in.readNull();
			break;
		case LONG_ARRAY:
			in.readNull();
			break;
		case BIGDECIMAL:
			String bdString = in.readString();
			return new BigDecimal(bdString);
		case BIGINT:
			byte[] biBytes = in.readBytes();
			return new BigInteger(biBytes);
		case GEOMETRY:
			String srs = in.readString();
			if(geometryFactory == null)
				geometryFactory = new GeometryFactory(new PackedCoordinateSequenceFactory());
			WKBReader wkbReader = new WKBReader(geometryFactory);
			Geometry geom;
			try {
				geom = wkbReader.read(new InStream() {
					
					public void read(byte[] buf) throws IOException {
						int length = buf.length;
						int returned = in.readBytes(buf, 0, length);
					}
				});
			} catch(ParseException ex) {
				throw (IOException)new IOException(ex.getMessage()).initCause(ex);
			}
			return geom;
		case NULL:
			in.readNull();
			return null;
		case UNKNOWN_SERIALISABLE:
			return in.readObject();
		case UNKNOWN:
			String classname = in.readString();
			String value = in.readString();
			return classname + value;
		}
		return null;
	}
}
