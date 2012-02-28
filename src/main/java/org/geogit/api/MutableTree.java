/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

public interface MutableTree extends RevTree {

    public abstract void put(final Ref ref);

    public abstract Ref remove(final String key);

    public abstract void normalize();

}
