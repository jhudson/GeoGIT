package org.geogit.api;

import java.util.List;

import org.geogit.test.MultipleRepositoryTestCase;
import org.geotools.data.DataUtilities;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;

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
        client.merge().setComment("Merged remote, one commit merge").include(clientRemoteMaster).call();

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
        client.merge().setComment("Merged remote, simple").include(clientRemoteMaster).call();

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
        client.merge().setComment("Merged remote, criss cross test 1").include(clientRemoteMaster).call();

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
        client.merge().setComment("Merged remote, criss cross test 2").include(clientRemoteMaster).call();

        RevCommit clientHead = this.client.getRepository().getCommit(this.client.getRepository().getHead().getObjectId());
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
        client.merge().setComment("Merged remote, get features").include(clientRemoteMaster).call();

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

        assertEquals(clientRemoteMaster.getObjectId(), this.client.getRepository().getHead().getObjectId());
    }

    public void testMergeComplex() throws Exception {
//        insertAddCommit(this.server, points1);
//        insertAddCommit(this.server, points3);
//        this.server.getRepository().close();

        ObjectId featureRefId1 = insert(this.server, points1);
        ObjectId featureRefId2 = insert(this.server, points3);
        RevCommit serversLastCommit = server.commit().setMessage("commited a new feature").setAll(true).call();
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

        // add to client to server
        //insertAddCommit(this.client, points3_conflict);
        ObjectId featureRefId3 = insert(this.client, points3_modify);
        RevCommit clientsLastCommit = this.client.commit().setMessage("commited a modified feature").setAll(true).call();

        clientRemoteMaster = this.client.getRepository().getRef(Ref.REMOTES_PREFIX + "project0/" + Ref.MASTER);
        assertEquals(clientRemoteMaster.getObjectId(), this.server.getRepository().getHead().getObjectId());

        // merge the fetch
        MergeResult mergeResult = client.merge().include(clientRemoteMaster).call();

        RevCommit clientHead = this.client.getRepository().getCommit(this.client.getRepository().getHead().getObjectId());
        assertContains(clientHead.getParentIds(), clientsLastCommit, serversLastCommit);
        
        Feature feature1 = client.getRepository().getFeature(pointsType, points1.getIdentifier().getID(), featureRefId1);
        Feature feature2 = client.getRepository().getFeature(pointsType, points3.getIdentifier().getID(), featureRefId2);
        Feature feature3 = client.getRepository().getFeature(pointsType,  points3_modify.getIdentifier().getID(), featureRefId3);

        assertEquals(points1, feature1);
        assertEquals(points3, feature2);
        assertEquals(points3_modify, feature3);

        for (ObjectId id : mergeResult.getMerged()){
            Feature feature = this.client.getRepository().getFeature(pointsType, "Points.3", id);
            assertEquals(points3_modify, feature);
        }
    }

    private void assertContains(List<ObjectId> parentIds, RevCommit... commits) {
        for (RevCommit commit : commits) {
            assertTrue(parentIds.contains(commit.getId()));
        }
    }
}
