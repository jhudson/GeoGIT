/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.geogit.api.MutableTree;
import org.geogit.api.ObjectId;
import org.geogit.api.PrintTreeVisitor;
import org.geogit.api.Ref;
import org.geogit.api.RevBlob;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.TreeVisitor;
import org.geogit.test.RepositoryTestCase;
import org.w3c.dom.Document;

import com.vividsolutions.jts.util.Stopwatch;

public class RevSHA1TreeTest extends RepositoryTestCase {

    private ObjectDatabase odb;

    @Override
    protected void setUpInternal() throws Exception {
        odb = repositoryDatabase.getObjectDatabase();
    }

    public void testPutGet() throws Exception {

        final int numEntries = 1000 * 10;
        final ObjectId treeId;

        Stopwatch sw;
        sw = new Stopwatch();
        sw.start();
        treeId = createAndSaveTree(numEntries, true);
        sw.stop();

        System.err.println("\n" + sw.toString());
        System.err.println("... at " + (numEntries / ((double) sw.getTime() / 1000L)) + "/s");

        // System.err.println("\nPut " + numEntries + " in " + sw.getLastTaskTimeMillis() + "ms ("
        // + (numEntries / sw.getTotalTimeSeconds()) + "/s)");

        sw.start();
        RevTree tree = odb.get(treeId, WrappedSerialisingFactory.getInstance().createRevTreeReader(odb, 0));
        sw.stop();
        System.out.println("Retrieved tree in " + sw.getTime() + "ms");

        sw = new Stopwatch();
        sw.start();
        PrintWriter writer = new PrintWriter(System.err);
        PrintTreeVisitor visitor = new PrintTreeVisitor(writer, odb);
        tree.accept(visitor);
        writer.flush();
        sw.stop();
        System.err.println("\nTraversed " + numEntries + " in " + sw.getTime() + "ms ("
                + (numEntries / ((double) sw.getTime() / 1000L)) + "/s)\n");
        assertEquals(numEntries, visitor.visitedEntries);

        tree = odb.get(treeId, WrappedSerialisingFactory.getInstance().createRevTreeReader(odb, 0));
        sw = new Stopwatch();
        sw.start();
        System.err.println("Reading " + numEntries + " entries....");
        for (int i = 0; i < numEntries; i++) {
            if ((i + 1) % (numEntries / 10) == 0) {
                System.err.print("#" + (i + 1));
            } else if ((i + 1) % (numEntries / 100) == 0) {
                System.err.print('.');
            }
            String key = "Feature." + i;
            ObjectId oid = ObjectId.forString(key);
            Ref ref = tree.get(key);
            assertNotNull(ref);
            assertEquals(key, oid, ref.getObjectId());
        }
        sw.stop();
        System.err.println("\nGot " + numEntries + " in " + sw.getTime() + "ms ("
                + (numEntries / ((double) sw.getTime() / 1000L)) + "/s)\n");

    }

    public void testRemove() throws Exception {
        final int numEntries = 1000;
        ObjectId treeId = createAndSaveTree(numEntries, true);
        RevTree tree = odb.get(treeId, WrappedSerialisingFactory.getInstance().createRevTreeReader(odb, 0));

        // collect some keys to remove
        final Set<String> removedKeys = new HashSet<String>();
        tree.accept(new TreeVisitor() {
            int i = 0;

            public boolean visitSubTree(int bucket, ObjectId treeId) {
                return true;
            }

            public boolean visitEntry(final Ref entry) {
                if (i % 10 == 0) {
                    removedKeys.add(entry.getName());
                }
                return true;
            }
        });

        tree = tree.mutable();
        for (String key : removedKeys) {
            ((MutableTree) tree).remove(key);
        }

        for (String key : removedKeys) {
            assertNull(tree.get(key));
        }

        final ObjectId newTreeId = odb.put(WrappedSerialisingFactory.getInstance().createRevTreeWriter(tree));
        RevTree tree2 = odb.get(newTreeId, WrappedSerialisingFactory.getInstance().createRevTreeReader(odb, 0));

        for (String key : removedKeys) {
            assertNull(tree2.get(key));
        }
    }

    public void testSize() throws Exception {
        final int numEntries = RevSHA1Tree.SPLIT_FACTOR + 1000;
        ObjectId treeId = createAndSaveTree(numEntries, true);
        RevTree tree = odb.get(treeId, WrappedSerialisingFactory.getInstance().createRevTreeReader(odb, 0));

        int size = tree.size().intValue();
        assertEquals(numEntries, size);

        // add a couple more
        final int added = 25000;
        tree = tree.mutable();
        for (int i = numEntries; i < numEntries + added; i++) {
            put((MutableTree) tree, i);
        }

        size = tree.size().intValue();
        assertEquals(numEntries + added, size);

        // save and compute again
        treeId = odb.put(WrappedSerialisingFactory.getInstance().createRevTreeWriter(tree));
        tree = odb.get(treeId, WrappedSerialisingFactory.getInstance().createRevTreeReader(odb, 0));

        size = tree.size().intValue();
        assertEquals(numEntries + added, size);

        // remove some keys
        final int removed = RevSHA1Tree.SPLIT_FACTOR;
        tree = tree.mutable();
        for (int i = 1; i <= removed; i++) {
            String key = "Feature." + (size - i);
            ((MutableTree) tree).remove(key);
        }

        size = tree.size().intValue();
        assertEquals(numEntries + added - removed, tree.size().intValue());
        // save and compute again
        treeId = odb.put(WrappedSerialisingFactory.getInstance().createRevTreeWriter(tree));
        tree = odb.get(treeId, WrappedSerialisingFactory.getInstance().createRevTreeReader(odb, 0));
        size = tree.size().intValue();
        assertEquals(numEntries + added - removed, tree.size().intValue());

        // replacing an existing key should not change size
        tree = tree.mutable();
        for (int i = 0; i < size / 2; i += 2) {
            String key = "Feature." + i;
            ObjectId otherId = ObjectId.forString(key + "changed");
            ((MutableTree) tree).put(new Ref(key, otherId, TYPE.BLOB));
        }
        final int expected = size;
        size = tree.size().intValue();
        assertEquals(expected, tree.size().intValue());
        // save and compute again
        treeId = odb.put(WrappedSerialisingFactory.getInstance().createRevTreeWriter(tree));
        tree = odb.get(treeId, WrappedSerialisingFactory.getInstance().createRevTreeReader(odb, 0));
        size = tree.size().intValue();
        assertEquals(expected, tree.size().intValue());
    }

    public void testIterator() throws Exception {
        final int numEntries = RevSHA1Tree.SPLIT_FACTOR + 1000;
        ObjectId treeId = createAndSaveTree(numEntries, true);
        RevTree tree = odb.get(treeId, WrappedSerialisingFactory.getInstance().createRevTreeReader(odb, 0));

        Iterator<Ref> iterator = tree.iterator(null);
        assertNotNull(iterator);
        int count = 0;
        while (iterator.hasNext()) {
            assertNotNull(iterator.next());
            count++;
        }
        assertEquals(numEntries, count);
    }
    
    public void testPrint() throws Exception {
    	final int numEntries = RevSHA1Tree.SPLIT_FACTOR + 1000;
    	ObjectId treeId = createAndSaveTree(numEntries, true);
    	InputStream in = odb.getRaw(treeId);
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	WrappedSerialisingFactory.getInstance().createBlobPrinter().print(in, System.out);
    	
    	in = odb.getRaw(treeId);
    	WrappedSerialisingFactory.getInstance().createBlobPrinter().print(in, new PrintStream(out));
    	
    	Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    			.parse(new ByteArrayInputStream(out.toByteArray()));
    }

    /**
     * Assert two trees that have the same contents resolve to the same id regardless of the order
     * the contents were added
     * 
     * @throws Exception
     */
    public void testEquality() throws Exception {
        testEquality(100);
        testEquality(100 + RevSHA1Tree.SPLIT_FACTOR);
    }

    private void testEquality(final int numEntries) throws Exception {
        final ObjectId treeId1;
        final ObjectId treeId2;
        treeId1 = createAndSaveTree(numEntries, true);
        treeId2 = createAndSaveTree(numEntries, false);

        assertEquals(treeId1, treeId2);
    }

    private ObjectId createAndSaveTree(final int numEntries, final boolean insertInAscendingKeyOrder)
            throws Exception {
        final ObjectId treeId;

        RevTree tree = createTree(numEntries, insertInAscendingKeyOrder);
        treeId = odb.put(WrappedSerialisingFactory.getInstance().createRevTreeWriter(tree));
        return treeId;
    }

    private RevTree createTree(final int numEntries, final boolean insertInAscendingKeyOrder) {
        MutableTree tree = new RevSHA1Tree(odb).mutable();

        final int increment = insertInAscendingKeyOrder ? 1 : -1;
        final int from = insertInAscendingKeyOrder ? 0 : numEntries - 1;
        final int breakAt = insertInAscendingKeyOrder ? numEntries : -1;

        int c = 0;
        for (int i = from; i != breakAt; i += increment, c++) {
            put(tree, i);
            if ((c + 1) % (numEntries / 10) == 0) {
                System.err.print("#" + (c + 1));
            } else if ((c + 1) % (numEntries / 100) == 0) {
                System.err.print('.');
            }
        }
        System.err.print('\n');
        return tree;
    }

    private void put(MutableTree tree, int i) {
        String key;
        ObjectId oid;
        key = "Feature." + i;
        oid = ObjectId.forString(key);
        tree.put(new Ref(key, oid, TYPE.BLOB));
    }

    public static void main(String[] argv) {
        RevSHA1TreeTest test = new RevSHA1TreeTest();
        try {
            test.setUp();
            try {
                test.testPutGet();
            } finally {
                test.tearDown();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        System.exit(0);
    }
}
