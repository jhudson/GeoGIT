/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.repository.Index;
import org.geogit.repository.Repository;
import org.geogit.repository.Tuple;
import org.geogit.storage.FeatureWriter;
import org.geogit.storage.ObjectWriter;
import org.geogit.storage.RepositoryDatabase;
import org.geogit.storage.bdbje.EntityStoreConfig;
import org.geogit.storage.bdbje.EnvironmentBuilder;
import org.geogit.storage.bdbje.JERepositoryDatabase;
import org.geotools.data.DataUtilities;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.WKTReader2;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.geometry.BoundingBox;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.sleepycat.je.Environment;
import com.vividsolutions.jts.io.ParseException;

public abstract class RepositoryTestCase extends TestCase {

    protected static final String idL1 = "Lines.3";

    protected static final String idL2 = "Lines.2";

    protected static final String idL3 = "Lines.1";

    protected static final String idP3 = "Points.3";

    protected static final String idP2 = "Points.2";

    protected static final String idP1 = "Points.1";

    protected String pointsNs = "http://geogit.points";

    protected String pointsName = "Points";

    protected String pointsTypeSpec = "sp:String,ip:Integer,pp:Point:srid=4326";

    protected SimpleFeatureType pointsType;

    protected Feature points1;

    protected Feature points2;

    protected Feature points3;

    protected String linesNs = "http://geogit.lines";

    protected String linesName = "Lines";

    protected String linesTypeSpec = "sp:String,ip:Integer,pp:LineString:srid=4326";

    protected SimpleFeatureType linesType;

    protected Feature lines1;

    protected Feature lines2;

    protected Feature lines3;

    protected Repository repo;

    // prevent recursion
    private boolean setup = false;

    protected RepositoryDatabase repositoryDatabase;

    @Override
    protected final void setUp() throws Exception {
        if (setup) {
            throw new IllegalStateException("Are you calling super.setUp()!?");
        }
        setup = true;
        Logging.ALL.forceMonolineConsoleOutput();
        final File envHome = new File(new File("target"), "mockblobstore");
        final File repositoryHome = new File(envHome, "repository");
        final File indexHome = new File(envHome, "index");

        FileUtils.deleteDirectory(envHome);
        repositoryHome.mkdirs();
        indexHome.mkdirs();

        EntityStoreConfig config = new EntityStoreConfig();
        config.setCacheMemoryPercentAllowed(50);
        EnvironmentBuilder esb = new EnvironmentBuilder(config);
        Properties bdbEnvProperties = null;
        Environment environment;
        environment = esb.buildEnvironment(repositoryHome, bdbEnvProperties);

        Environment stagingEnvironment;
        stagingEnvironment = esb.buildEnvironment(indexHome, bdbEnvProperties);

        repositoryDatabase = new JERepositoryDatabase(environment, stagingEnvironment);

        // repositoryDatabase = new FileSystemRepositoryDatabase(envHome);

        repo = new Repository(repositoryDatabase);

        repo.create();

        pointsType = DataUtilities.createType(pointsNs, pointsName, pointsTypeSpec);

        points1 = feature(pointsType, idP1, "StringProp1_1", new Integer(1000), "POINT(1 1)");
        points2 = feature(pointsType, idP2, "StringProp1_2", new Integer(1000), "POINT(2 2)");
        points3 = feature(pointsType, idP3, "StringProp1_3", new Integer(3000), "POINT(3 3)");

        linesType = DataUtilities.createType(linesNs, linesName, linesTypeSpec);

        lines1 = feature(linesType, idL3, "StringProp2_1", new Integer(1000),
                "LINESTRING(1 1, 2 2)");
        lines2 = feature(linesType, idL2, "StringProp2_2", new Integer(2000),
                "LINESTRING(3 3, 4 4)");
        lines3 = feature(linesType, idL1, "StringProp2_3", new Integer(3000),
                "LINESTRING(5 5, 6 6)");

        setUpInternal();
    }

    @Override
    protected final void tearDown() throws Exception {
        setup = false;
        tearDownInternal();
        if (repo != null) {
            repo.close();
        }
    }

    /**
     * Called as the last step in {@link #setUp()}
     */
    protected abstract void setUpInternal() throws Exception;

    /**
     * Called before {@link #tearDown()}, subclasses may override as appropriate
     */
    protected void tearDownInternal() throws Exception {
        //
    }

    public Repository getRepository() {
        return repo;
    }

    protected Feature feature(SimpleFeatureType type, String id, Object... values)
            throws ParseException {
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (type.getDescriptor(i) instanceof GeometryDescriptor) {
                if (value instanceof String) {
                    value = new WKTReader2().read((String) value);
                }
            }
            builder.set(i, value);
        }
        return builder.buildFeature(id);
    }

    protected List<RevCommit> populate(boolean oneCommitPerFeature, Feature... features)
            throws Exception {
        return populate(oneCommitPerFeature, Arrays.asList(features));
    }

    protected List<RevCommit> populate(boolean oneCommitPerFeature, List<Feature> features)
            throws Exception {

        final GeoGIT ggit = new GeoGIT(getRepository());

        List<RevCommit> commits = new ArrayList<RevCommit>();

        for (Feature f : features) {
            insertAndAdd(f);
            if (oneCommitPerFeature) {
                RevCommit commit = ggit.commit().call();
                commits.add(commit);
            }
        }

        if (!oneCommitPerFeature) {
            RevCommit commit = ggit.commit().call();
            commits.add(commit);
        }

        return commits;
    }

    /**
     * Inserts the Feature to the index and stages it to be committed.
     */
    protected ObjectId insertAndAdd(Feature f) throws Exception {
        ObjectId objectId = insert(f);

        new GeoGIT(getRepository()).add().call();
        return objectId;
    }

    /**
     * Inserts the feature to the index but does not stages it to be committed
     */
    protected ObjectId insert(Feature f) throws Exception {
        final Index index = getRepository().getIndex();
        Name name = f.getType().getName();
        String namespaceURI = name.getNamespaceURI();
        String localPart = name.getLocalPart();
        String id = f.getIdentifier().getID();
        index.inserted(new FeatureWriter(f), f.getBounds(), namespaceURI, localPart, id);
        Ref ref = index.getDatabase()
                .getTreeChild(index.getUnstaged(), namespaceURI, localPart, id);
        ObjectId objectId = ref.getObjectId();
        return objectId;
    }

    protected void insertAndAdd(Feature... features) throws Exception {
        insert(features);
        new GeoGIT(getRepository()).add().call();
    }

    protected void insert(Feature... features) throws Exception {

        final Index index = getRepository().getIndex();

        Iterator<Tuple<ObjectWriter<?>, BoundingBox, List<String>>> iterator;
        Function<Feature, Tuple<ObjectWriter<?>, BoundingBox, List<String>>> function = new Function<Feature, Tuple<ObjectWriter<?>, BoundingBox, List<String>>>() {

            @Override
            public Tuple<ObjectWriter<?>, BoundingBox, List<String>> apply(final Feature f) {
                Name name = f.getType().getName();
                String namespaceURI = name.getNamespaceURI();
                String localPart = name.getLocalPart();
                String id = f.getIdentifier().getID();

                Tuple<ObjectWriter<?>, BoundingBox, List<String>> tuple;
                ObjectWriter<?> writer = new FeatureWriter(f);
                BoundingBox bounds = f.getBounds();
                List<String> path = Arrays.asList(namespaceURI, localPart, id);
                tuple = new Tuple<ObjectWriter<?>, BoundingBox, List<String>>(writer, bounds, path);
                return tuple;
            }
        };

        iterator = Iterators.transform(Iterators.forArray(features), function);

        index.inserted(iterator, new NullProgressListener(), null);

    }

    /**
     * Deletes a feature from the index
     * 
     * @param f
     * @return
     * @throws Exception
     */
    protected boolean deleteAndAdd(Feature f) throws Exception {
        boolean existed = delete(f);
        if (existed) {
            new GeoGIT(getRepository()).add().call();
        }

        return existed;
    }

    protected boolean delete(Feature f) throws Exception {
        final Index index = getRepository().getIndex();
        Name name = f.getType().getName();
        String namespaceURI = name.getNamespaceURI();
        String localPart = name.getLocalPart();
        String id = f.getIdentifier().getID();
        boolean existed = index.deleted(namespaceURI, localPart, id);
        return existed;
    }

    protected <E> List<E> toList(Iterator<E> logs) {
        List<E> logged = new ArrayList<E>();
        Iterators.addAll(logged, logs);
        return logged;
    }

    protected <E> List<E> toList(Iterable<E> logs) {
        List<E> logged = new ArrayList<E>();
        Iterables.addAll(logged, logs);
        return logged;
    }
}
