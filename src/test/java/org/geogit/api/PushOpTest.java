package org.geogit.api;

import org.geogit.test.MultipleRepositoryTestCase;

public class PushOpTest extends MultipleRepositoryTestCase {

    private GeoGIT client;
    
    private GeoGIT origin;

    public PushOpTest() {
        super(2/* one repository */);
    }

    @Override
    protected void setUpInternal() throws Exception {
        // setup repository 2 - acting as out client
        this.client = new GeoGIT(getRepository(0));

        // setup the 'origin'
        this.origin = new GeoGIT(getRepository(1));
        insertAddCommit(this.origin, points1);
        this.origin.getRepository().close();
        this.client.remoteAddOp().setName("origin").setFetch(Ref.MASTER)
                .setUrl("http://localhost:8080/geoserver/geogit/project1/geogit").call();
        this.client.pull().call();

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
    }

    public void testPullFiveClientChanges() throws Exception {
        insertAddCommit(this.client, points2);
        insertAddCommit(this.client, points3);
        insertAddCommit(this.client, lines1);
        insertAddCommit(this.client, lines2);
        insertAddCommit(this.client, lines3);

        // fetch the remotes
        PushResult pushResult = client.push().call();
        assertEquals(pushResult.getStatus(), PushResult.STATUS.OK_APPLIED);
    }

    public void testPullNonFastForward() throws Exception {
        insertAddCommit(this.client, points2);

        //reopen the server - put client out of sync
        this.origin = new GeoGIT(createRepo(1, false));
        insertAddCommit(this.origin, lines2);
        this.origin.getRepository().close();

        // fetch the remotes
        PushResult pushResult = client.push().call();
        assertEquals(pushResult.getStatus(), PushResult.STATUS.CONFLICT);
    }
}
