/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.geogit.api.ObjectId;
import org.geogit.api.RevBlob;

public class BlobReader implements ObjectReader<RevBlob> {

    @Override
    public RevBlob read(ObjectId id, InputStream rawData) throws IOException,
            IllegalArgumentException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int c;
        while ((c = rawData.read()) != -1) {
            output.write(c);
        }
        return new RevBlob(id, output.toByteArray());
    }

}
