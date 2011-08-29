/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.geogit.repository.StagingArea;
import org.geogit.test.RepositoryTestCase;
import org.geotools.util.Range;
import org.opengis.feature.Feature;

import com.google.common.collect.Iterators;

public class LogOpTest extends RepositoryTestCase {

    private GeoGIT ggit;

    private LogOp logOp;

    private StagingArea index;

    @Override
    protected void setUpInternal() throws Exception {
        ggit = new GeoGIT(repo);
        logOp = ggit.log();
        index = ggit.getRepository().getIndex();
    }

    public void testEmptyRepo() throws Exception {
        Iterator<RevCommit> logs = logOp.call();
        assertNotNull(logs);
        assertFalse(logs.hasNext());
    }

    public void testHeadWithSingleCommit() throws Exception {

        insertAndAdd(points1);
        final RevCommit firstCommit = ggit.commit().call();

        Iterator<RevCommit> iterator = logOp.call();
        assertNotNull(iterator);

        assertTrue(iterator.hasNext());
        assertEquals(firstCommit, iterator.next());
        assertFalse(iterator.hasNext());
    }

    public void testHeadWithTwoCommits() throws Exception {

        insertAndAdd(points1);
        final RevCommit firstCommit = ggit.commit().call();

        insertAndAdd(lines1);
        final RevCommit secondCommit = ggit.commit().call();

        Iterator<RevCommit> iterator = logOp.call();
        assertNotNull(iterator);

        assertTrue(iterator.hasNext());
        // by default returns most recent first
        assertEquals(secondCommit, iterator.next());

        assertTrue(iterator.hasNext());
        assertEquals(firstCommit, iterator.next());

        assertFalse(iterator.hasNext());
    }

    public void testHeadWithMultipleCommits() throws Exception {

        List<Feature> features = Arrays.asList(points1, lines1, points2, lines2, points3, lines3);
        LinkedList<RevCommit> expected = new LinkedList<RevCommit>();

        for (Feature f : features) {
            insertAndAdd(f);
            final RevCommit commit = ggit.commit().call();
            expected.addFirst(commit);
        }

        Iterator<RevCommit> logs = logOp.call();
        List<RevCommit> logged = new ArrayList<RevCommit>();
        for (; logs.hasNext();) {
            logged.add(logs.next());
        }

        assertEquals(expected, logged);
    }

    public void testPathFilterSingleFeature() throws Exception {

        List<Feature> features = Arrays.asList(points1, lines1, points2, lines2, points3, lines3);

        RevCommit expectedCommit = null;

        for (Feature f : features) {
            insertAndAdd(f);
            String id = f.getIdentifier().getID();
            final RevCommit commit = ggit.commit().call();
            if (id.equals(lines1.getIdentifier().getID())) {
                expectedCommit = commit;
            }
        }

        String[] path = { linesNs, linesName, lines1.getIdentifier().getID() };

        List<RevCommit> feature2_1Commits = toList(logOp.addPath(path).call());
        assertEquals(1, feature2_1Commits.size());
        assertEquals(Collections.singletonList(expectedCommit), feature2_1Commits);
    }

    public void testPathFilterByTypeName() throws Exception {

        List<Feature> features = Arrays.asList(points1, lines1, points2, lines2, points3, lines3);
        LinkedList<RevCommit> commits = new LinkedList<RevCommit>();

        Set<RevCommit> typeName1Commits = new HashSet<RevCommit>();

        for (Feature f : features) {
            insertAndAdd(f);
            final RevCommit commit = ggit.commit().call();
            commits.addFirst(commit);
            if (pointsName.equals(f.getType().getName().getLocalPart())) {
                typeName1Commits.add(commit);
            }
        }

        // path to filter commits on type1
        String[] path = { pointsNs, pointsName };

        List<RevCommit> logCommits = toList(logOp.addPath(path).call());
        assertEquals(typeName1Commits.size(), logCommits.size());
        assertEquals(typeName1Commits, new HashSet<RevCommit>(logCommits));
    }

    public void testLimit() throws Exception {

        List<Feature> features = Arrays.asList(points1, lines1, points2, lines2, points3, lines3);

        for (Feature f : features) {
            insertAndAdd(f);
            ggit.commit().call();
        }

        assertEquals(3, Iterators.size(logOp.setLimit(3).call()));
        assertEquals(1, Iterators.size(logOp.setLimit(1).call()));
        assertEquals(4, Iterators.size(logOp.setLimit(4).call()));
    }

    public void testTemporalConstraint() throws Exception {

        List<Feature> features = Arrays.asList(points1, lines1, points2, lines2, points3, lines3);
        List<Long> timestamps = Arrays.asList(Long.valueOf(1000), Long.valueOf(2000),
                Long.valueOf(3000), Long.valueOf(4000), Long.valueOf(5000), Long.valueOf(6000));

        LinkedList<RevCommit> allCommits = new LinkedList<RevCommit>();

        for (int i = 0; i < features.size(); i++) {
            Feature f = features.get(i);
            Long timestamp = timestamps.get(i);
            insertAndAdd(f);
            final RevCommit commit = ggit.commit().setTimestamp(timestamp).call();
            allCommits.addFirst(commit);
        }

        // test time range exclusive
        boolean minInclusive = false;
        boolean maxInclusive = false;
        Range<Date> commitRange = new Range<Date>(Date.class, new Date(2000), minInclusive,
                new Date(5000), maxInclusive);
        logOp.setTimeRange(commitRange);

        List<RevCommit> logged = toList(logOp.call());
        List<RevCommit> expected = allCommits.subList(2, 4);
        assertEquals(expected, logged);

        // test time range inclusive
        minInclusive = true;
        maxInclusive = true;
        commitRange = new Range<Date>(Date.class, new Date(2000), minInclusive, new Date(5000),
                maxInclusive);
        logOp = ggit.log().setTimeRange(commitRange);

        logged = toList(logOp.call());
        expected = allCommits.subList(1, 5);
        assertEquals(expected, logged);

        // test reset time range
        logOp = ggit.log().setTimeRange(commitRange).setTimeRange(null);
        logged = toList(logOp.call());
        expected = allCommits;
        assertEquals(expected, logged);
    }

    public void testSinceUntil() throws Exception {
        final ObjectId oid1_1 = insertAndAdd(points1);
        final RevCommit commit1_1 = ggit.commit().call();

        final ObjectId oid1_2 = insertAndAdd(points2);
        final RevCommit commit1_2 = ggit.commit().call();

        final ObjectId oid2_1 = insertAndAdd(lines1);
        final RevCommit commit2_1 = ggit.commit().call();

        final ObjectId oid2_2 = insertAndAdd(lines2);
        final RevCommit commit2_2 = ggit.commit().call();

        try {
            logOp.setSince(oid1_1).call();
            fail("Expected ISE as since is not a commit");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("since"));
        }

        try {
            logOp.setSince(null).setUntil(oid2_2).call();
            fail("Expected ISE as until is not a commit");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("until"));
        }

        List<RevCommit> logs;
        List<RevCommit> expected;

        logs = toList(logOp.setSince(commit1_2.getId()).setUntil(null).call());
        expected = Arrays.asList(commit2_2, commit2_1, commit1_2);
        assertEquals(expected, logs);

        logs = toList(logOp.setSince(commit2_2.getId()).setUntil(null).call());
        expected = Arrays.asList(commit2_2);
        assertEquals(expected, logs);

        logs = toList(logOp.setSince(commit1_2.getId()).setUntil(commit2_1.getId()).call());
        expected = Arrays.asList(commit2_1, commit1_2);
        assertEquals(expected, logs);

        logs = toList(logOp.setSince(null).setUntil(commit2_1.getId()).call());
        expected = Arrays.asList(commit2_1, commit1_2, commit1_1);
        assertEquals(expected, logs);
    }
}
