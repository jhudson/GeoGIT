package org.geogit.storage.hessian;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.vividsolutions.jts.geom.Geometry;

public enum EntityType implements Serializable {
	STRING,
	BOOLEAN,
	BYTE,
	DOUBLE,
	BIGDECIMAL,
	FLOAT,
	INT,
	BIGINT,
	LONG,
	BOOLEAN_ARRAY,
	BYTE_ARRAY,
	CHAR_ARRAY,
	DOUBLE_ARRAY,
	FLOAT_ARRAY,
	INT_ARRAY,
	LONG_ARRAY,
	GEOMETRY,
	NULL,
	UNKNOWN_SERIALISABLE,
	UNKNOWN;
	
	public static EntityType determineType(Object value) {
		if(value == null)
			return NULL;
		if(value instanceof String)
			return STRING;
		if(value instanceof Boolean)
			return BOOLEAN;
		if(value instanceof Byte)
			return BYTE;
		if(value instanceof Double)
			return DOUBLE;
		if(value instanceof BigDecimal)
			return BIGDECIMAL;
		if(value instanceof Float)
			return FLOAT;
		if(value instanceof Integer)
			return INT;
		if(value instanceof BigInteger)
			return BIGINT;
		if(value instanceof Long)
			return LONG;
		if(value instanceof boolean[])
			return BOOLEAN_ARRAY;
		if(value instanceof byte[])
			return BYTE_ARRAY;
		if(value instanceof char[])
			return CHAR_ARRAY;
		if(value instanceof double[])
			return DOUBLE_ARRAY;
		if(value instanceof float[])
			return FLOAT_ARRAY;
		if(value instanceof int[])
			return INT_ARRAY;
		if(value instanceof long[])
			return LONG_ARRAY;
		if(value instanceof Geometry)
			return GEOMETRY;
		if(value instanceof Serializable)
			return UNKNOWN_SERIALISABLE;
		return UNKNOWN;
	}
	
}