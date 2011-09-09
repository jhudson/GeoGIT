package org.geogit.api;

import org.geogit.test.MultipleRepositoryTestCase;
import org.opengis.feature.Feature;

public class FetchTest extends MultipleRepositoryTestCase {

    private GeoGIT server;
    private GeoGIT server2;
    private GeoGIT client;

    public FetchTest() {
        super(3/* three repositories */);
    }

    @Override
    protected void setUpInternal() throws Exception {

        // setup repository 1 - acting as our server
        this.server = new GeoGIT(getRepository(0));

        // setup repository 2 - acting as out client
        this.client = new GeoGIT(getRepository(1));
        
        // setup repository 1 - acting as our server
        this.server2 = new GeoGIT(getRepository(2));

        printHeads();
    }

    @Override
    protected void tearDownInternal() throws Exception {
        printHeads();
        super.tearDownInternal();
    }

    private void printHeads() {
        LOGGER.info("CLIENT REMOTE BRANCH : "
                + this.client.getRepository().getRefDatabase().getRefs(Ref.REMOTES_PREFIX));
        LOGGER.info("CLIENT HEAD          : " + this.client.getRepository().getHead());
        LOGGER.info("SERVER HEAD          : " + this.server.getRepository().getHead());
    }
    
    public void testFetchNoRemote() throws Exception {
        // fetch the remotes
        client.fetch().call();
        Ref clientRemoteMaster = this.client.getRepository().getRef(Ref.REMOTES_PREFIX+"project0/"+Ref.MASTER);
        assertEquals(clientRemoteMaster, null);
    }

    public void testFetchRemoteMasterTwoChanges() throws Exception {
        insertAddCommit(this.server, points1);
        insertAddCommit(this.server, points2);

        // setup the client to have a remote ref to the server
        this.client.remoteAddOp().setName("project0").setFetch("project0").setUrl(this.server.getRepository().getRepositoryHome().getAbsolutePath()).call();

        // fetch the remotes
        client.fetch().call();
        
        Ref clientRemoteMaster = this.client.getRepository().getRef(Ref.REMOTES_PREFIX+"project0/"+Ref.MASTER);
        assertEquals(clientRemoteMaster.getObjectId(), this.server.getRepository().getHead().getObjectId());
    }

    public void testFetchRemoteMasterFourChanges() throws Exception {
        insertAddCommit(this.server, points1);
        insertAddCommit(this.server, lines1);
        insertAddCommit(this.server, points2);
        insertAddCommit(this.server, lines2);

        // setup the client to have a remote ref to the server
        this.client.remoteAddOp().setName("project0").setFetch("project0").setUrl(this.server.getRepository().getRepositoryHome().getAbsolutePath()).call();

        // fetch the remotes
        client.fetch().call();
        
        Ref clientRemoteMaster = this.client.getRepository().getRef(Ref.REMOTES_PREFIX+"project0/"+Ref.MASTER);
        assertEquals(clientRemoteMaster.getObjectId(), this.server.getRepository().getHead().getObjectId());
    }
    
    public void testFetchRemoteMasterClientIsAhead() throws Exception {
        insertAddCommit(this.server, points1);
        insertAddCommit(this.server, lines1);
        insertAddCommit(this.server, points2);
        insertAddCommit(this.client, lines2);

        // setup the client to have a remote ref to the server
        this.client.remoteAddOp().setName("project0").setFetch("project0")
                .setUrl(this.server.getRepository().getRepositoryHome().getAbsolutePath()).call();

        // fetch the remotes
        client.fetch().call();
        
        Ref clientRemoteMaster = this.client.getRepository().getRef(Ref.REMOTES_PREFIX+"project0/"+Ref.MASTER);
        assertEquals(clientRemoteMaster.getObjectId(), this.server.getRepository().getHead().getObjectId());
    }

    public void testFetchRemoteMasterManyCommits() throws Exception {
        for( int i = 0; i < 1000; i++ ) {
            Feature point = feature(pointsType, null, "StringProp1_"+i+"", new Integer(1000), "POINT("+i+" "+i+")");
            insertAddCommit(this.server, point);
        }

        // setup the client to have a remote ref to the server
        this.client.remoteAddOp().setName("project0").setFetch("project0")
                .setUrl(this.server.getRepository().getRepositoryHome().getAbsolutePath()).call();

        // fetch the remotes
        client.fetch().call();
        
        Ref clientRemoteMaster = this.client.getRepository().getRef(Ref.REMOTES_PREFIX+"project0/"+Ref.MASTER);
        assertEquals(clientRemoteMaster.getObjectId(), this.server.getRepository().getHead().getObjectId());
    }
    
    public void testFetchRemoteMasterManyChangesOneCommit() throws Exception {
        for( int i = 0; i < 1000; i++ ) {
            Feature point = feature(pointsType, null, "StringProp1_"+i+"", new Integer(1000), "POINT("+i+" "+i+")");
            insert(this.server, point);
        }
        server.commit().setMessage("commited a new feature").setAll(true).call();

        // setup the client to have a remote ref to the server
        this.client.remoteAddOp().setName("project0").setFetch("project0")
                .setUrl(this.server.getRepository().getRepositoryHome().getAbsolutePath()).call();

        // fetch the remotes
        client.fetch().call();
        
        Ref clientRemoteMaster = this.client.getRepository().getRef(Ref.REMOTES_PREFIX+"project0/"+Ref.MASTER);
        assertEquals(clientRemoteMaster.getObjectId(), this.server.getRepository().getHead().getObjectId());
    }
    
    public void testFetchTwoRemoteMastersFourChanges() throws Exception {
        insertAddCommit(this.server, points1);
        insertAddCommit(this.server, lines1);
        insertAddCommit(this.server, points2);
        insertAddCommit(this.server, lines2);
        
        insertAddCommit(this.server2, points1);
        insertAddCommit(this.server2, lines1);
        insertAddCommit(this.server2, points2);
        insertAddCommit(this.server2, lines2);

        // setup the client to have a remote ref to the server
        this.client.remoteAddOp().setName("project0").setFetch("project0").setUrl(this.server.getRepository().getRepositoryHome().getAbsolutePath()).call();
        // setup the client to have a remote ref to the server
        this.client.remoteAddOp().setName("project2").setFetch("project2").setUrl(this.server2.getRepository().getRepositoryHome().getAbsolutePath()).call();
        
        // fetch the remotes
        client.fetch().call();
        
        Ref clientRemoteMaster = this.client.getRepository().getRef(Ref.REMOTES_PREFIX+"project0/"+Ref.MASTER);
        assertEquals(clientRemoteMaster.getObjectId(), this.server.getRepository().getHead().getObjectId());
    }
}
