/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.merge;

/**
 * Indicates there are is no branch to merge from
 * @author jhudson
 */
public class NoBranchToMergeException extends Exception {

    private static final long serialVersionUID = 1L;

    public NoBranchToMergeException(String msg) {
        super(msg);
    }
}
