/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

public class PlatformResolver implements CommitStateResolver {

    /**
     * @see org.CommitStateResolver.data.versioning.AuthenticationResolver#getAuthor()
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

    @Override
    public long getCommitTimeMillis() {
        return System.currentTimeMillis();
    }

}
