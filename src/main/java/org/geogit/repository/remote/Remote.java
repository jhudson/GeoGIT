package org.geogit.repository.remote;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.httpclient.URIException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.ByteArrayBuffer;
import org.geogit.api.ObjectId;
import org.geogit.api.RevBlob;
import org.geogit.api.RevCommit;
import org.geogit.api.RevTree;
import org.geogit.repository.Repository;
import org.geogit.repository.remote.payload.IPayload;
import org.geogit.repository.remote.payload.LocalPayload;
import org.geogit.storage.BlobReader;
import org.geogit.storage.hessian.HessianCommitReader;
import org.geogit.storage.hessian.HessianRevTreeReader;

/**
 * A Remote is a single end point of a request/response geogit instance which response to git
 * protocol
 * 
 * @author jhudson
 */
public class Remote extends AbstractRemote {

    private final String location;
    private char type_null = '\u0000';
    private final static int BUFFER_SIZE = 2048;

    public Remote( String location ) throws URIException, NullPointerException {
        this.location = location;
    }

    @Override
    public Repository getRepository() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setRepository( Repository repo ) {
        // TODO Auto-generated method stub
    }

    @Override
    public void dispose() {
        // TODO Auto-generated method stub
    }

    @Override
    public IPayload requestFetchPayload( Map<String, String> branchHeads ) {

        LocalPayload payload = null;

        StringBuffer branchBuffer = new StringBuffer();

        for( String branchName : branchHeads.keySet() ) {
            branchBuffer.append(branchName + ":" + branchHeads.get(branchName) + ",");
        }

        String branches = branchBuffer.toString();

        if (branches.length() > 0) {
            branches = branches.substring(branches.length() - 1);
        }

        DefaultHttpClient httpclient = new DefaultHttpClient();

        try {
            HttpGet httpget = new HttpGet(location + "?branches=" + branches);
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            payload = parsePayload(entity.getContent(), response);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            httpclient.getConnectionManager().shutdown();
        }

        return payload;
    }

    /**
     * This is a custom protocol which is used to transport all of the COMMIT/TREE/BLOB objects to
     * this client this is the protocol: [{C/T/B}{00000000000000000000}{0000000000}{PAYLOAD}] first
     * byte is a single character : 'C' for a commit 'T' for a tree 'B' for a blob 2nd byte to the
     * 21st byte are the objects ID - 20 bytes 22nd byte to the 31st byte is the objects length - 10
     * bytes the rest is the payload
     * 
     * @param instream
     * @param response
     * @return
     * @throws IOException
     */
    
    int read = 0;
    
    private LocalPayload parsePayload( InputStream instream, HttpResponse response )
            throws IOException {
        final LocalPayload payload = new LocalPayload();
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            char type = type_null;
            int length = 0;
            ObjectId objectId = null;

            /**
             * This is the main payload buffer, its filled from 0 to MAX with the current payload -
             * in this context the payload is the CURRENT only COMMT/TREE/BLOB
             */
            ByteArrayBuffer payloadBuffer = new ByteArrayBuffer(1);

            // consume until EOF
            while( (read = instream.read(buffer, read, BUFFER_SIZE)) != -1 ) {

                System.out.println("read: " + read);
                System.out.println("buffer.length " + buffer.length);
                System.out.println("buffer[0] " + (char) buffer[0]);

                if (type == type_null) { // first byte is our type
                    type = (char) buffer[0];

                    buffer = resetBuffer(buffer, 1);
                }

                if (objectId == null) {
                    objectId = extractObjectId(Arrays.copyOfRange(buffer, 0, 20));

                    buffer = resetBuffer(buffer, 20);
                }

                if (length == 0) {
                    length = extractLength(Arrays.copyOfRange(buffer, 0, 10));

                    buffer = resetBuffer(buffer, 10);
                }

                if (type != type_null && objectId != null && length != 0) {

                    System.out.println("TYPE: " + type);
                    System.out.println("OBJECT ID: " + objectId);
                    System.out.println("LENGTH: " + length);

                    if (length > buffer.length) {
                        payloadBuffer.append(buffer, 0, buffer.length);
                        buffer = resetBuffer(buffer, buffer.length);
                    } else {
                        int to = length - payloadBuffer.length();
                        // System.out.println("adding " + to + " to payload buffer");
                        payloadBuffer.append(buffer, 0, to);
                        buffer = resetBuffer(buffer, to);
                    }

                    if (payloadBuffer.buffer().length >= length) { /* YAY, we have full payload */
                        // System.out.println("Full payload of type : " + type);
                        if (type == 'C') {
                            RevCommit commit = extractCommit(objectId, payloadBuffer.buffer());
                            payload.addCommits(commit);
                            System.out.println(commit);
                        } else if (type == 'T') {
                            RevTree tree = extractTree(objectId, payloadBuffer.buffer());
                            payload.addTrees(tree);
                            System.out.println(tree);
                        } else if (type == 'B') {
                            RevBlob blob = extractBlob(objectId, payloadBuffer.buffer());
                            payload.addBlobs(blob);
                            System.out.println("blob");
                        }
                        type = type_null;
                        objectId = null;
                        length = 0;
                        buffer = Arrays.copyOfRange(buffer, 0, buffer.length + (BUFFER_SIZE - buffer.length)); /* push is back to big */
                        payloadBuffer = new ByteArrayBuffer(1);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // instream.close();
        }
        return payload;
    }

    private byte[] resetBuffer( byte[] buffer, int from ) {
        // System.out.println("Moving buffer forward: " + from );
        read -= from;
        return Arrays.copyOfRange(buffer, from, buffer.length);
    }

    private RevTree extractTree( ObjectId objectId, byte[] buffer ) throws IOException {
        ByteArrayInputStream b = new ByteArrayInputStream(buffer);
        HessianRevTreeReader tr = new HessianRevTreeReader(null);
        RevTree tree = tr.read(objectId, b);
        return tree;
    }

    private RevBlob extractBlob( ObjectId objectId, byte[] buffer ) throws IOException {
        ByteArrayInputStream b = new ByteArrayInputStream(buffer);
        BlobReader br = new BlobReader();
        RevBlob blob = br.read(objectId, b);
        return blob;
    }

    private RevCommit extractCommit( ObjectId objectId, byte[] buffer ) throws IOException {
        ByteArrayInputStream b = new ByteArrayInputStream(buffer);

        HessianCommitReader cr = new HessianCommitReader();
        RevCommit commit = cr.read(objectId, b);
        return commit;
    }

    private ObjectId extractObjectId( byte[] byteArray ) {
        return new ObjectId(byteArray);
    }

    private int extractLength( byte[] byteArray ) {
        String value = new String(byteArray).trim();
        return Integer.parseInt(value);
    }
}