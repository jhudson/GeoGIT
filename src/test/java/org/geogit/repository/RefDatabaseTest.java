/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import java.util.Arrays;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevObject.TYPE;
import org.geogit.storage.RefDatabase;
import org.geogit.test.RepositoryTestCase;

public class RefDatabaseTest extends RepositoryTestCase {

    private RefDatabase refDb;
    private GeoGIT ggit;

    @Override
    protected void setUpInternal() throws Exception {
        refDb = repo.getRefDatabase();
        ggit = new GeoGIT(repo);
    }

    public void testEmpty() {
        assertEquals(ObjectId.NULL, refDb.getRef(Ref.MASTER).getObjectId());
        assertEquals(ObjectId.NULL, refDb.getRef(Ref.HEAD).getObjectId());
    }

    public void testNewRef(){        
        ggit.remoteAddOp().setName("john").setFetch("john").setUrl("http://localhost:8080/projects/mygeogit").call();
        assertEquals(1, refDb.getRefs(Ref.REMOTES_PREFIX+"john").size());
        assertEquals(ObjectId.NULL, refDb.getRef(Ref.REMOTES_PREFIX+"john"+"/"+Ref.MASTER).getObjectId());
        assertEquals(1, refDb.getRefs(Ref.REMOTES_PREFIX).size());
    }

    public void testGetRef(){
        ggit.remoteAddOp().setName("john").setFetch("john").setUrl("http://localhost:8080/projects/mygeogit").call();
        assertEquals(ObjectId.NULL, ggit.getRepository().getRef(Ref.REMOTES_PREFIX+"john"+"/"+Ref.MASTER).getObjectId());
    }

    public void testPutGetRef() {
        byte[] raw = new byte[20];
        Arrays.fill(raw, (byte) 1);
        ObjectId oid = new ObjectId(raw);
        Ref ref = new Ref("refs/HEAD", oid, TYPE.COMMIT);
        assertTrue(refDb.put(ref));
        assertFalse(refDb.put(ref));

        Ref read = refDb.getRef("refs/HEAD");
        assertNotSame(ref, read);
        assertEquals(ref, read);
    }

}
