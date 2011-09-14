package org.geogit.repository.remote;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.httpclient.URIException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.geogit.repository.Repository;
import org.geogit.repository.remote.payload.IPayload;

 /**
  * A Remote is a single end point of a request/response geogit instance which response to git protocol  
  * 
  * @author jhudson
  */
public class Remote extends AbstractRemote {

    private final String location;
    
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
        HttpClient httpclient = new DefaultHttpClient();
        try {

            StringBuffer branchBuffer = new StringBuffer();

            for (String branchName : branchHeads.keySet()){
                branchBuffer.append(branchName+":"+branchHeads.get(branchName)+",");
            }

            String branches = branchBuffer.toString(); 
            
            if (branches.length()>0){
                branches = branches.substring(branches.length()-1);
            }
            
            HttpGet httpget = new HttpGet(location+"?branches="+branches);

            System.out.println("executing request " + httpget.getURI());

            // Create a response handler
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responseBody;
            try {
                responseBody = httpclient.execute(httpget, responseHandler);
                System.out.println("----------------------------------------");
                System.out.println(responseBody);
                System.out.println("----------------------------------------");
            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            

        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();
        }
        return null;
    }
}