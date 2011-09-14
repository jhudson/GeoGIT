package org.geogit.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

public interface BlobPrinter {
	public void print(final byte[] rawBlob, final PrintStream out) throws IOException;
    public void print(final InputStream rawBlob, final PrintStream out) throws IOException;

}
