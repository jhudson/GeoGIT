/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.io.IOException;
import java.io.PrintWriter;

import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.WrappedSerialisingFactory;

public class PrintTreeVisitor implements TreeVisitor {
    private final ObjectDatabase odb;

    private final PrintWriter writer;

    private int depth;

    private int printlimit;

    private int unprinted;

    private int subtreeEntries;

    public int visitedEntries;

    private boolean print = false;

    public PrintTreeVisitor(final PrintWriter writer, final ObjectDatabase odb) {
        this.writer = writer;
        this.odb = odb;
    }

    /**
     * @see org.geogit.api.TreeVisitor#visitEntry(org.geogit.api.Ref)
     */
    public boolean visitEntry(final Ref ref) {
        visitedEntries++;
        subtreeEntries++;
        printlimit++;
        if (printlimit <= 1) {
            indent();
            println(ref.getName());
            writer.flush();
        } else {
            unprinted++;
        }
        return true;
    }

    public boolean visitSubTree(final int bucket, final ObjectId treeId) {
        try {
            // if (unprinted > 0) {
            // indent();
            // writer.println("...and " + unprinted + " more.");
            // unprinted = 0;
            // }

            if (subtreeEntries > 0) {
                println(" (" + subtreeEntries + " entries)");
            } else {
                println('\n');
            }
            subtreeEntries = 0;
            depth++;
            indent();
            print("order/bucket: " + depth + "/" + bucket);
            printlimit = 0;
            RevTree tree = odb.get(treeId, WrappedSerialisingFactory.getInstance().createRevTreeReader(odb, depth));
            tree.accept(this);
            depth--;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    private void println(char c) {
        if (print)
            writer.println(c);
    }

    private void println(String string) {
        if (print)
            writer.println(string);
    }

    private void print(String string) {
        if (print)
            writer.print(string);
    }

    private void indent() {
        if (print)
            for (int i = 0; i < depth; i++) {
                print('\t');
            }
    }

    private void print(char c) {
        if (print)
            writer.print(c);
    }

}
