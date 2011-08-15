/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevTree;
import org.geogit.storage.FeatureWriter;
import org.geogit.storage.ObjectWriter;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.identity.ResourceId;
import org.opengis.geometry.BoundingBox;
import org.opengis.util.ProgressListener;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 * A working tree is the collection of Features for a single FeatureType in GeoServer that has a
 * repository associated with it (and hence is subject of synchronization).
 * <p>
 * It represents the set of Features tracked by some kind of geospatial data repository (like the
 * GeoServer Catalog). It is essentially a "tree" with various roots and only one level of nesting,
 * since the FeatureTypes held in this working tree are the equivalents of files in a git working
 * tree.
 * </p>
 * <p>
 * <ul>
 * <li>A WorkingTree represents the current working copy of the versioned feature types
 * <li>A WorkingTree has a Repository
 * <li>A Repository holds commits and branches
 * <li>You perform work on the working tree (insert/delete/update features)
 * <li>Then you commit to the current Repository's branch
 * <li>You can checkout a different branch from the Repository and the working tree will be updated
 * to reflect the state of that branch
 * </ul>
 * 
 * @author Gabriel Roldan
 * @see Repository
 */
@SuppressWarnings("rawtypes")
public class WorkingTree {

    private final Index index;

    private final Repository repository;

    private static final FilterFactory2 filterFactory = CommonFactoryFinder.getFilterFactory2(null);

    private static class RefToResourceId implements Function<Ref, FeatureId> {

        @Override
        public ResourceId apply(Ref input) {
            return filterFactory.resourceId(input.getName(), input.getObjectId().toString());
        }

    }

    public WorkingTree(final Repository repository) {
        Preconditions.checkNotNull(repository);
        this.repository = repository;
        this.index = repository.getIndex();
        Preconditions.checkState(index != null);
    }

    public void init(final FeatureType featureType) throws Exception {

        final Name typeName = featureType.getName();
        // index.created(Arrays.asList(typeName.getNamespaceURI(), typeName.getLocalPart()));
    }

    public void delete(final Name typeName) throws Exception {
        index.deleted(typeName.getNamespaceURI(), typeName.getLocalPart());
    }

    /**
     * Inserts a feature to the {@link Index} without staging it and returns the feature id.
     */
    public String insert(final Feature feature) throws Exception {

        ObjectWriter<?> featureWriter = new FeatureWriter(feature);
        final BoundingBox bounds = feature.getBounds();
        final Name typeName = feature.getType().getName();

        final String id = feature.getIdentifier().getID();

        String[] path = path(typeName, id);
        index.inserted(featureWriter, bounds, path);
        return id;
    }

    private String[] path(final Name typeName, final String id) {
        String path[];
        if (typeName.getNamespaceURI() == null) {
            path = new String[] { typeName.getLocalPart(), id };
        } else {
            path = new String[] { typeName.getNamespaceURI(), typeName.getLocalPart(), id };
        }
        return path;
    }

    /**
     * @param typeName
     * @param features
     * @param listener
     * @return
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    public List<FeatureId> insert(final FeatureCollection features, final ProgressListener listener)
            throws Exception {

        final int size = features.size();

        List<Ref> refs;
        Iterator<Feature> featureIterator = features.iterator();
        try {
            Iterator<Triplet<ObjectWriter<?>, BoundingBox, List<String>>> objects;
            objects = Iterators.transform(featureIterator,
                    new Function<Feature, Triplet<ObjectWriter<?>, BoundingBox, List<String>>>() {
                        @Override
                        public Triplet<ObjectWriter<?>, BoundingBox, List<String>> apply(
                                final Feature input) {

                            ObjectWriter<?> featureWriter = new FeatureWriter(input);
                            final BoundingBox bounds = input.getBounds();
                            final Name typeName = input.getType().getName();
                            final String id = input.getIdentifier().getID();
                            final List<String> path = Arrays.asList(typeName.getNamespaceURI(),
                                    typeName.getLocalPart(), id);

                            return new Triplet<ObjectWriter<?>, BoundingBox, List<String>>(
                                    featureWriter, bounds, path);
                        }
                    });

            refs = index.inserted(objects, listener, size <= 0 ? null : size);
        } finally {
            features.close(featureIterator);
        }
        List<FeatureId> inserted = Lists.transform(refs, new RefToResourceId());
        return inserted;
    }

    @Deprecated
    public void update(final Filter filter, final List<PropertyName> updatedProperties,
            List<Object> newValues2, final FeatureCollection newValues,
            final ProgressListener listener) throws Exception {

        insert(newValues, listener);
    }

    public void update(final FeatureCollection newValues, final ProgressListener listener)
            throws Exception {

        insert(newValues, listener);
    }

    public boolean hasRoot(final Name typeName) {
        String namespaceURI = typeName.getNamespaceURI() == null ? "" : typeName.getNamespaceURI();
        String localPart = typeName.getLocalPart();
        Ref typeNameTreeRef = repository.getRootTreeChild(namespaceURI, localPart);
        return typeNameTreeRef != null;
    }

    public void delete(final Name typeName, final Filter filter,
            final FeatureCollection affectedFeatures) throws Exception {

        final Index index = repository.getIndex();
        String namespaceURI = typeName.getNamespaceURI();
        String localPart = typeName.getLocalPart();
        FeatureIterator iterator = affectedFeatures.features();
        try {
            while (iterator.hasNext()) {
                String id = iterator.next().getIdentifier().getID();
                index.deleted(namespaceURI, localPart, id);
            }
        } finally {
            iterator.close();
        }
    }

    /**
     * @return
     */
    public List<Name> getFeatureTypeNames() {
        List<Name> names = new ArrayList<Name>();
        RevTree root = repository.getHeadTree();
        if (root != null) {
            Iterator<Ref> namespaces = root.iterator(null);
            while (namespaces.hasNext()) {
                final Ref nsRef = namespaces.next();
                final String nsUri = nsRef.getName();
                final ObjectId nsTreeId = nsRef.getObjectId();
                final RevTree nsTree = repository.getTree(nsTreeId);
                final Iterator<Ref> typeNameRefs = nsTree.iterator(null);
                while (typeNameRefs.hasNext()) {
                    Name typeName = new NameImpl(nsUri, typeNameRefs.next().getName());
                    names.add(typeName);
                }
            }
        }
        return names;
    }
}
