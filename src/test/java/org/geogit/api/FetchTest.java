package org.geogit.api;

import java.util.List;

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
    }

    @Override
    protected void tearDownInternal() throws Exception {
        System.out.println("CLIENT REMOTE BRANCH : "
                + this.client.getRepository().getRefDatabase().getRefs(Ref.REMOTES_PREFIX));
        System.out.println("CLIENT HEAD          : " + this.client.getRepository().getHead());
        System.out.println("SERVER HEAD          : " + this.server.getRepository().getHead());
        super.tearDownInternal();
    }

    public void testFetchOrigin() throws Exception {
        insertAddCommit(this.server, points1);
        insertAddCommit(this.server, points2);
        insertAddCommit(this.client, points1);

        // fetch the remotes
        client.fetch().call();
    }

    public void testFullCommits() throws Exception {
        // add some inserts
        insertAddCommit(this.server, points1);
        insertAddCommit(this.server, points2);

        // setup the client to have a remote ref to the server
        this.client.remoteAddOp().setName(Ref.REMOTES_PREFIX + "project0")
                .setFetch(RemoteAddOp.REFS_HEADS_REFS_REMOTES + "projec0")
                .setUrl(this.client.getRepository().getRepositoryHome().getAbsolutePath()).call();

        // fetch the remotes
        client.fetch().call();
    }
}
