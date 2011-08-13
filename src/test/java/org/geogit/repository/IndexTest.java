/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import java.io.PrintWriter;

import org.geogit.api.ObjectId;
import org.geogit.api.PrintVisitor;
import org.geogit.api.Ref;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.StagingDatabase;
import org.geogit.test.RepositoryTestCase;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.BoundingBox;

public class IndexTest extends RepositoryTestCase {

    private Index index;

    @Override
    protected void setUpInternal() throws Exception {
        index = repo.getIndex();
    }

    // two features with the same content and different fid should point to the same object
    public void testInsertIdenticalObjects() throws Exception {
        ObjectId oId1 = insertAndAdd(points1);
        Feature equalContentFeature = feature(pointsType, "DifferentId", ((SimpleFeature) points1)
                .getAttributes().toArray());

        ObjectId oId2 = insertAndAdd(equalContentFeature);

        // BLOBS.print(repo.getRawObject(insertedId1), System.err);
        // BLOBS.print(repo.getRawObject(insertedId2), System.err);
        assertNotNull(oId1);
        assertNotNull(oId2);
        assertEquals(oId1, oId2);
    }

    // two features with different content should point to different objects
    public void testInsertNonEqualObjects() throws Exception {
        ObjectId oId1 = insertAndAdd(points1);

        ObjectId oId2 = insertAndAdd(points2);
        assertNotNull(oId1);
        assertNotNull(oId2);
        assertFalse(oId1.equals(oId2));
    }

    public void testCreateEmptyTrees() throws Exception {
        String[] path1 = { "root", "to", "path1" };
        String[] path2 = { "root", "to", "path2" };
        String[] path3 = { "root", "to2", "path3" };

        index.created(path1);
        index.created(path2);
        index.created(path3);

        RevTree unstagedRoot = index.getUnstaged();
        Ref emptyTreeId1 = index.getDatabase().getTreeChild(unstagedRoot, path1);
        Ref emptyTreeId2 = index.getDatabase().getTreeChild(unstagedRoot, path1);
        Ref emptyTreeId3 = index.getDatabase().getTreeChild(unstagedRoot, path1);

        assertNotNull(emptyTreeId1);
        assertNotNull(emptyTreeId2);
        assertNotNull(emptyTreeId3);

        assertEquals(emptyTreeId1.getObjectId(), emptyTreeId2.getObjectId());
        assertEquals(emptyTreeId1.getObjectId(), emptyTreeId3.getObjectId());

        // index.writeTree(repo.newObjectInserter());
        //
        // RevTree root = repo.getRootTree();
        // assertEquals(emptyTreeId1, repo.getObjectDatabase().getTreeChild(root, path1));
        // assertEquals(emptyTreeId2, repo.getObjectDatabase().getTreeChild(root, path2));
        // assertEquals(emptyTreeId3, repo.getObjectDatabase().getTreeChild(root, path3));
    }

    public void testWriteTree() throws Exception {
        assertEquals(repo.getRootTreeId(), index.getDatabase().getStagedRootRef().getObjectId());

        insertAndAdd(points1, lines1);

        Tuple<ObjectId, BoundingBox> result = index.writeTree(repo.getHead());

        // this new root tree must exist on the repo db, but is not set as the current head. In
        // fact, it is headless, as there's no commit pointing to it. CommitOp does that.
        ObjectId newRootTreeId = result.getFirst();

        assertNotNull(newRootTreeId);
        assertFalse(repo.getRootTreeId().equals(newRootTreeId));
        // but the index staged root shall be pointing to it
        assertEquals(newRootTreeId, index.getStaged().getId());

        RevTree tree = repo.getTree(newRootTreeId);
        assertEquals(2, tree.size().intValue());

        ObjectDatabase odb = repo.getObjectDatabase();

        assertNotNull(odb.getTreeChild(tree, pointsNs, pointsName, points1.getIdentifier().getID()));

        assertNotNull(odb.getTreeChild(tree, linesNs, linesName, lines1.getIdentifier().getID()));

        index.deleted(linesNs, linesName, lines1.getIdentifier().getID());
        index.stage(null);

        result = index.writeTree(new Ref("", newRootTreeId, TYPE.TREE));

        newRootTreeId = result.getFirst();
        assertNotNull(newRootTreeId);
        assertFalse(repo.getRootTreeId().equals(newRootTreeId));

        tree = repo.getTree(newRootTreeId);
        assertNotNull(odb.getTreeChild(tree, pointsNs, pointsName, points1.getIdentifier().getID()));

        assertNull(odb.getTreeChild(tree, linesNs, linesName, lines1.getIdentifier().getID()));

    }

    public void testMultipleStaging() throws Exception {

        final StagingDatabase indexDb = index.getDatabase();

        // insert and commit feature1_1
        final ObjectId oId1_1 = insertAndAdd(points1);

        final RevTree staged1 = index.getStaged();
        System.err.println("++++++++++++ stage 1: " + staged1.getId() + " ++++++++++++++++++++");
        staged1.accept(new PrintVisitor(index.getDatabase(), new PrintWriter(System.err)));
        // check feature1_1 is there
        assertEquals(oId1_1, indexDb.getTreeChild(staged1, pointsNs, pointsName, idP1)
                .getObjectId());

        // insert and commit feature1_2, feature1_2 and feature2_1
        final ObjectId oId1_2 = insertAndAdd(points2);
        final ObjectId oId1_3 = insertAndAdd(points3);
        final ObjectId oId2_1 = insertAndAdd(lines1);

        final RevTree staged2 = index.getStaged();
        System.err.println("++++++++++++ stage 2:" + staged2.getId() + " ++++++++++++++++++++");
        staged2.accept(new PrintVisitor(index.getDatabase(), new PrintWriter(System.err)));

        // check feature1_2, feature1_2 and feature2_1
        Ref treeChild;
        assertNotNull(treeChild = indexDb.getTreeChild(staged2, pointsNs, pointsName, idP2));
        assertEquals(oId1_2, treeChild.getObjectId());

        assertNotNull(treeChild = indexDb.getTreeChild(staged2, pointsNs, pointsName, idP3));
        assertEquals(oId1_3, treeChild.getObjectId());

        assertNotNull(treeChild = indexDb.getTreeChild(staged2, linesNs, linesName, idL3));
        assertEquals(oId2_1, treeChild.getObjectId());

        // as well as feature1_1 from the previous commit
        assertNotNull(treeChild = indexDb.getTreeChild(staged2, pointsNs, pointsName, idP1));
        assertEquals(oId1_1, treeChild.getObjectId());

        // delete feature1_1, feature1_3, and feature2_1
        assertTrue(deleteAndAdd(points1));
        assertTrue(deleteAndAdd(points3));
        assertTrue(deleteAndAdd(lines1));
        // and insert feature2_2
        final ObjectId oId2_2 = insertAndAdd(lines2);

        final RevTree staged3 = index.getStaged();
        System.err.println("++++++++++++ stage 3:" + staged3.getId() + " ++++++++++++++++++++");
        staged3.accept(new PrintVisitor(index.getDatabase(), new PrintWriter(System.err)));

        // and check only feature1_2 and feature2_2 remain
        assertNull(indexDb.getTreeChild(staged3, pointsNs, pointsName, idP1));
        assertNull(indexDb.getTreeChild(staged3, pointsNs, pointsName, idP3));
        assertNull(indexDb.getTreeChild(staged3, linesNs, linesName, idL3));

        assertEquals(oId1_2, indexDb.getTreeChild(staged3, pointsNs, pointsName, idP2)
                .getObjectId());
        assertEquals(oId2_2, indexDb.getTreeChild(staged3, linesNs, linesName, idL2).getObjectId());

    }

    public void testWriteTree2() throws Exception {

        final ObjectDatabase repoDb = repo.getObjectDatabase();

        // insert and commit feature1_1
        final ObjectId oId1_1 = insertAndAdd(points1);

        final ObjectId newRepoTreeId1;
        {
            Tuple<ObjectId, BoundingBox> writeResult;
            writeResult = index.writeTree(repo.getHead());

            newRepoTreeId1 = writeResult.getFirst();
            assertEquals(index.getDatabase().getStagedRootRef().getObjectId(), newRepoTreeId1);

            RevTree newRepoTree = repoDb.getTree(newRepoTreeId1);

            System.err.println("++++++++++ new repo tree 1: " + newRepoTreeId1 + " ++++++++++++");
            newRepoTree.accept(new PrintVisitor(repoDb, new PrintWriter(System.err)));
            // check feature1_1 is there
            assertEquals(oId1_1, repoDb.getTreeChild(newRepoTree, pointsNs, pointsName, idP1)
                    .getObjectId());

        }

        // insert and add (stage) points2, points3, and lines1
        final ObjectId oId1_2 = insertAndAdd(points2);
        final ObjectId oId1_3 = insertAndAdd(points3);
        final ObjectId oId2_1 = insertAndAdd(lines1);

        final ObjectId newRepoTreeId2;
        {
            Tuple<ObjectId, BoundingBox> writeResult;
            // write comparing the the previously generated tree instead of the repository HEAD, as
            // it was not updated (no commit op was performed)
            Ref repoRootRef = new Ref("", newRepoTreeId1, TYPE.TREE);
            writeResult = index.writeTree(repoRootRef);

            newRepoTreeId2 = writeResult.getFirst();
            assertEquals(index.getDatabase().getStagedRootRef().getObjectId(), newRepoTreeId2);

            System.err.println("++++++++ new root 2:" + newRepoTreeId2 + " ++++++++++");
            RevTree newRepoTree = repoDb.getTree(newRepoTreeId2);

            newRepoTree.accept(new PrintVisitor(repoDb, new PrintWriter(System.err)));

            // check feature1_2, feature1_2 and feature2_1
            Ref treeChild;
            assertNotNull(treeChild = repoDb.getTreeChild(newRepoTree, pointsNs, pointsName, idP2));
            assertEquals(oId1_2, treeChild.getObjectId());

            assertNotNull(treeChild = repoDb.getTreeChild(newRepoTree, pointsNs, pointsName, idP3));
            assertEquals(oId1_3, treeChild.getObjectId());

            assertNotNull(treeChild = repoDb.getTreeChild(newRepoTree, linesNs, linesName, idL3));
            assertEquals(oId2_1, treeChild.getObjectId());

            // as well as feature1_1 from the previous commit
            assertNotNull(treeChild = repoDb.getTreeChild(newRepoTree, pointsNs, pointsName, idP1));
            assertEquals(oId1_1, treeChild.getObjectId());
        }

        // delete feature1_1, feature1_3, and feature2_1
        assertTrue(deleteAndAdd(points1));
        assertTrue(deleteAndAdd(points3));
        assertTrue(deleteAndAdd(lines1));
        // and insert feature2_2
        final ObjectId oId2_2 = insertAndAdd(lines2);

        final ObjectId newRepoTreeId3;
        {
            Tuple<ObjectId, BoundingBox> writeResult;
            // write comparing the the previously generated tree instead of the repository HEAD, as
            // it was not updated (no commit op was performed)
            Ref repoRootRef = new Ref("", newRepoTreeId2, TYPE.TREE);
            writeResult = index.writeTree(repoRootRef);

            newRepoTreeId3 = writeResult.getFirst();
            assertEquals(index.getDatabase().getStagedRootRef().getObjectId(), newRepoTreeId3);

            System.err.println("++++++++ new root 3:" + newRepoTreeId3 + " ++++++++++");
            RevTree newRepoTree = repoDb.getTree(newRepoTreeId3);

            newRepoTree.accept(new PrintVisitor(repoDb, new PrintWriter(System.err)));

            // and check only feature1_2 and feature2_2 remain
            assertNull(repoDb.getTreeChild(newRepoTree, pointsNs, pointsName, idP1));
            assertNull(repoDb.getTreeChild(newRepoTree, pointsNs, pointsName, idP3));
            assertNull(repoDb.getTreeChild(newRepoTree, linesNs, linesName, idL3));

            assertEquals(oId1_2, repoDb.getTreeChild(newRepoTree, pointsNs, pointsName, idP2)
                    .getObjectId());
            assertEquals(oId2_2, repoDb.getTreeChild(newRepoTree, linesNs, linesName, idL2)
                    .getObjectId());
        }
    }

}
