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
import org.geogit.api.RevCommit;
import org.geogit.repository.Index;
import org.geogit.repository.Repository;
import org.geogit.storage.FeatureWriter;
import org.geogit.storage.RepositoryDatabase;
import org.geogit.storage.bdbje.EntityStoreConfig;
import org.geogit.storage.bdbje.EnvironmentBuilder;
import org.geogit.storage.bdbje.JERepositoryDatabase;
import org.geotools.data.DataUtilities;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.WKTReader2;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.sleepycat.je.Environment;
import com.vividsolutions.jts.io.ParseException;

public abstract class RepositoryTestCase extends TestCase {

    protected String namespace1 = "http://geoserver.org/test";

    protected String typeName1 = "TestType";

    protected String typeSpec1 = "sp:String,ip:Integer,pp:Point:srid=4326";

    protected SimpleFeatureType featureType1;

    protected Feature feature1_1;

    protected Feature feature1_2;

    protected Feature feature1_3;

    protected String namespace2 = "http://geosyncservice.example.com/test";

    protected String typeName2 = "TestLines";

    protected String typeSpec2 = "sp:String,ip:Integer,pp:LineString:srid=4326";

    protected SimpleFeatureType featureType2;

    protected Feature feature2_1;

    protected Feature feature2_2;

    protected Feature feature2_3;

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
        File envHome = new File(new File("target"), "mockblobstore");
        FileUtils.deleteDirectory(envHome);
        envHome.mkdirs();

        EntityStoreConfig config = new EntityStoreConfig();
        config.setCacheMemoryPercentAllowed(50);
        EnvironmentBuilder esb = new EnvironmentBuilder(config);
        Properties bdbEnvProperties = null;
        Environment environment;
        environment = esb.buildEnvironment(envHome, bdbEnvProperties);
        repositoryDatabase = new JERepositoryDatabase(environment);

        // repositoryDatabase = new FileSystemRepositoryDatabase(envHome);

        repo = new Repository(repositoryDatabase);

        repo.create();

        featureType1 = DataUtilities.createType(namespace1, typeName1, typeSpec1);

        feature1_1 = feature(featureType1, "TestType.feature.1", "StringProp1_1",
                new Integer(1000), "POINT(1 1)");
        feature1_2 = feature(featureType1, "TestType.feature.2", "StringProp1_2",
                new Integer(1000), "POINT(2 2)");
        feature1_3 = feature(featureType1, "TestType.feature.3", "StringProp1_3",
                new Integer(3000), "POINT(3 3)");

        featureType2 = DataUtilities.createType(namespace2, typeName2, typeSpec2);

        feature2_1 = feature(featureType2, "TestLines.1", "StringProp2_1", new Integer(1000),
                "LINESTRING(1 1, 2 2)");
        feature2_2 = feature(featureType2, "TestLines.2", "StringProp2_2", new Integer(2000),
                "LINESTRING(3 3, 4 4)");
        feature2_3 = feature(featureType2, "TestLines.3", "StringProp2_3", new Integer(3000),
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
        ObjectId objectId = index.inserted(new FeatureWriter(f), f.getBounds(), namespaceURI,
                localPart, id);
        return objectId;
    }

    /**
     * Deletes a feature from the index
     * 
     * @param f
     * @return
     * @throws Exception
     */
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
