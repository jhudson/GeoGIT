package org.geogit.repository.remote;

import java.io.File;
import java.util.Properties;

import org.geogit.repository.Repository;
import org.geogit.storage.RepositoryDatabase;
import org.geogit.storage.bdbje.EntityStoreConfig;
import org.geogit.storage.bdbje.EnvironmentBuilder;
import org.geogit.storage.bdbje.JERepositoryDatabase;

import com.sleepycat.je.Environment;

 /**
  * A local connection point to a locally copied repository. This class is responsible 
  * for creating and maintaining a read only copy of a local repository that is the 
  * origin of the containing repository  
  * 
  * @author jhudson
  */
public class LocalRemote extends AbstractRemote {

    private Repository repository;
    private File file;
        
    public LocalRemote(String location) {
        this.file = new File(location);
    }

    @Override
    public Repository getRepository() {
        if (this.repository!=null){
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
    
            RepositoryDatabase repositoryDatabase = new JERepositoryDatabase(environment, stagingEnvironment);

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