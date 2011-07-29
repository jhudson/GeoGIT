/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.geogit.repository.Repository;
import org.geotools.util.logging.Logging;

public abstract class AbstractGeoGitOp<T> implements Callable<T> {

    protected final Logger LOGGER;

    private final Repository repository;

    public AbstractGeoGitOp(Repository repository) {
        this.repository = repository;
        LOGGER = Logging.getLogger(getClass());
    }

    public Repository getRepository() {
        return repository;
    }

    /**
     * @see java.util.concurrent.Callable#call()
     */
    public abstract T call() throws Exception;

}
