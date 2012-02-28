/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Object writer that merely transfers the contents of an {@link InputStream} to the target
 * {@link OutputStream}, mainly used to copy raw objects from one {@link ObjectDatabase} to another.
 * 
 * @author groldan
 * 
 */
public class RawObjectWriter implements ObjectWriter<Object> {

    private final InputStream from;

    public RawObjectWriter(final InputStream from) {
        this.from = from;
    }

    @Override
    public void write(OutputStream out) throws IOException {
        int c;
        while ((c = from.read()) != -1) {
            out.write(c);
        }
    }

}
