package org.geogit.storage.hessian;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.repository.CommitBuilder;

import junit.framework.TestCase;

public class HessianCommitSerialisationTest extends TestCase {
	public void testCommitRoundTrippin() throws Exception {
		long currentTime = System.currentTimeMillis();
		CommitBuilder builder = new CommitBuilder();
		String author = "groldan";
		builder.setAuthor(author);
		String committer = "mleslie";
		builder.setCommitter(committer);
		builder.setTimestamp(currentTime);
		
		ObjectId commitId = ObjectId.forString("Fake commit");
		ObjectId treeId = ObjectId.forString("Fake tree");
		builder.setTreeId(treeId);
		
		ObjectId parent1 = ObjectId.forString("Parent 1 of fake commit");
		ObjectId parent2 = ObjectId.forString("Parent 2 of fake commit");
		List<ObjectId> parents = Arrays.asList(parent1, parent2);
		builder.setParentIds(parents);
		
		RevCommit cmtIn = builder.build(commitId);
		assertNotNull(cmtIn);
		
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		HessianCommitWriter write = new HessianCommitWriter(cmtIn);
		write.write(bout);
		
		byte[] bytes = bout.toByteArray();
		assertTrue(bytes.length > 0);
		
		ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
		HessianCommitReader read = new HessianCommitReader();
		
		RevCommit cmtOut = read.read(commitId, bin);
		
		assertEquals(treeId, cmtOut.getTreeId());
		assertEquals(parents, cmtOut.getParentIds());
		assertEquals(committer, cmtOut.getCommitter());
		assertEquals(author, cmtOut.getAuthor());
		assertEquals(currentTime, cmtOut.getTimestamp());
		
	}
}
