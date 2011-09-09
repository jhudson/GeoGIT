package org.geogit.api;

import org.geogit.test.MultipleRepositoryTestCase;

public class FetchTest extends MultipleRepositoryTestCase {

    private GeoGIT server;
    private GeoGIT client;

    public FetchTest() {
        super(2/* two repositories */);
    }

    @Override
    protected void setUpInternal() throws Exception {

        // setup repository 1 - acting as our server
        this.server = new GeoGIT(getRepository(0));

        // setup repository 2 - acting as out client
        this.client = new GeoGIT(getRepository(1));
        
        printHeads();
    }

    @Override
    protected void tearDownInternal() throws Exception {
        printHeads();
        super.tearDownInternal();
    }

    private void printHeads() {
        LOGGER.info("CLIENT REMOTE BRANCH : " + this.client.getRepository().getRefDatabase().getRefs(Ref.REMOTES_PREFIX));
        LOGGER.info("CLIENT HEAD          : " + this.client.getRepository().getHead());
        LOGGER.info("SERVER HEAD          : " + this.server.getRepository().getHead());
    }

    public void testFetchRemoteMaster() throws Exception {  
        insertAddCommit(this.server, points1);
        insertAddCommit(this.server, points2);

        // setup the client to have a remote ref to the server
        this.client.remoteAddOp().setName("project0").setFetch("projec0")
                .setUrl(this.server.getRepository().getRepositoryHome().getAbsolutePath()).call();

        // fetch the remotes
        client.fetch().call();
    }
}
