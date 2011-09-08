package org.geogit.storage.hessian;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.storage.ObjectWriter;

import com.caucho.hessian.io.Hessian2Output;

public class HessianCommitWriter extends HessianRevWriter implements
		ObjectWriter<RevCommit> {
	
	private RevCommit commit;

	public HessianCommitWriter(final RevCommit commit) {
		this.commit = commit;
	}

	@Override
	public void write(OutputStream out) throws IOException {
		Hessian2Output hout = new Hessian2Output(out);
		
		System.out.println("writing commit");
		hout.startMessage();
		
		writeObjectId(hout, commit.getTreeId());
		
		List<ObjectId> parentIds = commit.getParentIds();
		hout.writeInt(parentIds.size());
		for(ObjectId pId : parentIds) {
			writeObjectId(hout, pId);
		}
		hout.writeString(commit.getAuthor());
		hout.writeString(commit.getCommitter());
		hout.writeString(commit.getMessage());
		long timestamp = commit.getTimestamp();
		if(timestamp <= 0) {
			timestamp = System.currentTimeMillis();
		}
		hout.writeLong(timestamp);
		
		hout.completeMessage();
		
		hout.flush();
	}

}
