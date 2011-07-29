/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.custommonkey.xmlunit.XMLAssert;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.storage.BLOBS;
import org.geogit.storage.CommitReader;
import org.geogit.storage.CommitWriter;
import org.w3c.dom.Document;

import com.vividsolutions.jts.util.Stopwatch;

public class CommitReaderWriterTest extends TestCase {

    RevCommit commit;

    ObjectId treeId;

    ObjectId parentId1;

    ObjectId parentId2;

    @Override
    protected void setUp() throws Exception {
        CommitBuilder b = new CommitBuilder();
        b.setAuthor("groldan");
        b.setCommitter("jdeolive");
        b.setMessage("cool this works");
        b.setTimestamp(1000);

        treeId = ObjectId.forString("fake tree content");
        b.setTreeId(treeId);

        parentId1 = ObjectId.forString("fake parent content 1");
        parentId2 = ObjectId.forString("fake parent content 2");
        List<ObjectId> parentIds = Arrays.asList(parentId1, parentId2);
        b.setParentIds(parentIds);

        commit = b.build(ObjectId.NULL);
    }

    public void testBuildEmpty() throws Exception {
        CommitBuilder b = new CommitBuilder();
        b.setTreeId(ObjectId.NULL);
        commit = b.build(ObjectId.NULL);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new CommitWriter(commit).write(out);

        byte[] built = out.toByteArray();
        BLOBS.print(built, System.err);
        // transform to text xml for XPath evaluation
        out = new ByteArrayOutputStream();
        BLOBS.print(built, new PrintStream(out));

        Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(out.toByteArray()));
        XMLAssert.assertXpathEvaluatesTo("0000000000000000000000000000000000000000",
                "/commit/tree/objectid", dom);
        XMLAssert.assertXpathExists("/commit/parentids", dom);
        XMLAssert.assertXpathExists("/commit/author/null", dom);
        XMLAssert.assertXpathExists("/commit/committer/null", dom);
        XMLAssert.assertXpathExists("/commit/message/null", dom);
        XMLAssert.assertXpathExists("/commit/timestamp", dom);
    }

    public void testBuildFull() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new CommitWriter(commit).write(out);
        byte[] built = out.toByteArray();
        BLOBS.print(built, System.err);
        // transform to text xml for XPath evaluation
        out = new ByteArrayOutputStream();
        BLOBS.print(built, new PrintStream(out));

        Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(out.toByteArray()));
        XMLAssert.assertXpathEvaluatesTo(treeId.toString(), "/commit/tree/objectid", dom);
        XMLAssert
                .assertXpathEvaluatesTo(parentId1.toString(), "/commit/parentids/objectid[1]", dom);
        XMLAssert
                .assertXpathEvaluatesTo(parentId2.toString(), "/commit/parentids/objectid[2]", dom);
        XMLAssert.assertXpathEvaluatesTo("groldan", "/commit/author/string", dom);
        XMLAssert.assertXpathEvaluatesTo("jdeolive", "/commit/committer/string", dom);
        XMLAssert.assertXpathEvaluatesTo("cool this works", "/commit/message/string", dom);
        XMLAssert.assertXpathEvaluatesTo("1000", "/commit/timestamp/long", dom);
    }

    public void testBackAndForth() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new CommitWriter(commit).write(out);
        byte[] built = out.toByteArray();
        BLOBS.print(built, System.err);
        // transform to text xml for XPath evaluation
        out = new ByteArrayOutputStream();
        BLOBS.print(built, new PrintStream(out));

        RevCommit read = new CommitReader().read(ObjectId.NULL, new ByteArrayInputStream(built));
        assertNotNull(read);

        assertEquals(commit.getAuthor(), read.getAuthor());
        assertEquals(commit.getCommitter(), read.getCommitter());
        assertEquals(commit.getMessage(), read.getMessage());
        assertEquals(commit.getTimestamp(), read.getTimestamp());
        assertEquals(commit.getTreeId(), read.getTreeId());
        assertEquals(commit.getParentIds(), read.getParentIds());
    }

    public void testPerf() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int k = 5000;
        Stopwatch sw = new Stopwatch();
        sw.start();
        for (int i = 0; i < k; i++) {
            out.reset();
            new CommitWriter(commit).write(out);
        }
        sw.stop();
        // it's at ~1200/s
        System.err.printf("\nBuilt %d commits in %s, (%d/s)\n", k, sw.getTimeString(), k * 1000
                / sw.getTime());

        InputStream built = new ByteArrayInputStream(out.toByteArray());

        sw.start();
        // it's at ~700/s
        for (int i = 0; i < k; i++) {
            built.reset();
            new CommitReader().read(ObjectId.NULL, built);
        }
        sw.stop();
        System.err.printf("\nParsed %d commits in %s, (%d/s)\n", k, sw.getTimeString(), k * 1000
                / sw.getTime());
    }
}
