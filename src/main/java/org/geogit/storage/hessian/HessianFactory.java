package org.geogit.storage.hessian;

import org.geogit.api.RevCommit;
import org.geogit.api.RevTree;
import org.geogit.storage.BlobPrinter;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.ObjectWriter;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

public class HessianFactory implements ObjectSerialisingFactory {

    @Override
    public BlobPrinter createBlobPrinter() {
        return new HessianBlobPrinter();
    }

    @Override
    public ObjectReader<RevCommit> createCommitReader() {
        return new HessianCommitReader();
    }

    @Override
    public ObjectWriter<RevCommit> createCommitWriter(RevCommit commit) {
        return new HessianCommitWriter(commit);
    }

    @Override
    public ObjectReader<Feature> createFeatureReader(FeatureType featureType, String featureId) {
        return new HessianFeatureReader(featureType, featureId);
    }

    @Override
    public ObjectWriter<Feature> createFeatureWriter(Feature feature) {
        return new HessianFeatureWriter(feature);
    }

    @Override
    public ObjectReader<RevTree> createRevTreeReader(ObjectDatabase objectDb) {
        return new HessianRevTreeReader(objectDb);
    }

    @Override
    public ObjectReader<RevTree> createRevTreeReader(ObjectDatabase objectDb, int order) {
        return new HessianRevTreeReader(objectDb, order);
    }

    @Override
    public ObjectWriter<RevTree> createRevTreeWriter(RevTree tree) {
        return new HessianRevTreeWriter(tree);
    }

}
