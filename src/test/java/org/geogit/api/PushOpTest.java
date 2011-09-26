package org.geogit.api;

import org.geogit.test.MultipleRepositoryTestCase;

public class PushOpTest extends MultipleRepositoryTestCase {

    private GeoGIT client;

    public PushOpTest() {
        super(2/* one repository */);
    }

    @Override
    protected void setUpInternal() throws Exception {
        // setup repository 2 - acting as out client
        this.client = new GeoGIT(getRepository(0));
        
        //setup the 'origin'
        GeoGIT origin = new GeoGIT(getRepository(1));
        insertAddCommit(origin, points1);
        this.client.pull();
        origin.getRepository().close();

        this.client.remoteAddOp().setName("origin").setFetch("master")
        .setUrl("http://localhost:8080/geoserver/geogit/project1/geogit").call();

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
    }

    public void testPullRemoteMasterTwoChanges() throws Exception {
        insertAddCommit(this.client, points2);
        insertAddCommit(this.client, points3);
        insertAddCommit(this.client, lines1);
        insertAddCommit(this.client, lines2);
        insertAddCommit(this.client, lines3);

        // fetch the remotes
        PushResult pushResult = client.push().call();

    }
}
