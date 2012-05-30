/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

import org.geogit.api.DiffEntry;
import org.geogit.api.MutableTree;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevBlob;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.SpatialRef;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Environment;

/**
 * The Index (or Staging Area) object database.
 * <p>
 * This is a composite object database holding a reference to the actual repository object database
 * and a separate object database for the staging area itself.
 * <p>
 * Object look ups are first performed against the staging area database. If the object is not
 * found, then the look up is deferred to the actual repository database.
 * <p>
 * Object writes are always performed against the staging area object database.
 * <p>
 * The staing area database holds references to two root {@link RevTree trees}, one for the staged
 * objects and another one for the unstaged objects. When objects are added/changed/deleted to/from
 * the index, those modifications are written to the unstaged root tree. When objects are staged to
 * be committed, the unstaged objects are moved to the staged root tree.
 * <p>
 * A diff operation between the repository root tree and the index unstaged root tree results in the
 * list of unstaged objects.
 * <p>
 * A diff operation between the repository root tree and the index staged root tree results in the
 * list of staged objects.
 * 
 * @author groldan
 * 
 */
public class StagingDatabase implements ObjectDatabase {

    private Database unstagedEntries;

    private Database stagedEntries;

    StoredSortedMap<List<String>, DiffEntry> staged;

    StoredSortedMap<List<String>, DiffEntry> unstaged;

    private final PathBinding keyPathBinding = new PathBinding();

    private final DiffEntryBinding diffEntryBinding = new DiffEntryBinding();

    private final Environment env;

    // /////////////////////////////////////////
    /**
     * The staging area object database, contains only differences between the index and the
     * repository
     */
    private final ObjectDatabase stagingDb;

    private final ObjectDatabase repositoryDb;

    /**
     * @param referenceDatabase
     *            the repository reference database, used to get the head re
     * @param repoDb
     * @param stagingDb
     */
    public StagingDatabase(final ObjectDatabase repositoryDb, final ObjectDatabase stagingDb,
            final Environment env) {
        this.repositoryDb = repositoryDb;
        this.stagingDb = stagingDb;
        this.env = env;
    }

    public void create() {
        stagingDb.create();
        {
            DatabaseConfig unstagedDbConfig = new DatabaseConfig();
            unstagedDbConfig.setAllowCreate(true);
            unstagedDbConfig.setTransactional(env.getConfig().getTransactional());
            unstagedDbConfig.setSortedDuplicates(false);
            unstagedEntries = env.openDatabase(null, "UnstagedDb", unstagedDbConfig);
            unstaged = new StoredSortedMap<List<String>, DiffEntry>(this.unstagedEntries,
                    this.keyPathBinding, this.diffEntryBinding, true);

        }
        {
            DatabaseConfig stagedDbConfig = new DatabaseConfig();
            stagedDbConfig.setAllowCreate(true);
            // stagedDbConfig.setDeferredWrite(true);
            stagedDbConfig.setTransactional(env.getConfig().getTransactional());
            stagedDbConfig.setSortedDuplicates(false);
            stagedEntries = env.openDatabase(null, "StagedDb", stagedDbConfig);

            staged = new StoredSortedMap<List<String>, DiffEntry>(this.stagedEntries,
                    this.keyPathBinding, this.diffEntryBinding, true);
        }
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#close()
     */
    public void close() {
        stagingDb.close();

        unstagedEntries.close();
        stagedEntries.close();
    }

    public void reset() {
        unstaged.clear();
        staged.clear();
    }

    /**
     * Clears the unstaged tree
     */
    public void clearUnstaged() {
        this.unstaged.clear();
    }

    /**
     * Clears the staged tree
     */
    public void clearStaged() {
        this.staged.clear();
    }

    // //////////////////////////////////////////////////////////////////////////////////////////

    public void putUnstaged(final DiffEntry diffEntry) {
        unstaged.put(diffEntry.getPath(), diffEntry);
    }

    public void stage(DiffEntry diffEntry) {
        List<String> path = diffEntry.getPath();
        DiffEntry remove = unstaged.remove(path);
        if (remove != null) {
            staged.put(path, diffEntry);
        }
    }

    private static class DiffEntryBinding extends TupleBinding<DiffEntry> {

        private final PathBinding pathBinding;

        private final RefBinding refBinding;

        public DiffEntryBinding() {
            pathBinding = new PathBinding();
            refBinding = new RefBinding();
        }

        @Override
        public DiffEntry entryToObject(TupleInput input) {

            final List<String> path = pathBinding.entryToObject(input);
            final Ref oldObject;
            final Ref newObject;

            boolean refPresent = input.readByte() == 1;
            if (refPresent) {
                oldObject = refBinding.entryToObject(input);
            } else {
                oldObject = null;
            }

            refPresent = input.readByte() == 1;
            if (refPresent) {
                newObject = refBinding.entryToObject(input);
            } else {
                newObject = null;
            }

            DiffEntry entry = DiffEntry.newInstance(null, null, oldObject, newObject, path);
            return entry;
        }

        @Override
        public void objectToEntry(DiffEntry object, TupleOutput output) {
            final List<String> path = object.getPath();
            final Ref oldObject = object.getOldObject();
            final Ref newObject = object.getNewObject();

            pathBinding.objectToEntry(path, output);

            if (oldObject == null) {
                output.writeByte(0);
            } else {
                output.writeByte(1);
                refBinding.objectToEntry(oldObject, output);
            }

            if (newObject == null) {
                output.writeByte(0);
            } else {
                output.writeByte(1);
                refBinding.objectToEntry(newObject, output);
            }
        }

    }

    private static class PathBinding extends TupleBinding<List<String>> {

        @Override
        public List<String> entryToObject(TupleInput input) {
            List<String> path = new ArrayList<String>(3);
            final int size = input.readUnsignedByte();
            for (int i = 0; i < size; i++) {
                path.add(input.readString());
            }
            return path;
        }

        @Override
        public void objectToEntry(List<String> path, TupleOutput output) {
            int size = path.size();
            output.writeUnsignedByte(size);
            for (String step : path) {
                output.writeString(step);
            }
        }
    }

    private static class RefBinding extends TupleBinding<Ref> {
        private static final int REF = 0;

        private static final int SPATIAL_REF = 1;

        @Override
        public Ref entryToObject(TupleInput input) {
            String name = input.readString();
            String typeStr = input.readString();
            TYPE type = TYPE.valueOf(typeStr);

            byte[] raw = new byte[20];
            input.readFast(raw);
            ObjectId objectId = new ObjectId(raw);

            Ref ref;
            final int trailingMark = input.readByte();
            if (SPATIAL_REF == trailingMark) {
                String srs = input.readString();
                CoordinateReferenceSystem crs;
                try {
                    crs = CRS.decode(srs);
                } catch (Exception e) {
                    //e.printStackTrace();
                    crs = null;
                }

                double x1 = input.readDouble();
                double x2 = input.readDouble();
                double y1 = input.readDouble();
                double y2 = input.readDouble();
                BoundingBox bounds = new ReferencedEnvelope(x1, x2, y1, y2, crs);

                ref = new SpatialRef(name, objectId, type, bounds);
            } else {
                ref = new Ref(name, objectId, type);
            }
            return ref;
        }

        @Override
        public void objectToEntry(Ref ref, TupleOutput output) {
            String name = ref.getName();
            ObjectId objectId = ref.getObjectId();
            TYPE type = ref.getType();

            output.writeString(name);
            output.writeString(type.toString());
            output.write(objectId.getRawValue());

            final int trailingMark = ref instanceof SpatialRef ? SPATIAL_REF : REF;
            output.writeByte(trailingMark);

            if (ref instanceof SpatialRef) {
                SpatialRef sr = (SpatialRef) ref;
                BoundingBox bounds = sr.getBounds();
                CoordinateReferenceSystem crs = bounds.getCoordinateReferenceSystem();
                String srs = CRS.toSRS(crs);

                output.writeString(srs);
                final int dimension = 2;// bounds.getDimension();
                for (int d = 0; d < dimension; d++) {
                    output.writeDouble(bounds.getMinimum(d));
                    output.writeDouble(bounds.getMaximum(d));
                }
            }
        }
    }

    public int countUnstaged(final List<String> pathFilter) {
        if (pathFilter == null || pathFilter.size() == 0) {
            return unstaged.size();
        }
        SortedMap<List<String>, DiffEntry> subMap = unstaged.subMap(pathFilter, true, pathFilter,
                true);
        int size = subMap.size();
        return size;
    }

    public int countStaged(final List<String> pathFilter) {
        if (pathFilter == null || pathFilter.size() == 0) {
            return staged.size();
        }
        SortedMap<List<String>, DiffEntry> subMap = staged.tailMap(pathFilter, true);
        int size = subMap.size();
        return size;
    }

    public Iterator<DiffEntry> getUnstaged(final List<String> pathFilter) {
        if (pathFilter == null || pathFilter.size() == 0) {
            return unstaged.values().iterator();
        }
        SortedMap<List<String>, DiffEntry> subMap = unstaged.tailMap(pathFilter, true);
        return subMap.values().iterator();
    }

    public Iterator<DiffEntry> getStaged(final List<String> pathFilter) {
        if (pathFilter == null || pathFilter.size() == 0) {
            return staged.values().iterator();
        }
        SortedMap<List<String>, DiffEntry> subMap = staged.tailMap(pathFilter, true);
        return subMap.values().iterator();
    }

    public int removeStaged(final List<String> pathFilter) {
        SortedMap<List<String>, DiffEntry> subMap = staged;

        if (pathFilter != null && pathFilter.size() > 0) {
            subMap = staged.tailMap(pathFilter, true);
        }
        int size = subMap.size();
        subMap.clear();
        return size;
    }

    public int removeUnStaged(final List<String> pathFilter) {
        SortedMap<List<String>, DiffEntry> subMap = unstaged;

        if (pathFilter != null && pathFilter.size() > 0) {
            subMap = staged.subMap(pathFilter, true, pathFilter, true);
        }
        int size = subMap.size();
        subMap.clear();
        return size;
    }

    public DiffEntry findStaged(final String... path) {
        return findStaged(Arrays.asList(path));
    }

    public DiffEntry findStaged(final List<String> path) {
        DiffEntry diffEntry = staged.get(path);
        return diffEntry;
    }

    public DiffEntry findUnstaged(final String... path) {
        return findUnstaged(Arrays.asList(path));

    }

    public DiffEntry findUnstaged(final List<String> path) {
        DiffEntry diffEntry = unstaged.get(path);
        return diffEntry;
    }

    public ObjectDatabase getObjectDatabase() {
        return this.stagingDb;
    }

    // /////////////////////////////////////////////////////////////////////

    @Override
    public boolean exists(ObjectId id) {
        boolean exists = stagingDb.exists(id);
        if (!exists) {
            exists = repositoryDb.exists(id);
        }
        return exists;
    }

    @Override
    public InputStream getRaw(ObjectId id) throws IOException {
        if (stagingDb.exists(id)) {
            return stagingDb.getRaw(id);
        }
        return repositoryDb.getRaw(id);
    }

    @Override
    public List<ObjectId> lookUp(String partialId) {
        Set<ObjectId> lookUp = new HashSet<ObjectId>(stagingDb.lookUp(partialId));
        lookUp.addAll(repositoryDb.lookUp(partialId));
        return new ArrayList<ObjectId>(lookUp);
    }

    @Override
    public <T> T get(ObjectId id, ObjectReader<T> reader) throws IOException {
        if (stagingDb.exists(id)) {
            return stagingDb.get(id, reader);
        }
        return repositoryDb.get(id, reader);
    }

    @Override
    public <T> T getCached(ObjectId id, ObjectReader<T> reader) throws IOException {
        if (stagingDb.exists(id)) {
            return stagingDb.getCached(id, reader);
        }
        return repositoryDb.getCached(id, reader);
    }

    @Override
    public <T> ObjectId put(ObjectWriter<T> writer) throws Exception {
        return stagingDb.put(writer);
    }

    @Override
    public boolean put(ObjectId id, ObjectWriter<?> writer) throws Exception {
        return stagingDb.put(id, writer);
    }

    @Override
    public ObjectId writeBack(MutableTree root, RevTree tree, List<String> pathToTree)
            throws Exception {
        return stagingDb.writeBack(root, tree, pathToTree);
    }

    @Override
    public ObjectInserter newObjectInserter() {
        return stagingDb.newObjectInserter();
    }

    @Override
    public RevBlob getBlob(ObjectId objectId) {
        if (stagingDb.exists(objectId)) {
            return stagingDb.getBlob(objectId);
        }
        return repositoryDb.getBlob(objectId);
    }

    @Override
    public RevCommit getCommit(ObjectId commitId) {
        if (stagingDb.exists(commitId)) {
            return stagingDb.getCommit(commitId);
        }
        return repositoryDb.getCommit(commitId);
    }

    @Override
    public RevTree getTree(ObjectId treeId) {
        if (stagingDb.exists(treeId)) {
            return stagingDb.getTree(treeId);
        }
        return repositoryDb.getTree(treeId);
    }

    @Override
    public MutableTree getOrCreateSubTree(RevTree parent, List<String> childPath) {
        Ref treeChild = repositoryDb.getTreeChild(parent, childPath);
        if (null != treeChild) {
            return repositoryDb.getOrCreateSubTree(parent, childPath);
        }
        return stagingDb.getOrCreateSubTree(parent, childPath);
    }

    @Override
    public MutableTree newTree() {
        return stagingDb.newTree();
    }

    @Override
    public Ref getTreeChild(RevTree root, String... path) {
        return getTreeChild(root, Arrays.asList(path));
    }

    @Override
    public Ref getTreeChild(RevTree root, List<String> path) {
        Ref treeChild = stagingDb.getTreeChild(root, path);
        if (null != treeChild) {
            return treeChild;
        }
        return repositoryDb.getTreeChild(root, path);
    }

    @Override
    public boolean delete(ObjectId objectId) {
        return stagingDb.delete(objectId);
    }
}
