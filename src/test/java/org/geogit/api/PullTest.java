/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.api;

import org.geogit.test.MultipleRepositoryTestCase;
import org.opengis.feature.Feature;

public class PullTest extends MultipleRepositoryTestCase {

    private GeoGIT server;

    private GeoGIT client;

    public PullTest() {
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
        LOGGER.info("CLIENT REMOTE BRANCH : "
                + this.client.getRepository().getRefDatabase().getRefs(Ref.REMOTES_PREFIX));
        LOGGER.info("CLIENT HEAD          : " + this.client.getRepository().getHead());
        LOGGER.info("SERVER HEAD          : " + this.server.getRepository().getHead());
    }

    public void testPullNoRemote() throws Exception {
        // fetch the remotes
        client.fetch().call();
        Ref clientRemoteMaster = this.client.getRepository().getRef(
                Ref.REMOTES_PREFIX + "project0/" + Ref.MASTER);
        assertEquals(clientRemoteMaster, null);
    }

    public void testOnlyOrigin() throws Exception {
        insertAddCommit(this.server, points1);
        this.client.remoteAddOp().setName("origin").setFetch("master")
                .setUrl("http://localhost:8080/geoserver/geogit/project0/geogit").call();
        this.client.pull();
    }

    public void testPullRemoteMasterTwoChanges() throws Exception {
        insertAddCommit(this.server, points1);
        insertAddCommit(this.server, points2);
        insertAddCommit(this.server, points3);
        insertAddCommit(this.server, lines1);
        insertAddCommit(this.server, lines2);
        insertAddCommit(this.server, lines3);

        this.server.getRepository().close();

        // setup the client to have a remote ref to the server
        this.client.remoteAddOp().setName("project0").setFetch("project0")
                .setUrl("http://localhost:8080/geoserver/geogit/project0/geogit").call();

        // fetch the remotes
        MergeResult mergeResults = client.pull().setRepository("project0/" + Ref.MASTER).call();

        Ref clientRemoteMaster = this.client.getRepository().getRef(
                Ref.REMOTES_PREFIX + "project0/" + Ref.MASTER);

        // re-open the server
        this.server = new GeoGIT(createRepo(0, false));

        assertEquals(clientRemoteMaster.getObjectId(), this.server.getRepository().getHead()
                .getObjectId());
        assertEquals(0, mergeResults.getMerged().size());
    }
}
