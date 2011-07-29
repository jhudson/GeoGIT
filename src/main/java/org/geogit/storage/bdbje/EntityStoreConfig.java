/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.storage.bdbje;

import org.springframework.util.Assert;

public class EntityStoreConfig {

    private Integer cacheMemoryPercentAllowed;

    private Integer cacheSizeMB;

    /**
     * Percentage of the JVM heap size that can be used for the store's memory cache
     * <p>
     * This value and {@link #getCacheSizeMB()} are mutually exclusive. If both are present this
     * value takes precedence over {@link #getCacheSizeMB()}
     * </p>
     * 
     * @return {@code null} if not set, an integer between 0 and 100 otherwise, representing the max
     *         percentage of JVM assigned heap size the store can use for its internal cache.
     */
    public Integer getCacheMemoryPercentAllowed() {
        return cacheMemoryPercentAllowed;
    }

    /**
     * Maximum size in MB that can be used for the store's memory cache
     * <p>
     * This value and {@link #getCacheMemoryPercentAllowed()} are mutually exclusive. If both are
     * present {@link #getCacheMemoryPercentAllowed()} value takes precedence over this value;
     * </p>
     * 
     * @return {@code null} if not set, an integer between 0 and 100 otherwise, representing the max
     *         percentage of JVM assigned heap size the store can use for its internal cache.
     */
    public Integer getCacheSizeMB() {
        return cacheSizeMB;
    }

    public void setCacheMemoryPercentAllowed(int cacheMemoryPercentAllowed) {
        Assert.isTrue(cacheMemoryPercentAllowed > 0 && cacheMemoryPercentAllowed <= 100);
        this.cacheMemoryPercentAllowed = cacheMemoryPercentAllowed;
        this.cacheSizeMB = null;
    }

    public void setCacheSizeMB(int cacheSizeMB) {
        Assert.isTrue(cacheSizeMB > 0);
        this.cacheSizeMB = cacheSizeMB;
        this.cacheMemoryPercentAllowed = null;
    }

}
