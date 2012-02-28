/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.io.IOException;

import org.geogit.test.MultipleRepositoryTestCase;

public class PushOpOnlineTest extends MultipleRepositoryTestCase {

    private GeoGIT client;

    private GeoGIT origin;

    public PushOpOnlineTest() {
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
                .setUrl(GEOGIT_URL+"/project1/geogit").call();
        this.client.pull().call();

        printHeads();
    }

    @Override
    protected void tearDownInternal() throws Exception {
        printHeads();
        this.origin.getRepository().close();
        this.client.getRepository().close();
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
        
        // push to origin
        PushResult pushResult = client.push().call();
        assertEquals(pushResult.getStatus(), PushResult.STATUS.OK_APPLIED);

        this.origin = new GeoGIT(createRepo(1, false));
        Ref originHead = this.origin.getRepository().getHead();
        assertEquals(this.client.getRepository().getHead(), originHead);

        LOGGER.info("ORIGIN HEAD          : " + this.origin.getRepository().getHead());
    }

    public void testPullNonFastForward() throws Exception {
        insertAddCommit(this.client, points2);

        // reopen the server - put client out of sync
        this.origin = new GeoGIT(createRepo(1, false));
        insertAddCommit(this.origin, lines2);
        this.origin.getRepository().close();

        // push to origin
        PushResult pushResult = client.push().call();
        assertEquals(pushResult.getStatus(), PushResult.STATUS.CONFLICT);

        this.origin = new GeoGIT(createRepo(1, false));
        Ref originHead = this.origin.getRepository().getHead();
        assertNotSame(this.client.getRepository().getHead(), originHead);

        LOGGER.info("ORIGIN HEAD          : " + this.origin.getRepository().getHead());
    }

    public void testPullNonFastForwardItsokIllfetchFirst() throws Exception {
        insertAddCommit(this.client, points2);

        // reopen the server - put client out of sync
        this.origin = new GeoGIT(createRepo(1, false));
        insertAddCommit(this.origin, lines2);
        this.origin.getRepository().close();

        // push to origin
        PushResult pushResult = client.push().call();
        assertEquals(pushResult.getStatus(), PushResult.STATUS.CONFLICT);

        this.origin = new GeoGIT(createRepo(1, false));
        Ref originHead = this.origin.getRepository().getHead();
        assertNotSame(this.client.getRepository().getHead(), originHead);
        LOGGER.info("ORIGIN HEAD          : " + this.origin.getRepository().getHead());
        this.origin.getRepository().close();

        PullOp pull = new PullOp(this.client.getRepository());
        pull.call();

        // push to origin
        pushResult = client.push().call();
        assertEquals(pushResult.getStatus(), PushResult.STATUS.OK_APPLIED);
        
        this.origin = new GeoGIT(createRepo(1, false));
        originHead = this.origin.getRepository().getHead();
        assertEquals(this.client.getRepository().getHead(), originHead);
    }
}
