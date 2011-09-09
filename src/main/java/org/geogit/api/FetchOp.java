package org.geogit.api;

import java.util.List;
import java.util.Map;

import org.geogit.api.RevObject.TYPE;
import org.geogit.api.config.RefIO;
import org.geogit.api.config.RemoteConfigObject;
import org.geogit.repository.Repository;
import org.geogit.repository.remote.IRemote;
import org.geogit.repository.remote.RemoteRepositoryFactory;
import org.geogit.storage.CommitWriter;

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
         * For each remote_service : get the refs/remotes/REF_NAME/REMOTE(S) For ALL
         * res/remotes/REF_NAME/REMOTE_ID send remote_service (REF_NAMES<REMOTE_ID> receive packfile
         * with 1. REMOTE_NAME <commits> 2. Add commits to object DB 3. update ref in reb DB 4.
         * write config
         */
        for( RemoteConfigObject remote : remotes.values() ) {
            if (remote != null) {

                IRemote remoteRepo = RemoteRepositoryFactory.createRemoteRepositroy(remote.getUrl());

                List<RevCommit> remoteChanges = remoteRepo.requestCommitFetch(getRepository().getHead());
                Map<String, Ref> remoteBranches = remoteRepo.requestBranchFetch();

                int objects = 0;

                for (RevCommit commit: remoteChanges) {
                    objects++;

                    // need to get the remote repos refs DB and add it to my own with the
                    // remotes full name.
                    ObjectId commitId = getRepository().getObjectDatabase().put(new CommitWriter(commit));
                    Ref ref = new Ref(remote.getName(), commitId, TYPE.COMMIT);
                    getRepository().getRefDatabase().put(ref);
                }

                LOGGER.info("Remote: counted " + objects + " objects, done.");
                LOGGER.info("Added " + objects + " new objects added to repository");
                
                for (String branchName: remoteBranches.keySet()) {
                    Ref ref = remoteBranches.get(branchName);
                    /*
                     * Now we must write out all the remote heads - so we can keep track of them for fetches
                     */
                    Ref remoteRef = new Ref(Ref.REMOTES_PREFIX+remote.getName()+"/"+Ref.MASTER, ref.getObjectId(), TYPE.REMOTE);
                    
                    LOGGER.info("  " + remoteRef.getObjectId().printSmallId() + " " +remoteRef.getName());
                    getRepository().updateRef(remoteRef);
                    RefIO.writeRef(getRepository().getRepositoryHome(), remote.getName(), branchName, ref.getObjectId());
                }

                // close the remote
                remoteRepo.dispose();
            }
        }

        return null;
    }
}
