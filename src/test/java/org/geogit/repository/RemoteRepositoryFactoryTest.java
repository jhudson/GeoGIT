package org.geogit.repository;

import org.geogit.repository.remote.IRemote;
import org.geogit.repository.remote.LocalRemote;
import org.geogit.repository.remote.Remote;
import org.geogit.repository.remote.RemoteRepositoryFactory;
import org.geogit.test.RepositoryTestCase;
import org.junit.Test;

public class RemoteRepositoryFactoryTest extends RepositoryTestCase{

    @Override
    protected void setUpInternal() throws Exception {
    }

    @Test
    public void testLocal() {
        IRemote remoteRepo = RemoteRepositoryFactory.createRemoteRepositroy("c:/java/GeoGIT/target0/.geogit");
        assertTrue(remoteRepo instanceof IRemote);
        assertTrue(remoteRepo instanceof LocalRemote);
        assertFalse(remoteRepo instanceof Remote);
    }

    @Test
    public void testRemote() {
        IRemote remoteRepo = RemoteRepositoryFactory.createRemoteRepositroy("http://localhost:81/projects/target0.geogit"); //$NON-NLS-1$
        assertTrue(remoteRepo instanceof Remote);
        assertTrue(remoteRepo instanceof IRemote);
        assertFalse(remoteRepo instanceof LocalRemote);
    }

}
