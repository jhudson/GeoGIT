package org.geogit.storage.hessian;

enum BlobType {
	FEATURE(0),
	REVTREE(1),
	COMMIT(2);

	private int value;

	private BlobType(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return this.value;
	}
	
	public static BlobType fromValue(int value) {
		for(BlobType type : BlobType.values()) {
			if(type.value == value)
				return type;
		}
		return null;
	}
}
