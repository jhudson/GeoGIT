/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.merge;

import org.geogit.api.Ref;
import org.geogit.repository.Repository;

/**
 * IMergeOp interface for custom merge operations
 * @author jhudson
 */
public interface IMergeOp {
	public MergeResult call() throws Exception;
	public void setBranch(final Ref branch);
	public void setComment(final String comment);
	public void setRepository(final Repository respoitory);
}
