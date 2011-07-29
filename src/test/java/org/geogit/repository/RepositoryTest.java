/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.test.RepositoryTestCase;

public class RepositoryTest extends RepositoryTestCase {

    @Override
    protected void setUpInternal() throws Exception {
    }

    public void testInitialize() {
        // "master" points to the latest commit in the "master" branch. There're no commits yet,
        // hence no master branch
        ObjectId master = repo.resolve(Ref.MASTER);
        assertNotNull(master);
        assertEquals(ObjectId.NULL, master);

        // HEAD points to the latest commit in the current branch. There are no commits yet, hence
        // no HEAD
        ObjectId head = repo.resolve(Ref.HEAD);
        assertNotNull(head);
        assertEquals(ObjectId.NULL, head);
    }
}
