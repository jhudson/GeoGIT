/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.repository;

public class Tuple<M, N, O> {

    private M first;

    private N middle;

    private O last;

    public Tuple(M first, N middle) {
        this(first, middle, null);
    }

    public Tuple(M first, N middle, O last) {
        this.first = first;
        this.middle = middle;
        this.last = last;
    }

    public M getFirst() {
        return first;
    }

    public N getMiddle() {
        return middle;
    }

    public O getLast() {
        return last;
    }
}
