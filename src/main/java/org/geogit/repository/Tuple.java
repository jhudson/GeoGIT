/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.repository;

public class Tuple<M, N> {

    private M first;

    private N middle;

    public Tuple(M first, N middle) {
        this.first = first;
        this.middle = middle;
    }

    public M getFirst() {
        return first;
    }

    public N getMiddle() {
        return middle;
    }
}
