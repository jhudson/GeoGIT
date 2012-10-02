package org.geogit.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.geogit.api.FetchResult;
import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.PushResult;
import org.geogit.api.PushResult.STATUS;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.config.RefIO;
import org.geogit.api.merge.MergeResult;
import org.geogit.repository.Repository;
import org.geogit.repository.StagingArea;
import org.geogit.storage.WrappedSerialisingFactory;
import org.geogit.storage.bdbje.EntityStoreConfig;
import org.geogit.storage.bdbje.EnvironmentBuilder;
import org.geogit.storage.bdbje.JERepositoryDatabase;
import org.geogit.util.RepositoryUtils;
import org.geotools.data.DataUtilities;
import org.geotools.factory.Hints;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.WKTReader2;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;

import com.sleepycat.je.Environment;
import com.vividsolutions.jts.io.ParseException;

public class ResourcePassingTest {

	private static FetchResourceService fetch;
	private static int PORT = 8765;
	private static String REMOTE_ENV_HOME = "remote_repo";
	private static String LOCAL_ENV_HOME = "local_repo";
	private static String INDEX_PATH = "index";
	private static String REPO_PATH = "repository";
	private static String CENTRAL_URL = "http://localhost:8765";
	
	protected static final String sampleNs = "http://geogit.sample";
    protected static final String sampleName = "Sample";
    protected static final String sampleTypeSpec = "st:String,it:Integer,pn:Point:srid=4326,db:Double";
    private static String newString1 = "New String Value";
    private static String newString2 = "Another new string";
    private static String newString3 = "Third iteration change.";
    protected SimpleFeatureType sampleType;
	private SimpleFeature sample1;
	private SimpleFeature sample2;
	private SimpleFeature sample3;
    protected static final String idS1 = "Sample.1";
    protected static final String idS2 = "Sample.2";
    protected static final String idS3 = "Sample.3";


    protected Name sampleTypeName = new NameImpl(sampleNs, sampleName);

    /**
     * Logging instance
     */
    static Logger logger = org.geotools.util.logging.Logging.getLogger(ResourcePassingTest.class); //$NON-NLS-1$

	
	public static void setUpClass() throws IOException {
		/*
		 * Start the Fetch Resource Service to act as a remote repository.
		 */
		System.out.println("Starting service.");
		File repoRootName = File.createTempFile("mock", "");
		if(!repoRootName.delete()) {
			throw new IOException("Could not delete " + repoRootName.getAbsolutePath());
		}
		repoRoot = new File(repoRootName.getPath() + "blobStore");
		if(!repoRoot.mkdir()) {
			throw new IOException("Could not create directory " + repoRoot.getAbsolutePath());
		}
		remoteEnvHome = new File(repoRoot, REMOTE_ENV_HOME);
		remoteEnvHome.mkdir();
		localEnvHome = new File(repoRoot, LOCAL_ENV_HOME);
		localEnvHome.mkdir();
		fetch = new FetchResourceService(PORT, remoteEnvHome.getAbsolutePath(), INDEX_PATH, REPO_PATH);
		Thread thread = new Thread(fetch);
		thread.start();
	}
	
	
	public static void tearDownClass() throws Exception {
		System.out.println("Stopping service.");
		fetch.stop();
	}
	
	private static File repoRoot;
	private Repository localRepo;
	//private File localRepoRoot;
	private Repository remoteRepo;
	private static File localEnvHome;
	private static File remoteEnvHome;

	
	@Before
	public void setUp() throws Exception {
		System.out.println("Starting service.");
		File repoRootName = File.createTempFile("mock", "");
		if(!repoRootName.delete()) {
			throw new IOException("Could not delete " + repoRootName.getAbsolutePath());
		}
		repoRoot = new File(repoRootName.getPath() + "blobStore");
		if(!repoRoot.mkdir()) {
			throw new IOException("Could not create directory " + repoRoot.getAbsolutePath());
		}
		remoteEnvHome = new File(repoRoot, REMOTE_ENV_HOME);
		remoteEnvHome.mkdir();
		localEnvHome = new File(repoRoot, LOCAL_ENV_HOME);
		localEnvHome.mkdir();
		fetch = new FetchResourceService(PORT, remoteEnvHome.getAbsolutePath(), INDEX_PATH, REPO_PATH);
		Thread thread = new Thread(fetch);
		thread.start();
		
		/*
		 * Initialise test data type and features.
		 */
		this.sampleType = DataUtilities.createType(sampleNs, sampleName, sampleTypeSpec);

        this.sample1 = (SimpleFeature) this.feature(this.sampleType, idS1, "Sample String 1",
                new Integer(1), "POINT (0 1)", new Double(2.34));
        this.sample1.getUserData().put(Hints.USE_PROVIDED_FID, Boolean.TRUE);
        this.sample2 = (SimpleFeature) this.feature(this.sampleType, idS2, "Sample String 2",
                new Integer(4), "POINT (1 0)", new Double(3380));
        this.sample2.getUserData().put(Hints.USE_PROVIDED_FID, Boolean.TRUE);
        this.sample3 = (SimpleFeature) this.feature(this.sampleType, idS3, "Sample String 3",
                new Integer(81), "POINT (2 2)", new Double(78.2));
        this.sample3.getUserData().put(Hints.USE_PROVIDED_FID, Boolean.TRUE);
        

		
	
		
		/*
		 * Initialise remote repository.
		 */

		
		File remoteRepositoryHome = new File(remoteEnvHome, REPO_PATH);
		remoteRepositoryHome.mkdir();
		File remoteIndexHome = new File(remoteEnvHome, INDEX_PATH);
		remoteIndexHome.mkdir();
		remoteRepo = RepositoryUtils.createRepository( remoteEnvHome, remoteRepositoryHome, remoteIndexHome);
		remoteRepo.close();
		
		/*
		 * Initialise local repository.
		 */


		File localRepositoryHome = new File(localEnvHome, REPO_PATH);
		localRepositoryHome.mkdir();
		File localIndexHome = new File(localEnvHome, INDEX_PATH);
		localIndexHome.mkdir();
		
		
		this.copySiteFromCentral();
		System.out.println("finished setup");
	}
	
    private void copySiteFromCentral() throws Exception {

        if (this.remoteEnvHome.exists()) {
            ResourcePassingTest.copyFile(remoteEnvHome, localEnvHome);
            Repository client = this.createRepo(localEnvHome.getAbsolutePath(), true);
            GeoGIT gg = new GeoGIT(client);

            /*
             * using a localremote object to handle a fetch from a HDD locally, can be done like this: 
             */

            //gg.remoteAddOp().setName("origin").setFetch(Ref.MASTER)
            //       .setUrl(this.remoteEnvHome.getAbsolutePath()).call();

            gg.remoteAddOp().setName("origin").setFetch(Ref.MASTER)
            .setUrl(ResourcePassingTest.CENTRAL_URL).call();
            
            /*
             * We need to write out a new ref for the remote 'branch' 
             * - this is used when fetching updates
             */
//            Ref remoteRef = new Ref(Ref.REMOTES_PREFIX + Ref.ORIGIN + Ref.MASTER, gg.getRepository().getHead().getObjectId(), TYPE.REMOTE);
//            gg.getRepository().updateRef(remoteRef);
//            RefIO.writeRemoteRefs(gg.getRepository().getRepositoryHome(), "origin", Ref.HEAD, gg.getRepository().getHead().getObjectId());  //$NON-NLS-1$
//            

            /*
             * some logging to visually check...
             */
            Repository server = this.createRepo(localEnvHome.getAbsolutePath(), false);
            GeoGIT serverGG = new GeoGIT(server);
            System.out.println("+++++++++++++++++++++++++++ SERVER: " + serverGG.getRepository().getHead().getObjectId());
            System.out.println("+++++++++++++++++++++++++++ CLIENT: " + gg.getRepository().getHead().getObjectId());

            serverGG.getRepository().close();
            gg.getRepository().close();
        }
    }
    
    protected Repository createRepo(String themeHome, boolean delete) throws IOException {
        final File envHome = new File(themeHome);
        final File repositoryHome = new File(envHome, "repository");
        final File indexHome = new File(envHome, "index");

        if (delete) {
            FileUtils.deleteDirectory(envHome);
            repositoryHome.mkdirs();
            indexHome.mkdirs();
        }

        EntityStoreConfig config = new EntityStoreConfig();
        config.setCacheMemoryPercentAllowed(50);
        EnvironmentBuilder esb = new EnvironmentBuilder(config);
        Properties bdbEnvProperties = null;
        Environment environment;
        environment = esb.buildEnvironment(repositoryHome, bdbEnvProperties);

        Environment stagingEnvironment;
        stagingEnvironment = esb.buildEnvironment(indexHome, bdbEnvProperties);

        JERepositoryDatabase repositoryDatabase = new JERepositoryDatabase(environment,
                stagingEnvironment);

        Repository repo = new Repository(repositoryDatabase, envHome);
        repo.create();

        return repo;
    }
    
    public static void copyFile(File src, File dest) throws IOException {

        if (src.isDirectory()) {

            // if directory not exists, create it
            if (!dest.exists()) {
                dest.mkdir();
                System.out.println("Directory copied from " + src + "  to " + dest);
            }

            // list all the directory contents
            String files[] = src.list();

            for (String file : files) {
                // construct the src and dest file structure
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);
                // recursive copy
                try {
                copyFile(srcFile, destFile);
            } catch (IOException ex){
            	throw new IOException("could not copy " + srcFile.getAbsolutePath(), ex);
            }
            }

        } else {
            // if file, then copy it
            // Use bytes stream to support all file types
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dest);

            byte[] buffer = new byte[1024];

            int length;
            // copy the file content in bytes
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            in.close();
            out.close();
            System.out.println("File copied from " + src + " to " + dest);
        }
    }
	
	@After
	public void tearDown() throws Exception {
		System.out.println("Stopping service.");
		fetch.stop();
		
	}
	@Test
	public void testFetch() throws Exception{
		Repository clientRepo = this.createRepo(localEnvHome.getAbsolutePath(), false);
		GeoGIT client = new GeoGIT(clientRepo);
		
		Repository serverRepo = this.createRepo(this.remoteEnvHome.getAbsolutePath(), false);
		GeoGIT server = new GeoGIT(serverRepo);
		
	        logger.log(Level.WARNING, "++++++++++++++++++++++ Doing a fetch ++++++++++++++++++++++");
	        FetchResult result = client.fetch().call();
	        assertEquals(0, result.getCommits());
	        assertEquals(0, result.getTrees());
	        assertEquals(0, result.getBlobs());
	        assertEquals(1, result.getBranches());
	        
	        // insert remote feature
	        this.printLogs(server, 0);
	        
	        this.insertAddCommit(server, this.sample1, "commited a new feature into site");
	        this.printLogs(server, 1);
	        
	        logger.log(Level.WARNING, "++++++++++++++++++++++ Doing a fetch ++++++++++++++++++++++");
	        result = client.fetch().call();
	        assertEquals(1, result.getCommits());
	        assertEquals(3, result.getTrees());
	        assertEquals(1, result.getBlobs());
	        assertEquals(1, result.getBranches());
	}
	
	@Test
	public void testPush() throws Exception {
		Repository clientRepo = this.createRepo(localEnvHome.getAbsolutePath(), false);
		GeoGIT client = new GeoGIT(clientRepo);
		
		Repository serverRepo = this.createRepo(this.remoteEnvHome.getAbsolutePath(), false);
		GeoGIT server = new GeoGIT(serverRepo);
        
		this.printLogs(server, 0);
		this.printLogs(client, 0);
		//insert remote
		this.insertAddCommit(server, this.sample1, "commited a new feature into site");
        this.printLogs(server, 1);
        

        
        logger.log(Level.WARNING, "++++++++++++++++++++++ Doing a push ++++++++++++++++++++++");
        PushResult result3 = client.push().call();
        // N0 head sent since there hasn't been a commit or a pull
        assertEquals(STATUS.INCORRECT_PARAMETER, result3.getStatus());
        this.printLogs(server, 1);
        
        logger.log(Level.WARNING, "++++++++++++++++++++++ Doing a pull ++++++++++++++++++++++");
        this.printLogs(client, 0);
        MergeResult result2 = client.pull().call();
        assertEquals(0, result2.getDiffs().size());
        this.printLogs(client, 1);

		
        
        // insert local feature
        this.printLogs(client, 1);
        RevCommit revCommit =  this.insertAddCommit(client, this.sample2, "commited a new feature into site");
        System.out.println("commit after local insert: " +revCommit.getId());
        this.printLogs(client, 2);
        this.printLogs(server, 1);
        logger.log(Level.WARNING, "++++++++++++++++++++++ Doing a push ++++++++++++++++++++++");
         result3 = client.push().call();
         logger.log(Level.WARNING, "++++++++++++++++++++++ Doing a second push no changes ++++++++++++++++++++++");
         this.printLogs(server, 2);
        assertEquals(STATUS.OK_APPLIED, result3.getStatus());
        result3 = client.push().call();
       assertEquals(STATUS.NO_CHANGE, result3.getStatus());
	}
	
	@Test
	public void testPull() throws Exception {
		Repository clientRepo = this.createRepo(localEnvHome.getAbsolutePath(), false);
		GeoGIT client = new GeoGIT(clientRepo);
		
		Repository serverRepo = this.createRepo(this.remoteEnvHome.getAbsolutePath(), false);
		GeoGIT server = new GeoGIT(serverRepo);
        
        // insert remote feature
        this.printLogs(server, 0);
        RevCommit revCommit =  this.insertAddCommit(server, this.sample1, "commited a new feature into site");

        this.printLogs(server, 1);
        
        logger.log(Level.WARNING, "++++++++++++++++++++++ Doing a pull ++++++++++++++++++++++");
        MergeResult result = client.pull().call();
        assertEquals(0, result.getDiffs());
        
	}
	
    /**
     * A method to create a feature.
     * 
     * @param type
     * @param id
     * @param values
     * @return
     * @throws ParseException
     */
    protected Feature feature(SimpleFeatureType type, String id, Object... values)
            throws ParseException {
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (type.getDescriptor(i) instanceof GeometryDescriptor) {
                if (value instanceof String) {
                    value = new WKTReader2().read((String) value);
                }
            }
            builder.set(i, value);
        }
        return builder.buildFeature(id);
    }
	
    /**
     * Inserts the Feature to the index and stages it to be committed.
     */
    protected RevCommit insertAddCommit(GeoGIT gg, Feature f, String message) throws Exception {
        this.insert(gg, f);
        gg.add().call();
        return gg.commit().setMessage(message).setAll(true).call();
    }
    /**
     * Inserts the feature to the index but does not stages it to be committed
     */
    protected ObjectId insert(GeoGIT gg, Feature feature) throws Exception {
        final StagingArea index = gg.getRepository().getIndex();
        Name name = feature.getType().getName();
        String namespaceURI = name.getNamespaceURI();
        String localPart = name.getLocalPart();
        String id = feature.getIdentifier().getID();

        WrappedSerialisingFactory fact = WrappedSerialisingFactory.getInstance();
        Ref ref = index.inserted(fact.createFeatureWriter(feature), feature.getBounds(),
                namespaceURI, localPart, id);
        ObjectId objectId = ref.getObjectId();
        return objectId;
    }

    private void printLogs(GeoGIT gg, int expectedLogs) {
        Iterator<RevCommit> it;
        try {
            it = gg.log().call();
            int total = 0;
            System.out.println("+++++++++++++COMMITS+++++++++++++");
            while (it.hasNext()) {
                System.out.println(it.next());
                total++;
            }
            System.out.println("+++++++++++++COMMITS+++++++++++++");
            assertEquals(expectedLogs, total);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
