/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.security.MessageDigest;
import java.util.Arrays;

import com.google.common.base.Preconditions;
import com.sleepycat.persist.model.Persistent;

/**
 * A semi-mutable SHA-1 abstraction.
 * <p>
 * An ObjectId is mutable as long as it has not been assigned a raw value already
 * </p>
 */
@Persistent
public class ObjectId implements Comparable<ObjectId> {
    public static final ObjectId NULL = new ObjectId(new byte[20]);

    private byte[] raw;

    public ObjectId() {
        this(NULL.raw);
    }

    public ObjectId(byte[] raw) {
        Preconditions.checkNotNull(raw);
        Preconditions.checkArgument(raw.length == 20);
        this.raw = raw;
    }

    public boolean isNull() {
        return NULL.equals(this);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ObjectId)) {
            return false;
        }
        return Arrays.equals(raw, ((ObjectId) o).raw);
    }

    @Override
    public int hashCode() {
        final int hashCode = readInt(raw, 4);
        return hashCode;
    }

    /**
     * @return a human friendly representation of this SHA1
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final byte[] hash = this.raw;
        return toString(hash);
    }

    /**
     * Returns the objectid represented by its string form, this method is the inverse of
     * {@link #toString()}
     * 
     * @return
     */
    public static ObjectId valueOf(final String hash) {
        Preconditions.checkNotNull(hash);
        Preconditions.checkArgument(hash.length() == 40);

        // this is perhaps the worse way of doing this...

        final byte[] raw = new byte[20];
        final int radix = 16;
        for (int i = 0; i < 20; i++) {
            raw[i] = (byte) Integer.parseInt(hash.substring(2 * i, 2 * i + 2), radix);
        }
        return new ObjectId(raw);
    }

    public static byte[] toRaw(final String hash) {
        Preconditions.checkNotNull(hash);
        for (int i = 0; i < hash.length(); i++) {
            char c = hash.charAt(i);
            if (-1 == Character.digit(c, 16)) {
                throw new IllegalArgumentException("At index " + i
                        + ": partialId is not a valid hash subsequence '" + hash + "'");
            }
        }

        final byte[] raw = new byte[hash.length() / 2];
        final int radix = 16;
        for (int i = 0; i < raw.length; i++) {
            raw[i] = (byte) Integer.parseInt(hash.substring(2 * i, 2 * i + 2), radix);
        }
        return raw;
    }

    public static String toString(final byte[] hash) {
        // not sure if there's an utility someplace to convert the hash to an hex string, took this
        // from the web
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < hash.length; i++) {
            int halfbyte = (hash[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    buf.append((char) ('0' + halfbyte));
                else
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = hash[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final ObjectId o) {
        int c;
        for (int i = 0; i < raw.length; i++) {
            c = raw[i] - o.raw[i];
            if (c != 0) {
                return c;
            }
        }
        return 0;
        // int i1 = readInt(raw, 0);
        // int i2 = readInt(o.raw, 0);
        // if (i1 != i2) {
        // return i2 - i1;
        // }
        //
        // i1 = readInt(raw, 4);
        // i2 = readInt(o.raw, 4);
        // if (i1 != i2) {
        // return i2 - i1;
        // }
        //
        // i1 = readInt(raw, 8);
        // i2 = readInt(o.raw, 8);
        // if (i1 != i2) {
        // return i2 - i1;
        // }
        //
        // i1 = readInt(raw, 16);
        // i2 = readInt(o.raw, 16);
        //
        // return i2 - i1;
    }

    private static final int readInt(final byte[] raw, final int from) {
        int ch1 = raw[from] << 24;
        int ch2 = raw[from + 1] << 16;
        int ch3 = raw[from + 2] << 8;
        int ch4 = raw[from + 3] << 0;

        return ch1 + ch2 + ch3 + ch4;
    }

    public byte[] getRawValue() {
        return raw.clone();
    }

    /**
     * Creates a new SHA-1 ObjectId for the byte[] contents of the given string.
     * <p>
     * Note this method is to hash a string, not to convert the string representation of an ObjectId
     * </p>
     * 
     * @param strToHash
     * @return
     */
    public static ObjectId forString(final String strToHash) {
        Preconditions.checkNotNull(strToHash);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] raw = md.digest(strToHash.getBytes("UTF-8"));
            return new ObjectId(raw);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Prints the object ID just like the git command "0000000..0000000"
     * @return
     */
    public String printSmallId(){
        String out = toString();
        return out.substring(0, 7)+".."+out.substring(out.length()-7, out.length());
    }

}
