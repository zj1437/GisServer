package org.pygisserver.web.wmsprotocol.entity;

import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.vector.ClipProcess;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PyClippedFeatureSource<T extends FeatureType, F extends Feature> extends PyDecoratingFeatureSource<T, F> {
    static final Logger LOGGER = Logging.getLogger(PyClippedFeatureSource.class);

    Geometry clip;

    public PyClippedFeatureSource(FeatureSource<T, F> delegate, Geometry clipGeometry) {
        super(delegate);
        this.clip = reproject(delegate.getInfo().getCRS(), clipGeometry);
    }

    private Geometry reproject(CoordinateReferenceSystem fsCRS, Geometry clipGeom) {
        try {
            CoordinateReferenceSystem geomCRS = CRS.decode("EPSG:" + clipGeom.getSRID());
            if (CRS.isTransformationRequired(geomCRS, fsCRS)) {
                MathTransform mt = CRS.findMathTransform(geomCRS, fsCRS);
                clipGeom = JTS.transform(clipGeom, mt);
                clipGeom.setSRID(CRS.lookupEpsgCode(fsCRS, false));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "failed to reproject " + clipGeom.toText());
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        return clipGeom;
    }

    @Override
    public ReferencedEnvelope getBounds() throws IOException {
        ReferencedEnvelope orignalBounds = super.getBounds();
        return JTS.toEnvelope(clip.intersection(JTS.toGeometry(orignalBounds)));
    }

    @Override
    public ReferencedEnvelope getBounds(Query query) throws IOException {
        return this.getFeatures(query).getBounds();
    }

    @Override
    public int getCount(Query query) throws IOException {
        return this.getFeatures(query).size();
    }

    @Override
    public FeatureCollection<T, F> getFeatures() throws IOException {
        return getClippedCollection(super.getFeatures(), clip);
    }

    @Override
    public FeatureCollection<T, F> getFeatures(Filter filter) throws IOException {
        return getClippedCollection(super.getFeatures(filter), clip);
    }

    @Override
    public FeatureCollection<T, F> getFeatures(Query query) throws IOException {
        return getClippedCollection(super.getFeatures(query), clip);
    }

    @SuppressWarnings("unchecked")
    private FeatureCollection<T, F> getClippedCollection(
            FeatureCollection<T, F> fc, Geometry clipGeom) {
        if (fc instanceof SimpleFeatureCollection) {
            return (FeatureCollection<T, F>)
                    new ClipProcess().execute((SimpleFeatureCollection) fc, clipGeom, false);
        }
        return fc;
    }
}
