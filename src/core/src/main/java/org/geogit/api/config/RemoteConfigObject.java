/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.config;

import com.google.common.base.Preconditions;

/**
 * Object representing a Remote config element as supplied in the config file
 *
 * @author jhudson
 */
public class RemoteConfigObject {
    private final String name;
    private final String url;
    private final String fetch;
    public RemoteConfigObject( String name, String fetch, String url ) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(url);
        Preconditions.checkNotNull(fetch);
        this.name = name;
        this.url = url;
        this.fetch = fetch;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getFetch() {
        return fetch;
    }

    @Override
    public String toString() {
        return "RemoteService [name=" + name + ", url=" + url + ", fetch=" + fetch + "]";
    }
}
