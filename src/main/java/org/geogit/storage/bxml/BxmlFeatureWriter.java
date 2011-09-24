/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.storage.bxml;

import static org.geogit.storage.bxml.BLOBS.BIGDECIMAL;
import static org.geogit.storage.bxml.BLOBS.BIGINT;
import static org.geogit.storage.bxml.BLOBS.BOOLEAN;
import static org.geogit.storage.bxml.BLOBS.BOOLEAN_ARRAY;
import static org.geogit.storage.bxml.BLOBS.BYTE;
import static org.geogit.storage.bxml.BLOBS.BYTE_ARRAY;
import static org.geogit.storage.bxml.BLOBS.CHAR_ARRAY;
import static org.geogit.storage.bxml.BLOBS.CONVERTED_FROM_ATT;
import static org.geogit.storage.bxml.BLOBS.CONVERTED_TO_STRING;
import static org.geogit.storage.bxml.BLOBS.DOUBLE;
import static org.geogit.storage.bxml.BLOBS.DOUBLE_ARRAY;
import static org.geogit.storage.bxml.BLOBS.FEATURE;
import static org.geogit.storage.bxml.BLOBS.FLOAT;
import static org.geogit.storage.bxml.BLOBS.FLOAT_ARRAY;
import static org.geogit.storage.bxml.BLOBS.GEOMETRY_WKB;
import static org.geogit.storage.bxml.BLOBS.INT;
import static org.geogit.storage.bxml.BLOBS.INT_ARRAY;
import static org.geogit.storage.bxml.BLOBS.LONG;
import static org.geogit.storage.bxml.BLOBS.LONG_ARRAY;
import static org.geogit.storage.bxml.BLOBS.NULL;
import static org.geogit.storage.bxml.BLOBS.STRING;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;

import javax.xml.XMLConstants;

import org.geogit.storage.ObjectWriter;
import org.geotools.referencing.CRS;
import org.geotools.util.Converters;
import org.gvsig.bxml.stream.BxmlOutputFactory;
import org.gvsig.bxml.stream.BxmlStreamWriter;
import org.gvsig.bxml.stream.EventType;
import org.opengis.feature.Attribute;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.base.Preconditions;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.OutStream;
import com.vividsolutions.jts.io.WKBWriter;

class BxmlFeatureWriter implements ObjectWriter<Feature> {

    private final Feature feature;

    public BxmlFeatureWriter(final Feature feature) {
        Preconditions.checkArgument(feature instanceof SimpleFeature,
                "Only SimpleFeature is supported so far");
        this.feature = feature;
    }

    public void write(final OutputStream out) throws IOException {
        Collection<Property> properties = feature.getProperties();

        final BxmlOutputFactory outputFactory = BLOBS.cachedOutputFactory;
        final BxmlStreamWriter writer = outputFactory.createSerializer(out);

        try {
            writer.writeStartDocument();
            writer.writeStartElement(FEATURE);
            for (Property p : properties) {
                if (!(p instanceof Attribute)) {
                    continue;
                }
                Attribute att = (Attribute) p;
                Name name = att.getName();
                Object value = att.getValue();
                if (value instanceof Geometry) {
                    Geometry geom = (Geometry) value;
                    if (!(geom.getUserData() instanceof CoordinateReferenceSystem)) {
                        GeometryDescriptor descriptor = null;
                        if (att instanceof GeometryAttribute) {
                            descriptor = ((GeometryAttribute) att).getDescriptor();
                        } else if (feature instanceof SimpleFeature) {
                            // SimpleFeatureImpl keeps on missbehaving?
                            descriptor = ((SimpleFeature) feature).getFeatureType()
                                    .getGeometryDescriptor();
                        }
                        if (descriptor != null) {
                            CoordinateReferenceSystem crs = descriptor
                                    .getCoordinateReferenceSystem();
                            geom.setUserData(crs);
                        }
                    }
                }
                writeValue(writer, value);
            }
            writer.writeEndElement();
            writer.writeEndDocument();
        } finally {
            writer.flush();
        }
    }

    private void writeValue(final BxmlStreamWriter writer, final Object value) throws IOException {
        if (value == null) {
            writer.writeStartElement(NULL);
            writer.writeEndElement();
            return;
        }

        if (value instanceof CharSequence) {
            writer.writeStartElement(STRING);
            writer.writeValue(String.valueOf(value));
        } else if (value instanceof Boolean) {
            writer.writeStartElement(BOOLEAN);
            writer.writeValue(((Boolean) value).booleanValue());
        } else if (value instanceof Byte) {
            writer.writeStartElement(BYTE);
            writer.writeValue(((Byte) value).byteValue());
        } else if (value instanceof Double) {
            writer.writeStartElement(DOUBLE);
            writer.writeValue(((Double) value).doubleValue());
        } else if (value instanceof BigDecimal) {
            writer.writeStartElement(BIGDECIMAL);
            BigDecimal bigDecimal = (BigDecimal) value;
            String string = bigDecimal.toEngineeringString();
            writer.writeValue(string);
        } else if (value instanceof Float) {
            writer.writeStartElement(FLOAT);
            writer.writeValue(((Float) value).floatValue());
        } else if (value instanceof Integer) {
            writer.writeStartElement(INT);
            writer.writeValue(((Integer) value).intValue());
        } else if (value instanceof BigInteger) {
            writer.writeStartElement(BIGINT);
            byte[] byteArray = ((BigInteger) value).toByteArray();
            writer.writeValue(byteArray, 0, byteArray.length);
        } else if (value instanceof Long) {
            writer.writeStartElement(LONG);
            writer.writeValue(((Long) value).longValue());
        } else if (value instanceof boolean[]) {
            writer.writeStartElement(BOOLEAN_ARRAY);
            boolean[] array = (boolean[]) value;
            writer.writeValue(array, 0, array.length);
        } else if (value instanceof byte[]) {
            writer.writeStartElement(BYTE_ARRAY);
            byte[] array = (byte[]) value;
            writer.writeValue(array, 0, array.length);
        } else if (value instanceof char[]) {
            writer.writeStartElement(CHAR_ARRAY);
            char[] array = (char[]) value;
            writer.writeValue(array, 0, array.length);
        } else if (value instanceof double[]) {
            writer.writeStartElement(DOUBLE_ARRAY);
            double[] array = (double[]) value;
            writer.writeValue(array, 0, array.length);
        } else if (value instanceof float[]) {
            writer.writeStartElement(FLOAT_ARRAY);
            float[] array = (float[]) value;
            writer.writeValue(array, 0, array.length);
        } else if (value instanceof int[]) {
            writer.writeStartElement(INT_ARRAY);
            int[] array = (int[]) value;
            writer.writeValue(array, 0, array.length);
        } else if (value instanceof long[]) {
            writer.writeStartElement(LONG_ARRAY);
            long[] array = (long[]) value;
            writer.writeValue(array, 0, array.length);
        } else if (value instanceof Geometry) {
            final Geometry geometry = (Geometry) value;
            String srs;
            if (geometry.getUserData() instanceof CoordinateReferenceSystem) {
                srs = CRS.toSRS((CoordinateReferenceSystem) geometry.getUserData());
            } else {
                srs = "urn:ogc:def:crs:EPSG::4326";
            }
            writer.writeStartElement(GEOMETRY_WKB);
            writer.writeStartAttribute(XMLConstants.NULL_NS_URI, "crs");
            writer.writeValue(srs);
            writer.writeEndAttributes();

            // writes the wkb geometry in byte[] chunks as delivered by WKBWriter, effectively
            // streaming out the write process so that it's not needed to get a full byte[] for the
            // whole wkb in memory first
            final OutStream wkbSerializingOut = new OutStream() {
                /**
                 * @see com.vividsolutions.jts.io.OutStream#write(byte[], int)
                 */
                public void write(byte[] buf, int len) throws IOException {
                    if (len == 1) {// slight optimization
                        writer.writeValue(buf[0]);
                    } else {
                        writer.startArray(EventType.VALUE_BYTE, len);
                        writer.writeValue(buf, 0, len);
                        writer.endArray();
                    }
                }
            };
            WKBWriter wkbWriter = new WKBWriter();
            wkbWriter.write(geometry, wkbSerializingOut);

            // WKBWriter wkbWriter = new WKBWriter();
            // byte[] wkb = wkbWriter.write(((Geometry) value));
            // writer.writeValue(wkb, 0, wkb.length);

        } else {
            writer.writeStartElement(CONVERTED_TO_STRING);
            writer.writeStartAttribute(CONVERTED_FROM_ATT);
            writer.writeValue(value.getClass().getName());
            writer.writeEndAttributes();

            String converted = Converters.convert(value, String.class);
            writer.writeValue(converted);
        }

        writer.writeEndElement();
    }

}
