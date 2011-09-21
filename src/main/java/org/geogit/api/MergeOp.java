/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.api;

import org.geogit.api.RevObject.TYPE;
import org.geogit.repository.Repository;

public class MergeOp extends AbstractGeoGitOp<MergeResult> {

    private Ref branch;
    
    public MergeOp(Repository repository) {
        super(repository);
    }

    public MergeOp include(final Ref branch) {
        this.branch = branch;
        return this;
    }

    public MergeResult call() throws Exception {
        if (branch == null){
            return null;
        }
        /**
         * 1. make it the master head
         */
        Ref newRef = new Ref(Ref.HEAD, branch.getObjectId(), TYPE.COMMIT);
        getRepository().updateRef(newRef);

        MergeResult mergeResult = new MergeResult();
        return mergeResult;
    }
}