package org.geogit.api;

public interface MutableTree extends RevTree {

    public abstract void put(final Ref ref);

    public abstract Ref remove(final String key);

    public abstract void normalize();

}
