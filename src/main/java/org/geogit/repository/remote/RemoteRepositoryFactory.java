package org.geogit.repository.remote;

import java.net.URI;

public class RemoteRepositoryFactory {

    /**
     * Create a remote repository connection to the parameter location string, if its a local repository 
     * (i.e. can be reached on the local machines addressable drives return a {@link LocalRemote} else if
     * it is remotely accessible via the network expect a {@link Remote} object
     *  
     * @param location String to specify the location of the remote repository
     * @return an IRemote
     * @author jhudson
     */
    public static IRemote createRemoteRepositroy(final String location) {        
        try {
            URI uri = URI.create(location);
            if ( uri.getHost() != null ) { 
                return new Remote( location );
            } else {
                return new LocalRemote( location );
            }
        } catch ( Throwable e ) {
            return new LocalRemote( location );
        }
    }
}
