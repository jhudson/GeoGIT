package org.geogit.repository.remote;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.httpclient.URIException;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
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
        //HttpClient httpclient = new DefaultHttpClient();
       try {

            StringBuffer branchBuffer = new StringBuffer();

            for (String branchName : branchHeads.keySet()){
                branchBuffer.append(branchName+":"+branchHeads.get(branchName)+",");
            }

            String branches = branchBuffer.toString(); 
            
            if (branches.length()>0){
                branches = branches.substring(branches.length()-1);
            }
            
            //HttpGet httpget = new HttpGet(location+"?branches="+branches);
            
            // Create a response handler
//            ResponseHandler<String> responseHandler = new BasicResponseHandler();
//            String responseBody;
//            try {
//                responseBody = httpclient.execute(httpget, responseHandler);
//                System.out.println("----------------------------------------");
//                System.out.println(responseBody);
//                System.out.println("----------------------------------------");
//            } catch (ClientProtocolException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
            
            DefaultHttpClient httpclient = new DefaultHttpClient();
            
            try {
                httpclient.addRequestInterceptor(new HttpRequestInterceptor() {

                    public void process(
                            final HttpRequest request,
                            final HttpContext context) throws HttpException, IOException {
                        if (!request.containsHeader("Accept-Encoding")) {
                            request.addHeader("Accept-Encoding", "gzip");
                        }
                    }

                });

                httpclient.addResponseInterceptor(new HttpResponseInterceptor() {

                    public void process(
                            final HttpResponse response,
                            final HttpContext context) throws HttpException, IOException {
                        HttpEntity entity = response.getEntity();
                        Header ceheader = entity.getContentEncoding();
                        if (ceheader != null) {
                            HeaderElement[] codecs = ceheader.getElements();
                            for (int i = 0; i < codecs.length; i++) {
                                if (codecs[i].getName().equalsIgnoreCase("gzip")) {
                                    response.setEntity(
                                            new GzipDecompressingEntity(response.getEntity()));
                                    return;
                                }
                            }
                        }
                    }

                });

                HttpGet httpget = new HttpGet(location+"?branches="+branches);
                
                System.out.println("executing request " + httpget.getURI());

                // Execute HTTP request
                System.out.println("executing request " + httpget.getURI());
                HttpResponse response = httpclient.execute(httpget);

                System.out.println("----------------------------------------");
                System.out.println(response.getStatusLine());
                System.out.println(response.getLastHeader("Content-Encoding"));
                System.out.println(response.getLastHeader("Content-Length"));
                System.out.println("----------------------------------------");

                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    String content = EntityUtils.toString(entity);
                    System.out.println(content);
                    System.out.println("----------------------------------------");
                    System.out.println("Uncompressed size: "+content.length());
                }

            } catch (Exception e){} 
            finally {
                // When HttpClient instance is no longer needed,
                // shut down the connection manager to ensure
                // immediate deallocation of all system resources
                httpclient.getConnectionManager().shutdown();
            }
            

        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
           // httpclient.getConnectionManager().shutdown();
        }
        return null;
    }
    
    
}