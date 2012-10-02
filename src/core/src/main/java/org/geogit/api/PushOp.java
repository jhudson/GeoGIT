/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.io.File;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.geogit.api.config.RefIO;
import org.geogit.repository.Repository;
import org.geogit.repository.remote.LocalRemote;
import org.geogit.repository.remote.PayloadEntity;
import org.geogit.repository.remote.payload.IPayload;

/**
 * Push operation to push the latest commits/tree/blob to the upstream remote
 * 
 * @author jhudson
 * @since 1.2.0
 */
public class PushOp extends AbstractGeoGitOp<PushResult> {

    /**
     * URL of the upstream, this could be refactored to be a URI
     */
    private String upstream;

    public PushOp(Repository repository, String upstream) {
        super(repository);
        this.upstream = upstream;
    }

    public void setUpstream(final String upstream) {
        this.upstream = upstream;
    }

    @Override
    public PushResult call() throws Exception {
        PushResult result = new PushResult();
        DefaultHttpClient httpclient = new DefaultHttpClient();

        try {
            /**
             * create a payload to send to the server: this should be a set of commits upto this
             * clients knowledge of the upstream HEAD
             */
            LocalRemote lr = new LocalRemote(getRepository());
            Map<String,String> originMaster = RefIO.getRemoteList(getRepository().getRepositoryHome(), "origin");
            IPayload payload = lr.requestFetchPayload(originMaster);

            HttpPost post = new HttpPost(upstream);
            post.setHeader(Ref.HEAD,originMaster.get(Ref.HEAD)); /*Set a header ID so the server can reject/accept*/
            post.setEntity(new PayloadEntity(payload));

            HttpResponse response = httpclient.execute(post);
            if (response.getStatusLine().getStatusCode()== HttpStatus.SC_CONFLICT){
                result.setStatus(PushResult.STATUS.CONFLICT);
            } if (response.getStatusLine().getStatusCode()== HttpStatus.SC_EXPECTATION_FAILED){
                result.setStatus(PushResult.STATUS.INCORRECT_PARAMETER);
            } else if (response.getStatusLine().getStatusCode()== HttpStatus.SC_NOT_ACCEPTABLE){
                result.setStatus(PushResult.STATUS.NO_CHANGE);
            } else if (response.getStatusLine().getStatusCode()== HttpStatus.SC_OK) {
                result.setStatus(PushResult.STATUS.OK_APPLIED);
                
                System.out.println(" trying to write commit: " + getRepository().getHead().getObjectId());
                RefIO.writeRemoteRefs( getRepository().getRepositoryHome(), "origin",
                		"HEAD", getRepository().getHead().getObjectId() );
                
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            httpclient.getConnectionManager().shutdown();
        }

        return result;
    }
}