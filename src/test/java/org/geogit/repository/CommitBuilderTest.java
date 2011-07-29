/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;

public class CommitBuilderTest extends TestCase {

    @Override
    protected void setUp() throws Exception {

    }

    public void testBuildEmpty() throws Exception {
        CommitBuilder b = new CommitBuilder();
        try {
            b.build(null);
            fail("expected IAE on null id");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Id"));
        }

        try {
            b.build(ObjectId.NULL);
            fail("expected IllegalStateException on null tree id");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("tree"));
        }
    }

    public void testBuildFull() throws Exception {
        CommitBuilder b = new CommitBuilder();
        b.setAuthor("groldan");
        b.setCommitter("jdeolive");
        b.setMessage("cool this works");
        b.setTimestamp(1000);

        final ObjectId commitId = ObjectId.forString("fake commit content");
        ObjectId treeId = ObjectId.forString("fake tree content");

        b.setTreeId(treeId);

        ObjectId parentId1 = ObjectId.forString("fake parent content 1");
        ObjectId parentId2 = ObjectId.forString("fake parent content 2");
        List<ObjectId> parentIds = Arrays.asList(parentId1, parentId2);
        b.setParentIds(parentIds);

        RevCommit build = b.build(commitId);

        assertEquals(commitId, build.getId());
        assertEquals(treeId, build.getTreeId());
        assertEquals(parentIds, build.getParentIds());
        assertEquals("groldan", build.getAuthor());
        assertEquals("jdeolive", build.getCommitter());
        assertEquals("cool this works", build.getMessage());
        assertEquals(1000L, build.getTimestamp());
    }
}
