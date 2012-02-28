/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.util.Map;

import org.geogit.api.config.RefIO;
import org.geogit.api.config.RemoteConfigObject;
import org.geogit.repository.Repository;
import org.geogit.repository.remote.IRemote;
import org.geogit.repository.remote.RemoteRepositoryFactory;
import org.geogit.repository.remote.payload.IPayload;

import com.google.common.base.Preconditions;

/**
 * Download objects and refs from another repository, currently only works for fast forwards from
 * HEAD of remote http://git-scm.com/gitserver.txt
 * 
 * TODO: roll back on any errors
 * 
 * @author jhudson
 */
public class FetchOp extends AbstractGeoGitOp<Void> {

    private Map<String, RemoteConfigObject> remotes;

    public FetchOp( Repository repository, Map<String, RemoteConfigObject> remotes ) {
        super(repository);
        this.remotes = remotes;
    }

    @Override
    public Void call() throws Exception {
        /**
         * For each remote_service : 
         * 		get the refs/remotes/REF_NAME/REMOTE(S)
					For ALL res/remotes/REF_NAME/REMOTE_ID send remote_service (REF_NAMES<REMOTE_ID> receive packfile
         * with 1. REMOTE_NAME <commits> 2. Add commits to object DB 3. update ref in reb DB 4.
         * write config
         */
        for( RemoteConfigObject remote : remotes.values() ) {
            if (remote != null) {
            	IRemote remoteRepo = RemoteRepositoryFactory.createRemoteRepositroy(remote.getUrl());
            	Preconditions.checkNotNull(remoteRepo);

            	IPayload payload = remoteRepo.requestFetchPayload(RefIO.getRemoteList(getRepository().getRepositoryHome(),remote.getName()));

            	PayloadUtil payloadUtil = new PayloadUtil(getRepository());
                payloadUtil.applyPayloadTo(remote.getName(), payload);
                // clean up
                remoteRepo.dispose();
            }
        }

        return null;
    }
}
