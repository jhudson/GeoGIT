package org.geogit.repository.remote;

import java.util.List;
import java.util.Map;

import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.repository.Repository;

public interface IRemote {
    public Repository getRepository();
    public void setRepository( Repository repo );
    public void dispose();
    public List<RevCommit> requestCommitFetch( Ref head );
    public Map<String, Ref> requestBranchFetch();
}