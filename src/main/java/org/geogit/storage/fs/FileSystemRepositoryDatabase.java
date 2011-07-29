/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.storage.fs;

import java.io.File;

import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.RefDatabase;
import org.geogit.storage.RepositoryDatabase;

public class FileSystemRepositoryDatabase implements RepositoryDatabase {

    private final File environment;

    private FileObjectDatabase objectDatabase;

    private RefDatabase referenceDatabase;

    public FileSystemRepositoryDatabase(final File environment) {
        this.environment = environment;
        objectDatabase = new FileObjectDatabase(environment);
        referenceDatabase = new RefDatabase(objectDatabase);
    }

    @Override
    public RefDatabase getReferenceDatabase() {
        return referenceDatabase;
    }

    @Override
    public ObjectDatabase getObjectDatabase() {
        return objectDatabase;
    }

    @Override
    public void create() {
        objectDatabase.create();
        referenceDatabase.create();
    }

    @Override
    public void close() {
        referenceDatabase.close();
        objectDatabase.close();
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
