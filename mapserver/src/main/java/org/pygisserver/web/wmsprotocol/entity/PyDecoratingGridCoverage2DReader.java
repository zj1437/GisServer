package org.pygisserver.web.wmsprotocol.entity;

import it.geosolutions.imageio.maskband.DatasetLayout;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.geometry.GeneralEnvelope;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;

import javax.media.jai.ImageLayout;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public abstract class PyDecoratingGridCoverage2DReader implements GridCoverage2DReader {

    protected GridCoverage2DReader delegate;

    public PyDecoratingGridCoverage2DReader(GridCoverage2DReader delegate) {
        this.delegate = delegate;
    }

    public Format getFormat() {
        return delegate.getFormat();
    }

    public Object getSource() {
        return delegate.getSource();
    }

    public String[] getMetadataNames() throws IOException {
        return delegate.getMetadataNames();
    }

    public GeneralEnvelope getOriginalEnvelope() {
        return delegate.getOriginalEnvelope();
    }

    public GeneralEnvelope getOriginalEnvelope(String coverageName) {
        return delegate.getOriginalEnvelope(coverageName);
    }

    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return delegate.getCoordinateReferenceSystem();
    }

    public CoordinateReferenceSystem getCoordinateReferenceSystem(String coverageName) {
        return delegate.getCoordinateReferenceSystem(coverageName);
    }

    public GridEnvelope getOriginalGridRange() {
        return delegate.getOriginalGridRange();
    }

    public String[] getMetadataNames(String coverageName) throws IOException {
        return delegate.getMetadataNames(coverageName);
    }

    public GridEnvelope getOriginalGridRange(String coverageName) {
        return delegate.getOriginalGridRange(coverageName);
    }

    public MathTransform getOriginalGridToWorld(PixelInCell pixInCell) {
        return delegate.getOriginalGridToWorld(pixInCell);
    }

    public MathTransform getOriginalGridToWorld(String coverageName, PixelInCell pixInCell) {
        return delegate.getOriginalGridToWorld(coverageName, pixInCell);
    }

    public GridCoverage2D read(GeneralParameterValue[] parameters)
            throws IllegalArgumentException, IOException {
        return delegate.read(parameters);
    }

    public GridCoverage2D read(String coverageName, GeneralParameterValue[] parameters)
            throws IllegalArgumentException, IOException {
        return delegate.read(coverageName, parameters);
    }

    public String getMetadataValue(String name) throws IOException {
        return delegate.getMetadataValue(name);
    }

    public String getMetadataValue(String coverageName, String name) throws IOException {
        return delegate.getMetadataValue(coverageName, name);
    }

    public void dispose() throws IOException {
        delegate.dispose();
    }

    public Set<ParameterDescriptor<List>> getDynamicParameters() throws IOException {
        return delegate.getDynamicParameters();
    }

    public Set<ParameterDescriptor<List>> getDynamicParameters(String coverageName)
            throws IOException {
        return delegate.getDynamicParameters(coverageName);
    }

    public double[] getReadingResolutions(OverviewPolicy policy, double[] requestedResolution)
            throws IOException {
        return delegate.getReadingResolutions(policy, requestedResolution);
    }

    public double[] getReadingResolutions(
            String coverageName, OverviewPolicy policy, double[] requestedResolution)
            throws IOException {
        return delegate.getReadingResolutions(coverageName, policy, requestedResolution);
    }

    public String[] getGridCoverageNames() throws IOException {
        return delegate.getGridCoverageNames();
    }

    public int getGridCoverageCount() throws IOException {
        return delegate.getGridCoverageCount();
    }

    public ImageLayout getImageLayout() throws IOException {
        return delegate.getImageLayout();
    }

    public ImageLayout getImageLayout(String coverageName) throws IOException {
        return delegate.getImageLayout(coverageName);
    }

    public double[][] getResolutionLevels() throws IOException {
        return delegate.getResolutionLevels();
    }

    public double[][] getResolutionLevels(String coverageName) throws IOException {
        return delegate.getResolutionLevels(coverageName);
    }

    @Override
    public DatasetLayout getDatasetLayout() {
        return delegate.getDatasetLayout();
    }

    @Override
    public DatasetLayout getDatasetLayout(String coverageName) {
        return delegate.getDatasetLayout(coverageName);
    }
}
