/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.repository;

public class Triplet<M, N, O> extends Tuple<M, N> {

    private O last;

    public Triplet(M first, N middle, O last) {
        super(first, middle);
        this.last = last;
    }

    public O getLast() {
        return last;
    }
}
