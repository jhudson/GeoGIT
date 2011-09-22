package org.geogit.api;

import java.util.List;

import org.geogit.test.MultipleRepositoryTestCase;
import org.opengis.feature.Feature;

public class MergeOpTest extends MultipleRepositoryTestCase {

    private GeoGIT server;

    private GeoGIT server2;

    private GeoGIT client;

    public MergeOpTest() {
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

    public void testOneCommitMerge() throws Exception {
        // setup the client to have a remote ref to the server
        this.client.remoteAddOp().setName("project0").setFetch("project0")
                .setUrl("http://localhost:8080/geoserver/geogit/project0/geogit").call();

        /**
         * INSERT 1 - SERVER
         */
        insertAddCommit(this.server, points1);
        this.server.getRepository().close();

        // fetch the remotes
        client.fetch().call();

        // re-open the server
        this.server = new GeoGIT(createRepo(0, false));

        Ref clientRemoteMaster = this.client.getRepository().getRef(
                Ref.REMOTES_PREFIX + "project0/" + Ref.MASTER);
        assertEquals(clientRemoteMaster.getObjectId(), this.server.getRepository().getHead()
                .getObjectId());

        /**
         * MERGE - REBASE
         */
        client.merge().include(clientRemoteMaster).call();

        assertEquals(this.client.getRepository().getHead().getObjectId(), this.server
                .getRepository().getHead().getObjectId());
    }

    public void testMergeOneRemoteNonFastForward() throws Exception {
        insertAddCommit(this.server, points1);
        insertAddCommit(this.server, points3);
        this.server.getRepository().close();

        // setup the client to have a remote ref to the server
        this.client.remoteAddOp().setName("project0").setFetch("project0")
                .setUrl("http://localhost:8080/geoserver/geogit/project0/geogit").call();

        // fetch the remotes
        client.fetch().call();

        // re-open the server
        this.server = new GeoGIT(createRepo(0, false));

        Ref clientRemoteMaster = this.client.getRepository().getRef(
                Ref.REMOTES_PREFIX + "project0/" + Ref.MASTER);
        assertEquals(clientRemoteMaster.getObjectId(), this.server.getRepository().getHead()
                .getObjectId());

        printHeads();

        // add more to server
        insertAddCommit(this.server, lines1);
        insertAddCommit(this.server, lines2);
        insertAddCommit(this.server, lines3);
        this.server.getRepository().close();

        // fetch the remotes
        client.fetch().call();

        // re-open the server
        this.server = new GeoGIT(createRepo(0, false));

        clientRemoteMaster = this.client.getRepository().getRef(
                Ref.REMOTES_PREFIX + "project0/" + Ref.MASTER);

        assertEquals(clientRemoteMaster.getObjectId(), this.server.getRepository().getHead()
                .getObjectId());

        // merge the fetch
        client.merge().include(clientRemoteMaster).call();

        assertEquals(clientRemoteMaster.getObjectId(), this.client.getRepository().getHead()
                .getObjectId());
    }

    public void testMergeCrissCrossTest() throws Exception {
        // setup the client to have a remote ref to the server
        this.client.remoteAddOp().setName("project0").setFetch("project0")
                .setUrl("http://localhost:8080/geoserver/geogit/project0/geogit").call();

        /**
         * INSERT 1 - SERVER
         */
        insertAddCommit(this.server, points1);
        this.server.getRepository().close();

        // fetch the remotes
        client.fetch().call();

        // re-open the server
        this.server = new GeoGIT(createRepo(0, false));

        Ref clientRemoteMaster = this.client.getRepository().getRef(
                Ref.REMOTES_PREFIX + "project0/" + Ref.MASTER);
        assertEquals(clientRemoteMaster.getObjectId(), this.server.getRepository().getHead()
                .getObjectId());

        /**
         * MERGE - REBASE
         */
        client.merge().include(clientRemoteMaster).call();

        /**
         * INSERT 1 - CLIENT
         */
        insertAddCommit(this.client, points2);

        /**
         * INSEERT 2 - SERVER
         */
        insertAddCommit(this.server, lines1);
        this.server.getRepository().close();

        // fetch the remotes
        client.fetch().call();

        // re-open the server
        this.server = new GeoGIT(createRepo(0, false));

        clientRemoteMaster = this.client.getRepository().getRef(
                Ref.REMOTES_PREFIX + "project0/" + Ref.MASTER);
        assertEquals(clientRemoteMaster.getObjectId(), this.server.getRepository().getHead()
                .getObjectId());

        // merge the fetch
        client.merge().include(clientRemoteMaster).call();

        /**
         * INSERT 2 - CLIENT
         */
        RevCommit clientsLastCommit = insertAddCommit(this.client, points3);

        /**
         * INSEERT 3 - SERVER
         */
        RevCommit serversLastCommit = insertAddCommit(this.server, lines3);
        this.server.getRepository().close();

        // fetch the remotes
        client.fetch().call();

        // re-open the server
        this.server = new GeoGIT(createRepo(0, false));

        clientRemoteMaster = this.client.getRepository().getRef(
                Ref.REMOTES_PREFIX + "project0/" + Ref.MASTER);
        assertEquals(clientRemoteMaster.getObjectId(), this.server.getRepository().getHead()
                .getObjectId());

        // merge the fetch
        client.merge().include(clientRemoteMaster).call();

        RevCommit clientHead = this.client.getRepository().getCommit(
                this.client.getRepository().getHead().getObjectId());
        assertContains(clientHead.getParentIds(), clientsLastCommit, serversLastCommit);
    }

    public void testMergeRemoteMasterRetrieveFeature() throws Exception {
        ObjectId featureRefId1 = insert(this.server, points1);
        ObjectId featureRefId2 = insert(this.server, points2);
        ObjectId featureRefId3 = insert(this.server, lines1);
        ObjectId featureRefId4 = insert(this.server, lines2);
        server.commit().setMessage("commited a new feature").setAll(true).call();
        this.server.getRepository().close();

        // setup the client to have a remote ref to the server
        this.client.remoteAddOp().setName("project0").setFetch("project0")
                .setUrl("http://localhost:8080/geoserver/geogit/project0/geogit").call();

        // fetch the remotes
        client.fetch().call();

        // re-open the server
        this.server = new GeoGIT(createRepo(0, false));

        Ref clientRemoteMaster = this.client.getRepository().getRef(
                Ref.REMOTES_PREFIX + "project0/" + Ref.MASTER);
        assertEquals(clientRemoteMaster.getObjectId(), this.server.getRepository().getHead()
                .getObjectId());

        // merge the fetch
        client.merge().include(clientRemoteMaster).call();

        Feature feature1 = client.getRepository().getFeature(points1.getType(),
                points1.getIdentifier().getID(), featureRefId1);
        Feature feature2 = client.getRepository().getFeature(points2.getType(),
                points2.getIdentifier().getID(), featureRefId2);
        Feature feature3 = client.getRepository().getFeature(lines1.getType(),
                lines1.getIdentifier().getID(), featureRefId3);
        Feature feature4 = client.getRepository().getFeature(lines2.getType(),
                lines2.getIdentifier().getID(), featureRefId4);

        assertEquals(points1, feature1);
        assertEquals(points2, feature2);
        assertEquals(lines1, feature3);
        assertEquals(lines2, feature4);

        assertEquals(clientRemoteMaster.getObjectId(), this.client.getRepository().getHead()
                .getObjectId());

    }

    private void assertContains(List<ObjectId> parentIds, RevCommit... commits) {
        for (RevCommit commit : commits) {
            assertTrue(parentIds.contains(commit.getId()));
        }
    }

    private void assertHasFeatuers(final GeoGIT ggit, final RevTree tree, final int expected) {
        final int found[] = new int[1];
        tree.accept(new TreeVisitor() {

            @Override
            public boolean visitSubTree(int bucket, ObjectId treeId) {
                RevTree tree = ggit.getRepository().getTree(treeId);
                tree.accept(this);
                return true;
            }

            @Override
            public boolean visitEntry(Ref ref) {
                if (ref.getType().equals(RevObject.TYPE.TREE)) {
                    RevTree tree = ggit.getRepository().getTree(ref.getObjectId());
                    tree.accept(this);
                } else {

                    RevBlob blob = (RevBlob) client.getRepository().getBlob(ref.getObjectId());

                    // Feature theFeature = ggit.getRepository().getFeature(feature.getType(),
                    // feature.getIdentifier().getID(), blob.getId());
                    // assertNotNull(theFeature);
                    // assertEquals(feature, theFeature);

                    found[0]++;/* hax */
                }
                return true;
            }
        });
        assertEquals(expected, found[0]);
    }
}
