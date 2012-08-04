/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import org.geogit.repository.Repository;

public class BranchCreateOp extends AbstractGeoGitOp<Ref> {

    private String branchName;

    public BranchCreateOp(Repository repository) {
        super(repository);
    }

    public BranchCreateOp setName(final String branchName) {
        this.branchName = branchName;
        return this;
    }

    public Ref call() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

}
