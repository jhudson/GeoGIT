/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.io.InputStream;
import java.io.PrintStream;

import org.geogit.repository.Repository;
import org.geogit.storage.bxml.BLOBS;

public class ShowOp extends AbstractGeoGitOp<Void> {

    private PrintStream out;

    private ObjectId oid;

    public ShowOp(final Repository repository) {
        super(repository);
        this.out = System.err;
    }

    public ShowOp setPrintStream(final PrintStream out) {
        this.out = out;
        return this;
    }

    public ShowOp setObjectId(final ObjectId oid) {
        this.oid = oid;
        return this;
    }

    @Override
    public Void call() throws Exception {
        final Repository repo = getRepository();
        final InputStream raw = repo.getRawObject(oid);
        final PrintStream out = this.out;
        try {
            BLOBS.print(raw, out);
        } finally {
            raw.close();
        }

        return null;
    }

}
