package org.geogit.repository.remote;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.geogit.api.Ref;
import org.geogit.repository.Repository;
import org.geogit.repository.remote.payload.IPayload;

 /**
  * A Remote is a single end point of a request/response geogit instance which response to git protocol  
  * 
  * @author jhudson
  */
public class Remote extends AbstractRemote {

    private final URI uri;
    
    public Remote( String location ) throws URIException, NullPointerException {
        this.uri = new URI(location, true);
    }

    public URI getUri() {
        return uri;
    }

    @Override
    public String toString() {
        return "Remote [uri=" + getUri() + "]";
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
	public IPayload requestFetchPayload(Ref head) {
		// TODO Auto-generated method stub
		return null;
	}





}