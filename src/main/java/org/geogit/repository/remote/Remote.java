<<<<<<< HEAD
=======
/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.0 license, available at the root
 * application directory.
 */
>>>>>>> upstream/master
package org.geogit.repository.remote;

import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
<<<<<<< HEAD
=======

import org.apache.http.util.ByteArrayBuffer;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevBlob;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;

>>>>>>> upstream/master
import org.geogit.repository.remote.payload.IPayload;
import org.geogit.repository.remote.payload.Payload;

/**
<<<<<<< HEAD
 * Using the NetworkIO class retrieve a remote GeoGIT repository
=======
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
>>>>>>> upstream/master
 * 
 * @author jhudson
 */
public class Remote extends AbstractRemote {

    private final String location;

    public Remote(String location) throws NullPointerException {
        this.location = location;
    }

    @Override
    public void dispose() {
        // TODO Auto-generated method stub
    }

    @Override
    public IPayload requestFetchPayload(Map<String, String> branchHeads) {
<<<<<<< HEAD

=======
        
>>>>>>> upstream/master
        Payload payload = null;

        StringBuffer branchBuffer = new StringBuffer();

        for (String branchName : branchHeads.keySet()) {
            branchBuffer.append(branchName + ":" + branchHeads.get(branchName) + ",");
        }

        String branches = branchBuffer.toString();

        if (branches.length() > 0) {
<<<<<<< HEAD
            branches = branches.substring(branches.length() - 1);
=======
            branches = branches.substring(0,branches.length() - 1);
>>>>>>> upstream/master
        }

        DefaultHttpClient httpclient = new DefaultHttpClient();

        try {
            HttpGet httpget = new HttpGet(location + "?branches=" + branches);
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            payload = NetworkIO.receivePayload(entity.getContent());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            httpclient.getConnectionManager().shutdown();
        }

        return payload;
    }
}