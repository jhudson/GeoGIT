/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.math.BigInteger;
import java.util.Iterator;

import com.google.common.base.Predicate;

public abstract class RevTree extends RevObject {

    public RevTree(ObjectId id) {
        super(id);
    }

    @Override
    public final TYPE getType() {
        return TYPE.TREE;
    }

    public abstract void put(final Ref ref);

    public abstract Ref get(final String key);

    public abstract Ref remove(final String key);

    public abstract void accept(TreeVisitor visitor);

    public abstract BigInteger size();

    public abstract Iterator<Ref> iterator(Predicate<Ref> filter);

    public abstract void normalize();

    public abstract boolean isNormalized();
}