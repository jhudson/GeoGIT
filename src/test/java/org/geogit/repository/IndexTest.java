/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import org.geogit.api.ObjectId;
import org.geogit.api.ShowOp;
import org.geogit.test.RepositoryTestCase;
import org.geotools.data.DataUtilities;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.io.WKTReader;

public class IndexTest extends RepositoryTestCase {

    private Index index;

    @Override
    protected void setUpInternal() throws Exception {
        index = repo.getIndex();
    }

    // two features with the same content and different fid should point to the same object
    public void testInsertIdenticalObjects() throws Exception {
        ObjectId oId1 = insertAndAdd(feature1_1);
        Feature equalContentFeature = feature(featureType1, "DifferentId",
                ((SimpleFeature) feature1_1).getAttributes().toArray());

        ObjectId oId2 = insertAndAdd(equalContentFeature);

        // BLOBS.print(repo.getRawObject(insertedId1), System.err);
        // BLOBS.print(repo.getRawObject(insertedId2), System.err);
        assertNotNull(oId1);
        assertNotNull(oId2);
        assertEquals(oId1, oId2);
    }

    // two features with different content should point to different objects
    public void testInsertNonEqualObjects() throws Exception {
        ObjectId oId1 = insertAndAdd(feature1_1);

        ObjectId oId2 = insertAndAdd(feature1_2);
        assertNotNull(oId1);
        assertNotNull(oId2);
        assertFalse(oId1.equals(oId2));
    }

    public void testWriteTree() throws Exception {
        String namespace = "http://geoserver.org/test";
        String typeName = "TestType";
        String typeSpec = "sp:String,ip:Integer,pp:LineString:srid=4326";
        SimpleFeatureType featureType = DataUtilities.createType(namespace, typeName, typeSpec);

        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
        builder.set("sp", "String Property");
        builder.set("ip", Integer.valueOf(1000));
        builder.set("pp", new WKTReader()
                .read("LINESTRING(1 1, 2 2, 3 3, 4 4, 5 5, 6 6, 7 7, 8 8, 9 9 , 10 10)"));

        Feature feature1 = builder.buildFeature("TestType.feature.1");

        // same data as above
        builder = new SimpleFeatureBuilder(featureType);
        builder.set("sp", "String Property");
        builder.set("ip", Integer.valueOf(1000));
        builder.set("pp", new WKTReader()
                .read("LINESTRING(1 1, 2 2, 3 3, 4 4, 5 5, 6 6, 7 7, 8 8, 9 9 , 10 10)"));

        Feature feature2 = builder.buildFeature("TestType.feature.2");

        final ObjectId insertedId1 = insertAndAdd(feature1);
        final ObjectId insertedId2 = insertAndAdd(feature2);
        assertEquals(insertedId1, insertedId2);

        Repository mockRepo = mock(Repository.class);
        InputStream value = repo.getRawObject(insertedId1);

        when(mockRepo.getRawObject(eq(insertedId1))).thenReturn(value);
        ShowOp showOp = new ShowOp(mockRepo);
        showOp.setObjectId(insertedId1).call();
    }
}
