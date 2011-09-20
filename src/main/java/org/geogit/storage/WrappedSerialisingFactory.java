package org.geogit.storage;

import org.geogit.api.BlobPrinter;
import org.geogit.api.ObjectSerialisingFactory;
import org.geogit.api.RevCommit;
import org.geogit.api.RevTree;
import org.geogit.repository.ConfigurationContext;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

public class WrappedSerialisingFactory implements ObjectSerialisingFactory {
	private static WrappedSerialisingFactory instance;
	
	private ObjectSerialisingFactory wrappedFactory;
	
	public ObjectWriter<RevCommit> createCommitWriter(RevCommit commit) {
		return wrappedFactory.createCommitWriter(commit);
	}

	public ObjectWriter<Feature> createFeatureWriter(Feature feature) {
		return wrappedFactory.createFeatureWriter(feature);
	}

	public ObjectWriter<RevTree> createRevTreeWriter(RevTree tree) {
		return wrappedFactory.createRevTreeWriter(tree);
	}

	public ObjectReader<RevCommit> createCommitReader() {
		return wrappedFactory.createCommitReader();
	}

	public ObjectReader<Feature> createFeatureReader(FeatureType featureType,
			String featureId) {
		return wrappedFactory.createFeatureReader(featureType, featureId);
	}

	public ObjectReader<RevTree> createRevTreeReader(ObjectDatabase objectDb) {
		return wrappedFactory.createRevTreeReader(objectDb);
	}

	public ObjectReader<RevTree> createRevTreeReader(ObjectDatabase objectDb,
			int order) {
		return wrappedFactory.createRevTreeReader(objectDb, order);
	}

	public BlobPrinter createBlobPrinter() {
		return wrappedFactory.createBlobPrinter();
	}

	public WrappedSerialisingFactory() {
		this.wrappedFactory = (ObjectSerialisingFactory)ConfigurationContext
				.getInstance().getBean("serialisingFactory");
	}
	
	public static synchronized WrappedSerialisingFactory getInstance() {
		if(instance == null) {
			instance = new WrappedSerialisingFactory();
		}
		return instance;
	}
	
	public static boolean isInitialised() {
		return (instance != null);
	}
}
