package org.geogit.storage.bxml;

import org.geogit.api.RevCommit;
import org.geogit.api.RevTree;
import org.geogit.storage.BlobPrinter;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.ObjectWriter;
import org.geotools.factory.Hints;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

import com.vividsolutions.jts.geom.GeometryFactory;

public class BxmlFactory implements ObjectSerialisingFactory {
	private BxmlBlobPrinter printer;

	@Override
	public ObjectReader<RevCommit> createCommitReader() {
		return new BxmlCommitReader();
	}

	@Override
	public BxmlFeatureReader createFeatureReader(FeatureType featureType,
			String featureId) {
		return new BxmlFeatureReader(featureType, featureId);
	}
	
    @Override
    public ObjectReader<Feature> createFeatureReader(FeatureType featureType, String featureId,
            Hints hints) {
        BxmlFeatureReader featureReader = createFeatureReader(featureType, featureId);
        GeometryFactory gf = (GeometryFactory) hints.get(Hints.GEOMETRY_FACTORY);
        if(gf != null){
            featureReader.setGeometryFactory(gf);
        }
        return featureReader;
    }

        @Override
	public ObjectReader<RevTree> createRevTreeReader(ObjectDatabase objectDb) {
		return new BxmlRevTreeReader(objectDb);
	}
	
	@Override
	public ObjectReader<RevTree> createRevTreeReader(ObjectDatabase objectDb, int order) {
		return new BxmlRevTreeReader(objectDb, order);
	}

	@Override
	public ObjectWriter<RevCommit> createCommitWriter(final RevCommit commit) {
		return new BxmlCommitWriter(commit);
	}

	@Override
	public ObjectWriter<Feature> createFeatureWriter(final Feature feature) {
		return new BxmlFeatureWriter(feature);
	}

	@Override
	public ObjectWriter<RevTree> createRevTreeWriter(RevTree tree) {
		return new BxmlRevTreeWriter(tree);
	}

	@Override
	public BlobPrinter createBlobPrinter() {
		if(printer == null) {
			printer = new BxmlBlobPrinter();
		}
		return printer;
	}

}
