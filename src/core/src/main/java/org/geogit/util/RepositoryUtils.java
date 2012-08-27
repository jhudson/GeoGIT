package org.geogit.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.geogit.api.GeoGIT;
import org.geogit.api.Ref;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.repository.Repository;
import org.geogit.repository.StagingArea;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.RefDatabase;
import org.geogit.storage.WrappedSerialisingFactory;
import org.geogit.storage.bdbje.EntityStoreConfig;
import org.geogit.storage.bdbje.EnvironmentBuilder;
import org.geogit.storage.bdbje.JERepositoryDatabase;
import org.geotools.feature.NameImpl;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.Name;

import com.google.common.base.Preconditions;
import com.sleepycat.je.Environment;

public class RepositoryUtils {
    private static final Logger LOGGER = Logging.getLogger(RepositoryUtils.class);
    
    public static final String TYPE_NAMES_REF_TREE = "typeNames"; //$NON-NLS-1$

    public static final String NULL_NAMESPACE = ""; //$NON-NLS-1$

    private static final String defaultNamespace = NULL_NAMESPACE;

    /**
     * Retrieves a list of Names for all the feature types described within the given repository.
     * This may not be a comprehensive listing of the feature types stored in the repository, as it
     * only lists the feature types that have been defined in the repository, such as by the
     * createGeoGITSchema function.
     * 
     * @param repository
     * @return
     * @throws IOException
     */
    public static List<Name> getGeoGITNames(Repository repository) throws IOException {
        final RefDatabase refDatabase = repository.getRefDatabase();
        final ObjectDatabase objectDatabase = repository.getObjectDatabase();

        final Ref typesTreeRef = refDatabase.getRef(TYPE_NAMES_REF_TREE);
        Preconditions.checkState(typesTreeRef != null);

        RevTree namespacesTree = objectDatabase.getTree(typesTreeRef.getObjectId());
        Preconditions.checkState(null != namespacesTree,
                "Referenced types tree does not exist: " + typesTreeRef); //$NON-NLS-1$

        List<Name> names = new ArrayList<Name>();
        for (Iterator<Ref> namespaces = namespacesTree.iterator(null); namespaces.hasNext();) {
            final Ref namespaceRef = namespaces.next();
            Preconditions.checkState(TYPE.TREE.equals(namespaceRef.getType()));
            final String nsUri = namespaceRef.getName();
            final RevTree typesTree = objectDatabase.getTree(namespaceRef.getObjectId());
            for (Iterator<Ref> simpleNames = typesTree.iterator(null); simpleNames.hasNext();) {
                final Ref typeNameRef = simpleNames.next();
                final String simpleTypeName = typeNameRef.getName();
                names.add(new NameImpl(nsUri, simpleTypeName));
            }
        }

        return names;
    }

    /**
     * Create a GeoGit repository from a file location
     * 
     * @param envHome The home directory of the location of the GeoGit repository (eg.
     *        C:\data\THEME_NAME\)
     * @param repositoryHome The repository location - this is almost ALWAYS {envhome}\repository\
     * @param indexHome The index location - this is almost ALWAYS {envhome}\index\
     * @return A Repository for reading and writing to - you have the lock, with great power comes
     *         great responsibility!
     */
    public static Repository createRepository(File envHome, File repositoryHome, File indexHome) {
        EntityStoreConfig config = new EntityStoreConfig();
        config.setCacheMemoryPercentAllowed(50);
        EnvironmentBuilder esb = new EnvironmentBuilder(config);
        Properties bdbEnvProperties = null;
        Environment environment;
        environment = esb.buildEnvironment(repositoryHome, bdbEnvProperties);

        Environment stagingEnvironment;
        stagingEnvironment = esb.buildEnvironment(indexHome, bdbEnvProperties);

        JERepositoryDatabase repositoryDatabase = new JERepositoryDatabase(environment,
                stagingEnvironment);

        Repository repo = new Repository(repositoryDatabase, envHome);

        repo.create();

        return repo;
    }

    /**
     * Add a feature to a repository and add it to the staging area
     * 
     * @param geoGit
     * @param features
     * @throws Exception
     */
    public static void insertAndAdd(GeoGIT geoGit, List<SimpleFeature> features) throws Exception {
        for (SimpleFeature feature : features) {
            insert(geoGit, feature);
        }
        geoGit.add().call();
    }

    /**
     * Insert a feature into repository - not staged (GEOGIT.add().call() not called), use
     * insertAndAdd for that!
     * 
     * @param geoGit
     * @param feature
     * @throws Exception
     */
    public static void insert(GeoGIT geoGit, SimpleFeature feature) throws Exception {
        final StagingArea index = geoGit.getRepository().getIndex();
        Name name = feature.getType().getName();
        String namespaceURI = name.getNamespaceURI();
        String localPart = name.getLocalPart();
        String id = feature.getIdentifier().getID();
        index.inserted(WrappedSerialisingFactory.getInstance().createFeatureWriter(feature),
                feature.getBounds(), namespaceURI, localPart, id);
    }

    /**
     * Remove features from a geogit repository and add the changes to the staging area
     * 
     * @param geoGit
     * @param features
     * @return boolean true if the feature was removed, false otherwise
     * @throws Exception
     */
    public static void deleteAndAdd(GeoGIT geoGit, List<SimpleFeature> features) throws Exception {
        for (SimpleFeature feature : features) {
            if (delete(geoGit, feature)) {
                geoGit.add().call();
            }
        }
    }

    /**
     * Delete a feature from a repository - not staged (GEOGIT.add().call() not called), use
     * deleteAndAdd() for that!
     * 
     * @param geoGit
     * @param feature
     * @return true if the feature was removed, false otherwise
     * @throws Exception
     */
    public static boolean delete(GeoGIT geoGit, SimpleFeature feature) throws Exception {
        final StagingArea index = geoGit.getRepository().getIndex();
        Name name = feature.getType().getName();
        String namespaceURI = name.getNamespaceURI();
        String localPart = name.getLocalPart();
        String id = feature.getIdentifier().getID();
        return index.deleted(namespaceURI, localPart, id);
    }

}
