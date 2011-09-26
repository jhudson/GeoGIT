package org.geogit.api;

import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.ByteArrayBuffer;
import org.geogit.repository.Repository;

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

    public PushOp( Repository repository, String upstream) {
        super(repository);
        this.upstream = upstream;
    }

    public void setUpstream(final String upstream){
        this.upstream = upstream;
    }

    @Override
    public PushResult call() throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();

        try {
            HttpPost put = new HttpPost(upstream);
           // ByteArrayEntity id = new ByteArrayEntity(getRepository().getHead().getObjectId().getRawValue());
           // put.setEntity(id);
            HttpResponse response = httpclient.execute(put);
            InputStream instream = response.getEntity().getContent();

            /**
             * READ THE ORIGIN HEAD
             */
            ByteArrayBuffer payloadBuffer = new ByteArrayBuffer(0);
            while (payloadBuffer.length() < 20) {
                int cc = instream.read();
                payloadBuffer.append(cc);
            }

            ObjectId originHeadId = new ObjectId(payloadBuffer.toByteArray());
            payloadBuffer = new ByteArrayBuffer(0);

            /**
             * we MUST have the head in our repo - or we drop out 
             */
            if (ObjectId.NULL.equals(originHeadId) || getRepository().commitExists(originHeadId)){
                /**
                 * Send the payload
                 */
                System.out.println("Sending payload to server");
                //put = new HttpPost(upstream);
                //id = new ByteArrayEntity(getRepository().getHead().getObjectId().getRawValue());
                put.setEntity(new StringEntity("JOHN WAS HERE"));
                //put.setEntity(id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            httpclient.getConnectionManager().shutdown();
        }

        return new PushResult();
    }
}