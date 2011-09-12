package org.geogit.api;

import org.geogit.api.RevObject.TYPE;
import org.geogit.api.config.Config;
import org.geogit.api.config.RemoteConfigObject;
import org.geogit.repository.Repository;

import com.google.common.base.Preconditions;

/**
 * Add a remote to the repository
 * 
 * @author jhudson
 * @since 1.2.0
 */
public class RemoteAddOp extends AbstractGeoGitOp<Void> {
    
    private static final String REFS_HEADS_REFS_REMOTES = "refs/heads/*:refs/remotes/";

    Config config;
    String name;
    String url;
    String fetch;

    public RemoteAddOp( Repository repository, Config config ) {
        super(repository);
        this.config = config;
    }

    @Override
    public Void call() {
        Preconditions.checkNotNull(this.name);
        Preconditions.checkNotNull(this.url);
        Preconditions.checkNotNull(this.fetch);
        RemoteConfigObject remote = new RemoteConfigObject(name, fetch, url);
        config.addRemoteConfigObject(remote);
        Ref remoteRef = new Ref(Ref.REMOTES_PREFIX+this.name+"/"+Ref.MASTER, ObjectId.forString(this.name), TYPE.REMOTE);
        getRepository().getRefDatabase().addRef(remoteRef);
        return null;
    }

    public RemoteAddOp setName(final String name) {
        this.name = name;
        return this;
    }
    
    public RemoteAddOp setFetch(final String fetch) {
        this.fetch = REFS_HEADS_REFS_REMOTES + fetch;
        return this;
    }
    
    public RemoteAddOp setUrl(final String url) {
        this.url = url;
        return this;
    }

}
