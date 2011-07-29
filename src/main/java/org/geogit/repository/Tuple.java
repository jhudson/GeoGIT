/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.repository;

public class Tuple<M, N> {

    private M first;

    private N last;

    public Tuple(M first, N last) {
        this.first = first;
        this.last = last;
    }

    public M getFirst() {
        return first;
    }

    public N getLast() {
        return last;
    }
}
