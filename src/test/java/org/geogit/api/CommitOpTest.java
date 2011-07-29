/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.util.List;

import org.geogit.repository.Index;
import org.geogit.storage.BLOBS;
import org.geogit.test.RepositoryTestCase;

public class CommitOpTest extends RepositoryTestCase {

    // used to prepare stuff to be committed
    private Index index;

    // used to get a CommitOp command
    private GeoGIT ggit;

    @Override
    protected void setUpInternal() throws Exception {
        this.index = getRepository().getIndex();
        this.ggit = new GeoGIT(getRepository());
    }

    public void testInitialCommit() throws Exception {
        try {
            ggit.add().addPattern(".").call();
            ggit.commit().setAuthor("groldan").call();
            fail("expected NothingToCommitException");
        } catch (NothingToCommitException e) {
            assertTrue(true);
        }

        Index index = repo.getIndex();

        ObjectId oid1 = insertAndAdd(feature1_1);
        // BLOBS.print(repo.getRawObject(insertedId1), System.err);

        ObjectId oid2 = insertAndAdd(feature1_2);
        // BLOBS.print(repo.getRawObject(insertedId2), System.err);

        ggit.add().addPattern(".").call();
        RevCommit commit = ggit.commit().setAuthor("groldan").call();
        assertNotNull(commit);
        assertNotNull(commit.getParentIds());
        assertEquals(1, commit.getParentIds().size());
        assertTrue(commit.getParentIds().get(0).isNull());
        assertNotNull(commit.getId());
        assertEquals("groldan", commit.getAuthor());

        ObjectId treeId = commit.getTreeId();
        // BLOBS.print(repo.getRawObject(treeId), System.err);

        assertNotNull(treeId);
        RevTree root = repo.getTree(treeId);
        assertNotNull(root);

        Ref nsTreeId = root.get(namespace1);
        assertNotNull(nsTreeId);
        // BLOBS.print(repo.getRawObject(nsTreeId), System.err);
        RevTree nstree = repo.getTree(nsTreeId.getObjectId());
        assertNotNull(nstree);

        Ref typeTreeId = nstree.get(typeName1);
        assertNotNull(typeTreeId);
        // BLOBS.print(repo.getRawObject(typeTreeId), System.err);
        RevTree typeTree = repo.getTree(typeTreeId.getObjectId());
        assertNotNull(typeTree);

        String featureId = feature1_1.getIdentifier().getID();
        Ref featureBlobId = typeTree.get(featureId);
        assertNotNull(featureBlobId);
        assertEquals(oid1, featureBlobId.getObjectId());

        Ref head = repo.getRef(Ref.HEAD);
        assertEquals(commit.getId(), head.getObjectId());
    }

    public void testMultipleCommits() throws Exception {

        final ObjectId oId1_1 = insertAndAdd(feature1_1);
        // BLOBS.print(repo.getRawObject(oId1_1), System.err);

        // final ObjectId oId2_1 = index.inserted(new FeaturePersister(feature2_1), namespace2,
        // typeName2, feature2_1.getIdentifier().getID());
        // BLOBS.print(repo.getRawObject(oId2_1), System.err);

        ggit.add().addPattern(".").call();
        final RevCommit commit1 = ggit.commit().setAuthor("groldan").call();
        assertNotNull(commit1);
        assertTrue(commit1.getParentIds().get(0).isNull());
        assertNotNull(commit1.getTreeId());
        assertNotNull(commit1.getId());
        assertEquals("groldan", commit1.getAuthor());
        assertNotNull(repo.getTree(commit1.getTreeId()));
        assertEquals(commit1.getId(), getRepository().getRef(Ref.HEAD).getObjectId());

        ObjectId type1SubTreeId = repo.getTreeChildId(namespace1, typeName1);
        ObjectId type2SubTreeId = repo.getTreeChildId(namespace2, typeName2);

        assertNotNull(type1SubTreeId);
        // assertNotNull(type2SubTreeId);

        final ObjectId oId1_2 = insertAndAdd(feature1_2);
        // BLOBS.print(repo.getRawObject(oId1_2), System.err);
        final ObjectId oId1_3 = insertAndAdd(feature1_3);
        // BLOBS.print(repo.getRawObject(oId1_3), System.err);

        // final ObjectId oId2_2 = index.inserted(new FeaturePersister(feature2_2), namespace2,
        // typeName2, feature2_2.getIdentifier().getID());
        // // BLOBS.print(repo.getRawObject(oId2_2), System.err);
        // final ObjectId oId2_3 = index.inserted(new FeaturePersister(feature2_3), namespace2,
        // typeName2, feature2_3.getIdentifier().getID());
        // // BLOBS.print(repo.getRawObject(oId2_3), System.err);

        ggit.add().addPattern(".").call();
        final RevCommit commit2 = ggit.commit().setAuthor("groldan").call();
        assertNotNull(commit2);
        List<ObjectId> parentIds = commit2.getParentIds();
        assertNotNull(parentIds);
        assertEquals(1, parentIds.size());
        assertEquals(commit1.getId(), parentIds.get(0));

        assertNotNull(commit2.getTreeId());
        assertFalse(commit1.getTreeId().equals(commit2.getTreeId()));
        BLOBS.print(repo.getRawObject(commit2.getTreeId()), System.err);
    }

}
