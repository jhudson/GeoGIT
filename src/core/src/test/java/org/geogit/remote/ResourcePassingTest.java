package org.geogit.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.geogit.repository.Repository;
import org.geogit.util.RepositoryUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ResourcePassingTest {

	private static FetchResourceService fetch;
	private static int PORT = 8765;
	private static String REMOTE_ENV_HOME = "remote_repo";
	private static String LOCAL_ENV_HOME = "local_repo";
	private static String INDEX_PATH = "index";
	private static String REPO_PATH = "repository";
	
	@BeforeClass
	public static void setUpClass() throws IOException {
		/*
		 * Start the Fetch Resource Service to act as a remote repository.
		 */
		System.out.println("Starting service.");
		fetch = new FetchResourceService(PORT, REMOTE_ENV_HOME, INDEX_PATH, REPO_PATH);
		Thread thread = new Thread(fetch);
		thread.start();
	}
	
	@AfterClass
	public static void tearDownClass() throws Exception {
		System.out.println("Stopping service.");
		fetch.stop();
	}
	
	private File repoRoot;
	
	@Before
	public void setUp() throws Exception {
		/*
		 * Initialise local repository.
		 */
		File repoRootName = File.createTempFile("mock", "");
		if(!repoRootName.delete()) {
			throw new IOException("Could not delete " + repoRootName.getAbsolutePath());
		}
		repoRoot = new File(repoRootName.getPath() + "BlobStore");
		if(!repoRoot.mkdir()) {
			throw new IOException("Could not create directory " + repoRoot.getAbsolutePath());
		}
		
		File localEnvHome = new File(repoRoot, LOCAL_ENV_HOME);
		localEnvHome.mkdir();
		File localRepositoryHome = new File(localEnvHome, REPO_PATH);
		localRepositoryHome.mkdir();
		File localIndexHome = new File(localEnvHome, INDEX_PATH);
		localIndexHome.mkdir();
		Repository localRepo = RepositoryUtils.createRepository(
				localEnvHome, localRepositoryHome, localIndexHome);
		
		
		
		/*
		 * Initialise remote repository.
		 */
		File remoteEnvHome = new File(repoRoot, LOCAL_ENV_HOME);
		remoteEnvHome.mkdir();
		File remoteRepositoryHome = new File(remoteEnvHome, REPO_PATH);
		remoteRepositoryHome.mkdir();
		File remoteIndexHome = new File(remoteEnvHome, INDEX_PATH);
		remoteIndexHome.mkdir();
		Repository remoteRepo = RepositoryUtils.createRepository(
				remoteEnvHome, remoteRepositoryHome, remoteIndexHome);
		
		
	}
	
	@After
	public void tearDown() throws Exception {
		
	}
	
	@Test
	public void testPush() throws Exception {
		fail("Test not implemented.");
	}
	
	@Test
	public void testPull() throws Exception {
		fail("Test not implemented.");
	}
	
	@Test
	public void testMerge() throws Exception {
		fail("Test not implemented.");
	}
	
}
