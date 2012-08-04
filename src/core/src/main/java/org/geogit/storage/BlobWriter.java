/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.io.IOException;
import java.io.OutputStream;

import org.geogit.api.RevBlob;

public class BlobWriter implements ObjectWriter<RevBlob> {

    private final byte[] blob;

    public BlobWriter(byte[] blob) {
        this.blob = blob;
    }

    @Override
    public void write(OutputStream out) throws IOException {
        out.write(blob);
    }

}
