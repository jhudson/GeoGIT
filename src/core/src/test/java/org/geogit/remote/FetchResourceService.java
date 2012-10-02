package org.geogit.remote;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geogit.api.ObjectId;
import org.geogit.api.PayloadUtil;
import org.geogit.api.RebaseOp;
import org.geogit.api.Ref;
import org.geogit.repository.remote.LocalRemote;
import org.geogit.repository.remote.NetworkIO;
import org.geogit.repository.remote.payload.IPayload;
import org.geogit.repository.remote.payload.Payload;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.handler.ContextHandler;

public class FetchResourceService extends AbstractHandler implements Runnable {
	private String versionedEnvironmentHome;
	private String versionedIndexHome;
	private String versionedRepositoryHome;
	private int port;
	
	private Server server;

	public FetchResourceService(int port, String versionedEnvironmentHome, 
			String versionedIndexHome, 
			String versionedRepositoryHome) {
		this.port = port;
		this.versionedEnvironmentHome = versionedEnvironmentHome;
		this.versionedIndexHome = versionedIndexHome;
		this.versionedRepositoryHome = versionedRepositoryHome;
	}
	/**
	 * Logging instance
	 */
	static Logger logger = org.geotools.util.logging.Logging.getLogger("qpws.parkinfo.rest.geogit"); //$NON-NLS-1$
	
	@Override
	public void run() {
		Server server = new Server(port);
		
		
//		ContextHandler context = new ContextHandler();
//		context.setContextPath(CONTEXT_PATH);
//		context.setHandler(this);
//		context.setResourceBase(".");
//		context.setClassLoader(Thread.currentThread().getContextClassLoader());
		
		server.setHandler(this);
		
		try {
			server.start();
			server.join();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	/**
	 * The actual code that does the handling of requests.
	 */
	@SuppressWarnings("nls")
	@Override
	public void handle(String target, HttpServletRequest request,
			HttpServletResponse response, int dispatch) {

		/*
		 * This is just of we get things from the request, like the branches and stuff 
		 */
		String ctxPath = request.getContextPath();
		String reqPath = request.getRequestURI();
		if(ctxPath != null) {
			reqPath = reqPath.substring(ctxPath.length() + 1);
		}

		try {

			/*
			 * If its a post its a push request from the client - so to ease of reading split it 
			 * off to be handled elsewhere. 
			 */
			if ("POST".equals(request.getMethod())) {
				/*
				 * handle the push 
				 */
				handlePost(request, response);
			} else {
				/*
				 * GeoGit pull request is handled here
				 */

				logger.log(Level.FINE,"++++++++++++++++++++++ Doing a fetch ++++++++++++++++++++++");

				/*
				 * The 'branches' is a string like so: 
				 * 
				 *  BRANCH_NAME:UUID
				 *  
				 *  Where the BRANCH_NAME is usually "HEAD" but can be other branch names
				 */
				String branchString = request.getParameter("branches");

				/*
				 * This bit is breaking up the branch names and UUID's so we can work on them separately 
				 */
				Map<String, String> branchHeads = new HashMap<String, String>();
				if (branchString != null || "".equals(branchString)) {
					String[] branches = branchString.split(",");
					for (String branch : branches) {
						String[] split = branch.split(":");
						if (split != null && split.length == 2) {
							/*The branch name is usually "HEAD"*/
							String branchName = split[0];
							/*
							 * The branchHead is the UUID of the latest UUID known by the client - 
							 * - this is used to by the server (this) to determine exactly WHAT to send 
							 *   back to the client requesting the pull operation
							 */
							String branchHead = split[1];
							/*
							 * The result of the split, stick it all in a map 
							 */
							branchHeads.put(branchName, branchHead);
							logger.log(Level.FINE,"     " + branchName + " " + branchHead);
						}
					}
				}

				/*
				 * But it all together and you get the repository:
				 */
//				File projHome = new File(new File(versionedEnvironmentHome), repository);
				File projHome = new File(versionedEnvironmentHome);

				/*
				 * This is a bit redundant - if they dont exist... well damn, something really bad has
				 *  occurred and I hope you had tape backups
				 */
				if (!projHome.exists()) {
					projHome.mkdirs();
				}

				/*
				 * Still putting things together to create the repository database - this could be 
				 * improved significantly by making them default in the GeoGit project and only 
				 * necessary if you change them...
				 */
				File indexHome = new File(projHome, versionedIndexHome);
				File repoHome = new File(projHome, versionedRepositoryHome);
				if (!indexHome.exists()) {
					indexHome.mkdirs();
				}
				if (!repoHome.exists()) {
					repoHome.mkdirs();
				}

				//logger.log(Level.FINE,"projHome-  " + projHome.getAbsolutePath());
				//logger.log(Level.FINE,"indexHome-  " + indexHome.getAbsolutePath());
				//logger.log(Level.FINE,"repoHome-  " + repoHome.getAbsolutePath());

				/*
				 * A LocalRemote is an object holder or just that a locally accessible (so not on a http 
				 *  connection but on the hard disk drive) remote repository - this is used to create a
				 *  Payload. 
				 */
				LocalRemote lr = new LocalRemote(projHome.getAbsolutePath());

				/*
				 * The Payload is the object representation of what we are going to send to the client
				 *   - this holds the Commits/Trees/Blobs/Branches(name:HEAD_UUID)/Tags.   
				 */
				IPayload payload = lr.requestFetchPayload(branchHeads);

				/*
				 * crazy output, woo!
				 */
				logger.log(Level.FINE,"    commits:  " + payload.getCommitUpdates().size());
				logger.log(Level.FINE,"    trees:    " + payload.getTreeUpdates().size());
				logger.log(Level.FINE,"    blobs:    " + payload.getBlobUpdates().size());
				logger.log(Level.FINE,"    branches: " + payload.getBranchUpdates().size());
				logger.log(Level.FINE,"    tags:     " + payload.getTagUpdates().size());

				/*
				 * This is our communication back to the clinet, it wants something 
				 * (BRANCH_NAME:FROM_HEAD_UUID) 
				 */
				OutputStream output = response.getOutputStream();
				/*
				 * This is some crazy response object - its art, checkout the protocol in NetworkIO in 
				 * the GeoGit project...
				 */
				response.setContentType("binary/octet-stream");

				/*
				 * The work horse - send the payload to the output steam
				 */
				NetworkIO.sendPayload(payload, output);

				/*
				 * lets clean up our mess
				 */
				lr.dispose();
				output.close();

				/*
				 * more crazy output, woo!
				 */
				logger.log(Level.FINE,"++++++++++++++++++++++++++++++++++ DONE ++++++++++++++++++++++++++++++++++");
			}
		} catch(Exception ex) {
			logger.severe(ex.toString());
			throw new RuntimeException(ex);
		}

	}

	/**
	 * This is where a GeoGit push is handled, when a client geogit send a "push" command to me.
	 * 
	 * @param request
	 * @param response
	 * @param project
	 * @throws Exception
	 */
	@SuppressWarnings("nls")
	private void handlePost(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		logger.log(Level.FINE,"++++++++++++++++++++++++ GeoGit Push Request ++++++++++++++++++++++++");

		/*
		 * Make a file object out of it.
		 */
//		File projHome = new File(new File(versionedEnvironmentHome), project);
		File projHome = new File(versionedEnvironmentHome);

		/*
		 * A LocalRemote is an object holder or just that a locally accessible (so not on a http 
		 *  connection but on the hard disk drive) remote repository - this is used to create a
		 *  Payload. 
		 */
		LocalRemote lr = new LocalRemote(projHome.getAbsolutePath());

		/*
		 * The lastKnownHead variable is the UUID of the last know UUID the client has of the
		 *   repository, it should NOT be null or "", if it is, bad dog!
		 *   This is used to check for conflicts as the UUID sent through *MUST* be the same as the 
		 *   local repositories HEAD UUID, if its not then the client is out of sync, fail!
		 * 
		 * You will notice Ref.HEAD here, we **ONLY** handle pushes to HEAD - no branch support (yet)
		 */
		System.out.println("+++++++++++++++++++++++initial HEAD for remote server" + lr.getRepository().getHead().getObjectId());
		String lastKnownHead = request.getHeader(Ref.HEAD);
System.out.println("lastKnownHead check: " + lastKnownHead);

		/*
		 * If this fails, do nothing... consider returning an error
		 */
		if (lastKnownHead != null && !"".equals(lastKnownHead)) { 
			/* 
			 * Grab the UUID of the local repository HEAD UUID 
			 */
			ObjectId id = ObjectId.valueOf(lastKnownHead);

			/*
			 * Check it against the one send from the client
			 */
			if (!lr.getRepository().getHead().getObjectId().toString().equals(lastKnownHead)) {
				/*
				 * Arc!! they differ, fail out.
				 */
				response.sendError(HttpServletResponse.SC_CONFLICT,
						"The server does not support non fast-forward push commits, please do a pull first");
			} else {
				/*
				 * Looks ok, lets merge in their changes
				 */

				/*
				 * Where payload babies come from... 
				 */
				InputStream in = request.getInputStream();

				/*
				 * Get the clients payload and apply it to the index of our repository. 
				 *   this is art, checkout the NetworkIO in the GeoGit project.
				 */
				Payload payload = NetworkIO.receivePayload(in);

				
				/*
				 * Apply the payload to the local repository
				 */
				PayloadUtil payloadUtil = new PayloadUtil(lr.getRepository());

				/*
				 * A note: this applies the commits/trees/blobs/branchNames/tags to the local 
				 *    repository if and only if they do not already exist. And it DOESNT update the
				 *    HEAD pointer you need to do that - see the next line of code.
				 */
				payloadUtil.applyPayloadTo(Ref.HEAD, payload);
				//response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE,"Nothing to update");

				/*
				 * Rebase to the new head - effectively move the HEAD pointer to the new top commit
				 */
				RebaseOp rebase = new RebaseOp(lr.getRepository());
				/*
				 * call it 
				 */
				Ref payloadHead = payload.getBranchUpdates().get(Ref.HEAD);
				//ensure that head is not the same. If it is then we should send back a nothing updated message. 
				//Caused by consecutive pushes without a commit.
				if (lr.getRepository().getHead().getObjectId().equals(payloadHead.getObjectId())){
					response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE,"Nothing to update");
					response.flushBuffer();
				} else {
				rebase.include(payload.getBranchUpdates().get(Ref.HEAD)).call();
				}
				
			}
			System.out.println("+++++++++++++++++++++++final HEAD for remote server " + lr.getRepository().getHead().getObjectId());
			response.setStatus(HttpServletResponse.SC_OK);

			
			response.flushBuffer();
		} else {
				response.sendError(HttpServletResponse.SC_EXPECTATION_FAILED,"last known commit is empty.");
		}

		/*
		 * lets clean up our mess
		 */
		lr.dispose();

		/*
		 * output, arrr!
		 */
		logger.log(Level.WARNING,"+++++++++++++++++++++++++++++ DONE +++++++++++++++++++++++++++++");
	}




}
