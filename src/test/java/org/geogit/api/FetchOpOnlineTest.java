package org.geogit.api;

import java.util.ArrayList;
import java.util.List;

import org.geogit.test.MultipleRepositoryTestCase;
import org.opengis.feature.Feature;

/**
 * To test a remote fetch using the GeoGIT protocol
 * @author jhudson
 * @since 1.2.0
 */
public class FetchOpOnlineTest extends MultipleRepositoryTestCase {

    private GeoGIT server;

    private GeoGIT server2;

    private GeoGIT client;

    public FetchOpOnlineTest() {
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
        this.client.getRepository().close();
        this.server.getRepository().close();
        this.server2.getRepository().close();
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
        Ref clientRemoteMaster = this.client.getRepository().getRef(
                Ref.REMOTES_PREFIX + "project0/" + Ref.MASTER);
        assertEquals(clientRemoteMaster, null);
    }

    public void testFetchRemoteMasterTwoChanges() throws Exception {
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
        client.fetch().call();

        Ref clientRemoteMaster = this.client.getRepository().getRef(
                Ref.REMOTES_PREFIX + "project0/" + Ref.MASTER);

        // re-open the server
        this.server = new GeoGIT(createRepo(0, false));

        assertEquals(clientRemoteMaster.getObjectId(), this.server.getRepository().getHead().getObjectId());
    }

    public void testFetchRemoteMasterRetrieveFeature() throws Exception {
        ObjectId featureRefId1 = insert(this.server, points1);
        ObjectId featureRefId2 = insert(this.server, points2);
        ObjectId featureRefId3 = insert(this.server, lines1);
        ObjectId featureRefId4 = insert(this.server, lines2);
        RevCommit commit = server.commit().setMessage("commited a new feature").setAll(true).call();

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

        RevCommit serverCommitOnClient = this.client.getRepository().getCommit(commit.getId());
        assertNotNull("Fetch Op failed to transfer the commit from server to client",
                serverCommitOnClient);

        RevTree tree = this.client.getRepository().getTree(serverCommitOnClient.getTreeId());
        assertHasFeatuers(this.client, tree, 4);

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
    }
    
    public void testFetchOneRemoteNonFastForward() throws Exception {
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
    }

    public void testFetchRemoteMasterManyCommits() throws Exception {
        List<RevCommit> commits = new ArrayList<RevCommit>();
        List<ObjectId> featureIds = new ArrayList<ObjectId>();
        List<Feature> features = new ArrayList<Feature>();

        for (int i = 0; i < 10; i++) {
            Feature point = feature(pointsType, "Points." + i, "StringProp1_" + i + "",
                    new Integer(1000 * i), "POINT(" + i + " " + i + ")");
            featureIds.add(insert(this.server, point));
            features.add(point);
            commits.add(server.commit().setMessage("commited a new feature").setAll(true).call());
        }

        this.server.getRepository().close();

        // setup the client to have a remote ref to the server
        this.client.remoteAddOp().setName("project0").setFetch("project0")
                .setUrl("http://localhost:8080/geoserver/geogit/project0/geogit").call();

        // fetch the remotes
        client.fetch().call();

        // re-open the server
        this.server = new GeoGIT(createRepo(0, false));

        int index = 0;
        for (RevCommit commit : commits) {
            RevCommit serverCommitOnClient = this.client.getRepository().getCommit(commit.getId());
            assertNotNull("Fetch Op failed to transfer the commit from server to client",
                    serverCommitOnClient);

            RevTree tree = this.client.getRepository().getTree(serverCommitOnClient.getTreeId());
            assertHasFeatuers(this.client, tree, index + 1);

            Feature feature = client.getRepository().getFeature(features.get(index).getType(),
                    features.get(index).getIdentifier().getID(), featureIds.get(index));

            assertEquals(features.get(index), feature);
            index++;
        }
    }

    public void testFetchRemoteMasterManyChangesOneCommit() throws Exception {
        RevCommit commit;
        List<ObjectId> featureIds = new ArrayList<ObjectId>();
        List<Feature> features = new ArrayList<Feature>();

        for (int i = 0; i < 10; i++) {
            Feature point = feature(pointsType, "Points." + i, "StringProp1_" + i + "",
                    new Integer(1000 * i), "POINT(" + i + " " + i + ")");
            featureIds.add(insert(this.server, point));
            features.add(point);
        }

        commit = server.commit().setMessage("commited a new feature").setAll(true).call();

        this.server.getRepository().close();

        // setup the client to have a remote ref to the server
        this.client.remoteAddOp().setName("project0").setFetch("project0")
                .setUrl("http://localhost:8080/geoserver/geogit/project0/geogit").call();

        // fetch the remotes
        client.fetch().call();

        // re-open the server
        this.server = new GeoGIT(createRepo(0, false));

        RevCommit serverCommitOnClient = this.client.getRepository().getCommit(commit.getId());
        assertNotNull("Fetch Op failed to transfer the commit from server to client",
                serverCommitOnClient);

        RevTree tree = this.client.getRepository().getTree(serverCommitOnClient.getTreeId());
        assertHasFeatuers(this.client, tree, 10);
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

        this.server.getRepository().close();
        this.server2.getRepository().close();

        // setup the client to have a remote ref to the server
        this.client.remoteAddOp().setName("project0").setFetch("project0")
                .setUrl("http://localhost:8080/geoserver/geogit/project0/geogit").call();
        this.client.remoteAddOp().setName("project2").setFetch("project2")
                .setUrl("http://localhost:8080/geoserver/geogit/project2/geogit").call();

        // fetch the remotes
        client.fetch().call();

        // re-open the server
        this.server = new GeoGIT(createRepo(0, false));
        // re-open the server
        this.server2 = new GeoGIT(createRepo(2, false));

        Ref clientRemoteMaster1 = this.client.getRepository().getRef(
                Ref.REMOTES_PREFIX + "project0/" + Ref.MASTER);
        Ref clientRemoteMaster2 = this.client.getRepository().getRef(
                Ref.REMOTES_PREFIX + "project2/" + Ref.MASTER);

        assertEquals(clientRemoteMaster1.getObjectId(), this.server.getRepository().getHead().getObjectId());
        assertEquals(clientRemoteMaster2.getObjectId(), this.server2.getRepository().getHead().getObjectId());
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
