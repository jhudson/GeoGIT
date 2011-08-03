/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.geogit.api.DiffEntry.ChangeType;
import org.geogit.test.RepositoryTestCase;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Unit test suite for {@link DiffOp}, must cover {@link DiffTreeWalk} too.
 * 
 * @author groldan
 * 
 */
public class DiffOpTest extends RepositoryTestCase {

    private GeoGIT ggit;

    private DiffOp diffOp;

    @Override
    protected void setUpInternal() throws Exception {
        this.ggit = new GeoGIT(getRepository());
        this.diffOp = ggit.diff();
    }

    public void testDiffPreconditions() throws Exception {
        try {
            diffOp.call();
            fail("Expected ISE: old version not specified");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Old version"));
        }
        Iterator<DiffEntry> difflist = ggit.diff().setOldVersion(ObjectId.NULL).call();
        assertNotNull(difflist);
        assertFalse(difflist.hasNext());

        final ObjectId oid1 = insertAndAdd(points1);
        final RevCommit commit1_1 = ggit.commit().call();
        try {
            diffOp.setOldVersion(oid1).call();
            fail("Expected IAE as oldVersion is not a commit");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("oldVersion"));
            assertTrue(e.getMessage().contains("does not exist"));
        }
        try {
            diffOp.setOldVersion(commit1_1.getId()).setNewVersion(oid1).call();
            fail("Expected IAE as newVersion is not a commit");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("newVersion"));
            assertTrue(e.getMessage().contains("does not exist"));
        }
    }

    public void testEmptyRepo() throws Exception {
        Iterator<DiffEntry> difflist = diffOp.setOldVersion(ObjectId.NULL).call();
        assertNotNull(difflist);
        assertFalse(difflist.hasNext());
    }

    public void testNoChangeSameCommit() throws Exception {

        final ObjectId newOid = insertAndAdd(points1);
        final RevCommit commit = ggit.commit().setAll(true).call();

        assertFalse(diffOp.setOldVersion(commit.getId()).setNewVersion(commit.getId()).call()
                .hasNext());
    }

    public void testSingleAddition() throws Exception {

        final ObjectId newOid = insertAndAdd(points1);
        final RevCommit commit = ggit.commit().setAll(true).call();

        List<DiffEntry> difflist = toList(diffOp.setOldVersion(ObjectId.NULL).call());

        assertNotNull(difflist);
        assertEquals(1, difflist.size());
        DiffEntry de = difflist.get(0);

        List<String> expectedPath = Arrays.asList(pointsNs, pointsName, points1.getIdentifier()
                .getID());
        assertEquals(expectedPath, de.getPath());

        assertEquals(DiffEntry.ChangeType.ADD, de.getType());
        assertEquals(ObjectId.NULL, de.getOldObjectId());

        assertEquals(commit.getId(), de.getNewCommitId());
        assertEquals(newOid, de.getNewObjectId());

    }

    public void testSingleAdditionReverseOrder() throws Exception {

        final ObjectId newOid = insertAndAdd(points1);
        final RevCommit commit = ggit.commit().setAll(true).call();

        List<DiffEntry> difflist = toList(diffOp.setOldVersion(commit.getId())
                .setNewVersion(ObjectId.NULL).call());

        assertNotNull(difflist);
        assertEquals(1, difflist.size());
        DiffEntry de = difflist.get(0);

        assertEquals(DiffEntry.ChangeType.DELETE, de.getType());
        assertEquals(ObjectId.NULL, de.getNewCommitId());
        assertEquals(ObjectId.NULL, de.getNewObjectId());

        assertEquals(commit.getId(), de.getOldCommitId());
        assertEquals(newOid, de.getOldObjectId());
    }

    public void testSingleDeletion() throws Exception {
        final ObjectId featureContentId = insertAndAdd(points1);
        final RevCommit addCommit = ggit.commit().setAll(true).call();

        assertTrue(deleteAndAdd(points1));
        final RevCommit deleteCommit = ggit.commit().setAll(true).call();

        List<DiffEntry> difflist = toList(diffOp.setOldVersion(addCommit.getId())
                .setNewVersion(deleteCommit.getId()).call());

        final List<String> path = Arrays.asList(pointsNs, pointsName, points1.getIdentifier()
                .getID());

        assertNotNull(difflist);
        assertEquals(1, difflist.size());
        DiffEntry de = difflist.get(0);
        assertEquals(path, de.getPath());

        assertEquals(DiffEntry.ChangeType.DELETE, de.getType());

        assertEquals(addCommit.getId(), de.getOldCommitId());
        assertEquals(featureContentId, de.getOldObjectId());

        assertEquals(deleteCommit.getId(), de.getNewCommitId());
        assertEquals(ObjectId.NULL, de.getNewObjectId());
    }

    public void testSingleDeletionReverseOrder() throws Exception {

        final ObjectId featureContentId = insertAndAdd(points1);
        final RevCommit addCommit = ggit.commit().setAll(true).call();

        assertTrue(deleteAndAdd(points1));
        final RevCommit deleteCommit = ggit.commit().setAll(true).call();

        // set old/new version in reverse order
        List<DiffEntry> difflist = toList(diffOp.setOldVersion(deleteCommit.getId())
                .setNewVersion(addCommit.getId()).call());

        final List<String> path = Arrays.asList(pointsNs, pointsName, points1.getIdentifier()
                .getID());

        // then the diff should report an ADD instead of a DELETE
        assertNotNull(difflist);
        assertEquals(1, difflist.size());
        DiffEntry de = difflist.get(0);
        assertEquals(path, de.getPath());

        assertEquals(DiffEntry.ChangeType.ADD, de.getType());

        assertEquals(deleteCommit.getId(), de.getOldCommitId());
        assertEquals(ObjectId.NULL, de.getOldObjectId());

        assertEquals(addCommit.getId(), de.getNewCommitId());
        assertEquals(featureContentId, de.getNewObjectId());
    }

    public void testSingleModification() throws Exception {

        final ObjectId oldOid = insertAndAdd(points1);
        final RevCommit insertCommit = ggit.commit().setAll(true).call();

        final String featureId = points1.getIdentifier().getID();
        final Feature modifiedFeature = feature((SimpleFeatureType) points1.getType(), featureId,
                "changedProp", new Integer(1500), null);

        final ObjectId newOid = insertAndAdd(modifiedFeature);

        final RevCommit changeCommit = ggit.commit().setAll(true).call();

        List<DiffEntry> difflist = toList(diffOp.setOldVersion(insertCommit.getId())
                .setNewVersion(changeCommit.getId()).call());

        assertNotNull(difflist);
        assertEquals(1, difflist.size());
        DiffEntry de = difflist.get(0);
        List<String> expectedPath = Arrays.asList(pointsNs, pointsName, featureId);
        assertEquals(expectedPath, de.getPath());

        assertEquals(DiffEntry.ChangeType.MODIFY, de.getType());
        assertEquals(insertCommit.getId(), de.getOldCommitId());
        assertEquals(oldOid, de.getOldObjectId());

        assertEquals(changeCommit.getId(), de.getNewCommitId());
        assertEquals(newOid, de.getNewObjectId());
    }

    public void testFilterNamespaceNoChanges() throws Exception {

        // two commits on different trees
        insertAndAdd(points1);
        final RevCommit commit1 = ggit.commit().setAll(true).call();

        insertAndAdd(lines1);
        final RevCommit commit2 = ggit.commit().setAll(true).call();

        diffOp.setOldVersion(commit1.getId()).setNewVersion(commit2.getId());
        diffOp.setFilter(pointsNs);

        Iterator<DiffEntry> diffs = diffOp.call();
        assertFalse(diffs.hasNext());
    }

    public void testFilterTypeNameNoChanges() throws Exception {

        // two commits on different trees
        insertAndAdd(points1);
        final RevCommit commit1 = ggit.commit().setAll(true).call();

        insertAndAdd(lines1);
        final RevCommit commit2 = ggit.commit().setAll(true).call();

        diffOp.setOldVersion(commit1.getId()).setNewVersion(commit2.getId());
        diffOp.setFilter(pointsNs, pointsName);

        Iterator<DiffEntry> diffs = diffOp.call();
        assertFalse(diffs.hasNext());
    }

    public void testFilterDidntMatchAnything() throws Exception {

        // two commits on different trees
        insertAndAdd(points1);
        final RevCommit commit1 = ggit.commit().setAll(true).call();

        insertAndAdd(lines1);
        final RevCommit commit2 = ggit.commit().setAll(true).call();

        // set a filter that doesn't produce any match

        diffOp.setOldVersion(commit1.getId()).setNewVersion(commit2.getId());
        diffOp.setFilter(pointsNs, pointsName, "nonExistentId");

        Iterator<DiffEntry> diffs = diffOp.call();
        assertNotNull(diffs);
        assertFalse(diffs.hasNext());
    }

    public void testFilterFeatureIdNoChanges() throws Exception {

        // two commits on different trees
        insertAndAdd(points1);
        final RevCommit commit1 = ggit.commit().setAll(true).call();

        insertAndAdd(lines1);
        final RevCommit commit2 = ggit.commit().setAll(true).call();

        // filter on feature1_1, it didn't change between commit2 and commit1

        diffOp.setOldVersion(commit1.getId()).setNewVersion(commit2.getId());
        diffOp.setFilter(pointsNs, pointsName, points1.getIdentifier().getID());

        Iterator<DiffEntry> diffs = diffOp.call();
        assertFalse(diffs.hasNext());
    }

    public void testFilterMatchesSingleBlobChange() throws Exception {
        final ObjectId initialOid = insertAndAdd(points1);
        final RevCommit commit1 = ggit.commit().setAll(true).call();

        insertAndAdd(lines1);
        final RevCommit commit2 = ggit.commit().setAll(true).call();

        ((SimpleFeature) points1).setAttribute("sp", "modified");
        final ObjectId modifiedOid = insertAndAdd(points1);
        final RevCommit commit3 = ggit.commit().setAll(true).call();

        diffOp.setOldVersion(commit1.getId()).setNewVersion(commit3.getId());
        diffOp.setFilter(pointsNs, pointsName, points1.getIdentifier().getID());

        List<DiffEntry> diffs;
        DiffEntry diff;

        diffs = toList(diffOp.call());
        assertEquals(1, diffs.size());
        diff = diffs.get(0);
        assertEquals(ChangeType.MODIFY, diff.getType());
        assertEquals(initialOid, diff.getOldObjectId());
        assertEquals(modifiedOid, diff.getNewObjectId());

        assertTrue(deleteAndAdd(points1));
        final RevCommit commit4 = ggit.commit().setAll(true).call();
        diffOp.setOldVersion(commit2.getId()).setNewVersion(commit4.getId());
        diffOp.setFilter(pointsNs, pointsName, points1.getIdentifier().getID());
        diffs = toList(diffOp.call());
        assertEquals(1, diffs.size());
        diff = diffs.get(0);
        assertEquals(ChangeType.DELETE, diff.getType());
        assertEquals(initialOid, diff.getOldObjectId());
        assertEquals(ObjectId.NULL, diff.getNewObjectId());

        // invert the order of old and new commit
        diffOp.setOldVersion(commit4.getId()).setNewVersion(commit1.getId());
        diffOp.setFilter(pointsNs, pointsName, points1.getIdentifier().getID());
        diffs = toList(diffOp.call());
        assertEquals(1, diffs.size());
        diff = diffs.get(0);
        assertEquals(ChangeType.ADD, diff.getType());
        assertEquals(ObjectId.NULL, diff.getOldObjectId());
        assertEquals(initialOid, diff.getNewObjectId());

        // different commit range
        diffOp.setOldVersion(commit4.getId()).setNewVersion(commit3.getId());
        diffOp.setFilter(pointsNs, pointsName, points1.getIdentifier().getID());
        diffs = toList(diffOp.call());
        assertEquals(1, diffs.size());
        diff = diffs.get(0);
        assertEquals(ChangeType.ADD, diff.getType());
        assertEquals(ObjectId.NULL, diff.getOldObjectId());
        assertEquals(modifiedOid, diff.getNewObjectId());
    }

    public void testFilterAddressesNamespaceTree() throws Exception {

        // two commits on different trees
        final ObjectId oid11 = insertAndAdd(points1);
        final ObjectId oid12 = insertAndAdd(points2);
        final RevCommit commit1 = ggit.commit().setAll(true).call();

        final ObjectId oid21 = insertAndAdd(lines1);
        final ObjectId oid22 = insertAndAdd(lines2);
        final RevCommit commit2 = ggit.commit().setAll(true).call();

        List<DiffEntry> diffs;

        // filter on namespace1, no changes between commit1 and commit2
        diffOp.setOldVersion(commit1.getId());
        diffOp.setFilter(pointsNs);

        diffs = toList(diffOp.call());
        assertEquals(0, diffs.size());

        // filter on namespace2, all additions between commit1 and commit2
        diffOp.setOldVersion(commit1.getId());
        diffOp.setFilter(linesNs);

        diffs = toList(diffOp.call());
        assertEquals(2, diffs.size());
        assertEquals(ChangeType.ADD, diffs.get(0).getType());
        assertEquals(ChangeType.ADD, diffs.get(1).getType());

        assertEquals(ObjectId.NULL, diffs.get(0).getOldObjectId());
        assertEquals(ObjectId.NULL, diffs.get(1).getOldObjectId());

        // don't care about order
        Set<ObjectId> expected = new HashSet<ObjectId>();
        expected.add(oid21);
        expected.add(oid22);
        Set<ObjectId> actual = new HashSet<ObjectId>();
        actual.add(diffs.get(0).getNewObjectId());
        actual.add(diffs.get(1).getNewObjectId());
        assertEquals(expected, actual);
    }

    public void testMultipleDeletes() throws Exception {

        // two commits on different trees
        final ObjectId oid11 = insertAndAdd(points1);
        final ObjectId oid12 = insertAndAdd(points2);
        final ObjectId oid13 = insertAndAdd(points3);
        final RevCommit commit1 = ggit.commit().setAll(true).call();

        final ObjectId oid21 = insertAndAdd(lines1);
        final RevCommit commit2 = ggit.commit().setAll(true).call();

        deleteAndAdd(points1);
        deleteAndAdd(points3);
        final RevCommit commit3 = ggit.commit().setAll(true).call();

        List<DiffEntry> diffs;

        // filter on namespace1, no changes between commit1 and commit2
        diffOp.setOldVersion(commit1.getId()).setNewVersion(commit3.getId());
        diffOp.setFilter(pointsNs);

        diffs = toList(diffOp.call());
        assertEquals(2, diffs.size());
        assertEquals(ChangeType.DELETE, diffs.get(0).getType());
        assertEquals(ChangeType.DELETE, diffs.get(1).getType());

        assertEquals(oid11, diffs.get(0).getOldObjectId());
        assertEquals(oid13, diffs.get(1).getOldObjectId());
    }

    public void testTreeDeletes() throws Exception {

        // two commits on different trees
        final ObjectId oid11 = insertAndAdd(points1);
        final ObjectId oid12 = insertAndAdd(points2);
        final ObjectId oid13 = insertAndAdd(points3);
        final RevCommit commit1 = ggit.commit().setAll(true).call();

        final ObjectId oid21 = insertAndAdd(lines1);
        final ObjectId oid22 = insertAndAdd(lines2);
        final RevCommit commit2 = ggit.commit().setAll(true).call();

        deleteAndAdd(points1);
        deleteAndAdd(points2);
        deleteAndAdd(points3);
        final RevCommit commit3 = ggit.commit().setAll(true).call();

        List<DiffEntry> diffs;

        // filter on namespace1, no changes between commit1 and commit2
        diffOp.setOldVersion(commit1.getId());
        diffOp.setFilter(pointsNs);

        diffs = toList(diffOp.call());
        assertEquals(3, diffs.size());
    }
}
