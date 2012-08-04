/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.bdbje;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.geotools.util.logging.Logging;

import com.sleepycat.je.CacheMode;
import com.sleepycat.je.Durability;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

public class EnvironmentBuilder {

    private static final Logger LOGGER = Logging.getLogger(EnvironmentBuilder.class);

    private EntityStoreConfig config;

    public EnvironmentBuilder(EntityStoreConfig config) {
        this.config = config;
    }

    /**
     * 
     * @param storeDirectory
     * @param bdbEnvProperties
     *            properties for the {@link EnvironmentConfig}, or {@code null}. If not provided
     *            {@code environment.properties} will be looked up for inside {@code storeDirectory}
     * @return
     */
    public Environment buildEnvironment(final File storeDirectory, final Properties bdbEnvProperties) {

        EnvironmentConfig envCfg = new EnvironmentConfig();
        envCfg.setAllowCreate(true);
        envCfg.setCacheMode(CacheMode.MAKE_COLD);
        envCfg.setLockTimeout(1000, TimeUnit.MILLISECONDS);
        envCfg.setDurability(Durability.COMMIT_WRITE_NO_SYNC);
        envCfg.setSharedCache(true);
        envCfg.setTransactional(true);
        envCfg.setConfigParam("je.log.fileMax", String.valueOf(100 * 1024 * 1024));
        // check <http://www.oracle.com/technetwork/database/berkeleydb/je-faq-096044.html#35>
        envCfg.setConfigParam("je.evictor.lruOnly", "false");
        envCfg.setConfigParam("je.evictor.nodesPerScan", "100");

        Integer cacheMemoryPercentAllowed = config.getCacheMemoryPercentAllowed();
        Integer cacheSizeMB = config.getCacheSizeMB();
        if (cacheMemoryPercentAllowed == null) {
            if (cacheSizeMB == null) {
                LOGGER.fine("Neither cache memory percent nor cache size was provided."
                        + " Defaulting to 50% Heap Size");
                envCfg.setCachePercent(50);
            } else {
                LOGGER.info("Disk quota page store cache explicitly set to " + cacheSizeMB + "MB");
                envCfg.setCacheSize(cacheSizeMB);
            }
        } else {
            envCfg.setCachePercent(cacheMemoryPercentAllowed);
        }

        Environment env = new Environment(storeDirectory, envCfg);
        return env;
    }
}
