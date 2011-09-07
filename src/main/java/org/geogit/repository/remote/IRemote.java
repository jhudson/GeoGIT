package org.geogit.repository.remote;

import org.geogit.repository.Repository;

public interface IRemote {
    public Repository getRepository();
    public void setRepository( Repository repo );
    public void dispose();
}