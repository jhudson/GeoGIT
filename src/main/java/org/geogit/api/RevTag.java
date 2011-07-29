/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.api;

/**
 * An annotated tag.
 * 
 * @author groldan
 * 
 */
public class RevTag extends RevObject {

    private String name;

    private ObjectId commit;

    public RevTag(ObjectId id) {
        super(id);
    }

    @Override
    public TYPE getType() {
        return TYPE.TAG;
    }

}
