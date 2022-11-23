package org.pygisserver.web.wmsprotocol.entity;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.util.ProgressListener;

import java.io.IOException;
import java.util.Collection;

public class PyFeatureCollectionDecorator implements FeatureCollection<FeatureType, Feature> {

    public static Name getName(FeatureCollection fc) {
        if (fc instanceof PyFeatureCollectionDecorator) {
            return ((PyFeatureCollectionDecorator) fc).getName();
        } else {
            return fc.getSchema().getName();
        }
    }

    protected FeatureCollection fc;
    protected Name name;

    public PyFeatureCollectionDecorator(Name name, FeatureCollection fc) {
        this.name = name;
        this.fc = fc;
    }

    public Name getName() {
        return name;
    }

    public FeatureIterator<Feature> features() {
        return (FeatureIterator<Feature>) fc.features();
    }

    public FeatureType getSchema() {
        return fc.getSchema();
    }

    public String getID() {
        return fc.getID();
    }

    public void accepts(FeatureVisitor visitor, ProgressListener progress) throws IOException {
        fc.accepts(visitor, progress);
    }

    public FeatureCollection<FeatureType, Feature> subCollection(Filter filter) {
        return fc.subCollection(filter);
    }

    public FeatureCollection<FeatureType, Feature> sort(SortBy order) {
        return fc.sort(order);
    }

    public ReferencedEnvelope getBounds() {
        return fc.getBounds();
    }

    public boolean contains(Object o) {
        return fc.contains(o);
    }

    public boolean containsAll(Collection<?> o) {
        return fc.containsAll(o);
    }

    public boolean isEmpty() {
        return fc.isEmpty();
    }

    public int size() {
        try (FeatureIterator iterator = features()) {
            int count = 0;
            while (iterator.hasNext()) {
                count++;
                iterator.next();
            }
            return count;
        }
    }

    public Object[] toArray() {
        return fc.toArray();
    }

    public <O> O[] toArray(O[] a) {
        return (O[]) fc.toArray(a);
    }
}
