/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.hessian;

import java.io.IOException;
import java.io.OutputStream;

import org.geogit.api.MutableTree;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevTree;
import org.geogit.api.TreeVisitor;
import org.geogit.storage.ObjectWriter;
import org.geogit.storage.RevSHA1Tree;

import com.caucho.hessian.io.Hessian2Output;
import com.google.common.base.Throwables;

class HessianRevTreeWriter extends HessianRevWriter implements ObjectWriter<RevTree> {
    private final RevSHA1Tree tree;

    public HessianRevTreeWriter(RevTree tree) {
        this.tree = (RevSHA1Tree) tree;
    }

    @Override
    public void write(OutputStream out) throws IOException {
        RevTree revTree = this.tree;
        if (!revTree.isNormalized()) {
            revTree = revTree.mutable();
            ((MutableTree) revTree).normalize();
        }
        Hessian2Output hout = new Hessian2Output(out);
        try {
            hout.startMessage();
            hout.writeInt(BlobType.REVTREE.getValue());

            byte[] size = revTree.size().toByteArray();
            hout.writeBytes(size);

            TreeVisitor v = new WritingTreeVisitor(hout);
            revTree.accept(v);
            hout.writeInt(HessianRevTreeReader.Node.END.getValue());

            hout.completeMessage();
        } finally {
            hout.flush();
        }
    }

    private final class WritingTreeVisitor implements TreeVisitor {
        private Hessian2Output hout;

        public WritingTreeVisitor(Hessian2Output out) {
            this.hout = out;
        }

        @Override
        public boolean visitEntry(Ref ref) {
            try {
                HessianRevTreeWriter.this.writeRef(hout, ref);
            } catch (IOException ex) {
                Throwables.propagate(ex);
            }
            return true;
        }

        @Override
        public boolean visitSubTree(int bucket, ObjectId treeId) {
            try {
                hout.writeInt(HessianRevReader.Node.TREE.getValue());
                hout.writeInt(bucket);
                HessianRevTreeWriter.this.writeObjectId(hout, treeId);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return false;
        }
    }
}
