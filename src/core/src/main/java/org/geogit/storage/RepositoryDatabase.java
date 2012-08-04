/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage;

public interface RepositoryDatabase {

    public RefDatabase getReferenceDatabase();

    public ObjectDatabase getObjectDatabase();

    public StagingDatabase getStagingDatabase();

    public void create();

    public void close();

    public void beginTransaction();

    public void commitTransaction();

    public void rollbackTransaction();
}
