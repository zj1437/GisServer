package org.pygisserver.web.wmsprotocol.identifier.impl;

import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.enumm.DataTypeEnum;
import org.pygisserver.web.wmsprotocol.entity.PyClippedFeatureSource;
import org.pygisserver.web.wmsprotocol.entity.PyFeatureInfoRequestParameters;
import org.pygisserver.web.wmsprotocol.identifier.PyLayerIdentifier;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

import java.util.ArrayList;
import java.util.List;

public class PyAbstractVectorLayerIdentifier
        implements PyLayerIdentifier<FeatureSource<? extends FeatureType, ? extends Feature>> {

    private static final double TOLERANCE = 1e-6;

    public boolean canHandle(LayerBaseInfo layer) {
        int type = layer.getDataType();
        return type == DataTypeEnum.VECTOR.getFileCode();
    }

    @Override
    public List<FeatureCollection> identify(PyFeatureInfoRequestParameters params, LayerBaseInfo layer, int maxFeatures) throws Exception {
        return null;
    }

    protected List<Rule> getActiveRules(Style style, double scaleDenominator) {
        List<Rule> result = new ArrayList<>();

        for (FeatureTypeStyle fts : style.featureTypeStyles()) {
            for (Rule r : fts.rules()) {
                if ((r.getMinScaleDenominator() - TOLERANCE <= scaleDenominator)
                        && (r.getMaxScaleDenominator() + TOLERANCE > scaleDenominator)
                        && r.symbolizers() != null
                        && r.symbolizers().size() > 0) {
                    result.add(r);
                }
            }
        }
        return result;
    }

    public FeatureSource<? extends FeatureType, ? extends Feature> handleClipParam(
            PyFeatureInfoRequestParameters params,
            FeatureSource<? extends FeatureType, ? extends Feature> featureSource) {
        Geometry clipGeom = params.getClip();
        if (clipGeom == null) return featureSource;
        return new PyClippedFeatureSource<>(featureSource, clipGeom);
    }
}
