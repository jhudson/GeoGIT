/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.repository.remote;

import java.util.Map;

import org.geogit.repository.remote.payload.IPayload;

public interface IRemote {
    public void dispose();
    public IPayload requestFetchPayload( Map<String, String> branchHeads );
}