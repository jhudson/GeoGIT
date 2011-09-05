package org.geogit.storage;

import org.geogit.api.RevCommit;
import org.geogit.api.RevTree;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

public interface ObjectSerialisingFactory {
	public ObjectWriter<RevCommit> createCommitWriter(final RevCommit commit);
	public ObjectWriter<Feature> createFeatureWriter(final Feature feature);
	public ObjectWriter<RevTree> createRevTreeWriter(RevTree tree);
	public ObjectReader<RevCommit> createCommitReader(); 
	public ObjectReader<Feature> createFeatureReader(final FeatureType featureType, final String featureId);
	public ObjectReader<RevTree> createRevTreeReader(ObjectDatabase objectDb);
	public ObjectReader<RevTree> createRevTreeReader(ObjectDatabase objectDb, int order);

}
