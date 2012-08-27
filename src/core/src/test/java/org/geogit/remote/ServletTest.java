package org.geogit.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ServletTest {
	private static int PORT = 8765;
	private static String ENV_HOME = "remote_repo";
	private static String INDEX_PATH = "index";
	private static String REPO_PATH = "repository";
	
	private static FetchResourceService fetch = null;
	
	@BeforeClass
	public static void setUpClass() {
		System.out.println("Setting up service.");
		fetch = new FetchResourceService(PORT, ENV_HOME, INDEX_PATH, REPO_PATH);
		Thread thread = new Thread(fetch);
		thread.start();
	}
	
	@AfterClass
	public static void tearDownClass() throws Exception {
		System.out.println("Stopping service.");
		fetch.stop();
	}
	
	/**
	 * This is here for the sole purpose of ensuring that our FetchResponseService 
	 * is actually running and responding.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testEmptyRequest() throws Exception {
		assertNotNull(fetch);
		HttpResponse response = sendGet();
		assertNotNull(response);
		assertEquals(200, response.getStatusLine().getStatusCode());
		HttpEntity entity = response.getEntity();
		assertNotNull(entity);
	}
	
	private HttpResponse sendGet() throws Exception {
		DefaultHttpClient client = new DefaultHttpClient();
		
		HttpGet get = new HttpGet("http://localhost:" + PORT);
		HttpResponse response = client.execute(get);
		return response;
	}

}
