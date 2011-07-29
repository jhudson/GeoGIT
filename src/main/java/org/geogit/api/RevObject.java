/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.api;

/**
 * Base object type accessed during revision walking.
 * 
 * @author groldan
 * @see RevCommit
 * @see RevTree
 * @see RevBlob
 * @see RevTag
 */
public abstract class RevObject {

    public static enum TYPE {
        COMMIT {
            @Override
            public int value() {
                return 0;
            }
        },
        TREE {
            @Override
            public int value() {
                return 1;
            }
        },
        BLOB {
            @Override
            public int value() {
                return 2;
            }
        },
        TAG {
            @Override
            public int value() {
                return 3;
            }
        };

        public abstract int value();

        public static TYPE valueOf(final int value) {
            return TYPE.values()[value];
        }
    }

    private ObjectId id;

    public RevObject(final ObjectId id) {
        this.id = id;
    }

    public abstract TYPE getType();

    /**
     * Get the name of this object.
     * 
     * @return unique hash of this object.
     */
    public final ObjectId getId() {
        return id;
    }

    /**
     * Equality is based on id
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RevObject)) {
            return false;
        }
        return id.equals(((RevObject) o).getId());
    }
}
