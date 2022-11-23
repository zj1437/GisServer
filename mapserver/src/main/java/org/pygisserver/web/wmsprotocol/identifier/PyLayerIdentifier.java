package org.pygisserver.web.wmsprotocol.identifier;

import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.web.wmsprotocol.entity.PyFeatureInfoRequestParameters;
import org.geotools.feature.FeatureCollection;

import java.util.List;

public interface PyLayerIdentifier<T> {

    boolean canHandle(LayerBaseInfo layer);

    List<FeatureCollection> identify(PyFeatureInfoRequestParameters params, LayerBaseInfo layer, int maxFeatures)
            throws Exception;

    T handleClipParam(PyFeatureInfoRequestParameters params, T fs);
}
