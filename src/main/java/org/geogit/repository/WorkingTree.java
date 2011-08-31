/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.geogit.api.DiffEntry;
import org.geogit.api.MutableTree;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevTree;
import org.geogit.api.TreeVisitor;
import org.geogit.storage.FeatureWriter;
import org.geogit.storage.ObjectWriter;
import org.geogit.storage.StagingDatabase;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.Hints;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.identity.ResourceId;
import org.opengis.geometry.BoundingBox;
import org.opengis.util.ProgressListener;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
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

    private final StagingArea index;

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
        List<String> path = Arrays.asList(typeName.getNamespaceURI(), typeName.getLocalPart());
        index.created(path);
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

        List<String> path = path(typeName, id);
        index.inserted(featureWriter, bounds, path.toArray(new String[path.size()]));
        return id;
    }

    private List<String> path(final Name typeName, final String id) {
        List<String> path = new ArrayList<String>(3);
        if (typeName.getNamespaceURI() != null) {
            path.add(typeName.getNamespaceURI());
        }
        path.add(typeName.getLocalPart());
        if (id != null) {
            path.add(id);
        }

        return path;
    }

    /**
     * Inserts the given features into the index.
     * 
     * @param features
     *            the features to insert
     * @param forceUseProvidedFID
     *            whether to force the use of the existing Feature IDs or not. If {@code true} the
     *            existing provided feature ids will be used, if {@code false} new Feature IDS will
     *            be created, at least a specific Feature has the {@link Hints#USE_PROVIDED_FID}
     *            hint set to {@code Boolean.TRUE}.
     * @param listener
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "deprecation", "unchecked" })
    public List<FeatureId> insert(final FeatureCollection features,
            final boolean forceUseProvidedFID, final ProgressListener listener) throws Exception {

        final int size = features.size();

        List<Ref> refs;
        Iterator<Feature> iterator = features.iterator();
        try {
            Iterator<Triplet<ObjectWriter<?>, BoundingBox, List<String>>> objects;
            objects = Iterators.transform(iterator, new FeatureInserter(forceUseProvidedFID));

            refs = index.inserted(objects, listener, size <= 0 ? null : size);
        } finally {
            features.close(iterator);
        }
        List<FeatureId> inserted = Lists.transform(refs, new RefToResourceId());
        return inserted;
    }

    private static class FeatureInserter implements
            Function<Feature, Triplet<ObjectWriter<?>, BoundingBox, List<String>>> {

        private final boolean forceUseProvidedFID;

        public FeatureInserter(final boolean forceUseProvidedFID) {
            this.forceUseProvidedFID = forceUseProvidedFID;
        }

        @Override
        public Triplet<ObjectWriter<?>, BoundingBox, List<String>> apply(Feature input) {

            ObjectWriter<?> featureWriter = new FeatureWriter(input);
            final BoundingBox bounds = input.getBounds();
            final Name typeName = input.getType().getName();
            final String id;
            {
                final Object useProvidedFid = input.getUserData().get(Hints.USE_PROVIDED_FID);
                if (forceUseProvidedFID || Boolean.TRUE.equals(useProvidedFid)) {
                    id = input.getIdentifier().getID();
                } else {
                    id = UUID.randomUUID().toString();
                }
            }
            final List<String> path = Arrays.asList(typeName.getNamespaceURI(),
                    typeName.getLocalPart(), id);

            return new Triplet<ObjectWriter<?>, BoundingBox, List<String>>(featureWriter, bounds,
                    path);
        }
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    public void update(final FeatureCollection newValues, final ProgressListener listener)
            throws Exception {

        final int size = newValues.size();

        Iterator<Feature> features = newValues.iterator();
        try {
            Iterator<Triplet<ObjectWriter<?>, BoundingBox, List<String>>> objects;
            final boolean forceUseProvidedFID = true;
            objects = Iterators.transform(features, new FeatureInserter(forceUseProvidedFID));

            index.inserted(objects, listener, size <= 0 ? null : size);
        } finally {
            newValues.close(features);
        }
    }

    public boolean hasRoot(final Name typeName) {
        String namespaceURI = typeName.getNamespaceURI() == null ? "" : typeName.getNamespaceURI();
        String localPart = typeName.getLocalPart();
        Ref typeNameTreeRef = repository.getRootTreeChild(namespaceURI, localPart);
        return typeNameTreeRef != null;
    }

    public void delete(final Name typeName, final Filter filter,
            final FeatureCollection affectedFeatures) throws Exception {

        final StagingArea index = repository.getIndex();
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

    public RevTree getHeadVersion(final Name typeName) {
        List<String> path = path(typeName, null);
        Ref typeTreeRef = repository.getRootTreeChild(path);
        RevTree typeTree;
        if (typeTreeRef == null) {
            typeTree = repository.newTree();
        } else {
            typeTree = repository.getTree(typeTreeRef.getObjectId());
        }
        return typeTree;
    }

    public RevTree getStagedVersion(final Name typeName) {

        RevTree typeTree = getHeadVersion(typeName);

        List<String> path = path(typeName, null);
        StagingDatabase database = index.getDatabase();
        final int stagedCount = database.countStaged(path);
        if (stagedCount == 0) {
            return typeTree;
        }
        return new DiffTree(typeTree, path, index);
    }

    private static class DiffTree implements RevTree {

        private final RevTree typeTree;

        private final Map<String, Ref> inserts = new HashMap<String, Ref>();

        private final Map<String, Ref> updates = new HashMap<String, Ref>();

        private final Set<String> deletes = new HashSet<String>();

        public DiffTree(final RevTree typeTree, final List<String> basePath, final StagingArea index) {
            this.typeTree = typeTree;

            Iterator<DiffEntry> staged = index.getDatabase().getStaged(basePath);
            while (staged.hasNext()) {
                DiffEntry entry = staged.next();
                List<String> entryPath = entry.getPath();
                String fid = entryPath.get(entryPath.size() - 1);
                switch (entry.getType()) {
                case ADD:
                    inserts.put(fid, entry.getNewObject());
                    break;
                case DELETE:
                    deletes.add(fid);
                    break;
                case MODIFY:
                    updates.put(fid, entry.getNewObject());
                    break;
                default:
                    throw new IllegalStateException();
                }
            }
        }

        @Override
        public TYPE getType() {
            return TYPE.TREE;
        }

        @Override
        public ObjectId getId() {
            return null;
        }

        @Override
        public boolean isNormalized() {
            return false;
        }

        @Override
        public MutableTree mutable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Ref get(final String fid) {
            Ref ref = inserts.get(fid);
            if (ref == null) {
                ref = updates.get(fid);
            }
            if (ref == null) {
                ref = this.typeTree.get(fid);
            }
            return ref;
        }

        @Override
        public void accept(TreeVisitor visitor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public BigInteger size() {
            BigInteger size = typeTree.size();
            if (inserts.size() > 0) {
                size = size.add(BigInteger.valueOf(inserts.size()));
            }
            if (deletes.size() > 0) {
                size = size.subtract(BigInteger.valueOf(deletes.size()));
            }
            return size;
        }

        @Override
        public Iterator<Ref> iterator(Predicate<Ref> filter) {
            Iterator<Ref> current = typeTree.iterator(null);

            current = Iterators.filter(current, new Predicate<Ref>() {
                @Override
                public boolean apply(Ref input) {
                    boolean returnIt = !deletes.contains(input.getName());
                    return returnIt;
                }
            });
            current = Iterators.transform(current, new Function<Ref, Ref>() {
                @Override
                public Ref apply(Ref input) {
                    Ref update = updates.get(input.getName());
                    return update == null ? input : update;
                }
            });

            Iterator<Ref> inserted = inserts.values().iterator();
            if (filter != null) {
                inserted = Iterators.filter(inserted, filter);
                current = Iterators.filter(current, filter);
            }

            Iterator<Ref> diffed = Iterators.concat(inserted, current);
            return diffed;
        }

    }

}
