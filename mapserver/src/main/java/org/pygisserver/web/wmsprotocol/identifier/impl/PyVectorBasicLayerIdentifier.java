package org.pygisserver.web.wmsprotocol.identifier.impl;

import lombok.extern.slf4j.Slf4j;
import org.pygisserver.common.entity.PyRenderRequestParam;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.entity.PostGisConnectEntity;
import org.pygisserver.render.service.impl.PyFeatureSource;
import org.pygisserver.render.util.PyParseStyle;
import org.pygisserver.render.util.LayerBaseInfoUtil;
import org.pygisserver.web.wmsprotocol.entity.PyFeatureInfoRequestParameters;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.platform.ServiceException;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeImpl;
import org.geotools.filter.IllegalFilterException;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.renderer.lite.MetaBufferEstimator;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Or;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * static不加载此类，目前用不上
 */
@Component
@Slf4j
public class PyVectorBasicLayerIdentifier extends PyAbstractVectorLayerIdentifier {

    @Autowired
    private PyParseStyle parseStyle;

    @Autowired
    private LayerBaseInfoUtil layerBaseInfoUtil;

    static final Logger LOGGER = Logging.getLogger(PyVectorBasicLayerIdentifier.class);

    public static final String FEATUREINFO_DEFAULT_BUFFER =
            "org.geoserver.wms.featureinfo.minBuffer";
    protected static final int MIN_BUFFER_SIZE = Integer.getInteger(FEATUREINFO_DEFAULT_BUFFER, 5);

    public List<FeatureCollection> identify(PyFeatureInfoRequestParameters params, LayerBaseInfo layerBaseInfo, int maxFeatures)
            throws Exception {
        LOGGER.log(Level.FINER, "Appliying bbox based feature info identifier");
        final Filter filter = params.getFilter();
        final Style style = parseStyle.parserStyleByStyleName(layerBaseInfo.getStyleName());
        final List<Rule> rules = getActiveRules(style, params.getScaleDenominator());
        if (rules.isEmpty()) {
            return null;
        }
        double radius = getSearchRadius(params, layerBaseInfo, rules);
        ReferencedEnvelope queryEnvelope = getEnvelopeFilter(params, radius);
        CoordinateReferenceSystem requestedCRS = params.getRequestedCRS();
        CoordinateReferenceSystem dataCRS = params.getRenderReq().getCrs();
        if ((requestedCRS != null) && !CRS.equalsIgnoreMetadata(dataCRS, requestedCRS)) {
            if (dataCRS.getCoordinateSystem().getDimension() == 3
                    && requestedCRS.getCoordinateSystem().getDimension() == 2) {
                queryEnvelope = JTS.transformTo3D(queryEnvelope, dataCRS, true, 10);
            } else {
                queryEnvelope = queryEnvelope.transform(dataCRS, true);
            }
        }
        FeatureSource<? extends FeatureType, ? extends Feature> featureSource;
        SimpleFeatureType featureType = null;
        PyRenderRequestParam pyRenderRequestParam = new PyRenderRequestParam();
        pyRenderRequestParam.setLayerName(layerBaseInfo.getLayerName());
//        pyRenderRequestParam.setDataFormat();
        if (layerBaseInfo.getFilePath().split(";").length != 1){
            DataStore dataStore = DataStoreFinder.getDataStore(createVectorConnectionParam(pyRenderRequestParam));
            String typeName = layerBaseInfo.getFilePath().split(";")[1];
            featureType = (SimpleFeatureTypeImpl) dataStore.getSchema(typeName);
            featureType = rebuildFeatureType(featureType);
            SimpleFeatureSource simpleFeatureSource = dataStore.getFeatureSource(typeName);
            featureSource = new PyFeatureSource(simpleFeatureSource, featureType, Filter.INCLUDE, featureType.getCoordinateReferenceSystem());
        } else {
            ShapefileDataStoreFactory shapefileDataStoreFactory = new ShapefileDataStoreFactory();
            DataStore dataStore = shapefileDataStoreFactory.createDataStore(createVectorConnectionParam(pyRenderRequestParam));
            File file = new File(layerBaseInfo.getFilePath());
            String shapeFileName = file.getName().substring(0,file.getName().indexOf("."));
            featureSource = dataStore.getFeatureSource(shapeFileName);
            featureType = rebuildFeatureType((SimpleFeatureType) featureSource.getSchema());
            featureSource = new PyFeatureSource((FeatureSource<SimpleFeatureType, SimpleFeature>) featureSource, featureType, Filter.INCLUDE, featureType.getCoordinateReferenceSystem());
        }
        featureSource = super.handleClipParam(params, featureSource);
        FeatureType schema = featureSource.getSchema();

        Filter getFInfoFilter = null;
        FilterFactory2 ff = params.getFilterFactory();
        try {
            GeometryDescriptor geometryDescriptor = schema.getGeometryDescriptor();
            String localName = geometryDescriptor.getLocalName();
            Polygon queryPolygon = JTS.toGeometry(queryEnvelope);
            getFInfoFilter = ff.intersects(ff.property(localName), ff.literal(queryPolygon));
        } catch (IllegalFilterException e) {
            log.error("FeVectorBasicLayerIdentifier shape identify has error : " + e.getLocalizedMessage());
            throw new ServiceException("Internal error : " + e.getMessage(), e);
        }
        if (filter != null) {
            getFInfoFilter = ff.and(getFInfoFilter, filter);
        }

        Filter postFilter = Filter.INCLUDE;
        Filter rulesFilters = buildRulesFilter(ff, rules);
        if (!(featureSource.getSchema() instanceof SimpleFeatureType)
                || !(rulesFilters instanceof Or)
                || (rulesFilters instanceof Or && ((Or) rulesFilters).getChildren().size() <= 20)) {
            getFInfoFilter = ff.and(getFInfoFilter, rulesFilters);
        } else {
            postFilter = rulesFilters;
        }
//        // handle time/elevation
//        Filter timeElevationFilter =
//                wms.getTimeElevationToFilter(
//                        params.getTimes(), params.getElevations(), featureType);
//        getFInfoFilter = Filters.and(ff, getFInfoFilter, timeElevationFilter);

        // simplify the filter
        SimplifyingFilterVisitor simplifier = new SimplifyingFilterVisitor();
        getFInfoFilter = (Filter) getFInfoFilter.accept(simplifier, null);
        String typeName = schema.getName().getLocalPart();
        Query q =
                new Query(
                        typeName,
                        null,
                        getFInfoFilter,
                        maxFeatures,
                        params.getPropertyNames(),
                        null);
//        q.setSortBy(params.getSort());
        q.setSortBy(null);
        final Map<String, String> viewParams = params.getViewParams();
        if (viewParams != null && !viewParams.isEmpty()) {
            q.setHints(new Hints(Hints.VIRTUAL_TABLE_PARAMETERS, viewParams));
        }
        FeatureCollection<? extends FeatureType, ? extends Feature> match;
        match = featureSource.getFeatures(q);

//        if (!Filter.INCLUDE.equals(postFilter)) {
//            match = new FilteringFeatureCollection<>(match, postFilter);
//        }

        return Collections.singletonList(match);
    }

    private String getSrs(CoordinateReferenceSystem resourceCrs) throws FactoryException {
        String srsName = null;
        Set<ReferenceIdentifier> refrences = resourceCrs.getIdentifiers();
        if (refrences != null && !refrences.isEmpty()) {
            srsName = refrences.stream().findFirst().get().toString();
            return srsName;
        }else {
            //如果proj中不自带srs，则查询获取
            Integer epsgCode = CRS.lookupEpsgCode(resourceCrs, false);
            if (epsgCode == null) {
                return "EPSG:4326";
            } else {
                return "EPSG:" + epsgCode;
            }
        }
    }

    private SimpleFeatureType rebuildFeatureType(SimpleFeatureType featureType)
            throws NoSuchAuthorityCodeException, FactoryException {
        SimpleFeatureTypeBuilder sb = new SimpleFeatureTypeBuilder();
        sb.init(featureType);
        sb.setName(featureType.getName());
        sb.setAttributes((AttributeDescriptor[]) null);
        List<AttributeDescriptor> attributes = featureType.getAttributeDescriptors();
        for (AttributeDescriptor ad : attributes) {
            if (ad instanceof GeometryDescriptor) {
                GeometryDescriptor old = (GeometryDescriptor) ad;
                CoordinateReferenceSystem sourceCoordinate = old.getCoordinateReferenceSystem();
                String srsName = null;
                if (sourceCoordinate == null) {
                    srsName = "EPSG:404000";
                } else {
                    srsName = getSrs(sourceCoordinate);
                }
                AttributeTypeBuilder b = new AttributeTypeBuilder();
                b.init(old);
                b.setCRS(CRS.decode(srsName, true));
                ad = b.buildDescriptor(old.getLocalName());
            }
            sb.add(ad);
        }
        featureType = sb.buildFeatureType();
        return featureType;
    }

    private Map<String, Serializable> createVectorConnectionParam(PyRenderRequestParam mapBaseDataInfo) {
        // 创建默认的连接信息
        String fileUrl = mapBaseDataInfo.getFileUrl();
        Map<String, Serializable> connectionParameters = new HashMap<String, Serializable>();
        switch (mapBaseDataInfo.getDataFormat()) {
            case "Shape":
                LayerBaseInfo layerBaseInfo = layerBaseInfoUtil.queryServerBaseInfo(mapBaseDataInfo.getLayerName());
                connectionParameters.put("charset", layerBaseInfo.getEncoding());
                connectionParameters.put("filetype", "shapefile");
                connectionParameters.put("create spatial index", true);
                connectionParameters.put("memory mapped buffer", false);
                connectionParameters.put("enable spatial index", true);
                connectionParameters.put("cache and reuse memory maps", true);
                connectionParameters.put("url", "file://" + fileUrl);
                connectionParameters.put("namespace", "localhost");
                TimeZone zone = TimeZone.getDefault();
                connectionParameters.put("timezone", zone);
                connectionParameters.put("fstype", "shape");
                break;
            case "PostGIS":
                PostGisConnectEntity entity = mapBaseDataInfo.getPostgisConnection();
                //TODO: 获取当前数据对应的postgis的连接参数
                connectionParameters.put("host", entity.getDatabaseIp());
                connectionParameters.put("port", entity.getPort());
                connectionParameters.put("database", entity.getDatabaseName());
                connectionParameters.put("user", entity.getUserName());
                connectionParameters.put("passwd", new String(Base64.getDecoder().decode(entity.getPassword())));
                connectionParameters.put("dbtype", "postgis");
            default:
                break;
        }
        return connectionParameters;
    }

    private double getSearchRadius(
            PyFeatureInfoRequestParameters params, final LayerBaseInfo layer, final List<Rule> rules) {
        double radius;
        int buffer = params.getBuffer();
        if (buffer <= 0) {
            Integer layerBuffer = null;
//            final LayerInfo layerInfo = layer.getLayerInfo();
            final LayerInfo layerInfo = null;
            if (layerInfo != null) { // it is a local layer
                layerBuffer = layerInfo.getMetadata().get(LayerInfo.BUFFER, Integer.class);
            }
            if (layerBuffer != null && layerBuffer > 0) {
                radius = layerBuffer;
            } else {
                // estimate the radius given the currently active rules
                MetaBufferEstimator estimator = new MetaBufferEstimator();
                for (Rule rule : rules) {
                    rule.accept(estimator);
                }

                int estimatedRadius = estimator.getBuffer() / 2;
                if (estimatedRadius < MIN_BUFFER_SIZE) {
                    radius = MIN_BUFFER_SIZE;
                } else {
                    radius = estimatedRadius;
                }
            }
        } else {
            radius = buffer;
        }
//        int maxRadius = wms.getMaxBuffer();
        int maxRadius = 0;
        if (maxRadius > 0 && radius > maxRadius) {
            radius = maxRadius;
        }

        return radius;
    }

    private Filter buildRulesFilter(org.opengis.filter.FilterFactory ff, List<Rule> rules) {
        // build up a or of all the rule filters
        List<Filter> filters = new ArrayList<>();
        for (Rule rule : rules) {
            if (rule.getFilter() == null || rule.isElseFilter()) return Filter.INCLUDE;
            filters.add(rule.getFilter());
        }
        Filter or = ff.or(filters);
        SimplifyingFilterVisitor simplifier = new SimplifyingFilterVisitor();
        return (Filter) or.accept(simplifier, null);
    }

    private ReferencedEnvelope getEnvelopeFilter(
            PyFeatureInfoRequestParameters params, double radius) {
        final int x = params.getX();
        final int y = params.getY();
        final ReferencedEnvelope bbox = params.getRequestedBounds();
        final int width = params.getWidth();
        final int height = params.getHeight();
//        Coordinate upperLeft = WMS.pixelToWorld(x - radius, y - radius, bbox, width, height);
        Coordinate upperLeft = null;
//        Coordinate lowerRight = WMS.pixelToWorld(x + radius, y + radius, bbox, width, height);
        Coordinate lowerRight = null;

        return new ReferencedEnvelope(
                upperLeft.x,
                lowerRight.x,
                lowerRight.y,
                upperLeft.y,
                bbox.getCoordinateReferenceSystem());
    }
}
