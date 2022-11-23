package org.pygisserver.web.wmsprotocol.identifier.impl;

import it.geosolutions.jaiext.range.Range;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.xml.utils.XMLChar;
import org.pygisserver.common.entity.PyRenderRequestParam;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.entity.PostGisConnectEntity;
import org.pygisserver.common.enumm.DataTypeEnum;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.raster.PyRasterSymbolizerVisitor;
import org.pygisserver.web.dao.MapServerMapper;
import org.pygisserver.web.wmsprotocol.entity.*;
import org.pygisserver.render.util.PyParseStyle;
import org.pygisserver.render.util.PyRenderedUtil;
import org.pygisserver.web.wmsprotocol.identifier.PyLayerIdentifier;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.util.FeatureUtilities;
import org.geotools.data.DataUtilities;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.TransformedDirectPosition;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.ImageWorker;
import org.geotools.image.util.ImageUtilities;
import org.geotools.referencing.CRS;
import org.geotools.renderer.lite.RenderingTransformationHelper;
import org.geotools.styling.Style;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.coverage.PointOutsideCoverageException;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.TransformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.media.jai.PlanarImage;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.*;

@Component
@Slf4j
public class PyRasterLayerIdentifier implements PyLayerIdentifier<GridCoverage2DReader> {

    private static final Map<String, PostGisConnectEntity> postgisConnectionCache = new HashMap<String, PostGisConnectEntity>();

    @Autowired
    private MapServerMapper mapServerMapper;

    @Autowired
    private PyParseStyle parseStyle;

    @Override
    public boolean canHandle(LayerBaseInfo layer) {
        int type = layer.getDataType();
        return type == DataTypeEnum.RASTER.getFileCode();
    }

    @Override
    public List<FeatureCollection> identify(PyFeatureInfoRequestParameters params, LayerBaseInfo layer , int maxFeatures) throws Exception {
        final String layerName = params.getLayer();
        final Filter filter = params.getFilter();
        WmsRequestParam wmsRequestParam = new WmsRequestParam();
        wmsRequestParam.setService("GetFeatureInfo");
        ReferencedEnvelope envelope = params.getRequestedBounds();
        String bbox = new StringBuffer(String.valueOf(envelope.getMinX())).append(",")
                .append(envelope.getMinY()).append(",")
                .append(envelope.getMaxX()).append(",")
                .append(envelope.getMaxY()).toString();
        wmsRequestParam.setBbox(bbox);
        wmsRequestParam.setFeatureCount(0);
        wmsRequestParam.setFormat(params.getFormat());
        wmsRequestParam.setHeight(params.getHeight());
        wmsRequestParam.setWidth(params.getWidth());
        wmsRequestParam.setStyle(layer.getStyleName());
        wmsRequestParam.setLayerName(layerName);
        wmsRequestParam.setSrs(params.getRequestedCRS().getIdentifiers().toArray()[0].toString());
        wmsRequestParam.setXPixel(params.getX());
        wmsRequestParam.setYPixel(params.getY());
        final GridCoverage2DReader reader =
                handleClipParam(
                        params,
                        PyRenderedUtil.getGridCoverage2DReader(constructRenderRequestParam(wmsRequestParam,layer)));
        DirectPosition position = getQueryPosition(params, layer, reader);
        if (!reader.getOriginalEnvelope().contains(position)) {
            return null;
        }
        Map<Object, Object> map = new HashMap<>();
        map.put("Filter",filter);
        GeneralParameterValue[] parameters = PyRenderedUtil.getParameters(reader.getFormat().getReadParameters(), map, false);
        if (parameters == null) return null;

        GridCoverage2D coverage = readCoverage(params, reader, parameters, layer);
        if (coverage == null) {
            return null;
        }
        FeatureCollection pixel = null;
        try {
            final double[] pixelValues = coverage.evaluate(position, (double[]) null);
            if (params.isExcludeNodataResults() && pixelsAreNodata(coverage, pixelValues)) {
                for (int i = 0; i < pixelValues.length; i++) {
                    pixelValues[i] = Double.NaN;
                }
            }
            PyColorMapLabelMatcherExtractor labelVisitor =
                    new PyColorMapLabelMatcherExtractor(params.getScaleDenominator());
            parseStyle.parserStyleByStyleName(StringUtils.isBlank(layer.getStyleName()) ? "栅格" : layer.getStyleName()).accept(labelVisitor);
            List<PyColorMapLabelMatcher> colorMapLabelMatcherList =
                    labelVisitor.getColorMapLabelMatcherList();

            pixel =
                    wrapPixelInFeatureCollection(
                            coverage,
                            pixelValues,
                            new NameImpl(layerName),
                            colorMapLabelMatcherList);
        } catch (PointOutsideCoverageException e) {
            // do nothing,此处异常不处理bbox范围外的要素
            log.warn("FeRasterLayerIdentifier identify point out side coverage ");
        } finally {
            RenderedImage ri = coverage.getRenderedImage();
            coverage.dispose(true);
            if (ri instanceof PlanarImage) {
                ImageUtilities.disposePlanarImageChain((PlanarImage) ri);
            }
        }
        return Collections.singletonList(pixel);
    }

    static boolean isLabelReplacingValue(List<PyColorMapLabelMatcher> colorMapLabelMatchers) {
        return !colorMapLabelMatchers.isEmpty()
                && colorMapLabelMatchers
                .stream()
                .allMatch(
                        l ->
                                l.getLabelInclusion()
                                        .equals(PyColorMapLabelMatcher.LabelInFeatureInfoMode.REPLACE.name()));
    }

    static String descriptionToNcName(String description) {
        if (description == null || description.length() == 0) {
            return "Unknown";
        } else {
            char[] result = description.toCharArray();
            for (int i = 0; i < result.length; i++) {
                if ((i == 0 && !XMLChar.isNCNameStart(result[i]))
                        || (i > 0 && !XMLChar.isNCName(result[i]))) {
                    result[i] = '_';
                }
            }
            return new String(result);
        }
    }

    private void addBandNamesToFeatureType(
            GridSampleDimension[] sampleDimensions, SimpleFeatureTypeBuilder builder) {
        final Set<String> bandNames = new HashSet<>();
        for (int i = 0; i < sampleDimensions.length; i++) {
            String name = descriptionToNcName(sampleDimensions[i].getDescription().toString());
            if (bandNames.contains(name)) {
                name = new StringBuilder(name).append("_Band").append(i).toString();
            }
            bandNames.add(name);
            builder.add(name, Double.class);
        }
    }

    static int getLabelAttributeNameCount(List<PyColorMapLabelMatcher> colorMapLabelMatchers) {
        return Long.valueOf(
                colorMapLabelMatchers
                        .stream()
                        .filter(l -> l.getAttributeName() == "Label")
                        .count())
                .intValue();
    }

    private void addLabelAttributeNameToFeatureType(
            List<PyColorMapLabelMatcher> colorMapLabelMatcherList,
            SimpleFeatureTypeBuilder builder,
            GridCoverage2D coverage) {
        int numLabel = getLabelAttributeNameCount(colorMapLabelMatcherList);
        int indexLabel = 1;
        for (PyColorMapLabelMatcher lifi : colorMapLabelMatcherList) {
            String label = lifi.getAttributeName();
            Integer channelName = lifi.getChannel();
            if (label.equals("Label")) {
                String labelIndexed = numLabel > 1 ? label + indexLabel : label;
                indexLabel++;
                GridSampleDimension sampleDimension =
                        channelName != null
                                ? coverage.getSampleDimension(channelName.intValue() - 1)
                                : coverage.getSampleDimensions()[0];
                String sampleDimDesc = sampleDimension.getDescription().toString();
                label = labelIndexed + "_" + descriptionToNcName(sampleDimDesc);
            }
            builder.add(label, String.class);
        }
    }


    private SimpleFeatureCollection wrapPixelInFeatureCollection(
            GridCoverage2D coverage,
            double[] pixelValues,
            Name coverageName,
            List<PyColorMapLabelMatcher> colorMapLabelMatcherList) {
        GridSampleDimension[] sampleDimensions = coverage.getSampleDimensions();
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(coverageName);

        boolean isLabelActive = !colorMapLabelMatcherList.isEmpty();
        boolean isLabelReplacingValue = isLabelReplacingValue(colorMapLabelMatcherList);

        if (!isLabelReplacingValue) addBandNamesToFeatureType(sampleDimensions, builder);

        if (isLabelActive)
            addLabelAttributeNameToFeatureType(colorMapLabelMatcherList, builder, coverage);

        SimpleFeatureType gridType = builder.buildFeatureType();

        int valuesLength;
        int labelSize = colorMapLabelMatcherList.size();
        if (isLabelReplacingValue) valuesLength = labelSize;
        else if (isLabelActive) valuesLength = pixelValues.length + labelSize;
        else valuesLength = pixelValues.length;

        Object[] values = new Object[valuesLength];

        List<String> labels = new ArrayList<>();
        int lastOccupiedPosition = 0;
        for (int i = 0; i < pixelValues.length; i++) {
            double pixelVal = Double.valueOf(pixelValues[i]);
            if (isLabelActive)
                addValueToLabelListByPixel(labels, colorMapLabelMatcherList, pixelVal, i);
            if (!isLabelReplacingValue) {
                values[i] = pixelVal;
                lastOccupiedPosition++;
            }
        }
        if (isLabelActive) {
            for (String label : labels) {
                values[lastOccupiedPosition] = label;
                lastOccupiedPosition++;
            }
        }
        return DataUtilities.collection(SimpleFeatureBuilder.build(gridType, values, ""));
    }

    private void addValueToLabelListByPixel(
            List<String> labels,
            List<PyColorMapLabelMatcher> colorMapLabelMatcherList,
            double pixel,
            int currentPixelIdx) {
        for (PyColorMapLabelMatcher lifi : colorMapLabelMatcherList) {
            Integer channelName = lifi.getChannel();
            if (channelName == null || currentPixelIdx == channelName.intValue() - 1)
                labels.add(lifi.getLabelForPixel(pixel));
        }
    }

    private boolean pixelsAreNodata(GridCoverage2D coverage, final double values[]) {
        RenderedImage ri = coverage.getRenderedImage();
        ImageWorker worker = new ImageWorker(ri);
        Range nodata = worker.getNoData();
        int nodataValues = 0;
        if (nodata != null) {
            for (double value : values) {
                if (nodata.contains(value)) {
                    nodataValues++;
                }
            }
        }
        return nodataValues == values.length;
    }

    private GridCoverage2D readCoverage(
            PyFeatureInfoRequestParameters params,
            GridCoverage2DReader reader,
            GeneralParameterValue[] parameters,
            LayerBaseInfo layer)
            throws IOException, SchemaException, TransformException, FactoryException {
        GridCoverage2D coverage = reader.read(parameters);
        Style style = parseStyle.parserStyleByStyleName(StringUtils.isBlank(layer.getStyleName()) ? "栅格" : layer.getStyleName());
        PyRasterSymbolizerVisitor visitor =
                new PyRasterSymbolizerVisitor(params.getScaleDenominator(), null);
        style.accept(visitor);
        Expression transformation = visitor.getRasterRenderingTransformation();
        if (transformation != null) {
            RenderingTransformationHelper helper =
                    new RenderingTransformationHelper() {
                        @Override
                        protected GridCoverage2D readCoverage(
                                GridCoverage2DReader reader, Object params, GridGeometry2D readGG)
                                throws IOException {
                            throw new UnsupportedOperationException(
                                    "This helper is meant to be used with a coverage already read");
                        }
                    };
            Object result =
                    helper.applyRenderingTransformation(
                            transformation,
                            DataUtilities.source(FeatureUtilities.wrapGridCoverage(coverage)),
                            Query.ALL,
                            Query.ALL,
                            null,
                            coverage.getCoordinateReferenceSystem2D(),
                            null);
            if (result instanceof GridCoverage2D) {
                coverage = (GridCoverage2D) result;
            } else {
                coverage = null;
            }
        }
        return coverage;
    }

    @Override
    public GridCoverage2DReader handleClipParam(PyFeatureInfoRequestParameters params, GridCoverage2DReader reader) {
        Geometry roiGeom = params.getClip();
        if (roiGeom == null) return reader;
        return new PyCroppedGridCoverage2DReader(reader, roiGeom);
    }

    private DirectPosition getQueryPosition(
            PyFeatureInfoRequestParameters params, LayerBaseInfo layerBaseInfo, GridCoverage2DReader reader) {
        CoordinateReferenceSystem requestedCRS = params.getRequestedCRS();

        CoordinateReferenceSystem targetCRS = null;
        try {
            if (!StringUtils.equals(layerBaseInfo.getResourceSrs(),CRS.parseWKT(requestedCRS.toWKT()).getIdentifiers().toArray()[0].toString())){
                //重投影处理
                targetCRS = CRS.decode(layerBaseInfo.getNativeSrs());
            }else {
                targetCRS = CRS.decode(layerBaseInfo.getResourceSrs());
            }
        } catch (FactoryException e) {
            e.printStackTrace();
        }

        final Coordinate middle = pixelToWorld(
                        params.getX(),
                        params.getY(),
                        params.getRequestedBounds(),
                        params.getWidth(),
                        params.getHeight());
        double x = middle.x;
        double y = middle.y;
        DirectPosition targetCoverageMedianPosition = reader.getOriginalEnvelope().getMedian();
        if (requestedCRS != null && requestedCRS instanceof GeographicCRS) {
            TransformedDirectPosition coverageMedianPosition =
                    new TransformedDirectPosition(
                            targetCRS,
                            requestedCRS,
                            new Hints(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE));
            try {
                coverageMedianPosition.transform(targetCoverageMedianPosition);
            } catch (TransformException exception) {
                throw new CannotEvaluateException(
                        "Cannot find coverage median position in requested CRS", exception);
            }
            if (CRS.getAxisOrder(requestedCRS) == CRS.AxisOrder.NORTH_EAST) {
                y += 360 * Math.round((coverageMedianPosition.getOrdinate(1) - y) / 360);
            } else {
                x += 360 * Math.round((coverageMedianPosition.getOrdinate(0) - x) / 360);
            }
        }
        DirectPosition position = new DirectPosition2D(requestedCRS, x, y);
        if (requestedCRS != null) {
            final TransformedDirectPosition arbitraryToInternal =
                    new TransformedDirectPosition(
                            requestedCRS,
                            targetCRS,
                            new Hints(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE));
            try {
                arbitraryToInternal.transform(position);
            } catch (TransformException exception) {
                throw new CannotEvaluateException(
                        "Unable to answer the geatfeatureinfo", exception);
            }
            position = arbitraryToInternal;
        }
        if (targetCRS != null && targetCRS instanceof GeographicCRS) {
            x = position.getOrdinate(0);
            y = position.getOrdinate(1);
            if (CRS.getAxisOrder(targetCRS) == CRS.AxisOrder.NORTH_EAST) {
                y += 360 * Math.round((targetCoverageMedianPosition.getOrdinate(1) - y) / 360);
            } else {
                x += 360 * Math.round((targetCoverageMedianPosition.getOrdinate(0) - x) / 360);
            }
            position = new DirectPosition2D(targetCRS, x, y);
        }
        return position;
    }


    public static Coordinate pixelToWorld(
            double x, double y, ReferencedEnvelope map, double width, double height) {
        //获取像素点的世界坐标
        AffineTransform at = worldToScreenTransform(map, width, height);
        Point2D result = null;

        try {
            result =
                    at.inverseTransform(
                            new Point2D.Double(x, y),
                            new Point2D.Double());
        } catch (NoninvertibleTransformException e) {
            throw new RuntimeException(e);
        }

        Coordinate c = new Coordinate(result.getX(), result.getY());

        return c;
    }

    public static AffineTransform worldToScreenTransform(
            ReferencedEnvelope mapExtent, double width, double height) {
        //世界坐标转屏幕坐标
        CoordinateReferenceSystem crs = mapExtent.getCoordinateReferenceSystem();
        boolean swap = crs != null && CRS.getAxisOrder(crs) == CRS.AxisOrder.NORTH_EAST;
        if (swap) {
            mapExtent =
                    new ReferencedEnvelope(
                            mapExtent.getMinY(),
                            mapExtent.getMaxY(),
                            mapExtent.getMinX(),
                            mapExtent.getMaxX(),
                            null);
        }

        double scaleX = width / mapExtent.getWidth();
        double scaleY = height / mapExtent.getHeight();

        double tx = -mapExtent.getMinX() * scaleX;
        double ty = (mapExtent.getMinY() * scaleY) + height;

        AffineTransform at = new AffineTransform(scaleX, 0.0d, 0.0d, -scaleY, tx, ty);

        if (swap) {
            at.concatenate(new AffineTransform(0, 1, 1, 0, 0, 0));
        }

        return at;
    }

    private PyRenderRequestParam constructRenderRequestParam(WmsRequestParam requestParam, LayerBaseInfo layerBaseInfo) {
        String srs = requestParam.getSrs();
        PyRenderRequestParam renderParam = new PyRenderRequestParam();
        renderParam.setFileUrl(layerBaseInfo.getFilePath());
        String styleName = StringUtils.isBlank(layerBaseInfo.getStyleName()) ? "栅格" : layerBaseInfo.getStyleName();
        String[] requestBbox = requestParam.getBbox().split(",");
        ReferencedEnvelope envelope = null;
        try {
            envelope = new ReferencedEnvelope(Double.parseDouble(requestBbox[0]),
                    Double.parseDouble(requestBbox[2]), Double.parseDouble(requestBbox[1]),
                    Double.parseDouble(requestBbox[3]), CRS.decode(srs, true));
        } catch (FactoryException e) {
            log.error("WmsProtocolServiceImpl getMap has error:" + ExceptionsUtil.stackTraceToStr(e));
            throw new ResourceException(ExceptionsUtil.ERROR_500, ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, requestParam.getLayerName() + "服务坐标系信息"));
        }
        // 直接使用样式名称，保证在redis中已经处理
        renderParam.setStyleName(styleName);
        StringBuilder sb = new StringBuilder().append(envelope.getMinimum(0)).append(",").append(envelope.getMaximum(0))
                .append(",").append(envelope.getMinimum(1)).append(",").append(envelope.getMaximum(1));
        renderParam.setRequestBbox(sb.toString());
        renderParam.setFormat(requestParam.getFormat());
        renderParam.setLayerGroup(layerBaseInfo.getIsLayerGroup() == 1);
        int tileHeight = requestParam.getHeight() == 0 ? 256 : requestParam.getHeight();
        int tileWidth = requestParam.getWidth() == 0 ? 256 : requestParam.getWidth();
        renderParam.setMapHeight(tileHeight);
        renderParam.setMapWidth(tileWidth);
        renderParam.setTransparent(true);
        renderParam.setDataFormat(layerBaseInfo.getFileType());
        renderParam.setDataType(layerBaseInfo.getDataType());
        renderParam.setRequestSrs(srs);
        renderParam.setLayerName(requestParam.getLayerName());
        renderParam.setInputTransparentColor(layerBaseInfo.getInputTransparentColor());
        renderParam.setOutPutTransparentColor(layerBaseInfo.getOutPutTransparentColor());

        //如果是数据库发布的服务,则需要传递数据库连接参数
        if (layerBaseInfo.getFileType().contains("PostGIS")) {
            if (postgisConnectionCache.containsKey(requestParam.getLayerName())) {
                renderParam.setPostgisConnection(postgisConnectionCache.get(requestParam.getLayerName()));
            }else {
                String fileUrl = layerBaseInfo.getFilePath();
                String[] urls = fileUrl.split(";");
                PostGisConnectEntity entity = null;
                for (String string : urls) {
                    if (!string.contains("/") && !string.contains("\\")) {
                        entity = mapServerMapper.queryPostGisBySourceName(string);
                        if (entity != null) {
                            break;
                        }
                    }
                }
                postgisConnectionCache.put(requestParam.getLayerName(), entity);
                renderParam.setPostgisConnection(entity);
            }
        }
        return renderParam;
    }
}
