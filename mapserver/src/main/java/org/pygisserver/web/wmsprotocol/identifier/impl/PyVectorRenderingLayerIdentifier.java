package org.pygisserver.web.wmsprotocol.identifier.impl;

import lombok.extern.slf4j.Slf4j;
import org.pygisserver.common.entity.PyRenderRequestParam;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.entity.PostGisConnectEntity;
import org.pygisserver.common.mapper.MapServerDataMapper;
import org.pygisserver.common.raster.PyFeatureInfoStylePreprocessor;
import org.pygisserver.web.wmsprotocol.entity.PyDynamicBufferEstimator;
import org.pygisserver.web.wmsprotocol.entity.PyDynamicSizeStyleExtractor;
import org.pygisserver.web.wmsprotocol.entity.PyFeatureInfoRequestParameters;
import org.pygisserver.web.wmsprotocol.entity.PyListComplexFeatureCollection;
import org.pygisserver.render.entity.PyRenderedParam;
import org.pygisserver.render.service.impl.PyFeatureSource;
import org.pygisserver.render.util.PyParseStyle;
import org.pygisserver.render.util.LayerBaseInfoUtil;
import org.geoserver.platform.ExtensionPriority;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeImpl;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.renderer.lite.GraphicsAwareDpiRescaleStyleVisitor;
import org.geotools.renderer.lite.MetaBufferEstimator;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.renderer.style.StyleAttributeExtractor;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.visitor.DpiRescaleStyleVisitor;
import org.geotools.styling.visitor.UomRescaleStyleVisitor;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Envelope;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
@Slf4j
public class PyVectorRenderingLayerIdentifier extends PyAbstractVectorLayerIdentifier implements ExtensionPriority {

    @Autowired
    private PyParseStyle parseStyle;

    @Autowired
    private LayerBaseInfoUtil layerBaseInfoUtil;

    @Autowired
    private MapServerDataMapper mapServerDataMapper;


    static final Logger LOGGER = Logging.getLogger(PyVectorRenderingLayerIdentifier.class);
    private static final String FEATURE_INFO_RENDERING_ENABLED_KEY =
            "org.geoserver.wms.featureinfo.render.enabled";
    protected static final int MIN_BUFFER_SIZE =
            Integer.getInteger(PyVectorBasicLayerIdentifier.FEATUREINFO_DEFAULT_BUFFER, 3);
    public static boolean RENDERING_FEATUREINFO_ENABLED;

    private PyVectorBasicLayerIdentifier fallback;
    private static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    static {
        String value = System.getProperty(FEATURE_INFO_RENDERING_ENABLED_KEY, "true");
        RENDERING_FEATUREINFO_ENABLED = Boolean.valueOf(value);
        if (!RENDERING_FEATUREINFO_ENABLED) {
            LOGGER.info(
                    "Rendering based GetFeatureInfo disabled since "
                            + FEATURE_INFO_RENDERING_ENABLED_KEY
                            + " is set to "
                            + value);
        }
    }

    public PyVectorRenderingLayerIdentifier(PyVectorBasicLayerIdentifier fallback) {
        this.fallback = fallback;
    }

    @Override
    public boolean canHandle(LayerBaseInfo layer) {
        if (!RENDERING_FEATUREINFO_ENABLED) {
            return false;
        }
        return super.canHandle(layer);
    }

    @Override
    public List<FeatureCollection> identify(
            PyFeatureInfoRequestParameters params, LayerBaseInfo layerBaseInfo , final int maxFeatures) throws Exception {
        LOGGER.log(Level.FINER, "Applying rendering based feature info identifier");
        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = null;
        SimpleFeatureType featureType = null;
        PyRenderRequestParam pyRenderRequestParam = new PyRenderRequestParam();
        pyRenderRequestParam.setLayerName(layerBaseInfo.getLayerName());
        pyRenderRequestParam.setDataFormat(layerBaseInfo.getFileType());
        pyRenderRequestParam.setFileUrl(layerBaseInfo.getFilePath());
        pyRenderRequestParam.setLayerName(layerBaseInfo.getLayerName());
        if ("PostGIS".equalsIgnoreCase(layerBaseInfo.getFileType())){
            PostGisConnectEntity postGisConnectEntity = queryPostGisInfos(layerBaseInfo.getFilePath().substring(0, layerBaseInfo.getFilePath().indexOf(";")));
            pyRenderRequestParam.setPostgisConnection(postGisConnectEntity);
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
            featureType = rebuildFeatureType(featureSource.getSchema());
            featureSource = new PyFeatureSource(featureSource, featureType, Filter.INCLUDE, featureType.getCoordinateReferenceSystem());
        }
        if (!(featureSource.getSchema() instanceof SimpleFeatureType)) {
            return fallback.identify(params, layerBaseInfo,maxFeatures);
        }
        Style layerStyle = parseStyle.parserStyleByStyleName(layerBaseInfo.getStyleName());
        final Style style =
                preprocessStyle(layerStyle, featureType);
        final int userBuffer = params.getBuffer() > 0 ? params.getBuffer() : MIN_BUFFER_SIZE;
        final int buffer = userBuffer;
        final List<Rule> rules = getActiveRules(style, params.getScaleDenominator());
        if (rules.isEmpty()) {
            return null;
        }
        FeatureLayer layer  = new FeatureLayer(featureSource, layerStyle);
        AffineTransform worldToScreen =
                RendererUtilities.worldToScreenTransform(
                        params.getRequestedBounds(),
                        new Rectangle(params.getWidth(), params.getHeight()));
        AffineTransform screenToWorld = worldToScreen.createInverse();
        rescaleRules(rules, params);
        int radius = getSearchRadius(params, rules, layer, params.getRenderReq(), screenToWorld);
        if (radius < buffer) {
            radius = buffer;
        }
        Envelope targetRasterSpace =
                new Envelope(
                        params.getX() - radius,
                        params.getX() + radius,
                        params.getY() - radius,
                        params.getY() + radius);
        Envelope targetModelSpace =
                JTS.transform(targetRasterSpace, new AffineTransform2D(screenToWorld));
        //屏幕坐标转世界坐标后，在一定范围内取与之相交的要素
        ReferencedEnvelope queryEnvelope = new ReferencedEnvelope(targetModelSpace, params.getRenderReq().getCrs());
        // 不生成地图
//        GetMapOutputFormat rim = createMapOutputFormat(image, featureInfoListener);
//        rim.produceMap(mc);
        Query query = new Query();
        Filter filter = CQL
                .toFilter(String.format("INTERSECTS(%s,ENVELOPE(%s,%s,%s,%s))", featureSource.getSchema().getGeometryDescriptor().getLocalName(), queryEnvelope.getMinX(),
                        queryEnvelope.getMaxX(), queryEnvelope.getMinY(), queryEnvelope.getMaxY()));
        query.setFilter(filter);
        query.setHints(new Hints(Hints.FEATURE_2D, Boolean.TRUE));
        List<SimpleFeature> features = new ArrayList<>();
        FeatureIterator<SimpleFeature> iterator = featureSource.getFeatures(query).features();
        while (iterator.hasNext()){
            SimpleFeature next = iterator.next();
            features.add(next);
        }
        return aggregateByFeatureType(features);
    }

    public PostGisConnectEntity queryPostGisInfos(String sourceName) {
        return mapServerDataMapper.queryPostGisBySourceName(sourceName);
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

    private void rescaleRules(List<Rule> rules, PyFeatureInfoRequestParameters params) {
        Map<Object, Object> rendererParams = new HashMap<>();
        Integer requestedDpi = null;
        if (requestedDpi != null) {
            rendererParams.put(StreamingRenderer.DPI_KEY, requestedDpi);
        }
        double standardDpi = RendererUtilities.getDpi(rendererParams);
        if (requestedDpi != null && standardDpi != requestedDpi) {
            double scaleFactor = requestedDpi / standardDpi;
            DpiRescaleStyleVisitor dpiVisitor =
                    new GraphicsAwareDpiRescaleStyleVisitor(scaleFactor);
            for (int i = 0; i < rules.size(); i++) {
                rules.get(i).accept(dpiVisitor);
                Rule rescaled = (Rule) dpiVisitor.getCopy();
                rules.set(i, rescaled);
            }
        }
        double pixelsPerMeters =
                RendererUtilities.calculatePixelsPerMeterRatio(
                        params.getScaleDenominator(), rendererParams);
        UomRescaleStyleVisitor uomVisitor = new UomRescaleStyleVisitor(pixelsPerMeters);
        for (int i = 0; i < rules.size(); i++) {
            rules.get(i).accept(uomVisitor);
            Rule rescaled = (Rule) uomVisitor.getCopy();
            rules.set(i, rescaled);
        }
    }

    private Style preprocessStyle(Style style, FeatureType schema) {
        PyFeatureInfoStylePreprocessor preprocessor = new PyFeatureInfoStylePreprocessor(schema);
        style.accept(preprocessor);
        Style result = (Style) preprocessor.getCopy();
        return result;
    }

    private List<FeatureCollection> aggregateByFeatureType(
            List<? extends Feature> features) {
        Map<FeatureType, List<Feature>> map = new HashMap<>();
        for (Feature f : features) {
            FeatureType type = f.getType();
            List<Feature> list = map.get(type);
            if (list == null) {
                list = new ArrayList<>();
                map.put(type, list);
            }
            list.add(f);
        }
        List<FeatureCollection> result = new ArrayList<>();
        for (Map.Entry<FeatureType, List<Feature>> entry : map.entrySet()) {
            FeatureType type = entry.getKey();
            List<Feature> list = entry.getValue();
            if (type instanceof SimpleFeatureType) {
                @SuppressWarnings("unchecked")
                List<SimpleFeature> sf = new ArrayList<>((List) list);
                result.add(new ListFeatureCollection((SimpleFeatureType) type, sf));
            } else {
                result.add(new PyListComplexFeatureCollection(type, list));
            }
        }
        return result;
    }

    private int getSearchRadius(
            PyFeatureInfoRequestParameters params,
            List<Rule> rules,
            FeatureLayer layer,
            PyRenderedParam renderedParam,
            AffineTransform screenToWorld)
            throws TransformException, FactoryException, IOException {
        int requestBuffer = params.getBuffer();
        if (requestBuffer > 0) {
            return requestBuffer;
        }
        MetaBufferEstimator estimator = new MetaBufferEstimator();
        for (Rule rule : rules) {
            rule.accept(estimator);
        }
        int estimatedRadius = estimator.getBuffer() / 2;
        if (estimator.isEstimateAccurate()) {
            if (estimatedRadius < MIN_BUFFER_SIZE) {
                return MIN_BUFFER_SIZE;
            } else {
                return estimatedRadius;
            }
        } else {
            PyDynamicSizeStyleExtractor extractor = new PyDynamicSizeStyleExtractor();
            final List<Rule> dynamicRules = new ArrayList<>();
            for (Rule rule : rules) {
                rule.accept(extractor);
                Rule copy = (Rule) extractor.getCopy();
                if (copy != null) {
                    dynamicRules.add(copy);
                }
            }
            if (dynamicRules.isEmpty()) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(
                            "No dynamic rules found, even if the estimator initially though so, "
                                    + "using the static analysis result: "
                                    + estimatedRadius);
                }
                return estimatedRadius;
            }
            FeatureSource<?, ?> fs = layer.getFeatureSource();
            Envelope targetRasterSpace =
                    new Envelope(
                            -estimatedRadius,
                            params.getWidth() + estimatedRadius,
                            -estimatedRadius,
                            params.getWidth() + estimatedRadius);
            Envelope expanded =
                    JTS.transform(targetRasterSpace, new AffineTransform2D(screenToWorld));
            ReferencedEnvelope renderingBBOX = new ReferencedEnvelope(expanded, renderedParam.getCrs());
            ReferencedEnvelope queryBBOX =
                    renderingBBOX.transform(fs.getSchema().getCoordinateReferenceSystem(), true);
            Query query = layer.getQuery();
            BBOX bbox = FF.bbox(FF.property(""), queryBBOX);
            if (query.getFilter() == null || query.getFilter() == Filter.INCLUDE) {
                query.setFilter(bbox);
            } else {
                Filter and = FF.and(query.getFilter(), bbox);
                query.setFilter(and);
            }
            String[] dynamicProperties = getDynamicProperties(dynamicRules);
            query.setPropertyNames(dynamicProperties);
            final PyDynamicBufferEstimator dbe = new PyDynamicBufferEstimator();
            fs.getFeatures(query)
                    .accepts(
                            new FeatureVisitor() {

                                @Override
                                public void visit(Feature feature) {
                                    dbe.setFeature(feature);
                                    for (Rule rule : dynamicRules) {
                                        rule.accept(dbe);
                                    }
                                }
                            },
                            null);
            int dynamicBuffer = dbe.getBuffer();
            return Math.max(dynamicBuffer / 2, estimatedRadius);
        }
    }

    private String[] getDynamicProperties(List<Rule> dynamicRules) {
        StyleAttributeExtractor extractor = new StyleAttributeExtractor();
        for (Rule rule : dynamicRules) {
            rule.accept(extractor);
        }
        return extractor.getAttributeNames();
    }
    @Override
    public int getPriority() {
        return (ExtensionPriority.LOWEST + ExtensionPriority.HIGHEST) / 2;
    }

}
