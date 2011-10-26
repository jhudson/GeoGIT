package org.geogit.storage.bxml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;

import javax.xml.namespace.QName;

import org.geogit.api.ObjectId;
import org.geogit.storage.BlobPrinter;
import org.gvsig.bxml.stream.BxmlInputFactory;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKTWriter;

class BxmlBlobPrinter implements BlobPrinter {

    public void print(final byte[] rawBlob, final PrintStream out) throws IOException {
        print(new ByteArrayInputStream(rawBlob), out);
    }

    public void print(final InputStream rawBlob, final PrintStream out) throws IOException {
        final BxmlInputFactory inputFactory = BLOBS.cachedInputFactory;
        final BxmlStreamReader reader = inputFactory.createScanner(rawBlob);

        EventType event;
        QName elementName = null;
        boolean hasvalue = false;
        boolean needsClosure = false;
        int ilevel = 0;
        while (!(event = reader.next()).equals(EventType.END_DOCUMENT)) {
            if (event == EventType.START_ELEMENT) {
                if (needsClosure) {
                    out.print(">");
                }
                hasvalue = false;
                needsClosure = true;
                elementName = reader.getElementName();
                if (ilevel > 0) {
                    out.print('\n');
                }
                for (int i = 0; i < ilevel; i++) {
                    out.print(' ');
                }
                ilevel++;
                out.print('<');
                out.print(elementName.getLocalPart());
                final int attributeCount = reader.getAttributeCount();
                for (int i = 0; i < attributeCount; i++) {
                    out.print(' ');
                    out.print(reader.getAttributeName(i));
                    out.print("=\"");
                    out.print(reader.getAttributeValue(i));
                    out.print('\"');
                }
            } else if (event.isValue()) {
                hasvalue = true;
                needsClosure = false;
                out.print('>');
                event = printer(elementName).print(reader, out);
            }

            if (event == EventType.END_ELEMENT) {
                ilevel--;
                if (needsClosure) {
                    out.print("/>");
                    needsClosure = false;
                    continue;
                }
                needsClosure = false;
                if (!hasvalue) {
                    out.print('\n');
                    for (int i = 0; i < ilevel; i++) {
                        out.print(' ');
                    }
                }
                out.print("</" + reader.getElementName().getLocalPart() + ">");
                hasvalue = false;
            }
        }
        out.print('\n');
        out.flush();
    }

    private PrettyValuePrinter printer(QName name) {
        if (BLOBS.GEOMETRY_WKB.equals(name)) {
            return new WKBGeomPrinter();
        }
        if (BLOBS.OBJECT_ID.equals(name)) {
            return new ObjectIdPrinter();
        }
        return new PrettyValuePrinter();
    }

    private class PrettyValuePrinter {
        public EventType print(BxmlStreamReader reader, PrintStream out) throws IOException {
            String stringValue = reader.getStringValue();
            out.print(stringValue);
            return reader.getEventType();
        }
    }

    private class ObjectIdPrinter extends PrettyValuePrinter {
        @Override
        public EventType print(BxmlStreamReader reader, PrintStream out) throws IOException {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();

            EventType event = reader.getEventType();
            while (event.equals(EventType.VALUE_BYTE)) {
                final int chunkSize = reader.getValueCount();
                int byteValue;
                for (int i = 0; i < chunkSize; i++) {
                    byteValue = reader.getByteValue();
                    buf.write(byteValue);
                }
                event = reader.next();
            }
            byte[] rawId = buf.toByteArray();
            String strId = ObjectId.toString(rawId);
            out.print(strId);
            return reader.getEventType();
        }
    }

    private class WKBGeomPrinter extends PrettyValuePrinter {
        @Override
        public EventType print(BxmlStreamReader reader, PrintStream out) throws IOException {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();

            EventType event = reader.getEventType();
            while (event.equals(EventType.VALUE_BYTE)) {
                final int chunkSize = reader.getValueCount();
                int byteValue;
                for (int i = 0; i < chunkSize; i++) {
                    byteValue = reader.getByteValue();
                    buf.write(byteValue);
                }
                event = reader.next();
            }

            Geometry read;
            try {
                read = new WKBReader().read(buf.toByteArray());
                OutputStreamWriter w = new OutputStreamWriter(out);
                new WKTWriter().write(read, w);
                w.flush();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return reader.getEventType();
        }
    }
}
