package org.geogit.repository.remote;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.geogit.api.GeoGIT;
import org.geogit.api.LogOp;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject;
import org.geogit.api.RevTree;
import org.geogit.api.TreeVisitor;
import org.geogit.api.config.BranchConfigObject;
import org.geogit.api.config.Config;
import org.geogit.repository.Repository;
import org.geogit.repository.remote.payload.IPayload;
import org.geogit.repository.remote.payload.LocalPayload;
import org.geogit.storage.RepositoryDatabase;
import org.geogit.storage.bdbje.EntityStoreConfig;
import org.geogit.storage.bdbje.EnvironmentBuilder;
import org.geogit.storage.bdbje.JERepositoryDatabase;

import com.sleepycat.je.Environment;

/**
 * A local connection point to a locally copied repository. This class is responsible for creating
 * and maintaining a read only copy of a local repository that is the origin of the containing
 * repository
 * 
 * @author jhudson
 */
public class LocalRemote extends AbstractRemote {

    private Repository repository;
    private File file;

    public LocalRemote( String location ) {
        this.file = new File(location);
    }

    /**
     * Create a set of changes it can be applied to a repository 
     * 1. Get the local repository local branches 
     * 2. Compare them to the ones the client has sent us 
     * 2. Create a set of changes since the clients id for each branch
     */
    @Override
    public IPayload requestFetchPayload( Map<String, String> branchHeads ) {
        final LocalPayload payload = new LocalPayload();

        //for each branch
        //  grab the branch_name
        //  switch to branch_nane
        //  fill payload with branch updates

        /**
         * Since there is no concept of branching and current branch, lets just 
         * grab the 'master' as this is the only 'branch' the remote has
         */
        String branchHeadId = branchHeads.get("master");
        ObjectId branchId;
        if (branchHeadId == null){
            branchId = ObjectId.NULL;
        } else {
            branchId = new ObjectId(branchHeadId.getBytes());
        }
        
        LogOp logOp = new LogOp(getRepository());

        if (!getRepository().getHead().getObjectId().equals(branchId)) {

            /**
             * If local has no commits don't set since, since we need all refs
             */
            if (!ObjectId.NULL/* THE HEAD */.equals(branchId)) {
                logOp.setSince(branchId);
            }

            try {
                Iterator<RevCommit> logs = logOp.call();
                while( logs.hasNext() ) {
                    RevCommit commit = logs.next();
                    payload.addCommits(commit);

                    /**
                     * ok we have the commit, this should be a reference to the tree,blob,tag objects
                     */
                    ObjectId treeId = commit.getTreeId();
                    RevTree tree = getRepository().getTree(treeId);

                    /**
                     * Add Trees to payload
                     */
                    payload.addTrees(tree);
                    
                    tree.accept(new TreeVisitor(){
                        
                        @Override
                        public boolean visitSubTree( int bucket, ObjectId treeId ) {
                            /**
                             * add any subtrees
                             */
                            RevTree tree = getRepository().getTree(treeId);
                            /**
                             * add the subtree to our tree store, then see if there are any blobs
                             */
                            payload.addTrees(tree);
                            tree.accept(this);
                            return true;
                        }

                        @Override
                        public boolean visitEntry( Ref ref ) {
                            if (ref.getType().equals(RevObject.TYPE.TREE)) {
                                RevTree tree = getRepository().getTree(ref.getObjectId());
                                payload.addTrees(tree);
                                tree.accept(this);
                            } else {
                                /**
                                 * Add BLOB to store
                                 */
                                payload.addBlobs(getRepository().getObjectDatabase().getBlob(ref.getObjectId()));
                            }
                            return true;
                            
                        }
                    });
                    
                    /**
                     * Add Tags to payload, 
                     * 
                     * there are none for now...
                     */
                    
                }

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        addBranches(payload);

        return payload;
    }

    /**
     * Add this remotes branch head refs to the payload so the client can update its references  
     * @param payload
     */
    public void addBranches( final LocalPayload payload ) {
        GeoGIT ggit = new GeoGIT(repository);
        Config config = ggit.getConfig();
        Map<String, BranchConfigObject> branches = config.getBranches();

        for( BranchConfigObject branch : branches.values() ) {
            payload.addBranches(branch.getName(), getRepository().getRef(branch.getName()));
        }

        /*
         * Add the master branch
         */
        payload.addBranches("master", getRepository().getHead());
    }

    /**
     * Get the locally accessible 'remote' repository, only opens the repo and reads it does not
     * create a new one
     */
    public Repository getRepository() {
        if (this.repository != null) {
            return repository;
        } else {
            final File envHome = getFile();
            final File repositoryHome = new File(envHome, "repository");
            final File indexHome = new File(envHome, "index");

            EntityStoreConfig config = new EntityStoreConfig();
            config.setCacheMemoryPercentAllowed(50);
            EnvironmentBuilder esb = new EnvironmentBuilder(config);
            Properties bdbEnvProperties = null;
            Environment environment;
            environment = esb.buildEnvironment(repositoryHome, bdbEnvProperties);

            Environment stagingEnvironment;
            stagingEnvironment = esb.buildEnvironment(indexHome, bdbEnvProperties);

            RepositoryDatabase repositoryDatabase = new JERepositoryDatabase(environment,
                    stagingEnvironment);

            repository = new Repository(repositoryDatabase, envHome);

            repository.create();

            return repository;
        }
    }

    public File getFile() {
        return file;
    }

    public void setFile( File file ) {
        this.file = file;
    }

    public void setRepository( Repository repository ) {
        this.repository = repository;
    }

    @Override
    public String toString() {
        return "LocalRemote [repository=" + repository + ", file=" + file + "]";
    }

    @Override
    public void dispose() {
        repository.close();
    }
}