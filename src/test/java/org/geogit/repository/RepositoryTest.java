/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevBlob;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject;
import org.geogit.api.RevTree;
import org.geogit.storage.BlobWriter;
import org.geogit.storage.CommitWriter;
import org.geogit.storage.RevTreeWriter;
import org.geogit.test.RepositoryTestCase;

public class RepositoryTest extends RepositoryTestCase {

    @Override
    protected void setUpInternal() throws Exception {
    }

    public void testInitialize() {
        // "master" points to the latest commit in the "master" branch. There're no commits yet,
        // hence no master branch
        try {
            repo.resolve(Ref.MASTER);
            fail("expected IAE");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        // HEAD points to the latest commit in the current branch. There are no commits yet, hence
        // no HEAD
        try {
            repo.resolve(Ref.HEAD);
            fail("expected IAE");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    public void testResolve() throws Exception {
        RevCommit insert = new RevCommit(ObjectId.forString("id1"));
        insert.setTreeId(ObjectId.forString("treetest"));

        ObjectId commitId = repo.getObjectDatabase().put(new CommitWriter(insert));
        RevCommit commit = repo.getCommit(commitId);

        Ref ref = new Ref(Ref.HEAD, commitId, RevObject.TYPE.COMMIT);

        repo.getRefDatabase().put(ref);
        assertEquals(commit, repo.resolve(Ref.HEAD));

        assertEquals(commit, repo.resolve(commitId.toString().substring(0, 8)));
    }

    public void testResolveMultiple() throws Exception {
        byte[] raw1 = { 'a', 'b', 'c', 'd', 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        byte[] raw2 = { 'a', 'b', 'c', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

        ObjectId id1 = new ObjectId(raw1);
        ObjectId id2 = new ObjectId(raw2);
        String hash1 = id1.toString();
        String hash2 = id2.toString();

        repo.getObjectDatabase().put(id1, new BlobWriter(new byte[10]));
        repo.getObjectDatabase().put(id2, new BlobWriter(new byte[10]));

        String prefixSearch;
        for (int i = 1; i <= 3; i++) {
            prefixSearch = hash1.substring(0, 2 * i);
            try {
                repo.resolve(prefixSearch);
                fail("Expected IAE on multiple results");
            } catch (IllegalArgumentException e) {
                assertTrue(true);
            }
        }

        RevObject blob1 = repo.getBlob(id1);
        RevObject blob2 = repo.getBlob(id2);
        assertEquals(blob1, repo.resolve(hash1.substring(0, 8)));
        assertEquals(blob2, repo.resolve(hash2.substring(0, 8)));
    }

    public void testResolveType() throws Exception {
        byte[] raw1 = { 'a', 'b', 'c', 'd', 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        byte[] raw2 = { 'a', 'b', 'c', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

        ObjectId treeId = new ObjectId(raw1);
        String blobHash = treeId.toString();

        ObjectId commitId = new ObjectId(raw2);
        String commitHash = commitId.toString();

        repo.getObjectDatabase().put(treeId, new RevTreeWriter(repo.newTree()));

        {
            RevCommit c = new RevCommit(ObjectId.NULL);
            c.setTreeId(ObjectId.NULL);
            repo.getObjectDatabase().put(commitId, new CommitWriter(c));
        }

        String matchingPartialHash = blobHash.substring(0, 6);
        assertEquals(matchingPartialHash, commitHash.substring(0, 6));

        RevCommit resolvedCommit = repo.resolve(matchingPartialHash, RevCommit.class);
        assertEquals(commitId, resolvedCommit.getId());

        RevTree resolvedTree = repo.resolve(matchingPartialHash, RevTree.class);
        assertEquals(treeId, resolvedTree.getId());
    }
}
