package org.geogit.storage.hessian;

import org.geogit.api.BlobPrinter;
import org.geogit.api.ObjectSerialisingFactory;
import org.geogit.api.RevCommit;
import org.geogit.api.RevTree;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.ObjectWriter;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

public class HessianFactory implements ObjectSerialisingFactory {

	@Override
	public ObjectWriter<RevCommit> createCommitWriter(RevCommit commit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectWriter<Feature> createFeatureWriter(Feature feature) {
		return new HessianFeatureWriter(feature);
	}

	@Override
	public ObjectWriter<RevTree> createRevTreeWriter(RevTree tree) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectReader<RevCommit> createCommitReader() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectReader<Feature> createFeatureReader(FeatureType featureType,
			String featureId) {
		return new HessianFeatureReader(featureType, featureId);
	}

	@Override
	public ObjectReader<RevTree> createRevTreeReader(ObjectDatabase objectDb) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectReader<RevTree> createRevTreeReader(ObjectDatabase objectDb,
			int order) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BlobPrinter createBlobPrinter() {
		// TODO Auto-generated method stub
		return null;
	}

}
