package org.pygisserver.web.wmsprotocol.entity;

import org.geotools.data.*;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.decorate.AbstractDecorator;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

import java.awt.*;
import java.io.IOException;
import java.util.Set;

public abstract class PyDecoratingFeatureSource<T extends FeatureType, F extends Feature>
        extends AbstractDecorator<FeatureSource<T, F>> implements FeatureSource<T, F> {

    public PyDecoratingFeatureSource(FeatureSource<T, F> delegate) {
        super(delegate);
    }

    public void addFeatureListener(FeatureListener listener) {
        delegate.addFeatureListener(listener);
    }

    public ReferencedEnvelope getBounds() throws IOException {
        return delegate.getBounds();
    }

    public ReferencedEnvelope getBounds(Query query) throws IOException {
        return delegate.getBounds(query);
    }

    public int getCount(Query query) throws IOException {
        return delegate.getCount(query);
    }

    public DataAccess<T, F> getDataStore() {
        return delegate.getDataStore();
    }

    public FeatureCollection<T, F> getFeatures() throws IOException {
        return delegate.getFeatures();
    }

    public FeatureCollection<T, F> getFeatures(Filter filter) throws IOException {
        return delegate.getFeatures(filter);
    }

    public FeatureCollection<T, F> getFeatures(Query query) throws IOException {
        return delegate.getFeatures(query);
    }

    public ResourceInfo getInfo() {
        return delegate.getInfo();
    }

    public Name getName() {
        return delegate.getName();
    }

    public QueryCapabilities getQueryCapabilities() {
        return delegate.getQueryCapabilities();
    }

    public T getSchema() {
        return delegate.getSchema();
    }

    public Set<RenderingHints.Key> getSupportedHints() {
        return delegate.getSupportedHints();
    }

    public void removeFeatureListener(FeatureListener listener) {
        delegate.removeFeatureListener(listener);
    }
}
