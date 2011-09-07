package org.geogit.api;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geogit.api.config.RemoteConfigObject;
import org.geogit.repository.Repository;
import org.geogit.repository.remote.IRemote;
import org.geogit.repository.remote.RemoteRepositoryFactory;
import org.geogit.storage.CommitWriter;

/**
 * Download objects and refs from another repository, currently only works for fast forwards from HEAD of remote
 * http://git-scm.com/gitserver.txt 
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
        for (RemoteConfigObject remote : remotes.values()) {
            if (remote != null) {

                IRemote remoteRepo = RemoteRepositoryFactory.createRemoteRepositroy(remote.getUrl());

                LogOp logOp = new LogOp(remoteRepo.getRepository());

                /**
                 * If the repositories are at the same ref, return
                 */
                if (getRepository().getHead().getObjectId().equals(remoteRepo.getRepository().getHead().getObjectId())) {
                    LOGGER.info("Remote ("+ remote.getName()  + ") has same head ID, nothing to do");
                    remoteRepo.dispose();
                    return null;
                }

                /**
                 * If local has no commits don't set since, since we need all refs
                 */
                if (!getRepository().getHead().getObjectId().equals(ObjectId.NULL/* THE HEAD */)) {
                    logOp.setSince(getRepository().getHead().getObjectId());
                }

                Iterator<RevCommit> logs = logOp.call();

                int objects = 0;

                if (logs != null) {
                    while( logs.hasNext() ) {
                        RevCommit rc = logs.next();
                        LOGGER.info(rc.toString());
                        objects++;

                        // need to get the remote repos refs DB and add it to my own with the
                        // remotes full name.
                        ObjectId commitId = getRepository().getObjectDatabase().put(new CommitWriter(rc));
                        Ref ref = new Ref(remote.getName(), commitId, RevObject.TYPE.COMMIT);
                        getRepository().getRefDatabase().put(ref);
                    }
                }
                
                //set the current ref for this new set of commits
                List<Ref> remotes1 = remoteRepo.getRepository().getRefDatabase().getRefs(Ref.HEAD);

                for( Ref r : remotes1 ) { /*There will only be one*/
                    Ref ref = new Ref(remote.getName(), r.getObjectId(), RevObject.TYPE.COMMIT);
                    getRepository().getRefDatabase().put(ref);
                }

                LOGGER.info("Remote: counted " + objects + " objects, done.");
                LOGGER.info("Added " + objects + " objects to " + remote.getName());

                // close the remote
                remoteRepo.dispose();
            }
        }

        return null;
    }
}
