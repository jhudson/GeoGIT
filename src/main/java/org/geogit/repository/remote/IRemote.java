package org.geogit.repository.remote;

import java.util.Map;

import org.geogit.repository.Repository;
import org.geogit.repository.remote.payload.IPayload;

public interface IRemote {
    public Repository getRepository();
    public void setRepository( Repository repo );
    public void dispose();
    public IPayload requestFetchPayload( Map<String, String> branchHeads );
}