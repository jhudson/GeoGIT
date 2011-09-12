package org.geogit.api;

import java.util.Map;

import org.geogit.api.RevObject.TYPE;
import org.geogit.api.config.RefIO;
import org.geogit.api.config.RemoteConfigObject;
import org.geogit.repository.Repository;
import org.geogit.repository.remote.IRemote;
import org.geogit.repository.remote.RemoteRepositoryFactory;
import org.geogit.repository.remote.payload.IPayload;
import org.geogit.storage.BlobWriter;
import org.geogit.storage.CommitWriter;
import org.geogit.storage.ObjectInserter;
import org.geogit.storage.RevTreeWriter;

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
            	IPayload payload = remoteRepo.requestFetchPayload(RefIO.getRemoteList(getRepository().getRepositoryHome(),remote.getName()));
                
                int commits = 0;
                int deltas = 0;

                final ObjectInserter objectInserter = getRepository().newObjectInserter();
                
                /**
                 * Update the local repos commits
                 */
                for (RevCommit commit: payload.getCommitUpdates()) {
                    commits++;
                    ObjectId commitId = objectInserter.insert(new CommitWriter(commit));
                    getRepository().getRefDatabase().put(new Ref(remote.getName(), commitId, TYPE.COMMIT));
                }

                /**
                 * Update the local repos trees
                 */
                for (RevTree tree: payload.getTreeUpdates()) {
                    //LOGGER.info("Adding tree: " + tree.toString());
                    ObjectId treeId = objectInserter.insert(new RevTreeWriter(tree));
                    getRepository().getRefDatabase().put(new Ref(remote.getName(), treeId, TYPE.TREE));
                }

                /**
                 * Update the local repos blobs
                 */
                for (RevBlob blob: payload.getBlobUpdates()) {
                    deltas++;
                    ObjectId blobId = objectInserter.insert(new BlobWriter((byte[])blob.getParsed()));
                    getRepository().getRefDatabase().put(new Ref(remote.getName(), blobId, TYPE.BLOB));
                }

                /**
                 * Update the local repos tags, there are none...
                for (RevTag tag: payload.getTagUpdates()) {
                    deltas++;
                    ObjectId tagId = objectInserter.insert(new RevTagWriter(tag));
                    Ref ref = new Ref(remote.getName(), tagId, TYPE.TAG);
                    getRepository().getRefDatabase().put(ref);
                }
                */

                LOGGER.info("Remote: counted " + commits + " commits (" + deltas + " deltas), done.");
                LOGGER.info("Added " + commits + " new commits added to repository");
                LOGGER.info("Added " + deltas + " new deltas added to repository");

                /**
                 * Update the local repos branch refs for the remote
                 */
                for (String branchName: payload.getBranchUpdates().keySet()) {
                    Ref ref = payload.getBranchUpdates().get(branchName);
                    /*
                     * Now we must write out all the remote heads - so we can keep track of them for fetches
                     */
                    Ref remoteRef = new Ref(Ref.REMOTES_PREFIX+remote.getName()+"/"+Ref.MASTER, ref.getObjectId(), TYPE.REMOTE);
                    Ref oldRef = getRepository().getRef(remoteRef.getName());
                    
                    if (oldRef!=null && !oldRef.equals(remoteRef)){
                        LOGGER.info("  " + remoteRef.getObjectId().printSmallId() + " " + branchName + " -> " + remoteRef.getName());
                        getRepository().updateRef(remoteRef);
                        RefIO.writeRemoteRefs(getRepository().getRepositoryHome(), remote.getName(), branchName, ref.getObjectId());
                    }
                }

                // close the remote
                remoteRepo.dispose();
            }
        }

        return null;
    }
}
