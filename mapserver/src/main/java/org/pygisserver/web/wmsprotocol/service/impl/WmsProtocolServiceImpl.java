package org.pygisserver.web.wmsprotocol.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.opengis.wfs.FeatureCollectionType;
import org.apache.commons.lang.StringUtils;
import org.pygisserver.common.entity.PyRenderRequestParam;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.entity.PostGisConnectEntity;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.pytc.entity.TileRequestInfo;
import org.pygisserver.common.redis.util.RedisUtils;
import org.pygisserver.common.service.MapServerDataService;
import org.pygisserver.common.ui.entity.LayerGroup;
import org.pygisserver.common.util.CommonUtil;
import org.pygisserver.common.util.GeometryUtil;
import org.pygisserver.web.dao.MapServerMapper;
import org.pygisserver.web.wmsprotocol.entity.PyGetFeatureInfoRequest;
import org.pygisserver.render.entity.PyRenderedParam;
import org.pygisserver.render.service.RenderService;
import org.pygisserver.render.tiled.TiledInfoStrategy;
import org.pygisserver.render.util.LayerBaseInfoUtil;
import org.pygisserver.web.service.StyleInfoService;
import org.pygisserver.web.service.impl.MapServerServiceImpl;
import org.pygisserver.web.wmsprotocol.entity.WmsRequestParam;
import org.pygisserver.web.wmsprotocol.format.PyGetFeatureInfoOutputFormat;
import org.pygisserver.web.wmsprotocol.format.HtmlFeatureInfoOutputFormat;
import org.pygisserver.web.wmsprotocol.service.WmsProtocolService;
import org.geoserver.ows.OutputStrategyFactory;
import org.geoserver.ows.ServiceStrategy;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.Version;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.FactoryException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;

/**
 * @Description: wms协议service实现类
 * @author: 张亨特
 * @date: 2020/07/19
 */
@Service
@Slf4j
public class WmsProtocolServiceImpl implements WmsProtocolService {

    @Autowired
    private WmsGetCapabities wmsGetCapabities;

    @Autowired
    private WmsGetFeatureInfo wmsGetFeatureInfo;

    @Resource(name="renderService")
    private RenderService renderService;

    @Autowired
    private MapServerServiceImpl mapServerService;

    @Autowired
    private MapServerMapper mapServerMapper;
    
    @Autowired
    private StyleInfoService styleInfoService;

    @Autowired
    private ObjectFactory<TiledInfoStrategy> factory;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private MapServerDataService mapServerDataService;

    @Autowired
    private LayerBaseInfoUtil layerBaseInfoUtil;

    private static final Map<String,PostGisConnectEntity> postgisConnectionCache = new HashMap<String,PostGisConnectEntity>();

    @Override
    public byte[] getWmsCapabilities(String baseUrl, String version) {
        return wmsGetCapabities.generateGetCapabilities(baseUrl, version).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] getWmsImageByte(WmsRequestParam requestParam, String reqIP) {
        LayerBaseInfo layerBaseInfo = mapServerService.getLayerBaseInfo(requestParam.getLayerName(), reqIP);
        if (layerBaseInfo != null && CommonUtil.isTiledServer(layerBaseInfo.getFileType())) {
            if (layerBaseInfo.getIsLayerGroup() == 1){
                List<LayerGroup> listServer = layerBaseInfoUtil.getLayerGroups(requestParam.getLayerName());
                layerBaseInfo = mapServerDataService.queryLayerBaseInfo(listServer.get(0).getLayerName());
            }
            return getTileServerResponse(requestParam, layerBaseInfo);
        }

        // 编组预览时需从redis中获取基础图层信息
        if (layerBaseInfo == null) {
            List<LayerGroup> listServer = (List<LayerGroup>) redisUtils.getValue(requestParam.getLayerName());
            layerBaseInfo = new LayerBaseInfo();
            if (listServer != null && !listServer.isEmpty()) {
                layerBaseInfo.setIsLayerGroup(1);
                layerBaseInfo.setDataType(4);
                StringBuilder filePath = new StringBuilder();
                StringBuilder styleName = new StringBuilder();
                StringBuilder fileType = new StringBuilder();

                for (int index = 0; index < listServer.size(); index++) {
                    LayerGroup layerGroup = listServer.get(index);
                    LayerBaseInfo baseInfo = mapServerService.getLayerBaseInfo(layerGroup.getLayerName(), reqIP);
                    filePath.append(baseInfo.getFilePath());
                    styleName.append(StringUtils.isBlank(layerGroup.getStyleName()) ? "栅格" : layerGroup.getStyleName());
                    fileType.append(baseInfo.getFileType());
                    if (index < listServer.size() - 1) {
                        filePath.append(";");
                        styleName.append(";");
                        fileType.append(";");
                    }
                }
                layerBaseInfo.setFilePath(filePath.toString());
                layerBaseInfo.setFileType(fileType.toString());
                layerBaseInfo.setStyleName(styleName.toString());
            }
        }
        PyRenderRequestParam renderRequestParam = constructRenderRequestParam(requestParam, layerBaseInfo);

        //使用rend进行渲染
        return renderService.renderImageByRequestParam(renderRequestParam);
    }

    @Override
    public void getWmsFeatureInfo(WmsRequestParam requestParam){
        PyGetFeatureInfoRequest request = constructGetFeatureRequest(requestParam);
        FeatureCollectionType result = wmsGetFeatureInfo.run(request);
        if (result != null) {
            try {
                OutputStream output = requestParam.getHttpResponse().getOutputStream();
                List<String> operations = new ArrayList<String>();
                operations.add("GetFeatureInfo");
                Operation operation = new Operation("GetFeatureInfo",
                        new org.geoserver.platform.Service("wms",new Object(),new Version(request.getVersion()),operations),
                        null,
                        new Object[]{request});
                Assert.notNull(result, "value is null");
                Assert.notNull(operation, "operation is null");
                Assert.isTrue(true, "unrecognized result type:");
                Assert.isTrue(
                        operation.getParameters() != null
                                && operation.getParameters().length == 1
                                && operation.getParameters()[0] instanceof PyGetFeatureInfoRequest,
                        "Operation parameters should be a single GetFeatureInfoRequest");
                //不需要注册bean里找，目前只支持一种格式
//                FeGetFeatureInfoOutputFormat outputFormat = (FeGetFeatureInfoOutputFormat) GeoServerExtensions.extensions(FeGetFeatureInfoOutputFormat.class, SpringContextUtil.getApplicationContext());
                PyGetFeatureInfoOutputFormat outputFormat = new HtmlFeatureInfoOutputFormat(request.getInfoFormat());
                outputFormat.write(result, request, output);
            }catch (Exception e){
                log.error("WMS Service getWmsFeatureInfo has error : "+e.getLocalizedMessage());
            }
        }
    }

    ServiceStrategy findOutputStrategy(HttpServletResponse response) {
        OutputStrategyFactory factory = null;
        try {
            factory = (OutputStrategyFactory) GeoServerExtensions.bean("serviceStrategyFactory");
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
        return factory.createOutputStrategy(response);
    }

    private PyGetFeatureInfoRequest constructGetFeatureRequest(WmsRequestParam wmsRequestParam){
        //构造请求对象，主要是对象类型转换
        PyGetFeatureInfoRequest request = new PyGetFeatureInfoRequest();
        request.setQueryLayers(wmsRequestParam.getQueryLayers());
        request.setXPixel(wmsRequestParam.getXPixel());
        request.setYPixel(wmsRequestParam.getYPixel());
        request.setInfoFormat("text/html");
        request.setBuffer(wmsRequestParam.getBuffer());
//        request.setBaseUrl(wmsRequestParam);
//        layerName是否需要,aop是否处理
        request.setExceptionFormat(wmsRequestParam.getExceptions());
        request.setPropertyNames(wmsRequestParam.getPropertyName());
        request.setVersion(wmsRequestParam.getVersion());
        request.setRequest(wmsRequestParam.getRequest());
        request.setStyles(wmsRequestParam.getStyles());
        request.setFilter(wmsRequestParam.getFilter());
        request.setCqlFilter(wmsRequestParam.getCqlFilter());
        //构造渲染参数，类似于getMap请求
        PyRenderedParam renderedParam = new PyRenderedParam();
        try {
            renderedParam.setCrs(CRS.decode(wmsRequestParam.getSrs()));
        } catch (FactoryException e) {
            log.error("WmsProtocolService has error : error CoordinateReferenceSystem");
            throw new BaseException("unknow CoordinateReferenceSystem : " + wmsRequestParam.getSrs());
        }
        renderedParam.setBgcolor(Color.WHITE);
        renderedParam.setTransparent(true);
        ReferencedEnvelope envelope = null;
        try {
            String[] bbox = wmsRequestParam.getBbox().split(",");
            envelope = new ReferencedEnvelope(Double.parseDouble(bbox[0])
                    ,Double.parseDouble(bbox[2])
                    ,Double.parseDouble(bbox[1])
                    ,Double.parseDouble(bbox[3])
                    ,CRS.decode(wmsRequestParam.getSrs()));
        } catch (FactoryException e) {
            log.error("WmsProtocolService construtor envelope has error : " + e.getLocalizedMessage());
            throw new BaseException("can't consturtor  bbox : " + wmsRequestParam.getBbox());
        }
        renderedParam.setEnvelope(envelope);
        renderedParam.setCrs(envelope.getCoordinateReferenceSystem());
        renderedParam.setRenderHeight(wmsRequestParam.getWidth());
        renderedParam.setRenderWidth(wmsRequestParam.getHeight());
        request.setRenderedParam(renderedParam);
        HashMap<String, String> kvpMap = new HashMap<>();
        kvpMap.put("FORMAT",wmsRequestParam.getFormat());
        kvpMap.put("STYLE",wmsRequestParam.getStyle());
        kvpMap.put("STYLES",wmsRequestParam.getStyles());
        kvpMap.put("WIDTH",String.valueOf(wmsRequestParam.getWidth()));
        kvpMap.put("HEIGHT",String.valueOf(wmsRequestParam.getHeight()));
        kvpMap.put("LAYERS",wmsRequestParam.getLayerName());
        kvpMap.put("T",wmsRequestParam.getTime());
        kvpMap.put("REQUEST",wmsRequestParam.getRequest());
        kvpMap.put("SRS",wmsRequestParam.getSrs());
        kvpMap.put("BBOX",wmsRequestParam.getBbox());
        kvpMap.put("VERSION",wmsRequestParam.getVersion());
        kvpMap.put("X",String.valueOf(wmsRequestParam.getXPixel()));
        kvpMap.put("Y",String.valueOf(wmsRequestParam.getYPixel()));
        kvpMap.put("SERVICE",wmsRequestParam.getService());
        kvpMap.put("QUERY_LAYERS", Arrays.toString(wmsRequestParam.getQueryLayers().toArray()));
        kvpMap.put("TRANSPARENT","true");
        request.setRawKvp(kvpMap);
        return request;
    }


    private byte[] getTileServerResponse(WmsRequestParam requestInfo, LayerBaseInfo baseInfo) {
        // 如果请求的大于当前图片的最大级别，则返回
        String bbox = requestInfo.getBbox();
        String[] bboxArray = bbox.split(",");
        double targetNum = (Double.parseDouble(bboxArray[2]) - Double.parseDouble(bboxArray[0])) / requestInfo.getWidth();
        int currentLevel;
        if ("EPSG:4326".equalsIgnoreCase(requestInfo.getSrs())) {
            currentLevel = GeometryUtil.get4326CurrentLevel(targetNum, baseInfo.getTileWidth());
        } else {
            currentLevel = GeometryUtil.get3857CurrentLevel(targetNum, baseInfo.getTileWidth());
        }
        if (currentLevel > baseInfo.getSupportMaxLevel()) {
            return null;
        }
        TiledInfoStrategy tiledInfoStrategy = factory.getObject();
        TileRequestInfo tileRequestInfo = new TileRequestInfo();
        tileRequestInfo.setGridSet(requestInfo.getSrs());
        double minx = Double.parseDouble(bboxArray[0]);
        double miny = Double.parseDouble(bboxArray[1]);
        double maxx = Double.parseDouble(bboxArray[2]);
        double maxy = Double.parseDouble(bboxArray[3]);
        Envelope envelope = new Envelope(minx, maxx, miny, maxy);
        String format = requestInfo.getFormat();
        if (format.indexOf("/") > -1) {
            tileRequestInfo.setMineType(format.split("/")[1]);
        } else {
            tileRequestInfo.setMineType(format);
        }
        tileRequestInfo.setImageWidth(requestInfo.getWidth());
        tileRequestInfo.setImageHeight(requestInfo.getHeight());
        tileRequestInfo.setTransparent(requestInfo.isTransparent());
        tileRequestInfo.setLayerName(requestInfo.getLayerName());
        ReferencedEnvelope reqEnvelope = null;
        try {
            reqEnvelope = new ReferencedEnvelope(envelope, CRS.decode(tileRequestInfo.getGridSet(), true));
        } catch (FactoryException e) {
            log.error("WmsProtocolController convertBoundingBox has error:" + ExceptionsUtil.stackTraceToStr(e));
            throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_TRANSFOR, "坐标系Bbox信息"));
        }
        tileRequestInfo.setBbox(reqEnvelope);
        tiledInfoStrategy.setTiledInfo(tileRequestInfo, baseInfo);
        byte[] buffer = tiledInfoStrategy.getWmsResource();
//        try (OutputStream os = rep.getOutputStream()){
//            if (!mineType.startsWith("image")) {
//                mineType = "image/" + mineType;
//            }
//            rep.setContentType(mineType);
//            os.write(buffer);
//        } catch (IOException e) {
//            log.error("Respense Write dealReponseResult has error:" + ExceptionsUtil.stackTraceToStr(e));
//        }
        return buffer;
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
            throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, requestParam.getLayerName() + "服务坐标系信息"));
        }
        // 直接使用样式名称，保证在redis中已经处理
        renderParam.setStyleName(styleName);
        StringBuilder sb = new StringBuilder().append(envelope.getMinimum(0)).append(",").append(envelope.getMaximum(0))
                .append(",").append(envelope.getMinimum(1)).append(",").append(envelope.getMaximum(1));
        renderParam.setRequestBbox(sb.toString());
        renderParam.setFormat(requestParam.getFormat());
        renderParam.setLayerGroup(layerBaseInfo.getIsLayerGroup() != 0);
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
        renderParam.setService("WMS");
        renderParam.setCqlFilter(requestParam.getCqlFilter());
        renderParam.setOutPutTransparentColor(layerBaseInfo.getOutPutTransparentColor());
        renderParam.setInputTransparentColor(layerBaseInfo.getInputTransparentColor());
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


	@Override
	public byte[] getWmsLegendGraphic(WmsRequestParam requestParam, String reqIP) {
		PyRenderRequestParam feRenderRequestParam = new PyRenderRequestParam();
		feRenderRequestParam.setFormat(requestParam.getFormat());
		feRenderRequestParam.setStyleName(requestParam.getStyle());
		return styleInfoService.getStyleImageByStyleName(requestParam.getStyle());
	}
}
