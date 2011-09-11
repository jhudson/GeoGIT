package org.geogit.repository.remote;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.geogit.api.GeoGIT;
import org.geogit.api.LogOp;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevBlob;
import org.geogit.api.RevCommit;
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
    public IPayload requestFetchPayload( Ref head ) {
    	LocalPayload payload = new LocalPayload();

    		LogOp logOp = new LogOp(getRepository());

            if (!getRepository().getHead().getObjectId().equals(head)) {

                /**
                 * If local has no commits don't set since, since we need all refs
                 */
                if (!ObjectId.NULL/* THE HEAD */.equals(head.getObjectId())) {
                    logOp.setSince(head.getObjectId());
                }

                try {
                    Iterator<RevCommit> logs = logOp.call();
                    while (logs.hasNext()){
                    	RevCommit r = logs.next();
                    	RevBlob b = (RevBlob)getRepository().getBlob(r.getId());
                    	
                    	payload.addBlobs(b);
                    	payload.addCommits(r);
                    }
                    
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            addBranches(payload);
    	
        return payload;
    }
    
	public void addBranches(final LocalPayload payload) {
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