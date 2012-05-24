package org.geogit.api.merge;

import java.util.logging.Logger;

import org.geogit.api.Ref;
import org.geogit.repository.Repository;
import org.geotools.util.logging.Logging;

/**
 * Abstract merge op gives implementations simple methods for setters
 * @author jhudson
 *
 */
public class AbstractMergeOp implements IMergeOp {

	protected Logger LOGGER;
	protected Repository repository;
	protected Ref branch;
	protected String comment;
	
	protected AbstractMergeOp(){
		this.LOGGER = Logging.getLogger(getClass());
		this.comment = "";
	}
	
	@Override
	public MergeResult call() throws Exception {
		return null;
	}

	public Repository getRepository() {
		return repository;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public Ref getBranch() {
		return branch;
	}

	public void setBranch(Ref branch) {
		this.branch = branch;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}
}
