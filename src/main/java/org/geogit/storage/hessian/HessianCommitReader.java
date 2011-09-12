package org.geogit.storage.hessian;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.repository.CommitBuilder;
import org.geogit.storage.ObjectReader;

import com.caucho.hessian.io.Hessian2Input;

public class HessianCommitReader extends HessianRevReader implements
		ObjectReader<RevCommit> {

	@Override
	public RevCommit read(ObjectId id, InputStream rawData) throws IOException,
			IllegalArgumentException {
		Hessian2Input hin = new Hessian2Input(rawData);
		CommitBuilder builder = new CommitBuilder();
		
		hin.startMessage();
		int typeValue = hin.readInt();
		
		builder.setTreeId(readObjectId(hin));
		int parentCount = hin.readInt();
		List<ObjectId> pIds = new ArrayList<ObjectId>(parentCount);
		for(int i = 0; i < parentCount; i++) {
			pIds.add(readObjectId(hin));
		}
		builder.setParentIds(pIds);
		builder.setAuthor(hin.readString());
		builder.setCommitter(hin.readString());
		builder.setMessage(hin.readString());
		builder.setTimestamp(hin.readLong());
		
		hin.completeMessage();
		
		return builder.build(id);
	}
}
