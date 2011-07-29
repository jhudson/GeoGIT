/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.io.IOException;
import java.io.OutputStream;

public interface ObjectWriter<T> {

    /**
     * Writes the object to the given output stream. Does not close the output stream, as it doesn't
     * belong to this object. The calling code is responsible of the outputstream life cycle.
     * 
     * @param out
     * @throws IOException
     */
    public void write(OutputStream out) throws IOException;

}
