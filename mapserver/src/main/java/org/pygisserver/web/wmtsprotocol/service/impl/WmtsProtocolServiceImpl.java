package org.pygisserver.web.wmtsprotocol.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.pygisserver.common.entity.*;
import org.pygisserver.common.enumm.OgcEpsg;
import org.pygisserver.common.enumm.ResponseFormat;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.pytc.entity.TileRequestInfo;
import org.pygisserver.common.pytc.entity.TileResourceInfo;
import org.pygisserver.common.service.SystemLogService;
import org.pygisserver.common.ui.entity.LayerGroup;
import org.pygisserver.common.util.CommonUtil;
import org.pygisserver.common.util.GeometryUtil;
import org.pygisserver.common.util.ImageUtil;
import org.pygisserver.web.dao.MapServerMapper;
import org.pygisserver.render.config.TdbConfigFileGenerate;
import org.pygisserver.render.service.CustomDataLoad;
import org.pygisserver.render.service.RenderService;
import org.pygisserver.render.service.impl.CustomDataLoadImpl;
import org.pygisserver.render.tiled.GdalReadData;
import org.pygisserver.render.tiled.TiledInfoStrategy;
import org.pygisserver.render.util.TdbUtils;
import org.pygisserver.tileserver.service.TileCacheService;
import org.pygisserver.web.entity.GuassCoordinates;
import org.pygisserver.web.service.MapServerService;
import org.pygisserver.web.utils.PyLockProvider;
import org.pygisserver.web.utils.UserUtil;
import org.pygisserver.web.wmsprotocol.entity.WmsRequestParam;
import org.pygisserver.web.wmsprotocol.service.WmsProtocolService;
import org.pygisserver.web.wmtsprotocol.service.WmtsProtocolService;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

/**
 * @Title : WmtsProtocolServiceImpl
 * @Description：
 * @Author ：柳强
 * @Date ：Created in 2022/8/2 9:21
 * @Modified By：
 * @Version: $
 * Copyright: Copyright (c) 2019
 * Company: 恒歌科技
 */

@Service
@Slf4j
public class WmtsProtocolServiceImpl implements WmtsProtocolService {

    @DubboReference(version = "3.0.0")
    private TileCacheService tileCacheService;

    @Resource(name="renderService")
    private RenderService renderService;

    @Autowired
    private SystemLogService systemLogService;

    @Autowired
    private WmtsGetCapabities wmtsGetCapabities;

    @Autowired
    private ObjectFactory<TiledInfoStrategy> factory;

    @Autowired
    private PyLockProvider lockProvider;

    @Autowired
    private MapServerService mapServerService;

    @Autowired
    private MapServerMapper mapServerMapper;

    @Autowired
    private GuassCoordinates guassCoordinates;

    @Autowired
    private WmsProtocolService wmsProtocolService;

    @Autowired
    private ExceptionsUtil exceptionsUtil;

    private CustomDataLoad customDataLoad = new CustomDataLoadImpl();

    private static final Map<String,PostGisConnectEntity> postgisConnectionCache = new HashMap<>();

    @Override
    public byte[] getWmtsCapabilities(String urlPrefix) {
        // 移植tileserver中wmts的getcapabilities代码
        return wmtsGetCapabities.getCapabitiesByte(urlPrefix);
    }

    @Override
    public byte[] getWmtsTile(LayerBaseInfo baseInfo,
                              TileRequestInfo requestInfo,
                              String reqIP) {
        // 根据文件类型来处理，如果是瓦片，则按照瓦片的处理方式来执行
        byte[] resourceInfo = null;
        String srs = requestInfo.getGridSet();
        if ((CommonUtil.isTiledServer(baseInfo.getFileType()) && baseInfo.getIsLayerGroup() != 1)) {
            TiledInfoStrategy tiledInfo = factory.getObject();
            tiledInfo.setTiledInfo(requestInfo, baseInfo);
            resourceInfo = tiledInfo.getWmtsResource();
        } else {
            // 样式预览不走缓存(服务样式和请求样式不一样)
            if (baseInfo.getStyleName().equalsIgnoreCase(requestInfo.getStyle()) || StringUtils.isBlank(requestInfo.getStyle())) {
                //高斯暂时不走缓存
                if (!guassCoordinates.getGuassCoordinates().contains(srs)) {
                    // 查找本地缓存如果有
                    resourceInfo = findLocalCache(requestInfo);
                }
                if (resourceInfo == null) {
                    resourceInfo = getWMTSTileData(requestInfo, reqIP);
                }
            }
            if (resourceInfo == null){
                //切图缓存未找到，则直接渲染
                resourceInfo = renderTileImg(baseInfo,requestInfo,reqIP);
            }

        }
        return resourceInfo;
    }

    private byte[] findLocalCache(TileRequestInfo requestInfo) {
        String parentPath = System.getProperty("USER_VISIT_CACHE_DIR");
        requestInfo.setSystemCachePath(parentPath);
        String mineType = CommonUtil.simpleMineType(requestInfo.getMineType());
        String dataPath = TdbUtils.getVistitCacheDirName(requestInfo.getLayerName(), requestInfo.getGridSet(),
                mineType, requestInfo.getTileWidth() == 0 ? 256 : requestInfo.getTileWidth());
        File file = new File(dataPath);
        if (!file.exists()) {
            return null;
        }
        return customDataLoad.getImageInfo(requestInfo.getTileRow(), requestInfo.getTileCol(),
                requestInfo.getZoomLevel(), dataPath, requestInfo.getLayerName());
    }

    private byte[] getWMTSTileData(TileRequestInfo requestInfo, String reqIP) {
        byte[] resourceInfo = null;
        try {
                int tileWidth = requestInfo.getTileWidth() == 0 ? 256 : requestInfo.getTileWidth();
            int tileHeight = requestInfo.getTileWidth() == 0 ? 256 : requestInfo.getTileWidth();
            requestInfo.setTileWidth(tileWidth);
            requestInfo.setTileHeight(tileHeight);
            resourceInfo = tileCacheService.getWmtsTileInfo(requestInfo, reqIP);
        } catch (Exception e) {
            if (ExceptionsUtil.isPytcError(ExceptionsUtil.stackTraceToStr(e))) {
                systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR,
                        ExceptionsUtil.formatInfo(ExceptionsUtil.STATUS_PYTC_GET, requestInfo.getLayerName() + "WMTS瓦片数据"),
                        UserUtil.getUserName(), "Pytc服务", reqIP));
            }
            log.error("TileServiceController getWMTSTileData has error:" + ExceptionsUtil.stackTraceToStr(e));
        }
        return resourceInfo;
    }

    private byte[] renderTileImg(LayerBaseInfo layerBaseInfo,
                                 TileRequestInfo tileInfo,
                                 String reqIP) {
        byte[] buff;
        //高程处理两种情况  1、高程图层组  2、高程单图层
        if (layerBaseInfo.getIsLayerGroup() == 2) {
            return createLayerGroupDem(tileInfo, reqIP, layerBaseInfo);
        }else {
            if (layerBaseInfo.getStyleName() != null && layerBaseInfo.getStyleName().equals("高程")
                    && (tileInfo.getMineType().endsWith("tiff") || tileInfo.getMineType().endsWith("terrain"))) {
                buff = getDemBuf(tileInfo, layerBaseInfo.getFilePath(),layerBaseInfo.getResourceSrs());
            } else {
                //
                buff = renderTileImage(tileInfo, layerBaseInfo);

            }
        }
        return buff;
    }

    private byte[] createLayerGroupDem(TileRequestInfo tileInfo, String reqIP, LayerBaseInfo layerBaseInfo) {
        //获取对应的图层组中图层的信息
        List<LayerGroup> layerGroups = mapServerMapper.queryLayerGroupByServerName(layerBaseInfo.getLayerName());
        //判断图层组中每个图层的范围和请求范围进行对比，如果重合，则取精度较高的范围数据
        BoundingBox reqEnvelope = calcuteEnvelope(tileInfo);
        List<String> layerPaths = new ArrayList<>();
        List<String> layerNames = new ArrayList<>();
        //按照数据精度范围由高到低排序
        List<LayerBaseInfo> layerBaseInfos = new ArrayList<>();
        for (Iterator<LayerGroup> it = layerGroups.iterator(); it.hasNext();) {
            LayerGroup layerGroup = it.next();
            if (layerGroup.getMinLevel() > tileInfo.getZoomLevel() || layerGroup.getMaxlevel() < tileInfo.getZoomLevel()) {
                it.remove();
            }else {
                LayerBaseInfo baseInfo = mapServerService.getLayerBaseInfo(layerGroup.getLayerName(),reqIP);
                layerBaseInfos.add(baseInfo);
            }
        }
        //按照maxlevel值降序排列
        if (layerBaseInfos.isEmpty()) {
            return null;
        }
        layerBaseInfos.parallelStream().
                sorted(Comparator.comparing(LayerBaseInfo::getSupportMaxLevel).reversed()).collect(Collectors.toList());
        String reqSrs = tileInfo.getGridSet();
        for (LayerBaseInfo baseInfo : layerBaseInfos) {
            //如果请求的图层跟当前图层无交集，则直接跳过   请求的级别不在图层组设置的级别中，跳过
            String nativeBbox = baseInfo.getNativaBbox();
            String[] nativeBBoxArray = nativeBbox.split(";");
            double minX = Double.parseDouble(nativeBBoxArray[0]);
            double maxX = Double.parseDouble(nativeBBoxArray[2]);
            double minY = Double.parseDouble(nativeBBoxArray[1]);
            double maxY = Double.parseDouble(nativeBBoxArray[3]);
            try {
                if (!"EPSG:4326".equals(reqSrs)){
                    reqEnvelope = GeometryUtil.transformCoordinateSystem(reqEnvelope, "EPSG:4326");
                }
                ReferencedEnvelope nativeEnvelope = new ReferencedEnvelope(minX, maxX, minY, maxY, CRS.parseWKT(Constant.WGS_84_WKT));
                if (!nativeEnvelope.intersects(reqEnvelope)) {
                    continue;
                }
                if (nativeEnvelope.contains(reqEnvelope)) {
                    layerPaths.add(baseInfo.getFilePath());
                    layerNames.add(baseInfo.getLayerName());
                    break;
                } else if (reqEnvelope.contains(nativeEnvelope) || reqEnvelope.intersects(nativeEnvelope)) {
                    layerPaths.add(baseInfo.getFilePath());
                    layerNames.add(baseInfo.getLayerName());
                }
            } catch (MismatchedDimensionException | FactoryException e) {

            }
        }
        //挨个请求，再按照高精度去请求
        List<float[]> demBufs = new ArrayList<float[]>();
        int index = 0;
        for (String filePath:layerPaths) {
            tileInfo.setLayerName(layerNames.get(index));
            //先返回float的像素值，然后再合并，后面在输出
            //TODO 需要重新按照高程数组处理
            float[] demArray = getDemValueArray(tileInfo,filePath,layerBaseInfo.getResourceSrs());
            demBufs.add(demArray);
        }
        return createGroupDem(demBufs,tileInfo.getMineType());
    }

    private byte[] createGroupDem(List<float[]> demBuffers,String mimeType) {
        int m = 0,n = 0;
        float[] demArray = null;
        if (mimeType.endsWith("tiff")) {
            demArray = new float[256*256];
            m = n = 256;
        }else if (mimeType.endsWith("terrain")) {
            demArray = new float[65*65];
            m = n = 65;
        }
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                for (float[] floatArray : demBuffers) {
                    float demValue = floatArray[i*m+j];
                    //保证高精度数据有值即可
                    if (demValue != 0.0) {
                        demArray[i*m+j] = demValue;
                        break;
                    }
                }
            }
        }
        //如果是tiff数据，则需要做转化处理
        if (mimeType.endsWith("tiff")) {
            return createByteArrayFromFloatArray(demArray,256,256);
        }else if (mimeType.endsWith("terrain")) {
            return convetDemPixelData(demArray);
        }
        return null;
    }

    /**
     * 将高程数据的像素值解析出来，并按照online的数据要求进行转化处理
     *
     * @param pixelArray
     * @return
     */
    private byte[] convetDemPixelData(float[] pixelArray) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[65 * 65 * 2 + 2]);
        // int[] waterOrLand = new int[65*65];
        // boolean isWater = false;
        // boolean isLand = false;
        for (int j = 0; j < 65; j++) {
            for (int i = 0; i < 65; i++) {
                short pixel = (short) pixelArray[j * 65 + i];
                if (pixel < 0) {
                    pixel = 0;
                }
                // 像素大于0，陆地，小于零则是海洋
                // if (pixel >= 0) {
                // waterOrLand[j*65 + i] = 255;
                // isLand = true;
                // }else {
                // waterOrLand[j*65 + i] = 0;
                // isWater = true;
                // }
                short demHigh = (short) ((pixel + 1000) * 5);
                byte[] buf = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(demHigh).array();
                byteBuffer.put(buf);
            }
        }
        // 切片到最后一级的时候，子瓦片数设置为0
        byte[] buf = ByteBuffer.allocate(1).order(ByteOrder.LITTLE_ENDIAN).put((byte) 15).array();
        byteBuffer.put(buf);
        // 根据高程和陆地来确定最后一位值
        // if (isWater && !isLand) {
        // byte[] water =
        // ByteBuffer.allocate(1).order(ByteOrder.LITTLE_ENDIAN).put((byte) 0).array();
        // byteBuffer.put(water);
        // }
        // if (!isWater && isLand) {
        // 全部是陆地
        byte[] land = ByteBuffer.allocate(1).order(ByteOrder.LITTLE_ENDIAN).put((byte) 255).array();
        byteBuffer.put(land);
        // }
        // 直接弄成数据
        // if (isWater && isLand) {
        // for (int i = 0; i < 65*65; i++) {
        // byte[] temp =
        // ByteBuffer.allocate(1).order(ByteOrder.LITTLE_ENDIAN).put((byte)
        // waterOrLand[i]).array();
        // byteBuffer.put(temp);
        // }
        // }
        return byteBuffer.array();
    }

    private byte[] createByteArrayFromFloatArray(float[] rasterBuffer,int tileWidth,int tileHeight) {
        DataBufferFloat dataBuffer = new DataBufferFloat(rasterBuffer, tileWidth*tileHeight);
        ColorSpace space = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ComponentSampleModel sampleModle = new ComponentSampleModel(DataBuffer.TYPE_FLOAT,tileWidth , tileHeight, 1, tileWidth, new int[] {0});
        int[] bits = {32};
        ComponentColorModel color1 = new ComponentColorModel(space, bits, false, false, Transparency.TRANSLUCENT,DataBuffer.TYPE_FLOAT);
        WritableRaster raster = Raster.createWritableRaster(sampleModle,dataBuffer,new Point(0,0));
        BufferedImage image = new BufferedImage(color1,raster,false,null);
        return ImageUtil.formatImage2Tiff(image);
    }

    private float[] getDemValueArray(TileRequestInfo tileInfo, String filePath , String sourceSrs) {
        GdalReadData gdalReadData = new GdalReadData(tileInfo, filePath, false, 256, 256, sourceSrs);
        return gdalReadData.getDataRasterPiexl();
    }


    private byte[] getDemBuf(TileRequestInfo tileInfo, String filePath,String sourceSrs) {
        byte[] buff = null;
        GdalReadData gdalReadData = new GdalReadData(tileInfo, filePath, false, 256, 256, sourceSrs);
        if (tileInfo.getMineType().endsWith("tiff")) {
            buff = gdalReadData.getTifResource();
        }
        if (tileInfo.getMineType().endsWith("terrain")) {
            float[] pixelArray = gdalReadData.getDataRasterPiexl();
            buff = convetDemPixelData(pixelArray);
        }
        return buff;
    }


    private BoundingBox calcuteEnvelope(TileRequestInfo tileInfo) {
        BoundingBox envelope = null;
        int[] tileRange = new int[3];;
        tileRange[0] = tileInfo.getTileCol();
        tileRange[1] = tileInfo.getTileRow();
        tileRange[2] = tileInfo.getZoomLevel();
        String srs = tileInfo.getGridSet();
        if (OgcEpsg.MERCATOR_CODE == OgcEpsg.getOgcEpsgCodeType(srs)) {
            envelope = GeometryUtil.calcute3857bbox(tileRange[2], tileRange[1],tileRange[0], "EPSG:3857");

        } else if (OgcEpsg.DEGREE_CODE == OgcEpsg.getOgcEpsgCodeType(srs)) {
            envelope = GeometryUtil.calcute4326bbox(tileRange[2],tileRange[1],tileRange[0]);
        }
        return envelope;
    }

    public byte[] renderTileImage(TileRequestInfo tileInfo, LayerBaseInfo layerBaseInfo) {
        byte[] buff = null;
        String metaTileIndex = createMetaTileIndex(tileInfo);
        String srs = tileInfo.getGridSet();
        Lock lock = lockProvider.getLock(metaTileIndex);
        try {
            lock.lock();
            //高斯暂时不走缓存---样式预览不走缓存(服务样式和请求样式不一样)
            if (!guassCoordinates.getGuassCoordinates().contains(srs)
                    && tileInfo.getStyle() != null
                    && (tileInfo.getStyle().equalsIgnoreCase(layerBaseInfo.getStyleName())
                    || StringUtils.isBlank(tileInfo.getStyle()))) {
                //先去查找
                buff = findLocalCache(tileInfo);
            }
            if (buff != null) {
                log.info("tile has exists");
            } else {
                PyRenderRequestParam renderParam = constructRenderRequestParam(tileInfo, layerBaseInfo);
                if ("ImageMosaic".equals(layerBaseInfo.getFileType())) {
                    String bbox = GeometryUtil.convertRangeToBbox(tileInfo.getZoomLevel(), tileInfo.getTileRow(), tileInfo.getTileCol());
                    WmsRequestParam wmsRequestParam = new WmsRequestParam();
                    wmsRequestParam.setLayerName(layerBaseInfo.getLayerName());
                    wmsRequestParam.setSrs(tileInfo.getGridSet());
                    wmsRequestParam.setBbox(bbox);
                    wmsRequestParam.setFormat("image/png");
                    wmsRequestParam.setWidth(tileInfo.getTileWidth());
                    wmsRequestParam.setHeight(tileInfo.getTileHeight());
                    wmsRequestParam.setTransparent(true);
                    wmsRequestParam.setOutputTransparentColor(layerBaseInfo.getOutPutTransparentColor());
                    wmsRequestParam.setInputTransparentColor(layerBaseInfo.getInputTransparentColor());
                    buff = wmsProtocolService.getWmsImageByte(wmsRequestParam, "");
                } else {
                    buff = renderService.renderImageByRequestParam(renderParam);
                }
                if (iSMetaTileSize(tileInfo.getMineType())) {
                    buff = saveTileRangeAndGetCurrentTile(buff, tileInfo, renderParam.isCache());
                }
            }
        } catch (Exception e) {
            e.getStackTrace();
        } finally {
            lock.unlock();
        }
        return buff;
    }

    /**
     * 获取当前请求的瓦片返回，异步存储其它瓦片数据
     * @param bufferResource
     * @param tileInfo
     * @param isCache
     * @return byte
     * @throws IOException
     */
    private byte[] saveTileRangeAndGetCurrentTile(byte[] bufferResource, TileRequestInfo tileInfo, boolean isCache) {
        ByteArrayInputStream bais = new ByteArrayInputStream(bufferResource);
        BufferedImage image = null;
        try {
            image = ImageIO.read(bais);
        } catch (IOException e) {
            log.error("TileCacheServiceImpl saveTileRangeAndGetCurrentTile has error:{0}",e.getMessage());
        }
        int tileWidth = tileInfo.getTileWidth() == 0 ? 256 : tileInfo.getTileWidth();
        int tileHeight = tileInfo.getTileWidth() == 0 ? 256 : tileInfo.getTileWidth();
        String simpleMimeType = CommonUtil.simpleMineType(tileInfo.getMineType());
        int[] tileRange = tileInfo.getTileRequestRange();
        int offsetX = tileInfo.getTileCol() - tileRange[0];
        int offsety = tileInfo.getTileRow() - tileRange[1];
        BufferedImage tileImage = image.getSubimage(offsetX*tileWidth, offsety*tileHeight, tileWidth, tileHeight);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(tileImage, simpleMimeType, baos);
        } catch (IOException e) {
            log.error("TileCacheServiceImpl saveTileRangeAndGetCurrentTile has error:{0}",e.getMessage());
        }
        final BufferedImage resouceImage = image;
        if (isCache) {
            saveTileImage(tileInfo, resouceImage);
        }
        return baos.toByteArray();
    }

    private void saveTileImage(TileRequestInfo tileInfo, final BufferedImage resouceImage) {
        int tileWidth = tileInfo.getTileWidth();
        int tileHeight = tileInfo.getTileHeight();
        String simpleMimeType = CommonUtil.simpleMineType(tileInfo.getMineType());
        int[] tileRange = tileInfo.getTileRequestRange();
        //保存到tdb对应的缓存文件中
        String visitCachePath = getVistitCacheDirName(tileInfo, simpleMimeType);
        TDBDataConfig tdbConfig = checkTdbConfig(visitCachePath,tileInfo);
        int m = 0;
        int n = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = tileRange[1]; i <= tileRange[3]; i++) {
            for (int j = tileRange[0]; j <= tileRange[2]; j++) {
                BufferedImage imageTile = resouceImage.getSubimage(m*tileWidth, n*tileHeight, tileWidth, tileHeight);
                try {
                    ImageIO.write(imageTile, simpleMimeType, baos);
                } catch (IOException e) {
                    log.error("TileCacheServiceImpl saveTileImage has error:{0}",e.getMessage());
                }
                writeCacheFile(visitCachePath,tileInfo.getLayerName(),tdbConfig,baos.toByteArray(),j,i,tileInfo.getZoomLevel());
                baos.reset();
                m++;
            }
            n++;
            m=0;
        }
        try {
            baos.flush();
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getVistitCacheDirName(TileRequestInfo tileInfo,String format) {
        String parentPath = tileInfo.getSystemCachePath();
        String crs = tileInfo.getGridSet().replace(":", "_");
        StringBuilder sb = new StringBuilder();
        sb.append(parentPath).append(File.separator)
                .append(tileInfo.getLayerName()).append("_")
                .append(crs).append("_")
                .append(tileInfo.getTileWidth()).append("_")
                .append(format).append(File.separator);
        return sb.toString();
    }

    private void writeCacheFile(String path,
                                String layerName,
                                TDBDataConfig tdbConfig,
                                byte[] dataArray,
                                int col,
                                int row,
                                int level) {
        TileResourceInfo tileResourceInfo = new TileResourceInfo(dataArray,col,row,level);
//		int tileRow = (int) (Math.pow(2, level)) - row - 1;
        tileResourceInfo.setY(row);
        Map<String,String> tdbParams = new HashMap<String, String>();
        tdbParams.put("destPath", path + File.separator);
        tdbParams.put("serverName", layerName);
        if (dataArray != null) {
            customDataLoad.insertTileImage(tileResourceInfo, tdbConfig, tdbParams,"");
        }
    }



    private TDBDataConfig checkTdbConfig(String cachaPath,TileRequestInfo tileInfo) {
        File temp = new File(cachaPath);
        if (!temp.exists()) {
            temp.mkdirs();
        }
        String cacheKey = temp.getName();
        TDBDataConfig tdbConfig = null;
        if(TdbUtils.tdbMap.containsKey(cacheKey)) {
            tdbConfig = TdbUtils.tdbMap.get(cacheKey);
        } else {
            SeedTaskInstance tileTask = new SeedTaskInstance();
            tileTask.setTileWidth(tileInfo.getTileWidth());
            tileTask.setTileHeight(tileInfo.getTileHeight());
            tileTask.setZoomStart(0);
            tileTask.setZoomEnd(23);
            tileTask.setMinX(-180.0);
            tileTask.setMinY(-90.0);
            tileTask.setMaxX(180.0);
            tileTask.setMaxX(90.0);
            tdbConfig = TdbUtils.getTDBDataConfig(tileInfo, tileTask);
            if (!TdbUtils.tdbMap.containsKey(cacheKey)) {
                TdbUtils.tdbMap.put(cacheKey, tdbConfig);
            }
        }

        //写config.tab文件
        File configFile = new File(cachaPath,"config.tdb");
        if (!configFile.exists()) {
            TdbConfigFileGenerate.writeTDBConfigFile(cachaPath, tdbConfig);
        }
        return tdbConfig;
    }

    private boolean iSMetaTileSize(String mimeType) {
        if (ResponseFormat.GEOJSON.getFormat().equalsIgnoreCase(mimeType)
                || ResponseFormat.TOPOJSON.getFormat().equalsIgnoreCase(mimeType)
                || ResponseFormat.PBF.getFormat().equalsIgnoreCase(mimeType)) {
            return false;
        }
        return true;
    }

    private String createMetaTileIndex(TileRequestInfo tileInfo) {
        String tileIndex = createTileIndex(tileInfo);
        StringBuilder sb = new StringBuilder(tileIndex);
        long x =  tileInfo.getTileCol() / 4 * 4;
        long y = tileInfo.getTileRow() / 4 * 4;
        sb.append("_")
                .append(tileInfo.getZoomLevel())
                .append("_")
                .append(x)
                .append("_")
                .append(y);
        return sb.toString();
    }

    private String createTileIndex(TileRequestInfo tileInfo) {
        StringBuilder sb = new StringBuilder(tileInfo.getLayerName());
        String mineType = CommonUtil.simpleMineType(tileInfo.getMineType());
        sb.append("_")
                .append(tileInfo.getGridSet().split(":")[1])
                .append("_").append(mineType)
                .append("_")
                .append(tileInfo.getTileWidth());
        return sb.toString();
    }

    private PyRenderRequestParam constructRenderRequestParam(TileRequestInfo tileInfo, LayerBaseInfo layerBaseInfo) {
        Envelope envelope = null;
        ReferencedEnvelope leftUpper = null;
        ReferencedEnvelope rightLower = null;
        int[] tileRange = null;
        List<String> tilecount = null;
        String srs = tileInfo.getGridSet();
        if (iSMetaTileSize(tileInfo.getMineType()) && !guassCoordinates.getGuassCoordinates().contains(srs)) {
            tileRange = constructTileRange(tileInfo, layerBaseInfo);
        } else {
            tileRange = new int[5];
            tileRange[0] = tileInfo.getTileCol();
            tileRange[2] = tileInfo.getTileCol();
            tileRange[1] = tileInfo.getTileRow();
            tileRange[3] = tileInfo.getTileRow();
            tileRange[4] = tileInfo.getZoomLevel();
        }
        if (OgcEpsg.MERCATOR_CODE == OgcEpsg.getOgcEpsgCodeType(srs)) {
            leftUpper = GeometryUtil.calcute3857bbox(tileRange[4], tileRange[1],tileRange[0], "EPSG:3857");
            rightLower = GeometryUtil.calcute3857bbox(tileRange[4],tileRange[3],tileRange[2],  "EPSG:3857");
        } else if (guassCoordinates != null && guassCoordinates.getGuassCoordinates().contains(srs)) {
            Map<String, List<String>> guassCoordinatePara = guassCoordinates.getTileParam().get(srs.replace(":", ""));
            tilecount = guassCoordinates.getTileParam().get(srs.replace(":", "")).get("tilecount");
            leftUpper = GeometryUtil.calcuteGuassBbox(tileRange[4], tileRange[1], tileRange[0], srs, guassCoordinatePara);
            rightLower = GeometryUtil.calcuteGuassBbox(tileRange[4], tileRange[3], tileRange[2], srs, guassCoordinatePara);
        } else if (OgcEpsg.DEGREE_CODE == OgcEpsg.getOgcEpsgCodeType(srs)) {
            leftUpper = GeometryUtil.calcute4326bbox(tileRange[4],tileRange[1],tileRange[0]);
            rightLower = GeometryUtil.calcute4326bbox(tileRange[4], tileRange[3],tileRange[2]);
        }
        int metaTileX = tileRange[2] - tileRange[0] + 1;
        int metaTileY = tileRange[3] - tileRange[1] + 1;
        leftUpper.expandToInclude(rightLower);
        envelope = leftUpper;

        // 获取服务图层基础信息
        tileInfo.setTileRequestRange(tileRange);
        PyRenderRequestParam renderParam = new PyRenderRequestParam();

        // TODO: 如果是远程文件路经，如何处理
        renderParam.setFileUrl(layerBaseInfo.getFilePath());

        String layerBaseStyle = StringUtils.isBlank(layerBaseInfo.getStyleName()) ? "栅格" : layerBaseInfo.getStyleName();
        String styleName;
        if (tileInfo.getStyle() != null && tileInfo.getStyle().equalsIgnoreCase(layerBaseInfo.getStyleName()) || StringUtils.isBlank(tileInfo.getStyle())) {
            renderParam.setCache(true);
            styleName = layerBaseStyle;
        } else {
            renderParam.setCache(false);
            styleName = tileInfo.getStyle();
        }
        // 直接使用样式名称，保证在redis中已经处理
        renderParam.setStyleName(styleName);
        StringBuilder sb = new StringBuilder().append(envelope.getMinimum(0)).append(",").append(envelope.getMaximum(0))
                .append(",").append(envelope.getMinimum(1)).append(",").append(envelope.getMaximum(1));
        renderParam.setRequestBbox(sb.toString());
        renderParam.setFormat(tileInfo.getMineType());

        // 1是图层组
        renderParam.setLayerGroup(layerBaseInfo.getIsLayerGroup() == 1);
        int tileHeight = tileInfo.getTileHeight() == 0 ? 256 : tileInfo.getTileHeight();
        int tileWidth = tileInfo.getTileWidth() == 0 ? 256 : tileInfo.getTileWidth();
        renderParam.setMapHeight(tileHeight * metaTileY);
        renderParam.setMapWidth(tileWidth * metaTileX);
        renderParam.setTransparent(true);
        renderParam.setDataFormat(layerBaseInfo.getFileType());
        renderParam.setDataType(layerBaseInfo.getDataType());
        renderParam.setRequestSrs(tileInfo.getGridSet());
        renderParam.setLayerName(tileInfo.getLayerName());
        // renderParam.setResourceMaxLevel(layerBaseInfo.getResourceMaxLevel());
        renderParam.setZoomLevel(tileInfo.getZoomLevel());
        renderParam.setTilecount(tilecount);
        renderParam.setInputTransparentColor(layerBaseInfo.getInputTransparentColor());
        renderParam.setOutPutTransparentColor(layerBaseInfo.getOutPutTransparentColor());
        renderParam.setResourcesSrs(layerBaseInfo.getResourceSrs());
        //如果是postgis,则需要传递数据库连接参数
        if (layerBaseInfo.getFileType().contains("PostGIS")) {
            if (postgisConnectionCache.containsKey(tileInfo.getLayerName())) {
                renderParam.setPostgisConnection(postgisConnectionCache.get(tileInfo.getLayerName()));
            } else {
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
                postgisConnectionCache.put(tileInfo.getLayerName(), entity);
                renderParam.setPostgisConnection(entity);
            }
        }
        return renderParam;
    }

    private int[] constructTileRange(TileRequestInfo tileInfo, LayerBaseInfo layerBaseInfo) {
        //构造请求的瓦片行列号的范围
        int level = tileInfo.getZoomLevel();
        int row = tileInfo.getTileRow();
        int col = tileInfo.getTileCol();

        //根据bbox 计算当前数据在当前级别的行列范围
        String nativeBbox = layerBaseInfo.getNativaBbox();
        String[] nativeBBox = nativeBbox.split(";");
        double minX = Double.parseDouble(nativeBBox[0]);
        double maxX = Double.parseDouble(nativeBBox[2]);
        double minY = Double.parseDouble(nativeBBox[1]);
        double maxY = Double.parseDouble(nativeBBox[3]);
        ReferencedEnvelope resourceBbox = null;
        int[] tileRange = null;
        //根据当前的x，y找到起始渲染的行列号
        long tileMinX =  col / 4 * 4;
        long tileMinY = row / 4 * 4;
        long tileMaxX = tileMinX + 4 - 1;
        long tileMaxY = tileMinY + 4 - 1;
        try {
            resourceBbox = new ReferencedEnvelope(minX, maxX, minY, maxY, CRS.decode("EPSG:4326",true));
        } catch (MismatchedDimensionException | FactoryException e) {
            e.printStackTrace();
        }
        String srs = tileInfo.getGridSet();
        if (OgcEpsg.MERCATOR_CODE == OgcEpsg.getOgcEpsgCodeType(srs)) {
            resourceBbox = GeometryUtil.transformCoordinateSystem(resourceBbox, "EPSG:3857");
            tileRange = GeometryUtil.convert3857ToTiledRange(level, resourceBbox);
        } else if (guassCoordinates.getGuassCoordinates().contains(srs)) {
            List<String> tilecount = guassCoordinates.getTileParam().get(srs.replace(":", "")).get("tilecount");
            List<String> guassCoordinateBbox = guassCoordinates.getTileParam().get(srs.replace(":", "")).get("bbox");
            BoundingBox resourceBoundingBox = convertBoundingBox(layerBaseInfo.getResourceBbox(),srs);
            tileRange = GeometryUtil.convertGuassToTiledRange(tilecount.get(level), guassCoordinateBbox, resourceBoundingBox, level);
        } else {
            tileRange = GeometryUtil.convert4326ToTiledRange(level, resourceBbox);
        }
        if (tileRange[0] < tileMinX) {
            tileRange[0] = (int) tileMinX;
        }
        if (tileRange[1] < tileMinY) {
            tileRange[1] = (int) tileMinY;
        }
        if (tileRange[2] > tileMaxX) {
            tileRange[2] = (int) tileMaxX;
        }
        if (tileRange[3] > tileMaxY) {
            tileRange[3] = (int) tileMaxY;
        }
        return tileRange;
    }

    private BoundingBox convertBoundingBox(String resourcebbox,String srs) {
        try {
            String[] nativeBboxArray = resourcebbox.split(";");
            return new ReferencedEnvelope(Double.parseDouble(nativeBboxArray[0]), Double.parseDouble(nativeBboxArray[2]), Double.parseDouble(nativeBboxArray[1]), Double.parseDouble(nativeBboxArray[3]), CRS.decode(srs,true));
        } catch (MismatchedDimensionException | FactoryException e) {
            log.error("MapServerServiceImpl convertBoundingBox has error:" + ExceptionsUtil.stackTraceToStr(e));
            throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_TRANSFOR, "坐标系Bbox信息"));
        }
    }
}
