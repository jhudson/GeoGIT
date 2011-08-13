/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.api;

import static org.geogit.api.DiffEntry.ChangeType.ADD;
import static org.geogit.api.DiffEntry.ChangeType.DELETE;

import java.util.Arrays;
import java.util.List;

import org.geogit.api.DiffEntry.ChangeType;
import org.geogit.test.RepositoryTestCase;

/**
 * Unit test suite for {@link DiffOp}, must cover {@link DiffTreeWalk} too.
 * 
 * @author groldan
 * 
 */
public class DiffOpFilterByIdTest extends RepositoryTestCase {

    private GeoGIT ggit;

    private DiffOp diffOp;

    /**
     * records the addition of #points1 and #lines1
     */
    private RevCommit commit1;

    /**
     * records the addition of #points2 and #lines2
     */
    private RevCommit commit2;

    /**
     * records the delete of #points1
     */
    private RevCommit commit3;

    private ObjectId points2Id;

    private ObjectId lines2Id;

    private ObjectId commit1Id;

    private ObjectId commit2Id;

    private ObjectId commit3Id;

    private ObjectId points1Id;

    private ObjectId lines1Id;

    private List<String> P1Path, P2Path, L1Path, L2Path;

    @Override
    protected void setUpInternal() throws Exception {
        this.ggit = new GeoGIT(getRepository());
        this.diffOp = ggit.diff();

        points1Id = insertAndAdd(points1);
        lines1Id = insertAndAdd(lines1);
        commit1 = ggit.commit().setAll(true).call();

        points2Id = insertAndAdd(points2);
        lines2Id = insertAndAdd(lines2);
        commit2 = ggit.commit().setAll(true).call();

        deleteAndAdd(points1);
        commit3 = ggit.commit().setAll(true).call();

        commit1Id = commit1.getId();
        commit2Id = commit2.getId();
        commit3Id = commit3.getId();

        P1Path = Arrays.asList(pointsNs, pointsName, idP1);
        P2Path = Arrays.asList(pointsNs, pointsName, idP2);
        L1Path = Arrays.asList(linesNs, linesName, idL1);
        L2Path = Arrays.asList(linesNs, linesName, idL2);
    }

    public void testFilterByObjectId1() throws Exception {
        diffOp.setOldVersion(commit1Id).setNewVersion(commit2Id);
        diffOp.setFilter(points2Id);
        List<DiffEntry> diffs = toList(diffOp.call());
        assertEquals(1, diffs.size());

        assertDiff(diffs.get(0), ADD, commit1Id, commit2Id, ObjectId.NULL, points2Id, P2Path);
    }

    public void testFilterByObjectId_NoToVersion() throws Exception {
        diffOp.setOldVersion(commit1Id);
        // newVersion shall resolve to current head, i.e., commit3
        diffOp.setFilter(points2Id);
        List<DiffEntry> diffs = toList(diffOp.call());
        assertEquals(1, diffs.size());

        assertDiff(diffs.get(0), ADD, commit1Id, commit3Id, ObjectId.NULL, points2Id, P2Path);

        diffOp.setFilter(points1Id);
        diffs = toList(diffOp.call());
        assertEquals(1, diffs.size());

        assertDiff(diffs.get(0), DELETE, commit1Id, commit3Id, points1Id, ObjectId.NULL, P1Path);
    }

    public void testFilterByObjectId_InverseOrder() throws Exception {
        // set old and new version in inverse order than testFilterByObjectId_NoToVersion and expect
        // delete and add instead of add and delete
        diffOp.setOldVersion(commit3Id);
        diffOp.setNewVersion(commit1Id);

        diffOp.setFilter(points2Id);
        List<DiffEntry> diffs = toList(diffOp.call());
        assertEquals(1, diffs.size());

        assertDiff(diffs.get(0), DELETE, commit3Id, commit1Id, points2Id, ObjectId.NULL, P2Path);

        diffOp.setFilter(points1Id);
        diffs = toList(diffOp.call());
        assertEquals(1, diffs.size());

        assertDiff(diffs.get(0), ADD, commit3Id, commit1Id, ObjectId.NULL, points1Id, P1Path);
    }

    private void assertDiff(final DiffEntry entry, final ChangeType changeType,
            final ObjectId oldCommitId, final ObjectId newCommitId, final ObjectId oldObjectId,
            final ObjectId newObjectId, final List<String> path) {

        assertEquals(changeType, entry.getType());
        assertEquals(oldCommitId, entry.getOldCommitId());
        assertEquals(newCommitId, entry.getNewCommitId());
        assertEquals(newObjectId, entry.getNewObjectId());
        assertEquals(oldObjectId, entry.getOldObjectId());
        assertEquals(path, entry.getPath());
    }
}
