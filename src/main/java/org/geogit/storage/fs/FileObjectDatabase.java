/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.storage.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.geogit.api.ObjectId;
import org.geogit.storage.AbstractObjectDatabase;
import org.geogit.storage.ObjectDatabase;

public class FileObjectDatabase extends AbstractObjectDatabase implements ObjectDatabase {

    private final File environment;

    private final String environmentPath;

    public FileObjectDatabase(final File environment) {
        this.environment = environment;
        this.environmentPath = environment.getAbsolutePath();
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
    }

    @Override
    public void create() {
        if (!environment.exists() && !environment.mkdirs()) {
            throw new IllegalStateException("Can't create environment: "
                    + environment.getAbsolutePath());
        }
        if (!environment.isDirectory()) {
            throw new IllegalStateException("Environment but is not a directory: "
                    + environment.getAbsolutePath());
        }
        if (!environment.canWrite()) {
            throw new IllegalStateException("Environment is not writable: "
                    + environment.getAbsolutePath());
        }
    }

    @Override
    public boolean exists(final ObjectId id) {
        File f = filePath(id);
        return f.exists();
    }

    @Override
    protected InputStream getRawInternal(ObjectId id) throws IOException {
        File f = filePath(id);
        return new FileInputStream(f);
    }

    /**
     * @see org.geogit.storage.AbstractObjectDatabase#putInternal(org.geogit.api.ObjectId, byte[])
     */
    @Override
    protected boolean putInternal(final ObjectId id, final byte[] rawData, final boolean override)
            throws IOException {
        final File f = filePath(id);
        if (!override && f.exists()) {
            return false;
        }

        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(f);
        } catch (FileNotFoundException dirDoesNotExist) {
            final File parent = f.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                throw new IOException("Can't create " + parent.getAbsolutePath());
            }
            fileOutputStream = new FileOutputStream(f);
        }
        fileOutputStream.write(rawData);
        fileOutputStream.flush();
        fileOutputStream.close();
        return true;
    }

    @Override
    public boolean delete(ObjectId objectId) {
        return filePath(objectId).delete();
    }

    private File filePath(final ObjectId id) {
        final String idName = id.toString();
        final char[] path1 = new char[2];
        final char[] path2 = new char[2];
        idName.getChars(0, 2, path1, 0);
        idName.getChars(2, 4, path2, 0);

        StringBuilder sb = new StringBuilder(environmentPath);
        sb.append(File.separatorChar).append(path1).append(File.separatorChar).append(path2)
                .append(File.separatorChar).append(idName);
        String filePath = sb.toString();
        return new File(filePath);
    }

    @Override
    protected List<ObjectId> lookUpInternal(byte[] raw) {
        throw new UnsupportedOperationException("This method is not yet implemented");
    }

}
