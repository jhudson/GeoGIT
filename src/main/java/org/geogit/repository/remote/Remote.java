/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.repository.remote;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
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
import org.geogit.storage.hessian.HessianRevTreeReader;

/**
 * A Remote is a single end point of a request/response geogit instance which response is the geogit
 * protocol:
 * 
 * This is a custom protocol which is used to transport all of the COMMIT/TREE/BLOB/BRANCH_HEAD objects to this
 * client this is the protocol: 
 * 
 * [{C/T/B}{00000000000000000000}{0000000000}{PAYLOAD}]
 * 
 * first byte is a single character : 
 * 'C' for a commit
 * 'T' for a tree
 * 'B' for a blob
 * 'N' for branch head
 * 
 * 2nd byte to the 21st byte are the objects ID - 20 bytes 
 * 22nd byte to the 31st byte is the objects length - 10 bytes
 * 
 * The rest is the payload in bytes
 * 
 * @author jhudson
 */
public class Remote extends AbstractRemote {

    private final String location;

    private char type_null = '\u0000';
    private char type = type_null;
    private int length = 0;
    private ObjectId objectId = null;

    public Remote(String location) throws NullPointerException {
        this.location = location;
    }

    @Override
    public void dispose() {
        // TODO Auto-generated method stub
    }

    @Override
    public IPayload requestFetchPayload(Map<String, String> branchHeads) {
        
        Payload payload = null;

        StringBuffer branchBuffer = new StringBuffer();

        for (String branchName : branchHeads.keySet()) {
            branchBuffer.append(branchName + ":" + branchHeads.get(branchName) + ",");
        }

        String branches = branchBuffer.toString();

        if (branches.length() > 0) {
            branches = branches.substring(0,branches.length() - 1);
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
     * Parses the actual payload from the server
     * 
     * @param instream
     * @param response
     * @return Payload
     * @throws IOException
     */
    private Payload parsePayload(InputStream instream, HttpResponse response) throws IOException {
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

    private RevTree extractTree(ObjectId objectId, byte[] buffer) throws IOException {
        ByteArrayInputStream b = new ByteArrayInputStream(buffer);
        HessianRevTreeReader tr = new HessianRevTreeReader(null);
        RevTree tree = tr.read(objectId, b);
        return tree;
    }

    private RevBlob extractBlob(ObjectId objectId, byte[] buffer) throws IOException {
        ByteArrayInputStream b = new ByteArrayInputStream(buffer);
        BlobReader br = new BlobReader();
        RevBlob blob = br.read(objectId, b);
        return blob;
    }

    private RevCommit extractCommit(ObjectId objectId, byte[] buffer) throws IOException {
        ByteArrayInputStream b = new ByteArrayInputStream(buffer);

        HessianCommitReader cr = new HessianCommitReader();
        RevCommit commit = cr.read(objectId, b);
        return commit;
    }

    private ObjectId extractObjectId(byte[] byteArray) {
        return new ObjectId(byteArray);
    }

    private int extractLength(byte[] byteArray) {
        String value = new String(byteArray).trim();
        return Integer.parseInt(value);
    }
}