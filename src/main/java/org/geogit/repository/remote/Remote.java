package org.geogit.repository.remote;

import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.geogit.repository.remote.payload.IPayload;
import org.geogit.repository.remote.payload.Payload;

/**
 * Using the NetworkIO class retrieve a remote GeoGIT repository
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

        for (String branchName : branchHeads.keySet()) {
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
            payload = NetworkIO.receivePayload(entity.getContent());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            httpclient.getConnectionManager().shutdown();
        }

        return payload;
    }
}