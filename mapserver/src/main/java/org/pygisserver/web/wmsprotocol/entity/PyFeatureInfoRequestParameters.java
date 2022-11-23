package org.pygisserver.web.wmsprotocol.entity;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.render.entity.PyMapContent;
import org.pygisserver.render.entity.PyRenderedParam;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.factory.GeoTools;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.sort.SortBy;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Slf4j
public class PyFeatureInfoRequestParameters {

    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    int x;

    int y;

    int buffer;

    List<Map<String, String>> viewParams;

    PyRenderedParam renderReq;

    CoordinateReferenceSystem requestedCRS;

    int width;

    int height;

    ReferencedEnvelope bbox;

    double scaleDenominator;

    String format;

    List<Filter> filters;

    List<SortBy[]> sorts;

    List<String> layers;

    String styles;

    List<Object> elevations;

    FilterFactory2 ff;

    //裁剪参数
    Geometry clip;

    private List<List<String>> propertyNames;

    int currentLayer = 0;

    int maxFeatures;

    boolean excludeNodataResults;

    public PyFeatureInfoRequestParameters(PyGetFeatureInfoRequest request) {
        this.layers = request.getQueryLayers();
        this.format = request.getInfoFormat();
//        this.sorts = request.getGetMapRequest().getSortByArrays();
        this.styles = request.getStyles();
        this.x = request.getXPixel();
        this.y = request.getYPixel();
        this.buffer = request.getBuffer();
//        this.viewParams = request.getViewParams();
        this.renderReq = request.getRenderedParam();
        this.requestedCRS = request.getRenderedParam().getCrs(); // optional, may be null
        this.maxFeatures = request.getFeatureCount();
        this.excludeNodataResults = request.isExcludeNodataResults();
        this.width = request.getRenderedParam().getRenderWidth();
        this.height = request.getRenderedParam().getRenderHeight();
        this.bbox = request.getRenderedParam().getEnvelope();
        this.scaleDenominator = getScaleDenominator(request);
//        this.elevations = request.getGetMapRequest().getElevation();高度目前不用
        this.ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
        this.propertyNames = request.getPropertyNames();
        if (!StringUtils.isEmpty(request.getFilter())){
            this.filters = new ArrayList<>();
            String cqlFilter = request.getCqlFilter();
            List<String> cqlFilters = new ArrayList<>(cqlFilter.split(";").length);
            for (int i = 0; i < cqlFilters.size() ; i++) {
                try {
                    Filter filter = CQL.toFilter(cqlFilters.get(i));
                    filters.add(filter);
                } catch (CQLException e) {
                    log.error("FeFeatureInfoRequestParameters Contrustor has error : unknow filter");
                    throw new BaseException(e.getLocalizedMessage());
                }
            }
        } else if (!StringUtils.isEmpty(request.getCqlFilter())){
            this.filters = new ArrayList<>();
            String cqlFilter = request.getCqlFilter();
            List<String> cqlFilters = new ArrayList<>(cqlFilter.split(";").length);
            for (int i = 0; i < cqlFilters.size() ; i++) {
                try {
                    Filter filter = ECQL.toFilter(cqlFilters.get(i));
                    filters.add(filter);
                } catch (CQLException e) {
                    log.error("FeFeatureInfoRequestParameters Contrustor has error : unknow ecql filter");
                    throw new BaseException(e.getLocalizedMessage());
                }
            }
        }
    }

    private double getScaleDenominator(PyGetFeatureInfoRequest request) {
        final Envelope envelope = request.getRenderedParam().getEnvelope();
        final CoordinateReferenceSystem mapcrs = request.getRenderedParam().getCrs();
        PyMapContent mapContent = new PyMapContent();
        if (mapcrs != null) {
            mapContent.getViewport().setBounds(new ReferencedEnvelope(envelope, mapcrs));
        } else {
            mapContent
                    .getViewport()
                    .setBounds(new ReferencedEnvelope(envelope, DefaultGeographicCRS.WGS84));
        }
        mapContent.setMapWidth(request.getRenderedParam().getRenderWidth());
        mapContent.setMapHeight(request.getRenderedParam().getRenderHeight());
        mapContent.setAngle(request.getRenderedParam().getAngle());
        mapContent.setAngle(0.0);

        return mapContent.getScaleDenominator(true);
    }

//    private List<Style> getStyles(final FeGetFeatureInfoRequest request, List<String> layers) {
//        //获取每个图层的样式,此方法只是给样式排序,服务端查询layerbaseinfo时就有
//        List<LayerBaseInfo> mapLayers = request.getQueryLayers();
//        List<Style> styles = new ArrayList<>();
//        mapLayers.stream().forEach(layerBaseInfo -> {
//            styles.add();
//        });
//        List<String> getMapLayers = request.getGetMapRequest().getLayers();
//        for (MapLayerInfo layer : layers) {
//            final String targetLayer = layer.getName();
//            Style s = null;
//            for (int j = 0; j < getMapLayers.size(); j++) {
//                if (getMapLayers.get(j).getName().equals(targetLayer)) {
//                    if (getMapStyles != null && getMapStyles.size() > j) {
//                        s = getMapStyles.get(j);
//                    } else {
//                        s = getMapLayers.get(j).getDefaultStyle();
//                    }
//                    break;
//                }
//            }
//            if (s != null) {
//                styles.add(s);
//            } else {
//                throw new ServiceException("Failed to locate style for layer " + targetLayer);
//            }
//        }
//        return styles;
//    }

    public void nextLayer() {
        currentLayer++;
    }

    public String getLayer() {
        return layers.get(currentLayer);
    }

//    public Style getStyle() {
//        return styles.get(currentLayer);
//    }

    public Filter getFilter() {
        if (filters == null || filters.size() <= currentLayer) {
            return null;
        } else {
            return filters.get(currentLayer);
        }
    }

//    public SortBy[] getSort() {
//        //获取图层组排序,无意义 不需要
//        if (sorts == null || sorts.size() <= currentLayer) {
//            return null;
//        } else {
//            SortBy[] layerSort = sorts.get(currentLayer);
//            final MapLayerInfo layer = layers.get(currentLayer);
//            if (layer.getType() == MapLayerInfo.TYPE_VECTOR
//                    || layer.getType() == MapLayerInfo.TYPE_REMOTE_VECTOR) {
//                SortBy[] result = new SortBy[layerSort.length];
//                for (int i = 0; i < layerSort.length; i++) {
//                    SortBy sb = layerSort[i];
//                    SortOrder order = sb.getSortOrder();
//                    SortBy reverse =
//                            FF.sort(
//                                    sb.getPropertyName().getPropertyName(),
//                                    order == SortOrder.ASCENDING || order == null
//                                            ? SortOrder.DESCENDING
//                                            : SortOrder.ASCENDING);
//                    result[i] = reverse;
//                }
//                return result;
//            } else {
//                // for rasters no need to invert
//                return layerSort;
//            }
//        }
//    }

    public String[] getPropertyNames() {
        if (propertyNames == null
                || propertyNames.isEmpty()
                || propertyNames.get(currentLayer) == null) {
            return Query.ALL_NAMES;
        } else {
            List<String> layerPropNames = propertyNames.get(currentLayer);
            return layerPropNames.toArray(new String[layerPropNames.size()]);
        }
    }

    public Map<String, String> getViewParams() {
        if (viewParams == null || viewParams.size() < currentLayer) {
            return null;
        } else {
            return viewParams.get(currentLayer);
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getBuffer() {
        return buffer;
    }

    public CoordinateReferenceSystem getRequestedCRS() {
        return requestedCRS;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public ReferencedEnvelope getRequestedBounds() {
        return bbox;
    }

    public double getScaleDenominator() {
        return scaleDenominator;
    }

    public List<Object> getElevations() {
        return elevations;
    }

    public FilterFactory2 getFilterFactory() {
        return ff;
    }

    public PyRenderedParam getRenderReq() {
        return renderReq;
    }

    public boolean isExcludeNodataResults() {
        return excludeNodataResults;
    }

}
