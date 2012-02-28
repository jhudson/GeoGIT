/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import org.geogit.api.Ref;
import org.geogit.api.SpatialRef;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.google.common.base.Throwables;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Utility methods to deal with various spatial operations
 * 
 * @author groldan
 * 
 */
public class SpatialOps {

    private static final GeometryFactory gfac = new GeometryFactory();

    /**
     * @param target
     *            bounds to be expanded (or created if null) to include {@code include} and then be
     *            returned
     * @param include
     *            bounds to ensure are included by {@code target}
     * @return
     */
    public static BoundingBox expandToInclude(BoundingBox target, BoundingBox include) {
        if (include == null) {
            return target;
        }
        CoordinateReferenceSystem targetCrs;
        if (target == null) {
            try {
                targetCrs = CRS.decode("urn:ogc:def:crs:EPSG::4326");
                target = new ReferencedEnvelope(targetCrs);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            targetCrs = target.getCoordinateReferenceSystem();
        }
        CoordinateReferenceSystem sourceCrs = include.getCoordinateReferenceSystem();
        if (sourceCrs == null) {
            sourceCrs = targetCrs;
        }
        try {
            Envelope env = include;
            if (!CRS.equalsIgnoreMetadata(targetCrs, sourceCrs)) {
                MathTransform mathTransform = CRS.findMathTransform(sourceCrs, targetCrs);
                env = CRS.transform(mathTransform, include);
            }
            target.include(env.getMinimum(0), env.getMinimum(1));
            target.include(env.getMaximum(0), env.getMaximum(1));
        } catch (Exception e) {
            Throwables.propagate(e);
        }
        return target;
    }

    public static BoundingBox aggregatedBounds(Ref oldObject, Ref newObject) {
        if (!(oldObject instanceof SpatialRef)) {
            return boundsOf(newObject);
        }

        if (!(newObject instanceof SpatialRef)) {
            return boundsOf(oldObject);
        }
        BoundingBox bounds1 = boundsOf(oldObject);
        BoundingBox bounds2 = boundsOf(newObject);
        return expandToInclude(bounds1, bounds2);
    }

    private static BoundingBox boundsOf(Ref ref) {
        if (!(ref instanceof SpatialRef)) {
            return null;
        }
        return ((SpatialRef) ref).getBounds();
    }

    /**
     * Creates and returns a geometry out of bounds (a point if bounds.getSpan(0) ==
     * bounds.getSpan(1) == 0D, a polygon otherwise), setting the bounds
     * {@link BoundingBox#getCoordinateReferenceSystem() CRS} as the geometry's
     * {@link Geometry#getUserData() user data}.
     */
    public static Geometry toGeometry(final BoundingBox bounds) {
        if (bounds == null) {
            return null;
        }
        Geometry geom;
        if (bounds.getSpan(0) == 0D && bounds.getSpan(1) == 0D) {
            geom = gfac.createPoint(new Coordinate(bounds.getMinX(), bounds.getMinY()));
        } else {
            geom = JTS.toGeometry(bounds, gfac);
        }
        geom.setUserData(bounds.getCoordinateReferenceSystem());
        return geom;
    }
}
