package org.geogit.storage.hessian;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.storage.BlobPrinter;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.caucho.hessian.io.Hessian2Input;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;

public class HessianBlobPrinter 
		extends HessianRevReader 
		implements BlobPrinter {
	
	/**
	 * This keeps a reference to the tags that have been opened to prevent
	 * me from making typos that break the well-formedness of the xml output.
	 */
	Stack<EntityState> entityStack;
	
	/**
	 * This evil little guy tracks whether the previous entity was a closing
	 * entity that didn't line wrap.  It's purely for readability in the 
	 * output, not readability in the code.
	 * 
	 * It will only be true when a non-wrapping tag has just been closed.
	 */
	boolean startNew;
	
	public HessianBlobPrinter() {
		entityStack = new Stack<EntityState>();	
		startNew = false;
	}

	@Override
	public void print(byte[] rawBlob, PrintStream out) throws IOException {
		print(new ByteArrayInputStream(rawBlob), out);
	}

	@Override
	public void print(InputStream rawBlob, PrintStream out) throws IOException {
		Hessian2Input hin = new Hessian2Input(rawBlob);
		hin.startMessage();
		
		BlobType type = BlobType.fromValue(hin.readInt());
		switch(type) {
		case FEATURE:
			printFeature(hin, out);
			break;
		case REVTREE:
			printRevTree(hin, out);
			break;
		case COMMIT:
			printCommit(hin, out);
			break;
		}
		
		hin.completeMessage();
	}
	
	/**
	 * Prints a simple xml representation of the feature.
	 * 
	 * The feature will be formatted similar to the following:
	 * 
	 * <pre>
	 * {@code
	 * <feature>
     *   <string>StringProp2_1</string>
     *   <int>1000</int>
     *   <wkb crs="urn.ogc.def.crs.EPSG::4326">LINESTRING (1 1, 2 2)</wkb>
     * </feature>
     * }
     * </pre>
	 * 
	 * @param hin Hessian input stream to parse the feature from
	 * @param out PrintStream to write into.
	 * @throws IOException
	 */
	private void printFeature(Hessian2Input hin, PrintStream out) throws IOException {
		openTag("feature", out);
		String typeString = hin.readString();
		int attrCount = hin.readInt();
		for(int i = 0; i < attrCount; i++) {
			Object obj = HessianFeatureReader.readValue(hin);
			printObject(obj, out);
		}
		closeTag(out);
	}

	/**
	 * Prints an xml representation of the RevTree object.
	 * 
	 * The tree will be formatted similar to the following:
	 * 
	 * <pre>
	 * {@code
	 * <tree size="66536">
     *   <tree>
     *   <bucket>0</bucket>
     *   <objectid>5e1bd061389a03853b6004c7cac2e295b4456d4a</objectid>
     *   </tree>
     * </tree>
     * }
     * </pre>
	 * 
	 * @param hin Hessian input stream to parse the tree from
	 * @param out PrintStream to write into
	 * @throws IOException
	 */
	private void printRevTree(Hessian2Input hin, PrintStream out) throws IOException {
		BigInteger size = new BigInteger(hin.readBytes());
		Map<String, String> attr = new HashMap<String, String>();
		attr.put("size", size.toString());
		openTag("tree", attr, out, true, false);
		while(true) {
			Node type = null;
			type = Node.fromValue(hin.readInt());
			if(type.equals(Node.REF)){
				Ref entryRef = readRef(hin);
				printRef(entryRef, out);
			} else if(type.equals(Node.TREE)) {
				openTag("tree", out);
				int bucket = hin.readInt();
				ObjectId id = readObjectId(hin);
				openTag("bucket", out, false);
				out.print(Integer.toString(bucket));
				closeTag(out);
				printObjectId(id, out);
				closeTag(out);
			} else if(type.equals(Node.END)){
				break;
			}
		}
		closeTag(out);
	}
	
	private class EntityState {
		boolean wrap;
		String entityName;
		
		public EntityState(String entityName, boolean wrap) {
			this.entityName = entityName;
			this.wrap = wrap;
		}
	}
	
	/**
	 * Prints an xml representation of the RevCommit object.
	 * 
	 * The commit will be formatted similar to:
	 * 
	 * <pre>
	 * {@code
	 * <commit xmlns="">
     *   <tree>
     *     <objectid>4efbc525a7143adbe0c5467ea161be80b7a9c7ac</objectid>
     *   </tree>
     *   <parentids>
     *     <objectid>56d67008b5d5e331a877de9aa0727ff85183fc1a</objectid>
     *     <objectid>cffb4d4a1c9f69c972cfd5d6851cb3c2415e2fda</objectid>
     *   </parentids>
     *   <author>
     *     <string>groldan</string>
     *   </author>
     *   ...
     * </commit>
	 * }
	 * </pre>
	 * 
	 * @param hin Hessian input stream to parse the tree from
	 * @param out PrintStream to write into
	 * @throws IOException
	 */
	private void printCommit(Hessian2Input hin, PrintStream out) throws IOException {
		Map<String, String> attrMap = new HashMap<String, String>();
		attrMap.put("xmlns", null);
		openTag("commit", attrMap, out, true, false);
		openTag("tree", out);
		printObjectId(readObjectId(hin), out);
		closeTag(out);
		int parentCount = hin.readInt();
		openTag("parentids", out);
		for(int i = 0; i < parentCount; i++) {
			printObjectId(readObjectId(hin), out);
		}
		closeTag(out);
		openTag("author", out);
		printString(hin.readString(), out);
		closeTag(out);
		openTag("committer", out);
		printString(hin.readString(), out);
		closeTag(out);
		openTag("message", out);
		printString(hin.readString(), out);
		closeTag(out);
		openTag("timestamp", out);
		printLong(hin.readLong(), out);
		closeTag(out);
		closeTag(out);
	}
	
	private void openTag(String entity, PrintStream out) throws IOException {
		openTag(entity, out, true);
	}
	
	private void openTag(String entity, PrintStream out, boolean wrap) throws IOException {
		openTag(entity, out, wrap, false);
	}
	
	private void openTag(String entity, PrintStream out, boolean wrap, boolean empty) throws IOException {
		openTag(entity, null, out, wrap, empty);
	}
	
	/**
	 * Prints an opening tag of the given entity, with the provided attributes.
	 * An EntityState object is then created and stuffed on the stack to 
	 * be used to close the tag appropriately.
	 * If the tag is empty, it will not be placed on the stack for later closing.
	 * 
	 * @param entity
	 * @param attrs
	 * @param out
	 * @param wrap true to print a new line and indentation before the tag
	 * @param empty true to print a tag with no content.
	 * @throws IOException
	 */
	private void openTag(String entity, Map<String, String> attrs, PrintStream out, boolean wrap, boolean empty) throws IOException {
		startNew = false;
		if(entityStack.size() > 0) {
			EntityState lastState = entityStack.peek();
			if(lastState.wrap)
				printIndent(out);
		}
		out.print("<");
		out.print(entity);
		if(attrs != null) {
			Set<String> keys = attrs.keySet();
			for(String key : keys) {
				String value = attrs.get(key);
				out.print(" ");
				out.print(key);
				out.print("=\"");
				if(value != null)
					out.print(value);
				out.print("\"");
			}
		}
		if(empty) {
			out.print("/>");
		} else {
			out.print(">");
			entityStack.push(new EntityState(entity, wrap));
		}
		if(wrap)
			out.print("\n");
	}
	
	/**
	 * Closes the tag at the top of the stack.
	 * @param out
	 * @throws IOException
	 */
	private void closeTag(PrintStream out) throws IOException {
		EntityState entity = entityStack.pop();
		if(entity.wrap) {
			out.print("\n");
			printIndent(out);
		}
		out.print("</");
		out.print(entity.entityName);
		out.print(">");
		out.print("\n");
	}
	
	private void printIndent(PrintStream out) throws IOException {
		for(int i = 0; i < entityStack.size(); i++) {
			out.print(" ");
		}
	}
	
	private void printRef(Ref ref, PrintStream out) throws IOException {
		Map<String, String> attr = new HashMap<String, String>();
		attr.put("name", ref.getName());
		attr.put("type", ref.getType().toString());
		openTag("ref", attr, out, true, false);
		printObjectId(ref.getObjectId(), out);
		closeTag(out);
	}
	
	private void printObjectId(ObjectId value, PrintStream out) throws IOException {
		openTag("objectid", out, false);
		out.print(value.toString());
		closeTag(out);
	}
	
	private void printObject(Object obj, PrintStream out) throws IOException {
		if(obj == null) {
			printNull(out);
		} else if(obj instanceof String) {
			printString((String)obj, out);
		} else if(obj instanceof Boolean) {
			printBoolean((Boolean)obj, out);
		} else if(obj instanceof Byte) {
			printByte((Byte)obj, out);
		} else if(obj instanceof Double) {
			printDouble((Double)obj, out);
		} else if(obj instanceof Float) {
			printFloat((Float)obj, out);
		} else if(obj instanceof Integer) {
			printInt((Integer)obj, out);
		} else if(obj instanceof Long) {
			printLong((Long)obj, out);
		} else if(obj instanceof byte[]) {
			printByteArray((byte[])obj, out);
		} else if(obj instanceof boolean[]) {
			printBooleanArray((boolean[])obj, out);
		} else if(obj instanceof char[]) {
			printCharArray((char[])obj, out);
		} else if(obj instanceof double[]) {
			printDoubleArray((double[])obj, out);
		} else if(obj instanceof float[]) {
			printFloatArray((float[])obj, out);
		} else if(obj instanceof int[]) {
			printIntArray((int[])obj, out);
		} else if(obj instanceof long[]) {
			printLongArray((long[])obj, out);
		} else if(obj instanceof BigDecimal) {
			printBigDecimal((BigDecimal)obj, out);
		} else if(obj instanceof BigInteger) {
			printBigInteger((BigInteger)obj, out);
		} else if(obj instanceof Geometry) {
			printGeometry((Geometry)obj, out);
		} else if(obj instanceof Serializable) {
			printSerialisable((Serializable)obj, out);
		} else {
			// There's a bit of a hole here.
		}
	}
	
	private void printGeometry(Geometry value, PrintStream out) throws IOException {
		String srs;
		if(value.getUserData() instanceof CoordinateReferenceSystem) {
			srs = CRS.toSRS((CoordinateReferenceSystem)value.getUserData());
		} else {
			srs = "urn.ogc.def.crs.EPSG::4326";
		}
		Map<String, String> attr = new HashMap<String, String>();
		attr.put("crs", srs);
		openTag("wkb", attr, out, false, false);
		WKTWriter writ = new WKTWriter();
		out.print(writ.writeFormatted(value));
		closeTag(out);
	}
	
	private void printSerialisable(Serializable value, PrintStream out) throws IOException {
		openTag("serialisable", out, false);
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ObjectOutputStream oout = new ObjectOutputStream(bout);
		oout.writeObject(value);
		out.print(formatArray(bout.toByteArray()));
		closeTag(out);
	}
	
	private void printBigInteger(BigInteger value, PrintStream out) throws IOException {
		openTag("biginteger", out, false);
		out.print(value.toString());
		closeTag(out);
	}
	
	private void printBigDecimal(BigDecimal value, PrintStream out) throws IOException {
		openTag("bigdecimal", out, false);
		out.print(value.toEngineeringString());
		closeTag(out);
	}
	
	private void printLongArray(long[] value, PrintStream out) throws IOException {
		openTag("longarray", out, false);
		out.print(formatArray(value));
		closeTag(out);
	}
	
	private void printIntArray(int[] value, PrintStream out) throws IOException {
		openTag("intarray", out, false);
		out.print(formatArray(value));
		closeTag(out);
	}
	
	private void printFloatArray(float[] value, PrintStream out) throws IOException {
		openTag("floatarray", out, false);
		out.print(formatArray(value));
		closeTag(out);
	}
	
	private void printDoubleArray(double[] value, PrintStream out) throws IOException {
		openTag("doublearray", out, false);
		out.print(formatArray(value));
		closeTag(out);
	}
	
	private void printCharArray(char[] value, PrintStream out) throws IOException {
		openTag("chararray", out, false);
		out.print(formatArray(value));
		closeTag(out);
	}
	
	private void printBooleanArray(boolean[] value, PrintStream out) throws IOException {
		openTag("booleanarray", out, false);
		out.print(formatArray(value));
		closeTag(out);
	}
	
	private void printByteArray(byte[] value, PrintStream out) throws IOException {
		openTag("bytearray", out, false);
		out.print(formatArray(value, "%x"));
		closeTag(out);
	}
	
	private String formatArray(Object array) {
		return formatArray(array, "%s");
	}
	
	private String formatArray(Object array, String elementFormat) {
		StringBuffer buf = new StringBuffer("[");
		for(int i = 0; i < Array.getLength(array); i++) {
			buf.append(String.format(elementFormat, Array.get(array, i)));
		}
		buf.append("]");
		return buf.toString();
	}
	
	private void printFloat(float value, PrintStream out) throws IOException {
		openTag("float", out, false);
		out.print(value);
		closeTag(out);
	}
	
	private void printDouble(double value, PrintStream out) throws IOException {
		openTag("double", out, false);
		out.print(value);
		closeTag(out);
	}
	
	private void printByte(byte value, PrintStream out) throws IOException {
		openTag("byte", out, false);
		out.print(String.format("%x", value));
		closeTag(out);
	}
	
	private void printBoolean(boolean value, PrintStream out) throws IOException {
		openTag("boolean", out, false);
		out.print(value);
		closeTag(out);
	}
	
	private void printInt(int value, PrintStream out) throws IOException {
		openTag("int", out, false);
		out.print(value);
		closeTag(out);
	}
	
	private void printLong(long value, PrintStream out) throws IOException {
		openTag("long", out, false);
		out.print(value);
		closeTag(out);
	}
	
	private void printString(String value, PrintStream out) throws IOException {
		if(value == null) {
			printNull(out);
		} else {
			openTag("string", out, false);
			out.print(value);
			closeTag(out);
		}
	}
	
	private void printNull(PrintStream out) throws IOException {
		openTag("null", out, true, true);
	}
}
