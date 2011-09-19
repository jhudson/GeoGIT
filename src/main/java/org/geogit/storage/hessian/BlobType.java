package org.geogit.storage.hessian;

/**
 * This enum describes what is encoded in a blob.
 * 
 * @author mleslie
 */
enum BlobType {
    /**
     * Blob encodes a feature object
     */
    FEATURE(0),
    /**
     * Blob encodes a RevTree
     */
    REVTREE(1),
    /**
     * Blob encodes a Commit
     */
    COMMIT(2);

    private int value;

    private BlobType(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static BlobType fromValue(int value) {
        for (BlobType type : BlobType.values()) {
            if (type.value == value)
                return type;
        }
        return null;
    }
}
