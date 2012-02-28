/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.hessian;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.vividsolutions.jts.geom.Geometry;

/**
 * This enum describes the data type of each encoded feature attribute.
 * 
 * The integer value of each data type should never be changed, for backwards compatibility
 * purposes. If the method of encoding of an attribute type is changed, a new type should be
 * created, with a new value, and the writers updated to use it. The readers should continue to
 * support both the old and new versions.
 * 
 * @author mleslie
 */
enum EntityType implements Serializable {
    STRING(0), BOOLEAN(1), BYTE(2), DOUBLE(3), BIGDECIMAL(4), FLOAT(5), INT(6), BIGINT(7), LONG(8), BOOLEAN_ARRAY(
            11), BYTE_ARRAY(12), CHAR_ARRAY(13), DOUBLE_ARRAY(14), FLOAT_ARRAY(15), INT_ARRAY(16), LONG_ARRAY(
            17), GEOMETRY(9), NULL(10), UNKNOWN_SERIALISABLE(18), UNKNOWN(19), UUID(20);

    public static EntityType determineType(Object value) {
        if (value == null)
            return NULL;
        if (value instanceof String)
            return STRING;
        if (value instanceof Boolean)
            return BOOLEAN;
        if (value instanceof Byte)
            return BYTE;
        if (value instanceof Double)
            return DOUBLE;
        if (value instanceof BigDecimal)
            return BIGDECIMAL;
        if (value instanceof Float)
            return FLOAT;
        if (value instanceof Integer)
            return INT;
        if (value instanceof BigInteger)
            return BIGINT;
        if (value instanceof Long)
            return LONG;
        if (value instanceof boolean[])
            return BOOLEAN_ARRAY;
        if (value instanceof byte[])
            return BYTE_ARRAY;
        if (value instanceof char[])
            return CHAR_ARRAY;
        if (value instanceof double[])
            return DOUBLE_ARRAY;
        if (value instanceof float[])
            return FLOAT_ARRAY;
        if (value instanceof int[])
            return INT_ARRAY;
        if (value instanceof long[])
            return LONG_ARRAY;
        if (value instanceof java.util.UUID)
            return UUID;
        if (value instanceof Geometry)
            return GEOMETRY;
        if (value instanceof Serializable)
            return UNKNOWN_SERIALISABLE;
        return UNKNOWN;
    }

    private int value;

    private EntityType(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    /**
     * Determines the EntityType given its integer value.
     * 
     * @param value The value of the desired EntityType, as read from the blob
     * @return The correct EntityType for the value, or null if none is found.
     */
    public static EntityType fromValue(int value) {
        for (EntityType type : EntityType.values()) {
            if (type.value == value)
                return type;
        }
        return null;
    }
}