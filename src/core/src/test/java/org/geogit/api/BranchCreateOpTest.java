/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import org.geogit.test.RepositoryTestCase;

public class BranchCreateOpTest extends RepositoryTestCase {

    private GeoGIT ggit;

    @Override
    protected void setUpInternal() throws Exception {
        this.ggit = new GeoGIT(getRepository());
    }

    public void testBranchHead() {

    }
}
