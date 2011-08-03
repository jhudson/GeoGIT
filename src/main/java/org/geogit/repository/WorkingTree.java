/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevTree;
import org.geogit.storage.FeatureWriter;
import org.geogit.storage.ObjectWriter;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;
import org.opengis.geometry.BoundingBox;
import org.opengis.util.ProgressListener;

import com.google.common.base.Preconditions;

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

        String path[];
        if (typeName.getNamespaceURI() == null) {
            path = new String[] { typeName.getLocalPart(), id };
        } else {
            path = new String[] { typeName.getNamespaceURI(), typeName.getLocalPart(), id };
        }
        index.inserted(featureWriter, bounds, path);
        return id;
    }

    /**
     * @param typeName
     * @param features
     * @param listener
     * @return the list of feature ids inserted, or {@code null} if
     *         {@link ProgressListener#isCanceled() listener.isCanceled()}
     * @throws Exception
     */
    public List<String> insert(final FeatureCollection features, final ProgressListener listener)
            throws Exception {

        List<String> fids = new ArrayList<String>();
        final float size = features.size();

        long t = System.currentTimeMillis();
        // be careful to preserve feature ids. MemoryDataStore does, but when changing it by
        // something production ready....
        FeatureIterator iterator = features.features();
        try {
            repository.beginTransaction();
            int count = 0;
            while (iterator.hasNext()) {
                if (listener.isCanceled()) {
                    repository.rollbackTransaction();
                    return null;
                }
                Feature next = iterator.next();
                String id = insert(next);
                fids.add(id);
                count++;
                if (listener.isCanceled()) {
                    repository.rollbackTransaction();
                    return null;
                }
                if (size > 0) {
                    listener.progress((count * 100) / size);
                }
            }
            repository.commitTransaction();
            listener.complete();
        } catch (Exception e) {
            repository.rollbackTransaction();
            throw e;
        } finally {
            iterator.close();
        }
        // t = System.currentTimeMillis() - t;
        // System.err.println("Imported " + size + " features from " + typeName.getLocalPart()
        // + " in " + t + "ms");
        return fids;
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

    public void delete(Name typeName, Filter filter, FeatureCollection affectedFeatures)
            throws Exception {
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
