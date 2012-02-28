/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.io.IOException;
import java.io.InputStream;

import org.geogit.api.ObjectId;

public interface ObjectReader<T> {

    /**
     * @param id
     * @param rawData
     * @return
     * @throws IOException
     * @throws IllegalArgumentException
     *             if the provided stream does not represents an object of the required type
     */
    public T read(ObjectId id, InputStream rawData) throws IOException, IllegalArgumentException;

}
