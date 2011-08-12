/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.api;

/**
 * A binary representation of the state of a Feature.
 * 
 * @author groldan
 * 
 */
public class RevBlob extends AbstractRevObject {

    private final byte[] content;

    public RevBlob(ObjectId id, byte[] content) {
        super(id, TYPE.BLOB);
        this.content = content;
    }

    public byte[] getContent() {
        return content;
    }
}
