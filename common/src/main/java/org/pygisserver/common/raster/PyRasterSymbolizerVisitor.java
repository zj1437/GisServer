package org.pygisserver.common.raster;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.FeatureTypes;
import org.geotools.styling.*;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.parameter.Parameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PyRasterSymbolizerVisitor implements StyleVisitor {

    double scaleDenominator;

    FeatureType featureType;

    List<RasterSymbolizer> symbolizers = new ArrayList<RasterSymbolizer>();

    boolean otherSymbolizers = false;

    Expression rasterTransformation = null;

    boolean otherRenderingTransformations = false;
    
    boolean setScaleDenominator = false;

    public PyRasterSymbolizerVisitor(double scaleDenominator, FeatureType featureType) {
        this.scaleDenominator = scaleDenominator;
        this.setScaleDenominator = true;
        this.featureType = featureType;
    }

    public PyRasterSymbolizerVisitor() {
    }

    public void reset() {
        symbolizers.clear();
        otherSymbolizers = false;
        rasterTransformation = null;
        otherRenderingTransformations = false;
    }

    public List<RasterSymbolizer> getRasterSymbolizers() {
        if (otherSymbolizers || otherRenderingTransformations) return Collections.emptyList();
        else return symbolizers;
    }

    public Expression getRasterRenderingTransformation() {
        return rasterTransformation;
    }

    public void visit(StyledLayerDescriptor sld) {
        for (StyledLayer sl : sld.getStyledLayers()) {
            if (sl instanceof UserLayer) {
                ((UserLayer) sl).accept(this);
            } else if (sl instanceof NamedLayer) {
                ((NamedLayer) sl).accept(this);
            }
        }
    }

    public void visit(NamedLayer layer) {
        for (Style s : layer.getStyles()) s.accept(this);
    }

    public void visit(UserLayer layer) {
        for (Style s : layer.getUserStyles()) s.accept(this);
    }

    public void visit(FeatureTypeConstraint ftc) {
        // nothing to do
    }

    public void visit(Style style) {
        for (FeatureTypeStyle fts : style.featureTypeStyles()) {
            fts.accept(this);
        }
    }

    public void visit(Rule rule) {
    	if (setScaleDenominator) {
    		if (rule.getMinScaleDenominator() < scaleDenominator
    				&& rule.getMaxScaleDenominator() > scaleDenominator) {
    			for (Symbolizer s : rule.symbolizers()) s.accept(this);
    		}
    	}else {
    		for (Symbolizer s : rule.symbolizers()) s.accept(this);
    	}
    }

    public void visit(FeatureTypeStyle fts) {
        // use the same logic as streaming renderer to decide if a fts is active
        if (featureType == null
                || (featureType.getName().getLocalPart() != null)
                && (fts.featureTypeNames().isEmpty()
                || fts.featureTypeNames()
                .stream()
                .anyMatch(tn -> FeatureTypes.matches(featureType, tn)))) {
            Expression tx = fts.getTransformation();
            if (tx != null) {
                boolean rasterTransformation = false;
                if (tx instanceof Function) {
                    Function f = (Function) tx;
                    FunctionName name = f.getFunctionName();
                    if (name != null) {
                        Parameter<?> result = name.getReturn();
                        if (result != null) {
                            if (GridCoverage2D.class.isAssignableFrom(result.getType())) {
                                rasterTransformation = true;
                                this.rasterTransformation = tx;
                            }
                        }
                    }
                }
                otherRenderingTransformations |= !rasterTransformation;
            }
            for (Rule r : fts.rules()) {
                r.accept(this);
            }
        }
    }

    public void visit(Fill fill) {
        // nothing to do

    }

    public void visit(Stroke stroke) {
        // nothing to do
    }

    public void visit(Symbolizer sym) {
        if (sym instanceof RasterSymbolizer) {
            visit((RasterSymbolizer) sym);
        } else {
            otherSymbolizers = true;
        }
    }

    public void visit(PointSymbolizer ps) {
        otherSymbolizers = true;
    }

    public void visit(LineSymbolizer line) {
        otherSymbolizers = true;
    }

    public void visit(PolygonSymbolizer poly) {
        otherSymbolizers = true;
    }

    public void visit(TextSymbolizer text) {
        otherSymbolizers = true;
    }

    public void visit(RasterSymbolizer raster) {
        this.symbolizers.add(raster);
    }

    public void visit(Graphic gr) {
        // nothing to do

    }

    public void visit(Mark mark) {
        // nothing to do

    }

    public void visit(ExternalGraphic exgr) {
        // nothing to do

    }

    public void visit(PointPlacement pp) {
        // nothing to do

    }

    public void visit(AnchorPoint ap) {
        // nothing to do
    }

    public void visit(Displacement dis) {
        // nothing to do
    }

    public void visit(LinePlacement lp) {
        // nothing to do
    }

    public void visit(Halo halo) {
        // nothing to do
    }

    public void visit(ColorMap colorMap) {
        // nothing to do
    }

    public void visit(ColorMapEntry colorMapEntry) {
        // nothing to do
    }

    public void visit(ContrastEnhancement contrastEnhancement) {
        // nothing to do
    }

    public void visit(ImageOutline outline) {
        // nothing to do
    }

    public void visit(ChannelSelection cs) {
        // nothing to do
    }

    public void visit(OverlapBehavior ob) {
        // nothing to do
    }

    public void visit(SelectedChannelType sct) {
        // nothing to do
    }

    public void visit(ShadedRelief sr) {
        // nothing to do
    }
}
