/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import org.geogit.api.RevCommit;
import org.geogit.api.RevTree;
import org.geotools.factory.Hints;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

/**
 * The ObjectSerialisingFactory is used to create instances of the various writers and readers used
 * to work with the serialised forms of various repository elements.
 * 
 * @author mleslie
 * 
 */
public interface ObjectSerialisingFactory {

    /**
     * Creates an instance of a commit writer to serialise the provided RevCommit
     * 
     * @param commit RevCommit to be written
     * @return commit writer
     */
    public ObjectWriter<RevCommit> createCommitWriter(final RevCommit commit);

    /**
     * Creates an instance of a commit reader.
     * 
     * @return commit reader
     */

    public ObjectReader<RevCommit> createCommitReader();

    /**
     * Creates an instance of a RevTree writer to serialise the provided RevTree
     * 
     * @param tree RevTree to be written
     * @return revtree writer
     */
    public ObjectWriter<RevTree> createRevTreeWriter(RevTree tree);

    /**
     * Creates an instance of a RevTree reader.
     * 
     * @param objectDb the ObjectDatabase the RevTree is stored in
     * @return revtree reader
     */
    public ObjectReader<RevTree> createRevTreeReader(ObjectDatabase objectDb);

    /**
     * Creates an instance of a RevTree reader that will start reading a the given tree depth.
     * 
     * @param objectDb the ObjectDatabase the RevTree is stored in
     * @param depth depth of the revtree's root
     * @return revtree reader
     */
    public ObjectReader<RevTree> createRevTreeReader(ObjectDatabase objectDb, int depth);

    /**
     * Creates an instance of a Feature writer to serialise the provided feature.
     * 
     * @param feature Feature to be written
     * @return feature writer
     */
    public ObjectWriter<Feature> createFeatureWriter(final Feature feature);

    /**
     * Creates an instance of a Feature reader that can parse features of the given feature type.
     * 
     * @param featureType FeatureType description of the feature to be read
     * @param featureId String representation of the feature id
     * @return feature reader
     */
    public ObjectReader<Feature> createFeatureReader(final FeatureType featureType,
            final String featureId);

    /**
     * Creates an instance of a Feature reader that can parse features of the given feature type.
     * 
     * @param featureType FeatureType description of the feature to be read
     * @param featureId String representation of the feature id
     * @param hints feature creation hints
     * @return feature reader
     * @see Hints#GEOMETRY_FACTORY
     */
    public ObjectReader<Feature> createFeatureReader(final FeatureType featureType,
            final String featureId, final Hints hints);

    /**
     * Creates a BlobPrinter that can parse serialised elements into a human-readable(ish)
     * representation, typically xml.
     * 
     * @return instance of a BlobPrinter for the current serialisation
     */
    public BlobPrinter createBlobPrinter();

}
