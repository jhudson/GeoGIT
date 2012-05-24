/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.geogit.api.RevObject.TYPE;
import org.geogit.api.merge.MergeResult;
import org.geogit.api.merge.MergeUtils;
import org.geogit.api.merge.strategy.ReverseRebaseMergeOp;
import org.geogit.repository.Repository;
import org.geogit.test.MultipleRepositoryTestCase;
import org.opengis.feature.Feature;

public class MergeOpOnlineTest extends MultipleRepositoryTestCase {

	private GeoGIT server;

	private GeoGIT server2;

	private GeoGIT client;

	public MergeOpOnlineTest() {
		super(3/* three repositories */);
	}

	@Override
	protected void setUpInternal() throws Exception {
		// setup repository 1 - acting as our server
		this.server = new GeoGIT(getRepository(0));

		// setup repository 2 - acting as out client
		this.client = new GeoGIT(getRepository(1));

		// setup repository 1 - acting as our server
		this.server2 = new GeoGIT(getRepository(2));

		printHeads();
	}

	@Override
	protected void tearDownInternal() throws Exception {
		printHeads();
		this.server.getRepository().close();
		this.server2.getRepository().close();
		this.client.getRepository().close();
		super.tearDownInternal();
	}

	private void printHeads() {
		LOGGER.info("CLIENT REMOTE BRANCH : "
				+ this.client.getRepository().getRefDatabase()
						.getRefs(Ref.REMOTES_PREFIX));
		LOGGER.info("CLIENT HEAD          : "
				+ this.client.getRepository().getHead());
		LOGGER.info("SERVER HEAD          : "
				+ this.server.getRepository().getHead());
	}

	public void testOneCommitMerge() throws Exception {
		// setup the client to have a remote ref to the server
		this.client.remoteAddOp().setName("project0").setFetch("project0")
				.setUrl(GEOGIT_URL + "/project0/geogit").call();

		/**
		 * INSERT 1 - SERVER
		 */
		insertAddCommit(this.server, points1);
		this.server.getRepository().close();

		// fetch the remotes
		client.fetch().call();

		// re-open the server
		this.server = new GeoGIT(createRepo(0, false));

		Ref clientRemoteMaster = this.client.getRepository().getRef(
				Ref.REMOTES_PREFIX + "project0/" + Ref.MASTER);
		assertEquals(clientRemoteMaster.getObjectId(), this.server
				.getRepository().getHead().getObjectId());

		/**
		 * MERGE - REBASE
		 */
		client.merge().setComment("Merged remote, one commit merge")
				.include(clientRemoteMaster).call();

		assertEquals(this.client.getRepository().getHead().getObjectId(),
				this.server.getRepository().getHead().getObjectId());
	}

	public void testMergeOneRemoteNonFastForward() throws Exception {
		insertAddCommit(this.server, points1);
		insertAddCommit(this.server, points3);
		this.server.getRepository().close();

		// setup the client to have a remote ref to the server
		this.client.remoteAddOp().setName("project0").setFetch("project0")
				.setUrl(GEOGIT_URL + "/project0/geogit").call();

		// fetch the remotes
		client.fetch().call();

		// re-open the server
		this.server = new GeoGIT(createRepo(0, false));

		Ref clientRemoteMaster = this.client.getRepository().getRef(
				Ref.REMOTES_PREFIX + "project0/" + Ref.MASTER);
		assertEquals(clientRemoteMaster.getObjectId(), this.server
				.getRepository().getHead().getObjectId());

		printHeads();

		// add more to server
		insertAddCommit(this.server, lines1);
		insertAddCommit(this.server, lines2);
		insertAddCommit(this.server, lines3);
		this.server.getRepository().close();

		// fetch the remotes
		client.fetch().call();

		// re-open the server
		this.server = new GeoGIT(createRepo(0, false));

		clientRemoteMaster = this.client.getRepository().getRef(
				Ref.REMOTES_PREFIX + "project0/" + Ref.MASTER);

		assertEquals(clientRemoteMaster.getObjectId(), this.server
				.getRepository().getHead().getObjectId());

		// merge the fetch
		client.merge().setComment("Merged remote, simple")
				.include(clientRemoteMaster).call();

		assertEquals(clientRemoteMaster.getObjectId(), this.client
				.getRepository().getHead().getObjectId());
	}

	public void testMergeCrissCrossTest() throws Exception {
		// setup the client to have a remote ref to the server
		this.client.remoteAddOp().setName("project0").setFetch("project0")
				.setUrl(GEOGIT_URL + "/project0/geogit").call();

		/**
		 * INSERT 1 - SERVER
		 */
		insertAddCommit(this.server, points1);
		this.server.getRepository().close();

		// fetch the remotes
		client.fetch().call();

		// re-open the server
		this.server = new GeoGIT(createRepo(0, false));

		Ref clientRemoteMaster = this.client.getRepository().getRef(
				Ref.REMOTES_PREFIX + "project0/" + Ref.MASTER);
		assertEquals(clientRemoteMaster.getObjectId(), this.server
				.getRepository().getHead().getObjectId());

		/**
		 * MERGE - REBASE
		 */
		client.merge().include(clientRemoteMaster).call();

		/**
		 * INSERT 1 - CLIENT
		 */
		insertAddCommit(this.client, points2);

		/**
		 * INSEERT 2 - SERVER
		 */
		insertAddCommit(this.server, lines1);
		this.server.getRepository().close();

		// fetch the remotes
		client.fetch().call();

		// re-open the server
		this.server = new GeoGIT(createRepo(0, false));

		clientRemoteMaster = this.client.getRepository().getRef(
				Ref.REMOTES_PREFIX + "project0/" + Ref.MASTER);
		assertEquals(clientRemoteMaster.getObjectId(), this.server
				.getRepository().getHead().getObjectId());

		// merge the fetch
		client.merge().setComment("Merged remote, criss cross test 1")
				.include(clientRemoteMaster).call();

		/**
		 * INSERT 2 - CLIENT
		 */
		RevCommit clientsLastCommit = insertAddCommit(this.client, points3);

		/**
		 * INSEERT 3 - SERVER
		 */
		RevCommit serversLastCommit = insertAddCommit(this.server, lines3);
		this.server.getRepository().close();

		// fetch the remotes
		client.fetch().call();

		// re-open the server
		this.server = new GeoGIT(createRepo(0, false));

		clientRemoteMaster = this.client.getRepository().getRef(
				Ref.REMOTES_PREFIX + "project0/" + Ref.MASTER);
		assertEquals(clientRemoteMaster.getObjectId(), this.server
				.getRepository().getHead().getObjectId());

		// merge the fetch
		client.merge().setComment("Merged remote, criss cross test 2")
				.include(clientRemoteMaster).call();

		RevCommit clientHead = this.client.getRepository().getCommit(
				this.client.getRepository().getHead().getObjectId());
		assertContains(clientHead.getParentIds(), clientsLastCommit,
				serversLastCommit);
	}

	public void testMergeRemoteMasterRetrieveFeature() throws Exception {
		ObjectId featureRefId1 = insert(this.server, points1);
		ObjectId featureRefId2 = insert(this.server, points2);
		ObjectId featureRefId3 = insert(this.server, lines1);
		ObjectId featureRefId4 = insert(this.server, lines2);
		server.commit().setMessage("commited a new feature").setAll(true)
				.call();
		this.server.getRepository().close();

		// setup the client to have a remote ref to the server
		this.client.remoteAddOp().setName("project0").setFetch("project0")
				.setUrl(GEOGIT_URL + "/project0/geogit").call();

		// fetch the remotes
		client.fetch().call();

		// re-open the server
		this.server = new GeoGIT(createRepo(0, false));

		Ref clientRemoteMaster = this.client.getRepository().getRef(
				Ref.REMOTES_PREFIX + "project0/" + Ref.MASTER);
		assertEquals(clientRemoteMaster.getObjectId(), this.server
				.getRepository().getHead().getObjectId());

		// merge the fetch
		client.merge().setComment("Merged remote, get features")
				.include(clientRemoteMaster).call();

		Feature feature1 = client.getRepository().getFeature(points1.getType(),
				points1.getIdentifier().getID(), featureRefId1);
		Feature feature2 = client.getRepository().getFeature(points2.getType(),
				points2.getIdentifier().getID(), featureRefId2);
		Feature feature3 = client.getRepository().getFeature(lines1.getType(),
				lines1.getIdentifier().getID(), featureRefId3);
		Feature feature4 = client.getRepository().getFeature(lines2.getType(),
				lines2.getIdentifier().getID(), featureRefId4);

		assertEquals(points1, feature1);
		assertEquals(points2, feature2);
		assertEquals(lines1, feature3);
		assertEquals(lines2, feature4);

		assertEquals(clientRemoteMaster.getObjectId(), this.client
				.getRepository().getHead().getObjectId());
	}

	public void testMergeComplex() throws Exception {
		ObjectId featureRefId1 = insert(this.server, points1);
		ObjectId featureRefId2 = insert(this.server, points3);
		RevCommit serversLastCommit = server.commit()
				.setMessage("commited a new feature").setAll(true).call();
		this.server.getRepository().close();

		// setup the client to have a remote ref to the server
		this.client.remoteAddOp().setName("project0").setFetch("project0")
				.setUrl(GEOGIT_URL + "/project0/geogit").call();

		// fetch the remotes
		client.fetch().call();

		// re-open the server
		this.server = new GeoGIT(createRepo(0, false));

		Ref clientRemoteMaster = this.client.getRepository().getRef(
				Ref.REMOTES_PREFIX + "project0/" + Ref.MASTER);
		assertEquals(clientRemoteMaster.getObjectId(), this.server
				.getRepository().getHead().getObjectId());

		// merge the fetch
		client.merge().include(clientRemoteMaster).call();

		// add to client to server
		// insertAddCommit(this.client, points3_conflict);
		ObjectId featureRefId3 = insert(this.client, points3_modify);
		RevCommit clientsLastCommit = this.client.commit()
				.setMessage("commited a modified feature").setAll(true).call();

		clientRemoteMaster = this.client.getRepository().getRef(
				Ref.REMOTES_PREFIX + "project0/" + Ref.MASTER);
		assertEquals(clientRemoteMaster.getObjectId(), this.server
				.getRepository().getHead().getObjectId());

		// merge the fetch
		MergeResult mergeResult = client.merge().include(clientRemoteMaster)
				.call();

		RevCommit clientHead = this.client.getRepository().getCommit(
				this.client.getRepository().getHead().getObjectId());
		assertContains(clientHead.getParentIds(), clientsLastCommit,
				serversLastCommit);

		Feature feature1 = client.getRepository().getFeature(pointsType,
				points1.getIdentifier().getID(), featureRefId1);
		Feature feature2 = client.getRepository().getFeature(pointsType,
				points3.getIdentifier().getID(), featureRefId2);
		Feature feature3 = client.getRepository().getFeature(pointsType,
				points3_modify.getIdentifier().getID(), featureRefId3);

		assertEquals(points1, feature1);
		assertEquals(points3, feature2);
		assertEquals(points3_modify, feature3);

		for (DiffEntry diff : mergeResult.getDiffs()) {
			ObjectId id = diff.getNewObjectId();
			Feature feature = this.client.getRepository().getFeature(
					pointsType, "Points.3", id);
			assertEquals(points3_modify, feature);
		}
	}

	public void testMergeRemoteMasterConflict() throws Exception {
		/**
		 * commit to the 'server'
		 */
		ObjectId featureRefId1 = insert(this.server, points_conflicting1_1);
		server.commit()
				.setMessage(
						"server - commited a new point feature at point(1 1)")
				.setAll(true).call();
		this.server.getRepository().close();

		/**
		 * Setup the client to have a remote ref to the server
		 */
		this.client.remoteAddOp().setName("project0").setFetch("project0")
				.setUrl(GEOGIT_URL + "/project0/geogit").call();

		/**
		 * Fetch the remotes
		 */
		client.fetch().call();

		/**
		 * Re-open the server
		 */
		this.server = new GeoGIT(createRepo(0, false));

		Ref clientRemoteMaster = this.client.getRepository().getRef(
				Ref.REMOTES_PREFIX + "project0/" + Ref.MASTER);
		assertEquals(clientRemoteMaster.getObjectId(), this.server
				.getRepository().getHead().getObjectId());

		// merge the fetch
		MergeResult mergeResults = client.merge().setMergeStrategy(new ReverseRebaseMergeOp())
				.setComment("Merged remote, conflicting features")
				.include(clientRemoteMaster).call();
		// for (DiffEntry diff : mergeResults.getDiffs()) {
		// System.out.println(diff);
		// }

		Feature featurePoints1 = client.getRepository().getFeature(
				points_conflicting1_1.getType(),
				points_conflicting1_1.getIdentifier().getID(), featureRefId1);

		/**
		 * Does the client has the server's committed feature?
		 */
		assertEquals(points_conflicting1_1, featurePoints1);

		/**
		 * yep...
		 * 
		 * ======== MAKE SOME CONFLICTS ========
		 * 
		 * Commit to the 'client' and to the 'server'
		 */
		insert(this.client, points1);
		ObjectId featureId3 = insert(this.client, points_conflicting3_3);
		this.client.commit().setMessage("client - commited changed the feature Points.4 from point(1 1) to point(3 3)").setAll(true).call();
		insert(this.client, points3_modify);
		this.client.commit().setMessage("client - commited some stuff Points.2").setAll(true).call();
		insert(this.client, points3);
		this.client.commit().setMessage("client - commited some stuff Points.3").setAll(true).call();
		
		insert(this.server, points3);
		insert(this.server, points2);
		insert(this.server, points_conflicting2_2);
		insert(this.server, points_conflicting4_4);
		RevCommit commit = this.server.commit().setMessage("server - commited changed the feature Points.4 from point(2 2) to point(4 4)").setAll(true).call();
		this.server.getRepository().close();

		/**
		 * Fetch from the server
		 */
		client.fetch().call();

		featurePoints1 = client.getRepository().getFeature(
				points_conflicting3_3.getType(),
				points_conflicting3_3.getIdentifier().getID(), featureId3);

		/**
		 * Does the client have the correctly committed feature still?
		 */
		assertEquals(points_conflicting3_3, featurePoints1);
		/**
		 * yep...
		 */

		/**
		 * Re-open the server
		 */
		this.server = new GeoGIT(createRepo(0, false));

		/**
		 * the fetch grabbed changes and stuck them into a temp branch called
		 * 'remotes/project0/master', is the ID of that the same as the HEAD ID
		 * of the server?
		 */
		clientRemoteMaster = this.client.getRepository().getRef(
				Ref.REMOTES_PREFIX + "project0/" + Ref.MASTER);
		assertEquals(clientRemoteMaster.getObjectId(), this.server
				.getRepository().getHead().getObjectId());

		/**
		 * 1. get a list of the features changed in the remote branch 2. check
		 * if they were also changed in the master 3. if no - continue if yes -
		 * merge the change - server wins, for the moment override the client
		 * change
		 * 
		 * ====================================================================
		 * ====================================================
		 */
		RevCommit branchHeadCommit = this.client.getRepository().getCommit(
				clientRemoteMaster.getObjectId());

		RevCommit branchSplit = MergeUtils.findBranchCommitSplit(branchHeadCommit,
				this.client.getRepository());
		//System.out.println(branchSplit);

		LogOp lo = new LogOp(this.client.getRepository());
		Iterator<RevCommit> logs = lo.setSince(branchSplit.getId())
				.setUntil(clientRemoteMaster.getObjectId()).call();
		System.out
				.println("++++++++++++++++++++++++++++TEST-PRE-MERGE-LOG-SERVER+++++++++++++++++++++++++++++++");
		Collection<Feature> serverFeatures = Collections.emptyList();
		while (logs.hasNext()) {
			RevCommit rc = logs.next();
			if (!MergeUtils.onBranch(this.client.getRepository().getHead().getObjectId(),rc.getId(), this.client.getRepository())){
				serverFeatures = extractFeatures(rc, this.client.getRepository());
			}
		}
		System.out
				.println("++++++++++++++++++++++++++++TEST-PRE-MERGE-LOG-SERVER+++++++++++++++++++++++++++++++");
		Collection<Feature> clientFeatures = Collections.emptyList();
		lo = new LogOp(this.client.getRepository());
		logs = lo.setSince(branchSplit.getParentIds().get(0)).call();
		System.out
				.println("++++++++++++++++++++++++++++TEST-PRE-MERGE-LOG-CLIENT+++++++++++++++++++++++++++++++");
		while (logs.hasNext()) {
			RevCommit rc = logs.next();
			clientFeatures = extractFeatures(rc, this.client.getRepository());
		}
		System.out
				.println("++++++++++++++++++++++++++++TEST-PRE-MERGE-LOG-CLIENT+++++++++++++++++++++++++++++++");
		System.out
				.println("++++++++++++++++++++++++++++TEST-PRE-MERGE-CONFLICT+++++++++++++++++++++++++++++++++");

		for (Feature serverFeature : serverFeatures) {
			for (Feature clientFeature : clientFeatures) {
				if (clientFeature.getIdentifier().equals(
						serverFeature.getIdentifier())) {

					System.out.println(clientFeature);
					System.out.println("is now -->");
					System.out.println(serverFeature);
				}
			}
		}
		System.out
				.println("++++++++++++++++++++++++++++PRE-MERGE-CONFLICT+++++++++++++++++++++++++++++++++");

		/**
		 * ====================================================================
		 * ===================================================================
		 */

		/**
		 * yep...
		 * 
		 * Merge the fetch
		 */
		mergeResults = client.merge().setMergeStrategy(new ReverseRebaseMergeOp())
				.setComment("Merged remote, conflicting features")
				.include(clientRemoteMaster).call();

		LogOp logOp = client.log();
		List<RevCommit> logList = toList(logOp.call());
		for (RevCommit log : logList) {
			extractFeatures(log, client.getRepository());
		}
		assertEquals(6, logList.size());
	}

	private Collection<Feature> extractFeatures(final RevCommit rc,
			final Repository repo) {
		System.out.println(rc);
		final Collection<Feature> features = new ArrayList<Feature>();
		RevTree t = repo.getTree(rc.getTreeId());
		t.accept(new TreeVisitor() {

			@Override
			public boolean visitSubTree(int bucket, ObjectId treeId) {
	            RevTree tree = client.getRepository().getTree(treeId);
	            tree.accept(this);
	            return true;
			}

			@Override
			public boolean visitEntry(Ref ref) {
				if (ref.getType().equals(RevObject.TYPE.TREE)) {
					RevTree tree = repo.getTree(ref.getObjectId());
					tree.accept(this);
				} else {
					if (TYPE.BLOB.equals(ref.getType())) {
						RevBlob revblob = repo.getObjectDatabase().getBlob(
								ref.getObjectId());
								Feature f = repo.getFeature(pointsType,
										ref.getName(), revblob.getId());
								System.out.println(f);
								features.add(f);
					}
				}

				return true;// continue
			}
		});
		return features;
	}
}
