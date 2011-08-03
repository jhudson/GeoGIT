package org.geogit.api;

import java.util.List;

import org.geogit.repository.Repository;
import org.geogit.repository.WorkingTree;
import org.geogit.test.RepositoryTestCase;
import org.geotools.data.DataUtilities;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.NullProgressListener;
import org.opengis.feature.simple.SimpleFeature;

public class IntegrationTest extends RepositoryTestCase {

    @Override
    protected void setUpInternal() throws Exception {
    }

    public void testInsertAddCommit() throws Exception {

        Repository repository = getRepository();
        WorkingTree workingTree = repository.getWorkingTree();

        FeatureCollection features = DataUtilities.collection(new SimpleFeature[] {
                (SimpleFeature) points1, (SimpleFeature) lines1 });

        List<String> fids = workingTree.insert(features, new NullProgressListener());

    }
}
