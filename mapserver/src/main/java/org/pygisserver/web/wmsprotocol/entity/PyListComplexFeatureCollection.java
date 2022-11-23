package org.pygisserver.web.wmsprotocol.entity;

import org.geotools.feature.FeatureIterator;
import org.geotools.feature.collection.BaseFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class PyListComplexFeatureCollection extends BaseFeatureCollection {

    private List<? extends Feature> list;

    private ReferencedEnvelope bounds = null;

    public PyListComplexFeatureCollection(FeatureType schema, List<? extends Feature> list) {
        super(schema);
        this.list = list;
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public FeatureIterator features() {
        return new ListComplexFeatureIterator(list);
    }

    @Override
    public synchronized ReferencedEnvelope getBounds() {
        if (bounds == null) {
            bounds = calculateBounds();
        }
        return bounds;
    }

    private ReferencedEnvelope calculateBounds() {
        ReferencedEnvelope extent = new ReferencedEnvelope();
        for (Feature feature : list) {
            if (feature == null) continue;
            ReferencedEnvelope bbox = ReferencedEnvelope.reference(feature.getBounds());
            if (bbox == null || bbox.isEmpty() || bbox.isNull()) continue;
            extent.expandToInclude(bbox);
        }
        return new ReferencedEnvelope(extent, schema.getCoordinateReferenceSystem());
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    private static class ListComplexFeatureIterator implements FeatureIterator {
        private Iterator<? extends Feature> iter;

        public ListComplexFeatureIterator(List<? extends Feature> features) {
            iter = features.iterator();
        }

        @Override
        public void close() {
            if (iter instanceof FeatureIterator) {
                ((FeatureIterator<?>) iter).close();
            }
        }

        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }

        @Override
        public Feature next() throws NoSuchElementException {
            return iter.next();
        }
    }
}
