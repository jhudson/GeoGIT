/* Copyright (c) 2011-2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.repository.remote;

import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.geogit.repository.remote.payload.IPayload;
import org.geogit.repository.remote.payload.Payload;

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

        /*
         * Create a string to send to the geogit server in the form of:
         * BRANCH_NAME:UUID,...
         */
        for (String branchName : branchHeads.keySet()) {
            branchBuffer.append(branchName + ":" + branchHeads.get(branchName) + ",");
        }

        String branches = branchBuffer.toString();

        /*
         * remote the last ',' 
         */
        if (branches.length() > 0) {
            branches = branches.substring(0,branches.length() - 1);
        } else branches="HEAD";

        /*
         * Use the http client to communicate with the server geogit
         */
        DefaultHttpClient httpclient = new DefaultHttpClient();

        try {
            HttpGet httpget = new HttpGet(location + "?branches=" + branches);
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            /*
             * The NetworkIO class knows how to translate an input stream of bytes into a payload 
             * that GeoGit understands.
             */
            payload = NetworkIO.receivePayload(entity.getContent());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            httpclient.getConnectionManager().shutdown();
        }

        return payload;
    }
}