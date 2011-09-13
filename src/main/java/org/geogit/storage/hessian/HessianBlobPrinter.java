package org.geogit.storage.hessian;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.geogit.api.BlobPrinter;
import org.geogit.api.ObjectId;

import com.caucho.hessian.io.Hessian2Input;

public class HessianBlobPrinter 
		extends HessianRevReader 
		implements BlobPrinter {
	
	Stack<EntityState> entityStack;
	
	public HessianBlobPrinter() {
		entityStack = new Stack<EntityState>();	
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
	
	private void printFeature(Hessian2Input hin, PrintStream out) throws IOException {
		out.println("Feature");
		int attrCount = hin.readInt();
		for(int i = 0; i < attrCount; i++) {
			Object obj = HessianFeatureReader.readValue(hin);
			out.println("\t- " + obj.toString());
		}
	}

	private void printRevTree(Hessian2Input hin, PrintStream out) throws IOException {
		
	}
	
	private class EntityState {
		boolean wrap;
		String entityName;
		
		public EntityState(String entityName, boolean wrap) {
			this.entityName = entityName;
			this.wrap = wrap;
		}
	}
	
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
	
	private void openTag(String entity, Map<String, String> attrs, PrintStream out, boolean wrap, boolean empty) throws IOException {
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
				else
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
	
	private void closeTag(PrintStream out) throws IOException {
		EntityState entity = entityStack.pop();
		if(entity.wrap) {
			out.print("\n");
			printIndent(out);
		}
		out.print("</");
		out.print(entity.entityName);
		out.print(">");
		if(entity.wrap)
			out.print("\n");
	}
	
	private void printIndent(PrintStream out) throws IOException {
		for(int i = 0; i < entityStack.size(); i++) {
			out.print(" ");
		}
	}
	
	private void printObjectId(ObjectId value, PrintStream out) throws IOException {
		openTag("objectid", out, false);
		out.print(value.toString());
		closeTag(out);
	}
	
	private void printLong(long value, PrintStream out) throws IOException {
		openTag("long", out, false);
		out.print(value);
		closeTag(out);
	}
	
	private void printString(String value, PrintStream out) throws IOException {
		if(value == null) {
			openTag("null", out, true, true);
		} else {
			openTag("string", out, false);
			out.print(value);
			closeTag(out);
		}
	}
}
