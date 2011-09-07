package org.geogit.api;

import java.util.Arrays;

import org.geogit.api.RevObject.TYPE;
import org.geogit.api.config.BranchConfigObject;
import org.geogit.api.config.RemoteConfigObject;
import org.geogit.storage.RefDatabase;
import org.geogit.test.RepositoryTestCase;

public class RemoteAddTest extends RepositoryTestCase {

    private RefDatabase refDb;
    private GeoGIT ggit;
    private RemoteConfigObject remote;
    private BranchConfigObject branch;
    
    @Override
    protected void setUpInternal() throws Exception {
        ggit = new GeoGIT(repo);
        refDb = repo.getRefDatabase();
        remote = new RemoteConfigObject("john", "john", "http://localhost/projectA.geogit");
        branch = new BranchConfigObject("johns_changes", "john", "refs/head/master");
    }

    public void testCreateRemoteConfigObject() {
        assertNotNull("The remotes name is null?", remote.getName());
        assertNotNull("The remotes fetch is null?", remote.getFetch());
        assertNotNull("The remotes url is null?", remote.getUrl());
    }

    public void testCreateBranchConfigObject() {
        assertNotNull("The branch name is null?", branch.getName());
        assertNotNull("The branch merge is null?", branch.getMerge());
        assertNotNull("The branch remote is null?", branch.getRemote());
    }

    public void testNewRemote() {
        ggit.remoteAddOp().setName(remote.getName()).setFetch(remote.getFetch()).setUrl(remote.getUrl()).call();
        assertEquals(ObjectId.NULL, refDb.getRef(Ref.REMOTES_PREFIX+"john"+"/"+Ref.MASTER).getObjectId());
        assertEquals(1, refDb.getRefs(Ref.REMOTES_PREFIX).size());
    }

    public void testPutGetRef() {
        byte[] raw = new byte[20];
        Arrays.fill(raw, (byte) 1);
        ObjectId oid = new ObjectId(raw);
        Ref ref = new Ref(Ref.REMOTES_PREFIX+remote.getName(), oid, TYPE.COMMIT);
        assertTrue(refDb.put(ref));
        assertFalse(refDb.put(ref));

        Ref read = refDb.getRef(Ref.REMOTES_PREFIX+remote.getName());
        assertNotSame(ref, read);
        assertEquals(ref, read);
    }
}
