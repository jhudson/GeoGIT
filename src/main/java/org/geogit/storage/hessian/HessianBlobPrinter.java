package org.geogit.storage.hessian;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.geogit.api.BlobPrinter;

import com.caucho.hessian.io.Hessian2Input;

public class HessianBlobPrinter 
		extends HessianRevReader 
		implements BlobPrinter {

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
	
	private void printCommit(Hessian2Input hin, PrintStream out) throws IOException {
		out.println("Commit");
		out.println("\tTreeId - " + readObjectId(hin));
		int parentCount = hin.readInt();
		out.println("\tParents");
		for(int i = 0; i < parentCount; i++) {
			out.println("\t\t- " + readObjectId(hin));
		}
		out.println("\tAuthor - " + hin.readString());
		out.println("\tCommitter - " + hin.readString());
		out.println("\tMessage - " + hin.readString());
		out.println("\tTimestamp - " + hin.readLong());
	}
}
