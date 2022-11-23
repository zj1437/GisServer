package org.pygisserver.web.wmsprotocol.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.WfsFactory;
import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.mapper.MapServerDataMapper;
import org.pygisserver.web.wmsprotocol.entity.PyFeatureCollectionDecorator;
import org.pygisserver.web.wmsprotocol.entity.PyFeatureInfoRequestParameters;
import org.pygisserver.web.wmsprotocol.entity.PyGetFeatureInfoRequest;
import org.pygisserver.web.wmsprotocol.identifier.PyLayerIdentifier;
import org.geoserver.feature.RetypingFeatureCollection;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Component
@Slf4j
public class WmsGetFeatureInfo {

    @Autowired
    private MapServerDataMapper mapServerDataMapper;


    public FeatureCollectionType run(final PyGetFeatureInfoRequest request) {
        List<FeatureCollection> results = null;
        try {
            results = execute(request);
        } catch (Exception e) {
            log.error("wms getFeatureInfo has error : " + e.getLocalizedMessage());
        }
        return buildResults(results);
    }

    private FeatureCollectionType buildResults(List<FeatureCollection> results) {
        FeatureCollectionType result = WfsFactory.eINSTANCE.createFeatureCollectionType();
        result.setTimeStamp(Calendar.getInstance());
        result.getFeature().addAll(results);
        return result;
    }

    private PyLayerIdentifier getLayerIdentifier(
            LayerBaseInfo layer, List<PyLayerIdentifier> identifiers) {
        for (PyLayerIdentifier identifier : identifiers) {
            if (identifier.canHandle(layer)) {
                return identifier;
            }
        }

        throw new ServiceException(
                "Could not find any identifier that can handle layer "
                        + layer.getLayerName()
                        + " among these identifiers: "
                        + identifiers);
    }

    private List<FeatureCollection> execute(PyGetFeatureInfoRequest request) throws Exception {
        final List<String> requestedLayers = request.getQueryLayers();
        PyFeatureInfoRequestParameters requestParams = new PyFeatureInfoRequestParameters(request);

        List<FeatureCollection> results = new ArrayList<>(requestedLayers.size());

        int maxFeatures = request.getFeatureCount();
        List<PyLayerIdentifier> identifiers = GeoServerExtensions.extensions(PyLayerIdentifier.class);
        for (final String layer : requestedLayers) {
            try {
                LayerBaseInfo layerBaseInfo = mapServerDataMapper.queryLayerInfo(layer);
                if (!StringUtils.isEmpty(requestParams.getStyles()) && !"population".equals(requestParams.getStyles())){
                    //输入特殊样式筛选
                    layerBaseInfo.setStyleName(requestParams.getStyles());
                } else {
                    layerBaseInfo.setStyleName(mapServerDataMapper.queryLayerStyleName(layer));
                }
                PyLayerIdentifier<?> identifier = getLayerIdentifier(layerBaseInfo, identifiers);
                List<FeatureCollection> identifiedCollections =
                        identifier.identify(requestParams, layerBaseInfo, maxFeatures);
                if (identifiedCollections != null) {
                    for (FeatureCollection identifierCollection : identifiedCollections) {
                        FeatureCollection fc =
                                selectProperties(requestParams, identifierCollection);
                        maxFeatures = addToResults(fc, results, layerBaseInfo, request, maxFeatures);
                    }
                    if (maxFeatures <= 0) {
                        break;
                    }
                }
            } catch (Exception e) {
                throw new ServiceException(
                        "Failed to run GetFeatureInfo on layer " + layer, e);
            }

            requestParams.nextLayer();
        }
        return results;
    }


    private int addToResults(
            FeatureCollection collection,
            List<FeatureCollection> results,
            final LayerBaseInfo layer,
            PyGetFeatureInfoRequest request,
            int maxFeatures) {
        if (collection != null) {
            if (!(collection.getSchema() instanceof SimpleFeatureType)) {
                Name name =
                        new NameImpl(
                                layer.getLayerName(),
                                layer.getLayerName());
                collection = new PyFeatureCollectionDecorator(name, collection);
            }

            int size = collection.size();
            if (size != 0) {
                if (!(collection instanceof SimpleFeatureCollection)) {
                    collection
                            .getSchema()
                            .getUserData()
                            .put("targetCrs", request.getRenderedParam().getCrs());
                    collection.getSchema().getUserData().put("targetVersion", "wms:getfeatureinfo");
                }

                results.add(collection);
                maxFeatures -= size;
                if (maxFeatures <= 0) {
                    return 0;
                }
            }
        }
        return maxFeatures;
    }

    protected FeatureCollection selectProperties(
            PyFeatureInfoRequestParameters params, FeatureCollection collection) throws IOException {
        String[] names = params.getPropertyNames();
        if (names != Query.ALL_NAMES && collection instanceof SimpleFeatureCollection) {
            SimpleFeatureCollection sfc = (SimpleFeatureCollection) collection;
            SimpleFeatureType source = sfc.getSchema();
            SimpleFeatureType target = SimpleFeatureTypeBuilder.retype(source, names);
            if (!target.equals(source)) {
                return new RetypingFeatureCollection(sfc, target);
            }
        }

        return collection;
    }

}
