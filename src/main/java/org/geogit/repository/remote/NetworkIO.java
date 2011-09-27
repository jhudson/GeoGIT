package org.geogit.repository.remote;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.apache.http.util.ByteArrayBuffer;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevBlob;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.repository.remote.payload.IPayload;
import org.geogit.repository.remote.payload.Payload;
import org.geogit.storage.BlobReader;
import org.geogit.storage.hessian.HessianCommitReader;
import org.geogit.storage.hessian.HessianCommitWriter;
import org.geogit.storage.hessian.HessianRevTreeReader;
import org.geogit.storage.hessian.HessianRevTreeWriter;

/**
 * <p>
 * Send and receive Payload objects with the GeoGIT protocol, as follows:
 * <p/>
 * 
 * The GeoGIT protocol is as follows:
 * <p/>
 * <p>
 * A Remote is a single end point of a request/response geogit instance which response is the geogit
 * protocol:
 * <p/>
 * <p>
 * This is a custom protocol which is used to transport all of the COMMIT/TREE/BLOB/BRANCH_HEAD objects to this
 * client this is the protocol: 
 * <p/>
 * <p>
 * [{C/T/B/N}{00000000000000000000}{0000000000}{PAYLOAD}] 
 * <p/>
 * <p>
 * first byte is a single character :
 * <ul>
 * <li>'C' for a commit</li>
 * <li>'T' for a tree </li>
 * <li>'B' for a blob</li>
 * <li>'N' for branch head</li>
 * </ul>
 * </p>
 * <p> 
 * 2nd byte to the 21st byte are the objects ID - 20 bytes
 * </p>
 * <p> 
 * 22nd byte to the 31st byte is the objects length - 10 bytes
 * </p>
 * <p>
 * The rest is the payload in bytes
 * </p>
 * @author jhudson
 * @since 1.2.0
 */
public class NetworkIO {

    private static char type_null = '\u0000';
    private static char type = type_null;
    private static int length = 0;
    private static ObjectId objectId = null;
    
    /**
     * Send a payload using the GeoGIT protocol
     * @param payload
     * @param out
     */
    public static void sendPayload(final IPayload payload, final OutputStream output) throws Exception {
        int commits = 0;
        for( RevCommit commit : payload.getCommitUpdates() ) {
            output.write("C".getBytes());
            output.write(commit.getId().getRawValue());            
            HessianCommitWriter cw = new HessianCommitWriter(commit);
            ByteArrayOutputStream bufferedCount = new ByteArrayOutputStream();
            cw.write(bufferedCount);
            output.write(Arrays.copyOf((String.valueOf(bufferedCount.size())).getBytes(), 10));
            cw.write(output);
            commits++;
            //System.out.println(commit);
        }
        //System.out.println("  " + commits + " new commits");

        int trees = 0;
        for( RevTree tree : payload.getTreeUpdates() ) {
            output.write("T".getBytes());
            output.write(tree.getId().getRawValue());            
            HessianRevTreeWriter tw = new HessianRevTreeWriter(tree);
            ByteArrayOutputStream bufferedCount = new ByteArrayOutputStream();
            tw.write(bufferedCount);
            output.write(Arrays.copyOf((String.valueOf(bufferedCount.size())).getBytes(), 10));
            tw.write(output);
            trees++;
            //System.out.println( tree.getId() + " : " + bufferedCount.size() + " : " + tree);
        }
        //System.out.println("  " + trees + " new trees");

        int blobs = 0;
        for( RevBlob blob : payload.getBlobUpdates() ) {
            byte[] blobBytes = (byte[])blob.getParsed();
            int length = blobBytes.length;

            output.write("B".getBytes());
            output.write(blob.getId().getRawValue());
            output.write(Arrays.copyOf(String.valueOf(length).getBytes(), 10));
            output.write((byte[])blob.getParsed());

            blobs++;
            //System.out.println( blob.getId() + " : " + length + " : " + blob);
        }
        //System.out.println("  " + blobs + " new blobs");

        /**
         * Send the branch heads: which is the master for now
         */
        int branches = 0;
        for( String branchName : payload.getBranchUpdates().keySet() ) {
            Ref branch = payload.getBranchUpdates().get(branchName);
            output.write("N".getBytes());
            output.write(branch.getObjectId().getRawValue());
            output.write(Arrays.copyOf(String.valueOf(branch.getName().getBytes().length).getBytes(), 10));
            output.write(branch.getName().getBytes());
            branches++;
            //System.out.println( branch );
        }
        //System.out.println("  " + branches + " new branches");
    }
    
    /**
     * Parses the actual payload from the server
     * 
     * @param instream
     * @param response
     * @return Payload
     * @throws IOException
     */
    public static Payload receivePayload(InputStream instream) throws IOException {
        final Payload payload = new Payload();
        try {
            ByteArrayBuffer payloadBuffer = new ByteArrayBuffer(0);

            int c;

            while ((c = instream.read()) != -1) {

                type = (char) c;

                while (payloadBuffer.length() < 20) {
                    int cc = instream.read();
                    payloadBuffer.append(cc);
                }

                objectId = extractObjectId(payloadBuffer.toByteArray());
                payloadBuffer = new ByteArrayBuffer(0);

                while (payloadBuffer.length() < 10) {
                    payloadBuffer.append(instream.read());
                }
                length = extractLength(payloadBuffer.toByteArray());
                payloadBuffer = new ByteArrayBuffer(0);

                while (payloadBuffer.length() < length) {
                    payloadBuffer.append(instream.read());
                }

                if (type == 'C') {
                    RevCommit commit = extractCommit(objectId, payloadBuffer.toByteArray());
                    payload.addCommits(commit);
                    // System.out.println(commit);
                } else if (type == 'T') {
                    RevTree tree = extractTree(objectId, payloadBuffer.toByteArray());
                    payload.addTrees(tree);
                    // System.out.println(tree);
                } else if (type == 'B') {
                    RevBlob blob = extractBlob(objectId, payloadBuffer.toByteArray());
                    payload.addBlobs(blob);
                    // System.out.println(blob);
                } else if (type == 'N') {
                    String branchName = new String(payloadBuffer.toByteArray());
                    Ref branchRef = new Ref(branchName, objectId, TYPE.REMOTE);
                    payload.addBranches(branchName, branchRef);
                    // System.out.println(branchName + " added to payload");
                }
                payloadBuffer = new ByteArrayBuffer(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            instream.close();
        }
        return payload;
    }

    private static RevTree extractTree(ObjectId objectId, byte[] buffer) throws IOException {
        ByteArrayInputStream b = new ByteArrayInputStream(buffer);
        HessianRevTreeReader tr = new HessianRevTreeReader(null);
        RevTree tree = tr.read(objectId, b);
        return tree;
    }

    private static RevBlob extractBlob(ObjectId objectId, byte[] buffer) throws IOException {
        ByteArrayInputStream b = new ByteArrayInputStream(buffer);
        BlobReader br = new BlobReader();
        RevBlob blob = br.read(objectId, b);
        return blob;
    }

    private static RevCommit extractCommit(ObjectId objectId, byte[] buffer) throws IOException {
        ByteArrayInputStream b = new ByteArrayInputStream(buffer);

        HessianCommitReader cr = new HessianCommitReader();
        RevCommit commit = cr.read(objectId, b);
        return commit;
    }

    private static ObjectId extractObjectId(byte[] byteArray) {
        return new ObjectId(byteArray);
    }

    private static int extractLength(byte[] byteArray) {
        String value = new String(byteArray).trim();
        return Integer.parseInt(value);
    }
}
