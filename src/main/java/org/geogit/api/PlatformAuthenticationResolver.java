package org.geogit.api;

public class PlatformAuthenticationResolver implements AuthenticationResolver {

    /**
     * @see org.geoserver.data.versioning.AuthenticationResolver#getAuthor()
     */
    @Override
    public String getAuthor() {
        String userName = System.getProperty("user.name", "anonymous");
        return userName;
    }

    @Override
    public String getCommitMessage() {
        return null;
    }

    @Override
    public String getCommitter() {
        return getAuthor();
    }

}
