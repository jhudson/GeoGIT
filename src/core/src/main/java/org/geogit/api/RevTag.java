/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

/**
 * An annotated tag.
 * 
 * @author groldan
 * 
 */
public class RevTag extends AbstractRevObject {

    private String name;

    private ObjectId commit;

    public RevTag(ObjectId id) {
        super(id, TYPE.TAG);
    }

}
