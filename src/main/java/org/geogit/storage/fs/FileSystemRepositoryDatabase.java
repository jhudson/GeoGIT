/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.storage.fs;

import java.io.File;

import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.RefDatabase;
import org.geogit.storage.RepositoryDatabase;
import org.geogit.storage.StagingDatabase;

public class FileSystemRepositoryDatabase implements RepositoryDatabase {

    private final File repositoryEnvironment;

    private final File stagingEnvironment;

    private RefDatabase referenceDatabase;

    private FileObjectDatabase repositoryObjectDb;

    private StagingDatabase stagingDatabase;

    public FileSystemRepositoryDatabase(final File repositoryEnvironment,
            final File stagingEnvironment) {

        this.repositoryEnvironment = repositoryEnvironment;
        this.stagingEnvironment = stagingEnvironment;
        this.repositoryObjectDb = new FileObjectDatabase(repositoryEnvironment);
        this.referenceDatabase = new RefDatabase(repositoryObjectDb);

        FileObjectDatabase stagingObjectDb = new FileObjectDatabase(stagingEnvironment);
        this.stagingDatabase = new StagingDatabase(repositoryObjectDb, stagingObjectDb);
    }

    @Override
    public void create() {
        repositoryObjectDb.create();
        referenceDatabase.create();
        stagingDatabase.create();
    }

    @Override
    public void close() {
        stagingDatabase.close();
        referenceDatabase.close();
        repositoryObjectDb.close();
    }

    @Override
    public RefDatabase getReferenceDatabase() {
        return referenceDatabase;
    }

    @Override
    public ObjectDatabase getObjectDatabase() {
        return repositoryObjectDb;
    }

    @Override
    public StagingDatabase getStagingDatabase() {
        return stagingDatabase;
    }

    @Override
    public void beginTransaction() {
        // TODO Auto-generated method stub
    }

    @Override
    public void commitTransaction() {
        // TODO Auto-generated method stub
    }

    @Override
    public void rollbackTransaction() {
        // TODO Auto-generated method stub
    }

}
