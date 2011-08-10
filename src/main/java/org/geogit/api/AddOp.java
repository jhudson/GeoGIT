/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.util.HashSet;
import java.util.Set;

import org.geogit.repository.Index;
import org.geogit.repository.Repository;

/**
 * Manipulates the index (staging area) by setting the unstaged changes that match this operation
 * criteria as staged.
 * 
 * @author groldan
 * 
 */
public class AddOp extends AbstractGeoGitOp<Index> {

    private Set<String> patterns;

    private boolean updateOnly;

    public AddOp(final Repository repository) {
        super(repository);
        patterns = new HashSet<String>();
    }

    /**
     * @see java.util.concurrent.Callable#call()
     */
    public Index call() throws Exception {
        final Index index = getRepository().getIndex();
        // this is add all, TODO: implement partial adds
        index.stage(getProgressListener(), null);
        return index;
    }

    /**
     * @param pattern
     *            a regular expression to match what content to be staged
     * @return {@code this}
     */
    public AddOp addPattern(final String pattern) {
        patterns.add(pattern);
        return this;
    }

    /**
     * @param updateOnly
     *            if {@code true}, only add already tracked features (either for modification or
     *            deletion), but do not stage any newly added one.
     * @return {@code this}
     */
    public AddOp setUpdateOnly(final boolean updateOnly) {
        this.updateOnly = updateOnly;
        return this;
    }

}
