/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.storage.bdbje;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geogit.api.ObjectId;
import org.geogit.storage.AbstractObjectDatabase;
import org.geogit.storage.ObjectDatabase;
import org.geotools.util.logging.Logging;
import org.springframework.util.Assert;

import com.sleepycat.collections.CurrentTransaction;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;

/**
 * @TODO: extract interface
 */
public class JEObjectDatabase extends AbstractObjectDatabase implements ObjectDatabase {

    private static final Logger LOGGER = Logging.getLogger(JEObjectDatabase.class);

    private final Environment env;

    private Database objectDb;

    private CurrentTransaction txn;

    public JEObjectDatabase(final Environment env) {
        super();
        this.env = env;
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#close()
     */
    @Override
    public void close() {
        objectDb.close();
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#create()
     */
    @Override
    public void create() {
        txn = CurrentTransaction.getInstance(env);
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        dbConfig.setTransactional(env.getConfig().getTransactional());
        this.objectDb = env.openDatabase(null, "BlobStore", dbConfig);
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#exists(org.geogit.api.ObjectId)
     */
    @Override
    public boolean exists(final ObjectId id) {
        Assert.notNull(id, "id");

        DatabaseEntry key = new DatabaseEntry(id.getRawValue());
        DatabaseEntry data = new DatabaseEntry();
        // tell db not to retrieve data
        data.setPartial(0, 0, true);

        final LockMode lockMode = LockMode.DEFAULT;
        CurrentTransaction.getInstance(env);
        OperationStatus status = objectDb.get(txn.getTransaction(), key, data, lockMode);
        return OperationStatus.SUCCESS == status;
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#getRaw(org.geogit.api.ObjectId)
     */
    @Override
    protected InputStream getRawInternal(final ObjectId id) throws IOException {
        Assert.notNull(id, "id");
        DatabaseEntry key = new DatabaseEntry(id.getRawValue());
        DatabaseEntry data = new DatabaseEntry();

        final LockMode lockMode = LockMode.READ_COMMITTED;
        Transaction transaction = txn.getTransaction();
        OperationStatus operationStatus = objectDb.get(transaction, key, data, lockMode);
        if (OperationStatus.NOTFOUND.equals(operationStatus)) {
            throw new IllegalArgumentException("Object does not exist: " + id.toString());
        }
        final byte[] cData = data.getData();

        return new ByteArrayInputStream(cData);
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#put(org.geogit.storage.ObjectWriter)
     */
    @Override
    protected boolean putInternal(final ObjectId id, final byte[] rawData, final boolean override)
            throws IOException {
        final byte[] rawKey = id.getRawValue();
        DatabaseEntry key = new DatabaseEntry(rawKey);
        DatabaseEntry data = new DatabaseEntry(rawData);

        OperationStatus status;
        if (override) {
            status = objectDb.put(txn.getTransaction(), key, data);
        } else {
            status = objectDb.putNoOverwrite(txn.getTransaction(), key, data);
        }
        final boolean didntExist = OperationStatus.SUCCESS.equals(status);

        if (LOGGER.isLoggable(Level.FINER)) {
            if (didntExist) {
                LOGGER.finer("Key already exists in blob store, blob reused for id: " + id);
            }
        }
        return didntExist;
    }
}
