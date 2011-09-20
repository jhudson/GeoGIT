package org.geogit.repository.remote;

import java.util.Map;

import org.geogit.repository.remote.payload.IPayload;

public interface IRemote {
    public void dispose();
    public IPayload requestFetchPayload( Map<String, String> branchHeads );
}