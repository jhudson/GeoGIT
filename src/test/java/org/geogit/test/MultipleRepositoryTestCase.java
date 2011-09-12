/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject;
import org.geogit.repository.Repository;
import org.geogit.repository.StagingArea;
import org.geogit.storage.CommitWriter;
import org.geogit.storage.FeatureWriter;
import org.geogit.storage.RepositoryDatabase;
import org.geogit.storage.bdbje.EntityStoreConfig;
import org.geogit.storage.bdbje.EnvironmentBuilder;
import org.geogit.storage.bdbje.JERepositoryDatabase;
import org.geotools.data.DataUtilities;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.WKTReader2;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;

import com.sleepycat.je.Environment;
import com.vividsolutions.jts.io.ParseException;

public abstract class MultipleRepositoryTestCase extends TestCase {

    protected static final String idL1 = "Lines.1";

    protected static final String idL2 = "Lines.2";

    protected static final String idL3 = "Lines.3";

    protected static final String idP1 = "Points.1";

    protected static final String idP2 = "Points.2";

    protected static final String idP3 = "Points.3";

    protected static final String pointsNs = "http://geogit.points";

    protected static final String pointsName = "Points";

    protected static final String pointsTypeSpec = "sp:String,ip:Integer,pp:Point:srid=4326";

    protected static final Name pointsTypeName = new NameImpl(pointsNs, pointsName);

    protected SimpleFeatureType pointsType;

    protected Feature points1;

    protected Feature points2;

    protected Feature points3;

    protected static final String linesNs = "http://geogit.lines";

    protected static final String linesName = "Lines";

    protected static final String linesTypeSpec = "sp:String,ip:Integer,pp:LineString:srid=4326";

    protected static final Name linesTypeName = new NameImpl(linesNs, linesName);

    protected SimpleFeatureType linesType;

    protected Feature lines1;

    protected Feature lines2;

    protected Feature lines3;

    protected Collection<Repository> repos;
    
    protected final Logger LOGGER;

    // prevent recursion
    private boolean setup = false;

    protected RepositoryDatabase repositoryDatabase;
    
    protected int numberOfRepos;
    
    public MultipleRepositoryTestCase( int numberOfRepos ) {
        super();
        LOGGER = Logging.getLogger(getClass());
        this.numberOfRepos = numberOfRepos;
    }

    @Override
    protected final void setUp() throws Exception {
        
        if (setup) {
            throw new IllegalStateException("Are you calling super.setUp()!?");
        }
        setup = true;
        Logging.ALL.forceMonolineConsoleOutput();
        
        repos = new ArrayList<Repository>();

        for (int i=0;i<numberOfRepos;i++){

            final File envHome = new File(new File("target/project"+i), "geogit");
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

            Repository repo = new Repository(repositoryDatabase, envHome);
    
            repo.create();
    
            pointsType = DataUtilities.createType(pointsNs, pointsName, pointsTypeSpec);
    
            points1 = feature(pointsType, idP1, "StringProp1_1", new Integer(1000), "POINT(1 1)");
            points2 = feature(pointsType, idP2, "StringProp1_2", new Integer(2000), "POINT(2 2)");
            points3 = feature(pointsType, idP3, "StringProp1_3", new Integer(3000), "POINT(3 3)");
    
            linesType = DataUtilities.createType(linesNs, linesName, linesTypeSpec);
    
            lines1 = feature(linesType, idL1, "StringProp2_1", new Integer(1000),
                    "LINESTRING(1 1, 2 2)");
            lines2 = feature(linesType, idL2, "StringProp2_2", new Integer(2000),
                    "LINESTRING(3 3, 4 4)");
            lines3 = feature(linesType, idL3, "StringProp2_3", new Integer(3000),
                    "LINESTRING(5 5, 6 6)");

            repos.add(repo);
        }

        setUpInternal();
    }

    @Override
    protected final void tearDown() throws Exception {
        setup = false;
        tearDownInternal();
        for (Repository repo : repos) {
            if (repo != null) {
                repo.close();
            }
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

    public Repository getRepository(int index) {
        return repos.toArray(new Repository[repos.size()])[index];
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
    
    /**
     * Inserts the Feature to the index and stages it to be committed.
     */
    protected RevCommit insertAddCommit(GeoGIT gg, Feature f) throws Exception {
        insert(gg, f);
        gg.add().call();
        return gg.commit().setMessage("commited a new feature").setAll(true).call();
    }

    /**
     * Inserts the feature to the index but does not stages it to be committed
     */
    protected ObjectId insert(GeoGIT gg, Feature feature) throws Exception {
        final StagingArea index = gg.getRepository().getIndex();
        Name name = feature.getType().getName();
        String namespaceURI = name.getNamespaceURI();
        String localPart = name.getLocalPart();
        String id = feature.getIdentifier().getID();

        Ref ref = index.inserted(new FeatureWriter(feature), feature.getBounds(), namespaceURI, localPart, id);
        ObjectId objectId = ref.getObjectId();
        return objectId;
    }
}
