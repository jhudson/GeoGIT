/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.storage.bdbje;

import org.geogit.storage.RefDatabase;
import org.geogit.storage.RepositoryDatabase;
import org.geogit.storage.StagingDatabase;

import com.sleepycat.je.Environment;

public class JERepositoryDatabase implements RepositoryDatabase {

    private final Environment repositoryEnvironment;

    private final Environment stagingEnvironment;

    private JEObjectDatabase repositoryObjectDb;

    private RefDatabase referenceDatabase;

    private StagingDatabase stagingDatabase;

    public JERepositoryDatabase(final Environment repositoryEnvironment,
            final Environment stagingEnvironment) {

        this.repositoryEnvironment = repositoryEnvironment;
        this.stagingEnvironment = stagingEnvironment;
        this.repositoryObjectDb = new JEObjectDatabase(repositoryEnvironment);
        this.referenceDatabase = new RefDatabase(repositoryObjectDb);

        JEObjectDatabase stagingObjectDb = new JEObjectDatabase(stagingEnvironment);
        this.stagingDatabase = new StagingDatabase(repositoryObjectDb, stagingObjectDb,
                stagingEnvironment);
    }

    /**
     * @see org.geogit.storage.RepositoryDatabase#create()
     */
    @Override
    public void create() {
        repositoryObjectDb.create();
        referenceDatabase.create();
        stagingDatabase.create();
    }

    /**
     * @see org.geogit.storage.RepositoryDatabase#close()
     */
    @Override
    public void close() {
        stagingDatabase.close();
        stagingEnvironment.close();

        referenceDatabase.close();
        repositoryObjectDb.close();
        repositoryEnvironment.close();
    }

    /**
     * @see org.geogit.storage.RepositoryDatabase#getReferenceDatabase()
     */
    @Override
    public RefDatabase getReferenceDatabase() {
        return referenceDatabase;
    }

    /**
     * @see org.geogit.storage.RepositoryDatabase#getObjectDatabase()
     */
    @Override
    public JEObjectDatabase getObjectDatabase() {
        return repositoryObjectDb;
    }

    /**
     * @see org.geogit.storage.RepositoryDatabase#getStagingDatabase()
     */
    @Override
    public StagingDatabase getStagingDatabase() {
        return stagingDatabase;
    }

    @Override
    public void beginTransaction() {
        // CurrentTransaction.getInstance(environment).beginTransaction(null);
    }

    @Override
    public void commitTransaction() {
        // CurrentTransaction.getInstance(environment).commitTransaction();
    }

    @Override
    public void rollbackTransaction() {
        // CurrentTransaction.getInstance(environment).abortTransaction();
    }

}
