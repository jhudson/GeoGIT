package org.geogit.api;

import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.ObjectWriter;
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
	public BlobPrinter createBlobPrinter();

}
