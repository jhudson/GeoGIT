/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.api;

import org.geogit.repository.Repository;

/**
 * 
 * As in git this is a concatenation of a fetch and a merge operation.
 * 
 * @author jhudson
 * @since 1.2.0
 */
public class PullOp extends AbstractGeoGitOp<MergeResult> {

    private String branchName;

    public PullOp(Repository repository) {
        super(repository);
    }
    
    public PullOp include(final String branchName) {
        this.branchName = branchName;
        return this;
    }

    @Override
    public MergeResult call() throws Exception {
        GeoGIT gg = new GeoGIT(getRepository());
        gg.fetch().call();
        Ref branch = getRepository().getRef(branchName);
        return gg.merge().include(branch).call();
    }
}
