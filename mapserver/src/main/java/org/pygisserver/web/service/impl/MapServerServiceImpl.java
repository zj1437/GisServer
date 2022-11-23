package org.pygisserver.web.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.pygisserver.common.entity.*;
import org.pygisserver.common.enumm.*;
import org.pygisserver.common.exception.*;
import org.pygisserver.common.pytc.entity.RequestQueryCondition;
import org.pygisserver.common.pytc.entity.TileRequestInfo;
import org.pygisserver.common.redis.util.JedisConfig;
import org.pygisserver.common.redis.util.RedisUtils;
import org.pygisserver.common.service.SystemLogService;
import org.pygisserver.common.ui.entity.LayerGroup;
import org.pygisserver.common.ui.entity.MapServerBatchInfo;
import org.pygisserver.common.util.*;
import org.pygisserver.render.service.CustomDataLoad;
import org.pygisserver.render.service.PyRenderImage;
import org.pygisserver.render.service.impl.CustomDataLoadImpl;
import org.pygisserver.render.tiled.TiledInfoStrategy;
import org.pygisserver.render.util.LayerBaseInfoUtil;
import org.pygisserver.render.util.TdbUtils;
import org.pygisserver.tileserver.service.RenderTileService;
import org.pygisserver.web.aop.MapLayerAnalysisAdvices;
import org.pygisserver.web.dao.MapServerMapper;
import org.pygisserver.web.entity.StyleInfo;
import org.pygisserver.web.entity.*;
import org.pygisserver.web.service.MapServerService;
import org.pygisserver.web.service.MapStatsticService;
import org.pygisserver.web.service.ReadDataInfo;
import org.pygisserver.web.startegy.CustomDataInfo;
import org.pygisserver.web.startegy.DataInfoStrategy;
import org.pygisserver.web.startegy.FileDataInfoFactory;
import org.pygisserver.web.utils.MapServerUtils;
import org.pygisserver.web.utils.PostGisStoreUtils;
import org.pygisserver.web.utils.UserUtil;
import org.pygisserver.web.websocket.WebSocketManager;
import org.pygisserver.web.wmsprotocol.entity.WmsRequestParam;
import org.pygisserver.web.wmsprotocol.service.WmsProtocolService;
import org.geoserver.catalog.*;
import org.geoserver.catalog.impl.CoverageStoreInfoImpl;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WMSStoreInfoImpl;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.DataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.geotools.gce.imagemosaic.ImageMosaicReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.ows.wms.Layer;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.GeoTools;
import org.locationtech.jts.geom.Envelope;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import redis.clients.jedis.Jedis;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @Description: 地图服务service实现类
 * @author: 
 * @date: 2018-10-29
 */
@Service
@Slf4j
public class MapServerServiceImpl implements MapServerService {
	@Resource
	private MapServerMapper mapServer;
	
	@Autowired
	private ExceptionsUtil exceptionsUtil;
	
	@Autowired
	private SystemLogService systemLogService;

	@Autowired
	private LayerBaseInfoUtil layerBaseInfoUtil;

	@Resource
	private MapStatsticService statsticService;

	/**
	 * 注入catalog
	 */
	@Autowired
	public Catalog catalog;

	/**
	 * 注入wmsServiceTarget
	 */
//	@Autowired
//	private DefaultWebMapService webMapService;

	@Autowired
	private WmsProtocolService wmsProtocolService;

	@Autowired
	private WebSocketManager webSocket;
	
	private String storeInfoId;

	@Autowired
	private StyleInfoServiceImpl styleInfoServiceImpl;
	
	@Autowired
	private RedisUtils redisUtils;
	
	@Autowired
	private ObjectFactory<TiledInfoStrategy> factory;

	@DubboReference(version="3.0.0")
	private RenderTileService renderTileService;
	
	private CustomDataLoad customDataLoad = new CustomDataLoadImpl();
	
	@Autowired
	private MapLayerAnalysisAdvices mapLayerAnalysisAdvices;

	@Autowired
	private SystemServiceImpl systemService;
	
	@Autowired
	private GuassCoordinates guassCoordinates;

	@Resource
	private LayerGroupServerServiceImpl layerGroupServerService;

	@Autowired
	private PyRenderImage renderImage;

	@Autowired
	private OgcProtocol ogcProtocol;

	private static Map<String,Integer> zoomCache = new HashMap<>();

	private static final ConcurrentHashMap<String, DataServerEntity> abnormalServer = new ConcurrentHashMap<>();

	private static final ConcurrentHashMap<String, DataServerEntity> abnormalRestServer = new ConcurrentHashMap<>();

	private static ExecutorService excutorService = Executors.newFixedThreadPool(60);

	public static List<String> cacheLayers = new CopyOnWriteArrayList<>();

	public static List<String> cacheRestLayers = new CopyOnWriteArrayList<>();

	private List<String> cacheSourceNames = new CopyOnWriteArrayList<>();
	private List<String> cacheSourceRestNames = new CopyOnWriteArrayList<>();


	private FeatureInfo featureInfo;

//	private static final Map<String,LayerBaseInfo> CACHE_LAYERBASE_IFNO = new HashMap<String,LayerBaseInfo>();

	public static Map<String,ServerProtocolInfo> protocolInfos = null;

	//服务支持发布的地图数据类型
	private static String[] serverTypes = new String[]{".shp",".tif",".tiff",".db",".mbtiles",".img"};

	private static String QUERY_FILED_SQL = "select a.attname as field_name from pg_class as c , pg_attribute as a where a.attrelid = c.oid and atttypid > 0 and a.attnum > 0 and c.relname = ?";
	private static List<String> supportServerTypes = Arrays.asList(serverTypes);

	private static final String sql = "select count(*) from information_schema.tables where table_schema='public' and table_type='BASE TABLE' and table_name= ? ";

	private boolean isFirst = true;

	private Map<String, Future> overViewsCache = new HashMap<>();

	/**
	 * 查看当前图层是否已经发布
	 *
	 * @param layer
	 * @return
	 */
	@Override
	public boolean isPublished(String layer) {
		String temName = layer.length() > 20 ? layer.substring(0,20) : layer;
		// 首先判断cacheLayers是否有值，没有就去查询
		if (cacheLayers.isEmpty()) {
			cacheLayers = mapServer.queryExistsLayers();
		}
		// 查询完毕
		if (cacheLayers.isEmpty()) {
			return false;
		}
		return cacheLayers.contains(temName);
	}

	/**
	 * 查看当前rest是否已经发布
	 * @param layer
	 * @return
	 */
	@Override
	public boolean ispublisedRest(String layer) {
		String temName = layer.length() > 20 ? layer.substring(0,20) : layer;
		// 首先判断cacheLayers是否有值，没有就去查询
		if (cacheRestLayers.isEmpty()) {
			cacheRestLayers = mapServer.queryExistsRestLayers();
		}
		// 查询完毕
		if (cacheRestLayers.isEmpty()) {
			return false;
		}
		return cacheRestLayers.contains(temName);
	}

	/**
	 * 查看即将发布的服务是否是高程
	 *
	 * @param dataUrl
	 * @param featureInfo
	 * @return boolean
	 */
	public boolean isDemServer(String dataUrl,FeatureInfo featureInfo) {
		dataUrl = MapServerUtils.getUTF8FilePath(dataUrl);
		if(SystemServiceImpl.isPostGIS(dataUrl)) {
			return false;
		}
		if (dataUrl.contains(".")) {
			String fileName = dataUrl.substring(dataUrl.lastIndexOf("."));
			if (supportServerTypes.contains(fileName)) {
				return false;
			}
		}
		File ff = normalizeFilePath(dataUrl,true);
		String filePath = ff == null ? null : ff.getPath();
		String fileType = filePath == null ? null : getFileType(filePath);
		if(fileType == null) {
			return false;
		}
		String type = null;
		int tileHeight = 0;
		if(featureInfo != null) {
			type = featureInfo.getTileExtension();
			tileHeight = featureInfo.getTileHeight();
		} else {
			if(fileType.equals("TDB")) {
				type = getTDBFileExtension(filePath);
			} else if (fileType.equals("FlatGis") || fileType.equals("TMS")) {
				CustomDataInfo customDataInfo = new CustomDataInfo(filePath,null);
				FlatGisEntity entity = null;
				entity = customDataInfo.parseXml(filePath);
				if(entity == null) {
					return false;
				}
				type = entity.getExtension();
				tileHeight = entity.getHeight();
			}
		}
		switch (type) {
			case "etdb":
			case TdbUtils.TDB_DEM_TYPE:
				return true;
			case "tiff":
			case "tif":
				if(tileHeight != 0 && tileHeight > 64) {
					return false;
				} else {
					return true;
				}
			case "osgb":
				return true;
			default:
				return false;
		}
	}

	public String getTDBFileExtension(String filePath) {
		String type = null;
		TDBDataConfig newConfig = customDataLoad.analysisNewTdbConfig(filePath, null);
		if(newConfig != null) {
			type = newConfig.getCSourceDataType() == 16 ? "etdb" : "itdb";
		}
		TDBDataConfigOld oldConfig = customDataLoad.analysisOldTdbConfig(filePath, null);
		if(type == null) {
			type = oldConfig.getExtension() == 0 ? "etdb" : "itdb";
		}
		return type;
	}

	/**
	 * 根据需要获取文件或目录
	 * @param dataUrl 文件路径，可能是文件，可能是目录
	 * @param isNeedFile 如果需要文件参数为true，否则为false
	 * @return 返回文件或目录
	 */
	private File normalizeFilePath(String dataUrl,boolean isNeedFile) {
		File file = new File(dataUrl);
		if(isNeedFile) {
			if(file.isFile()) {
				return file;
			} else {
				File[] files = file.listFiles(new FilenameFilter() {

					@Override
					public boolean accept(File dir, String name) {
						File ff = new File(dir,name);
						if(ff.isFile() && (name.endsWith(".tdb") || name.endsWith("tms.xml") || name.endsWith("config.xml"))) {
							return true;
						}
						return false;
					}
				});
				if(files.length > 0) {
					return files[0];
				} else {
					return null;
				}
			}
		} else if(file.isFile()) {
			if(file.isFile()) {
				return file.getParentFile();
			} else {
				return file;
			}
		} else {
			return null;
		}
	}

	private String getFileType(String path) {
		if(path.endsWith(".tdb")) {
			return "TDB";
		} else if (path.endsWith("tms.xml")) {
			return "TMS";
		} else if (path.endsWith("config.xml")) {
			return "FlatGis";
		}
		return null;
	}

	/**
	 * 获取最新的服务名称
	 *
	 * @param name
	 * @return
	 */
	public String getNewServerName(String name) {
		//处理特殊字符问题
		boolean isPostGis = false;
		if (name.endsWith(".postgis")) {
			name = name.substring(0,name.length() - 8);
			isPostGis = true;
		}
		name = name.replaceAll(Constant.SPECIAL_CHARACTER, "");
		if (name.length() == 0) {
			name = "default";
		}
		if (isPostGis == true) {
			cacheSourceNames.clear();
			cacheSourceNames = cacheSourceNames.isEmpty() ? mapServer.queryExistsSources() : cacheSourceNames;
			return cacheSourceNames.isEmpty() ? name : MapServerUtils.getNewServerNameByMaxName(MapServerUtils.getMaxFileName(cacheSourceNames, name));
		} else {
			List<String> cacheLayersList = cacheLayers;
			cacheLayersList = cacheLayersList.isEmpty() ? mapServer.queryExistsLayers() : cacheLayersList;
			return cacheLayersList.isEmpty() ? name : MapServerUtils.getNewServerNameByMaxName(MapServerUtils.getMaxFileName(cacheLayersList, name));
		}
	}

	/**
	 * 获取最新的rest服务名称
	 *
	 * @param name
	 * @return
	 */
	public String getNewRestName(String name) {
		//处理特殊字符问题
		boolean isPostGis = false;
		if (name.endsWith(".postgis")) {
			name = name.substring(0,name.length() - 8);
			isPostGis = true;
		}
		name = name.replaceAll(Constant.SPECIAL_CHARACTER, "");
		if (name.length() == 0) {
			name = "default";
		}
		if (isPostGis == true) {
			cacheSourceRestNames.clear();
			cacheSourceRestNames = cacheSourceRestNames.isEmpty() ? mapServer.queryExistsSources() : cacheSourceRestNames;
			return cacheSourceRestNames.isEmpty() ? name : MapServerUtils.getNewServerNameByMaxName(MapServerUtils.getMaxFileName(cacheSourceRestNames, name));
		} else {
			List<String> cacheLayersList = cacheRestLayers;
			cacheLayersList = cacheLayersList.isEmpty() ? mapServer.queryExistsRestLayers() : cacheLayersList;
			return cacheLayersList.isEmpty() ? name : MapServerUtils.getNewServerNameByMaxName(MapServerUtils.getMaxFileName(cacheLayersList, name));
		}
	}

	@Override
	@Transactional
	public void singlePublishServerData(DataServerEntity dataServer,String reqIP) {
		//对服务名称重复或名称超过20个字符进行处理
		if (isPublished(dataServer.getServerName())) {
			String serverName = getNewServerName(dataServer.getServerName());
			dataServer.setServerName(serverName);
		}
		try {
			saveMapServerData(dataServer);
		} catch (SqlException e) {
			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR,
					"服务发布失败:" + e.getErrorMsg(),UserUtil.getUserName(), "服务发布", reqIP));
			throw e;
		}

		// imageMosaic数据异步处理
		if (FileType.IMAGEMOSAIC.getFilterType().equalsIgnoreCase(dataServer.getFileType())) {
			Future future = statsticService.publishImageMosaic(dataServer, reqIP);
			overViewsCache.put(dataServer.getServerName(), future);
			return;
        }

        publishServerResult(dataServer, reqIP);
    }

	@Override
	@Transactional
	public void singlePublishRestData(DataServerEntity dataServer,String reqIP) {
		//对服务名称重复或名称超过20个字符进行处理
		if (isPublished(dataServer.getServerName())) {
			String serverName = getNewServerName(dataServer.getServerName());
			dataServer.setServerName(serverName);
		}
		try {
			saveRestServerData(dataServer);
		} catch (SqlException e) {
			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR,
					"服务发布失败:" + e.getErrorMsg(),UserUtil.getUserName(), "服务发布", reqIP));
			throw e;
		}
//		publishServerResult(dataServer, reqIP);
	}

    public void publishServerResult(DataServerEntity dataServer, String reqIP) {
        String msg = publishServerData(dataServer,reqIP);
        if (!"OK".equals(msg)) {
            List<Integer> ids = new ArrayList<Integer>(1);
            ids.add(dataServer.getServerId());
            dataServer.setStatus(ServerStatusEnum.ABNORMAL.getCode());
            mapServer.updateServerStatusByIds(ServerStatusEnum.ABNORMAL.getCode(), ids);
            //如果超时或者决绝连接提示用户
            if (msg.startsWith("服务发布超时")) {
                throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.STATUS_TIMEOUT, dataServer.getServerName()));
            }
        }
        //将瓦片服务基本信息存入Redis,需要将最大图层级别存入Redis
        cacheLayerInfo(dataServer,reqIP);
        // 异步渲染当前数据的最低级别，并将其压缩成一个缩略图
        dealThumbal(reqIP,dataServer);
    }

    public List<ServerProtocolEntity> getMapServerFormat(DataServerEntity dataServer) {
		if (protocolInfos == null) {
			protocolInfos = MapServerUtils.convertProtocol2Map(mapServer.queryServerProtocolInfo());
		}
		boolean isDem = false;
		boolean isTerrain = "terrain".equalsIgnoreCase(dataServer.getFeatureInfo().getTileExtension());
		boolean isOsgbFile = "osgb".equalsIgnoreCase(dataServer.getFeatureInfo().getTileExtension());
		boolean isVector = (DataTypeEnum.VECTOR.getFileCode() == dataServer.getDataType());
		boolean isGroup = dataServer.getLayerGroupFlag() == 1 ? true : false;
		String srs = dataServer.getFeatureInfo().getResourceSrs();
		if (dataServer.getStyleInfo() != null) {
			isDem = dataServer.getStyleInfo().getSldName().equals("高程");
		} else {
			isDem = dataServer.getFeatureInfo().getDataType() == 2;
		}
		boolean isTdb = (dataServer.getFileType().equals("TDB") && isDem) ? true : false;
        boolean isOnline = ( dataServer.getDataType() == DataTypeEnum.ONLINE.getFileCode()) ? true : false;
		List<ServerProtocolEntity> protocols = new ArrayList<ServerProtocolEntity>();
		String[] protocol = dataServer.getPublisedProctol().split(";");
		for (String p : protocol) {
			ServerProtocolEntity entity = new ServerProtocolEntity();
			entity.setServerId(dataServer.getServerId());
			switch (p) {
			case "WMS":
				entity.setProtocol(protocolInfos.get("WMS").getProtocolName());
				dealTileGridSet("WMS", srs, entity);
				entity.setFormats(protocolInfos.get("WMS").getSupportFormat());
				protocols.add(entity);
				break;
	        case "WMTS":
	        	entity.setProtocol(protocolInfos.get("WMTS").getProtocolName());
	        	dealTileGridSet("WMTS", srs, entity);
	        	if (isOsgbFile) {
	        		entity.setFormats("osgb");
	        	}else {
	        		dealProtocalFormat(isTerrain,isGroup,isTdb,isDem, isVector, isOnline,"WMTS", entity);
	        	}
	        	protocols.add(entity);
				break;
	        case "TMS":
	        	entity.setProtocol(protocolInfos.get("TMS").getProtocolName());
                if (isDem && dataServer.getDataType() == DataTypeEnum.TILED.getFileCode()) {
                    entity.setGridSets(dataServer.getFeatureInfo().getResourceSrs());
                } else {
                    dealTileGridSet("TMS", srs, entity);
                }
	        	if (isOsgbFile) {
	        		entity.setFormats("osgb");
	        	}else {
	        		dealProtocalFormat(isTerrain,isGroup,isTdb,isDem, isVector, isOnline,"TMS", entity);
	        	}
				protocols.add(entity);
		        break;
	        case "WFS":
	        	entity.setProtocol(protocolInfos.get("WFS").getProtocolName());
	        	dealTileGridSet("WFS", srs, entity);
				entity.setFormats(protocolInfos.get("WFS").getSupportFormat());
				protocols.add(entity);
		        break;
	        case "WCS":
	        	entity.setProtocol(protocolInfos.get("WCS").getProtocolName());
	        	dealTileGridSet("WCS", srs, entity);
				entity.setFormats(protocolInfos.get("WCS").getSupportFormat());
				protocols.add(entity);
		        break;
		    }
	    }
		return protocols;
	}
	
	private void dealTileGridSet(String protocalName, String srs, ServerProtocolEntity entity) {
		 String tileGridSet = protocolInfos.get(protocalName).getTileGridSet();
		 if(!tileGridSet.contains(srs)) {
			 tileGridSet = tileGridSet +";"+ srs ;
		 }
		entity.setGridSets(tileGridSet);
		
	}
	
	private void dealProtocalFormat(boolean isTerrain,boolean isGroup, boolean isTdb, boolean isDem, boolean isVector, boolean isOnline, String protocalName, ServerProtocolEntity entity) {
		String wformats = protocolInfos.get(protocalName).getSupportFormat();
		if(isTerrain) {
			entity.setFormats("terrain");
		}else {
			if (isOnline){
	            wformats = wformats.replace(";tiff","");
	        }
			if(isGroup) {
				wformats = wformats.replace(";geojson;mapbox-vector", "");
			}
			if(!isTdb) {
				wformats = wformats.replace(";heightarray", "");
			}
			if (!isVector) {
				wformats = wformats.replace(";geojson;mapbox-vector", "");
			}
			wformats = wformats.replace(";osgb", "");
			if (!isDem) {
				entity.setFormats(wformats.replace(";terrain", ""));
			} else {
				entity.setFormats(wformats);
			}
		}
        
	}

	@Override
	public void dealThumbal(final String reqIP,DataServerEntity...dataServer) {
		CompletableFuture.supplyAsync(() -> {
			String serverNames = "";
			List<String>  failName = new ArrayList<>();
			for (DataServerEntity dataServerEntity : dataServer) {
				try {
					serverNames = serverNames + "," + dataServerEntity.getServerName();
					dealThumbalUpdate(dataServerEntity,reqIP);
				} catch (Exception e) {
					failName.add(dataServerEntity.getServerName());
					continue;
				}
				// 处理金字塔缓存业务，如果已经存在缓存数据就不再生成
				File overViewFile = new File(dataServerEntity.getDataUrl() + ".ovr");
				if (dataServerEntity.getIsPyamid() == 1 && !overViewFile.exists()) {
					Future future = statsticService.buildOverviews(dataServerEntity);
					overViewsCache.put(dataServerEntity.getServerName(), future);
				} else if (dataServerEntity.getIsPyamid() == 1 && overViewFile.exists()) {
					//修改服务前清除redis中 数据缓存
//					redisUtils.deleteKey("system", dataServerEntity.getServerName());
					// 当发布服务勾选生成金字塔，但此服务已有缓存文件时不再生成金字塔缓存，修改服务状态
					dataServerEntity.setStatus(ServerStatusEnum.PUBLISHED.getCode());
					// 修改服务状态，否则预览时服务状态不是已发布，无法预览
					mapServer.updateStatusAndOverview(dataServerEntity.getServerId());
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("status", 200);
					jsonObject.put("msgKey", "mapServer");
					jsonObject.put("msg", dataServerEntity.getServerName() + "服务已存在金字塔缓存!");
					webSocket.sendMessage(jsonObject.toJSONString());
				}
			}
			
			String resultInfo = serverNames.substring(1) +"服务缩略图生成完毕";
			if (failName.size() > 0) {
				StringBuilder sBuilder = new StringBuilder();
				for(int i = 0;i < failName.size();i++) {
					sBuilder.append(failName.get(i) + ",");
				}
				resultInfo = sBuilder.toString().substring(0,sBuilder.toString().length()-1) + "缩略图生成失败";
			}
			return resultInfo;
		}, excutorService).whenComplete((result,r) -> {
			JSONObject jsonObject = new JSONObject();
			if (result.endsWith("生成失败")) {
				jsonObject.put("status", 500);
			}
			jsonObject.put("msg", result);
			jsonObject.put("msgKey", "mapServer");
			webSocket.sendMessage(jsonObject.toJSONString());
		});
	}
	
	/**
	 * 地图数据发布
	 */
	public String publishServerData(DataServerEntity dataServer, String reqIP) {
		if (dataServer.getFeatureInfo() == null) {
			dataServer.setFeatureInfo(featureInfo);
		}
		try {
			dealServerData(dataServer, reqIP);
		} catch (ResourceException e) {
			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR,
					"服务发布失败:" + e.getErrorMsg(), UserUtil.getUserName(),"服务发布", reqIP));
			throw new ResourceException(e.getErrorCode(), e.getErrorMsg());
		} catch (StatusException e) {
			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR,
					"服务发布失败:" + e.getErrorMsg(), UserUtil.getUserName(),"服务发布", reqIP));
			throw new StatusException(e.getErrorCode(), e.getErrorMsg());
		} catch (RuntimeException e) {
			if (e instanceof ResourceAccessException) {
				return "服务发布超时";
			}else {
				throw new RuntimeException(e.getLocalizedMessage(),e.getCause());
			}
		}
		return "OK";
	}

	public LayerBaseInfo cacheLayerInfo(DataServerEntity dataServer, String reqIP) {
		String styleName = null;
		String fileUrl = dataServer.getDataUrl();
		String fileType = dataServer.getFileType();
		if (dataServer.getStyleInfo() != null){
			styleName = dataServer.getStyleInfo().getSldName();
		}
		if (dataServer.getLayerGroupFlag() == 1 || dataServer.getLayerGroupFlag() == 2) {
			List<LayerGroup> list = dataServer.getLayerGroups();
			StringBuilder sb = new StringBuilder();
			StringBuilder sb1 = new StringBuilder();
			StringBuilder sb2 = new StringBuilder();
			int index = 0;
			for (LayerGroup layerGroup : list) {
				String layerName = layerGroup.getLayerName();
				sb.append(layerGroup.getStyleName());
				LayerBaseInfo layerBaseInfo = getLayerBaseInfo(layerName, reqIP);
				sb2.append(layerBaseInfo.getFileType());
				sb1.append(layerBaseInfo.getFilePath());
				if (index < list.size() - 1) {
					sb.append(";");
					sb1.append(";");
					sb2.append(";");
				}
				index++;
			}
			styleName = sb.toString();
			fileUrl = sb1.toString();
			fileType = sb2.toString();
		}
		LayerBaseInfo tiledBaseInfo = new LayerBaseInfo();
		tiledBaseInfo.setServerId(dataServer.getServerId());
		tiledBaseInfo.setStyleName(styleName);
		tiledBaseInfo.setFilePath(fileUrl);
		tiledBaseInfo.setFileType(fileType);
		tiledBaseInfo.setLayerName(dataServer.getServerName());
		tiledBaseInfo.setDataType(dataServer.getDataType());
		if (dataServer.getStyleInfo() != null){
			tiledBaseInfo.setStyleName(dataServer.getStyleInfo().getSldName());
		}
		tiledBaseInfo.setIsLayerGroup(dataServer.getLayerGroupFlag());
		if (dataServer.getFeatureInfo() != null) {
			FeatureInfo featureInfo = dataServer.getFeatureInfo();
			tiledBaseInfo.setResourceFormat(featureInfo.getTileExtension());
			tiledBaseInfo.setResourceSrs(featureInfo.getResourceSrs());
			tiledBaseInfo.setTileHeight(featureInfo.getTileHeight());
			tiledBaseInfo.setTileWidth(featureInfo.getTileWidth());
			tiledBaseInfo.setSupportMaxLevel(featureInfo.getZoomLevel());
			tiledBaseInfo.setResourceBbox(featureInfo.getResourceBbox());
			tiledBaseInfo.setNativaBbox(featureInfo.getNativeLatLon());
			if (DataTypeEnum.getFileCode(fileType) == 8) {
				tiledBaseInfo.setSeaChartSettingJson(featureInfo.getSeaChartSettingJson());
				tiledBaseInfo.setSeaChartIdentifier(featureInfo.getSeaChartIdentifier());
			}
			tiledBaseInfo.setOrigin(featureInfo.getOrigin());
			tiledBaseInfo.setInputTransparentColor(featureInfo.getInputTransParentColor());
			tiledBaseInfo.setOutPutTransparentColor(featureInfo.getOutPutTransParentColor());
		}
		tiledBaseInfo.setSupportProtocal(dataServer.getPublisedProctol());
		tiledBaseInfo.setStatus(dataServer.getStatus());
		tiledBaseInfo.setDataType(dataServer.getDataType());
		tiledBaseInfo.setEncoding(dataServer.getEncoding());
		redisUtils.storeValue(reqIP,dataServer.getServerName(), tiledBaseInfo);
		return  tiledBaseInfo;
	}

	/**
	 * 单独处理单个数据
	 * @param dataServer
	 */
	public void saveMapServerData(DataServerEntity dataServer) {

		// 目前无数据图层概念，暂时图层名与服务名一样
		dataServer.setLayerName(dataServer.getServerName());
		dataServer.setUpdateTime(new Date());
		//如果保存的数据是图层组，将fileType和dataUrl设置为''
		if(dataServer.getLayerGroupFlag() == 1|| dataServer.getLayerGroupFlag() == 2) {
			dataServer.setStatus(ServerStatusEnum.PUBLISHED.getCode());
			dataServer.setFileType("");
			dataServer.setDataUrl("");
			dataServer.setDataType(DataTypeEnum.GROUP.getFileCode());
			
		} else {
			// 设置数据文件类型
			if(FileType.IMAGEMOSAIC.getFilterType().equals(dataServer.getFileType())) {
				dataServer.setStatus(ServerStatusEnum.PROCESSING.getCode());
			} else {
				dataServer.setStatus(ServerStatusEnum.PUBLISHED.getCode());
			}
			dataServer.setDataType(getDataType(dataServer.getFileType()));
			if (!"MBTiles".equalsIgnoreCase(dataServer.getFileType()) && CommonUtil.isTiledServer(dataServer.getFileType())) {
				File file = new File(dataServer.getDataUrl());
				dataServer.setDataUrl(file.getParentFile().getAbsolutePath() + File.separator);
			}
		}


		//判断金字塔缓存是否存在  存在则将状态直接改为已发布
		File overViewFile = new File(dataServer.getDataUrl() + ".ovr");
		if (dataServer.getIsPyamid() == 1 && !overViewFile.exists()) {
			dataServer.setStatus(ServerStatusEnum.OVERVIEWS.getCode());
		} else if (dataServer.getIsPyamid() == 1 && overViewFile.exists()) {
			dataServer.setStatus(ServerStatusEnum.PUBLISHED.getCode());
		}
//		if(dataServer.getIsPyamid() == 1) {
//			dataServer.setStatus(ServerStatusEnum.OVERVIEWS.getCode());
//		}



		//如果协议以分号结尾，则将其去掉后再存储入库
		String serverProtocols = dataServer.getPublisedProctol();
		serverProtocols = serverProtocols.endsWith(";") ? serverProtocols.substring(0, serverProtocols.length() -1) : serverProtocols;
		dataServer.setPublisedProctol(serverProtocols.toUpperCase());
		
		if (!"wmsremote".equalsIgnoreCase(dataServer.getFileType())) {
			try {
				mapServer.saveMapData(dataServer);
			} catch (Exception e) {
				log.error("MapServerServiceImpl saveMapServerData has error:" + ExceptionsUtil.stackTraceToStr(e));
				throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_SAVE, dataServer.getServerName() + "服务实例信息"));
			}
		}
	}

	/**
	 * 单独处理单个rest数据
	 * @param dataServer
	 */
	public void saveRestServerData(DataServerEntity dataServer) {

		// 目前无数据图层概念，暂时图层名与服务名一样
		dataServer.setLayerName(dataServer.getServerName());
		dataServer.setUpdateTime(new Date());
		dataServer.setStatus(ServerStatusEnum.PUBLISHED.getCode());
		dataServer.setDataType(getDataType(dataServer.getFileType()));

		//如果协议以分号结尾，则将其去掉后再存储入库
		String serverProtocols = dataServer.getPublisedProctol();
		serverProtocols = serverProtocols.endsWith(";") ? serverProtocols.substring(0, serverProtocols.length() -1) : serverProtocols;
		dataServer.setPublisedProctol(serverProtocols.toUpperCase());

		try {
			mapServer.saveRestData(dataServer);
		} catch (Exception e) {
			log.error("MapServerServiceImpl saveMapServerData has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_SAVE, dataServer.getServerName() + "服务实例信息"));
		}
	}
	
	private BoundingBox convertBoundingBox(String nativeBboxStr) {
		try {
			if (StringUtils.isBlank(nativeBboxStr)) {
				return new ReferencedEnvelope(-180, 180, -90, 90, CRS.decode("EPSG:4326",true));
			}
			String[] nativeBboxArray = nativeBboxStr.split(";");
			return new ReferencedEnvelope(Double.parseDouble(nativeBboxArray[0]), Double.parseDouble(nativeBboxArray[2]), Double.parseDouble(nativeBboxArray[1]), Double.parseDouble(nativeBboxArray[3]), CRS.decode("EPSG:4326",true));
		} catch (MismatchedDimensionException | FactoryException e) {
			log.error("MapServerServiceImpl convertBoundingBox has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_TRANSFOR, "坐标系Bbox信息"));
		}
	}

	private void dealThumbalUpdate(DataServerEntity dataServerEntity, String reqIP) {
		String fileType = dataServerEntity.getFileType();
		String tileExtension = dataServerEntity.getFeatureInfo().getTileExtension();
		StringBuilder thumPath = new StringBuilder(MapServerUtils.getThumpath())
				.append(File.separator)
				.append(dataServerEntity.getServerName())
				.append(".png");
		if (CommonUtil.isTiledServer(fileType)) {
			byte[] bytesLeft = null;
			byte[] bytesRight = null;
			BufferedImage imageLeft = null;
			BufferedImage imageRight = null;
			if (StringUtils.equalsAnyIgnoreCase(tileExtension, "geojson", "protobuf", "pbf")) {
				PyRenderRequestParam requestParamLeft = new PyRenderRequestParam();
				requestParamLeft.setLayerName(dataServerEntity.getLayerName());
				requestParamLeft.setMapWidth(210);
				requestParamLeft.setMapHeight(150);
				String fileLeft = "TMS".equalsIgnoreCase(dataServerEntity.getFileType())
						? dataServerEntity.getDataUrl() + "0"+ File.separator + "0" + File.separator + "0." + tileExtension
						: dataServerEntity.getDataUrl();
				requestParamLeft.setFileType(dataServerEntity.getFileType());
				requestParamLeft.setFileUrl(fileLeft);
				requestParamLeft.setDataType(dataServerEntity.getDataType());
				requestParamLeft.setFormat("image/png");
				requestParamLeft.setRequestSrs(dataServerEntity.getFeatureInfo().getResourceSrs());
				requestParamLeft.setRequestBbox(dataServerEntity.getFeatureInfo().getResourceBbox().replaceAll(";", ","));
				requestParamLeft.setCache(false);
				requestParamLeft.setDataFormat(tileExtension);
				requestParamLeft.setLeftOrRight(0);
				if (new File(fileLeft).exists()) {
					bytesLeft = renderImage.renderImageByRequestParam(requestParamLeft);
				}
				PyRenderRequestParam requestParamRight = requestParamLeft;
				String fileRight = "TMS".equalsIgnoreCase(dataServerEntity.getFileType())
						? dataServerEntity.getDataUrl() + "0"+ File.separator + "1" + File.separator + "0." + tileExtension
						: dataServerEntity.getDataUrl();
				requestParamRight.setFileUrl(fileRight);
				requestParamRight.setLeftOrRight(1);
				if (new File(fileRight).exists()) {
					bytesRight = renderImage.renderImageByRequestParam(requestParamRight);
				}
			} else {
				TileRequestInfo tileRequestInfo = new TileRequestInfo();
				tileRequestInfo.setGridSet("EPSG:4326");
				tileRequestInfo.setMineType("image/png");
				tileRequestInfo.setTileCol(0);
				tileRequestInfo.setTileRow(0);
				tileRequestInfo.setZoomLevel(0);
				tileRequestInfo.setCreateThumbal(true);
				tileRequestInfo.setLayerName(dataServerEntity.getServerName());
				String nativeBbox = dataServerEntity.getFeatureInfo().getNativeLatLon();
				tileRequestInfo.setBbox(convertBoundingBox(nativeBbox));
				LayerBaseInfo baseInfo = getLayerBaseInfo(dataServerEntity.getServerName(),reqIP);
				TiledInfoStrategy tiledInfoStrategy = factory.getObject();
				tiledInfoStrategy.setTiledInfo(tileRequestInfo,baseInfo);
				bytesLeft = tiledInfoStrategy.getWmtsResource();
				tileRequestInfo.setTileCol(1);
				tiledInfoStrategy.setTiledInfo(tileRequestInfo,baseInfo);
				bytesRight = tiledInfoStrategy.getWmtsResource();
			}
			FileOutputStream fos = null;
			final ByteArrayInputStream bisLeft;
			try {
				if (bytesLeft != null) {
					bisLeft = new ByteArrayInputStream(bytesLeft);
					imageLeft = ImageIO.read(bisLeft);
				}
				final ByteArrayInputStream bisRight;
				if (bytesRight != null) {
					bisRight = new ByteArrayInputStream(bytesRight);
					imageRight = ImageIO.read(bisRight);
				}

				if (imageLeft !=null && imageRight != null) {
					BufferedImage image = ImageUtil.mergeImageByImage(1, "png",imageLeft,imageRight);
					image = ImageUtil.compressImage(image, 210, 140);
					fos = new FileOutputStream(new File(thumPath.toString()));
					ImageIO.write(image, "png", fos);
				}
				if (imageLeft !=null && imageRight == null) {
					imageLeft = ImageUtil.compressImage(imageLeft, 210, 140);
					fos = new FileOutputStream(new File(thumPath.toString()));
					ImageIO.write(imageLeft, "png", fos);
				}
				if (imageLeft ==null && imageRight != null) {
					imageRight = ImageUtil.compressImage(imageRight, 210, 140);
					fos = new FileOutputStream(new File(thumPath.toString()));
					ImageIO.write(imageRight, "png", fos);
				}
			} catch (IOException e) {
				log.error("MapServerServiceImpl dealThumbalUpdate has error:" + ExceptionsUtil.stackTraceToStr(e));
			} finally {
				FileUtil.closeStream(fos);
			}

			if (imageLeft ==null && imageRight == null) {
				return;
			}
		} else {
			WmsRequestParam wmsRequestParam = new WmsRequestParam();
			wmsRequestParam.setLayerName(dataServerEntity.getLayerName());
			wmsRequestParam.setSrs(dataServerEntity.getFeatureInfo().getResourceSrs());
			wmsRequestParam.setBbox(dataServerEntity.getFeatureInfo().getResourceBbox().replaceAll(";", ","));
			wmsRequestParam.setFormat("image/png");
			wmsRequestParam.setWidth(210);
			wmsRequestParam.setHeight(140);
			wmsRequestParam.setTransparent(true);
			wmsRequestParam.setOutputTransparentColor(dataServerEntity.getOutputTransparentColor());
			wmsRequestParam.setInputTransparentColor(dataServerEntity.getInputTransparentColor());
			byte[] wmsImageByte = wmsProtocolService.getWmsImageByte(wmsRequestParam, reqIP);

			try(BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(thumPath.toString()))) {
				outputStream.write(wmsImageByte);
			} catch (IOException e) {
				log.error("MapServerServiceImpl dealThumbalUpdate has error:" + ExceptionsUtil.stackTraceToStr(e));
			}
//				RenderedImageMap webMap = (RenderedImageMap) webMapService.getMap(layerGroupServerService.constructGetMapRequest(dataServerEntity,"image/png",210,140));
//				RenderedImage image = webMap.getImage();
//				fos = new FileOutputStream(new File(new StringBuilder(MapServerUtils.getThumpath()).append(File.separator).append(dataServerEntity.getServerName()).append(".png").toString()));
//				ImageIO.write(image, "png", fos);
//				if (image instanceof RenderedImageTimeDecorator) {
//					ImageUtilities.disposeImage(((RenderedImageTimeDecorator) image).getDelegate());
//				}
//				webMap.dispose();
		}
	}

	private void dealServerData(DataServerEntity dataServer, String reqIP) {
		if (dataServer != null) {
			// 存储storeInfo
			String fileType = dataServer.getFileType().equalsIgnoreCase("PostGis") ? "Shape" : dataServer.getFileType();
			StoreInfo info = getStoreInfoByDataType(fileType, dataServer.getDataUrl(), dataServer.getEncoding());
			info = dealStoreInfo(info);
			// 构建图层信息
			LayerInfo layerInfo = getLayerInfo(getDefaultResource(dataServer,info),dataServer,reqIP,false);

			// 原有数据发布
			publishedData(layerInfo,dataServer);
			// 如果rasterColor不为空，则存储其设置值
			if (!"wmsremote".equals(fileType) && dataServer.getRasterColor() != null) {
				dataServer.getRasterColor().setServerId(dataServer.getServerId());
				mapServer.saveRasterInfo(dataServer.getRasterColor());
			}
			if (!"wmsremote".equals(fileType)) {
				FeatureInfo feature = dataServer.getFeatureInfo();
				int zoomLevel = 0;
				if (feature != null) {
					zoomLevel = feature.getZoomLevel();
				} else if (featureInfo != null) {
					zoomLevel = featureInfo.getZoomLevel();
				}
				zoomCache.put(dataServer.getLayerName(), zoomLevel == 0 ? 21 : zoomLevel);
				saveFeatureInfo(dataServer, layerInfo);
				//保存存储数据和图层数据
//				StoreLayerInfo storeLayerInfo = new StoreLayerInfo(storeInfoId, dataServer.getServerId(),
//						layerInfo.getId());
			}
			//将服务与数据源各自的id存储到中间表中，此处存在一个数据源对应多个服务的情况
			if (SystemServiceImpl.isPostGIS(dataServer.getDataUrl())) {
				PostGisConnectEntity entity = mapServer.queryPostGisBySourceName(dataServer.getDataUrl().split(";")[0]);
				PostgisServerInfo psInfo = new PostgisServerInfo(dataServer.getServerId(), entity.getDbId());
				mapServer.savePostgisServerInfo(psInfo);
			}

			// 处理金字塔缓存业务，如果已经存在缓存数据就不再生成
//			File overViewFile = new File(dataServer.getDataUrl() + ".ovr");
//			if (dataServer.getIsPyamid() == 1 && !overViewFile.exists()) {
//				Future future = statsticService.buildOverviews(dataServer);
//				overViewsCache.put(dataServer.getServerName(), future);
//			} else if (dataServer.getIsPyamid() == 1 && overViewFile.exists()) {
//				// 当发布服务勾选生成金字塔，但此服务已有缓存文件时不再生成金字塔缓存，修改服务状态
//				mapServer.updateStatusAndOverview(dataServer.getServerId());
//
//				// 修改服务状态，否则预览时服务状态不是已发布，无法预览
//				dataServer.setStatus(ServerStatusEnum.PUBLISHED.getCode());
//				JSONObject jsonObject = new JSONObject();
//				jsonObject.put("status", 200);
//				jsonObject.put("msgKey", "mapServer");
//				jsonObject.put("msg", dataServer.getServerName() + "服务已存在金字塔缓存!");
//				webSocket.sendMessage(jsonObject.toJSONString());
//			}

			// 发布成功后将图层保存在缓存中
			cacheLayers.add(dataServer.getServerName());
			//处理自动切图功能
			if (dataServer.getOpenTiled() == 1 && dataServer.getServerShow() == 0) {
				excutorService.submit(() -> {
					boolean isServerSaveOk = false;
					try {
						//切图临时解决方案
						while(!isServerSaveOk) {
							RequestQueryCondition condition = new RequestQueryCondition();
							condition.setServerName(dataServer.getLayerName());
							List<DataServerEntity> servers = mapServer.queryServerData(condition);
							if (servers != null && !servers.isEmpty()) {
								isServerSaveOk = true;
							} else {
								Thread.sleep(1000);
							}
						}
						renderTileService.seedTaskByLayerName(dataServer.getLayerName(), dataServer.getServerId(), reqIP, dataServer.getIsPyamid());
					} catch (Exception e) {
						if(ExceptionsUtil.isPytcError(ExceptionsUtil.stackTraceToStr(e))) {
							systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, ExceptionsUtil.formatInfo(ExceptionsUtil.PYTC_SERVER_ERROR,
									dataServer.getServerName() + "服务切片信息"), UserUtil.getUserName(),"Pytc服务", reqIP));
						}
						log.error("MapServerServiceImpl dealServerData has error:" + ExceptionsUtil.stackTraceToStr(e));
						throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.STATUS_PYTC, dataServer.getServerName() + "服务切片"));
					}
				});
			}
		}
	}

	private StoreInfo dealStoreInfo(StoreInfo info) {
		if (info instanceof DataStoreInfo) {
			info = dealDataStoreInfo((DataStoreInfo) info);
		} else if (info instanceof CoverageStoreInfo) {
			info = dealCoverageStoreInfo((CoverageStoreInfo) info);
		}
//		else if (info instanceof TiledStoreInfo) {
//			info = dealTiledStoreInfo((TiledStoreInfo) info);
//		}
		else if (info instanceof WMSStoreInfo) {
			info = dealWmsStoreInfo((WMSStoreInfo) info);
		}
		return info;
	}

	private void closeInfo(StoreInfo info, LayerInfo layerInfo) {
		if (info != null) {
			catalog.remove(info);
		}
	}

	public void saveFeatureInfo(DataServerEntity dataServer, LayerInfo layerInfo) {
		FeatureInfo featureInfo = getFeatureInfo(dataServer, layerInfo);
		featureInfo.setSeaChartSettingJson(JSON.toJSONString(dataServer.getSeaChartSetting()));
		featureInfo.setInputTransParentColor(dataServer.getInputTransparentColor());
		featureInfo.setOutPutTransParentColor(dataServer.getOutputTransparentColor());
		dataServer.setFeatureInfo(featureInfo);
		mapServer.saveFeatureData(featureInfo);
	}

	// 数据存放当前数据4326的坐标系
	private FeatureInfo getFeatureInfo(DataServerEntity dataServer, LayerInfo layerInfo) {
		FeatureInfo featureInfo = dataServer.getFeatureInfo();
		if (featureInfo == null) featureInfo = new FeatureInfo();
		featureInfo.setServerId(dataServer.getServerId());
		if (layerInfo != null) {
			ResourceInfo reource = layerInfo.getResource();
			if (reource.getProjectionPolicy() != null) {
				featureInfo.setProjectionPolicy(reource.getProjectionPolicy().name());
			}
		}
		featureInfo.setProjectionPolicy("FORCE_DECLARED");
		return featureInfo;
	}

	/**
	 * 根据文件类型获取数据类型
	 *
	 * @param fileType
	 * @return
	 */
	private int getDataType(String fileType) {
		int dataType = 0;
		switch (fileType) {
			case "Shape":
			case "PostGIS":
			case "kml":
				dataType = DataTypeEnum.VECTOR.getFileCode();
				break;
			case "GeoTIFF":
            case "ImageMosaic":
			case "ERDASImg":
				dataType = DataTypeEnum.RASTER.getFileCode();
				break;
			case "TDB":
			case "TMS":
			case "FlatGis":
			case "MBTiles":
				dataType = DataTypeEnum.TILED.getFileCode();
				break;
			case "wms":
				dataType = DataTypeEnum.WMS.getFileCode();
				break;
			case "rest":
				dataType = DataTypeEnum.REST.getFileCode();
				break;
			default:
				break;
		}
		return dataType;
	}

	/**
	 * huoqu
	 *
	 * @param
	 * @return
	 * @throws IOException
	 */
	private String getDefaultResource(DataServerEntity dataServer,StoreInfo info) {
		// 根据文件路径获取当前的文件信息
		String url = dataServer.getDataUrl();
		String dataType = dataServer.getFileType();
		if (dataType.equalsIgnoreCase("PostGis")) {
			return url;
		}
		File file = new File(url);
		//如果是远程服务，则直接适配远程图层
		if (!file.exists() && (!url.startsWith("http://") && !url.startsWith("https://"))) {
			return null;
		}
		if (DataTypeEnum.TILED.getFileCode() == dataServer.getDataType()) {
			return dataServer.getLayerName();
		}
		String localPart = file.getName();
		if (localPart.indexOf(".") > -1) {
			localPart = localPart.substring(0,localPart.lastIndexOf("."));
			//针对解决.shp.shp矢量文件无法从catalog里获取图层信息问题
			if (localPart.contains(".")){
				localPart.replaceAll("\\.","");
			}
		}
		if ("wmsremote".equalsIgnoreCase(dataType)) {
			if (info instanceof WMSStoreInfo) {
				List<Layer> list = null;
				try {
					list = ((WMSStoreInfo) info).getWebMapServer(null).getCapabilities().getLayerList();
				} catch (IOException e) {
					log.error("MapServerServiceImpl getDefaultResource has error:" + ExceptionsUtil.stackTraceToStr(e));
					throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, "远端WMS " + dataServer.getServerName() + "服务图层信息"));
				}
				for(Layer l : list) {
					if(l.getName() == null) {
						continue;
					}
					localPart = l.getName();
				}
			}
		}
		return localPart;
	}

	/**
	 * 该方法用于校验服务在localhost场景下，重发服务时服务原始数据是否存在，防止数据文件已经不存在，但是仍然发布成功情况
	 * @param filePath
	 */
	private void checkServerDataStatus(String filePath) {
		File tempFile = new File(filePath);
		if(!tempFile.exists()) {
			if (checkDBServerDataStatus(filePath)==false) {
				throw new ResourceException(ExceptionsUtil.ERROR_500, ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_NOT_FOUND, filePath));
			}
		}
	}

	private boolean checkDBServerDataStatus(String db){
		int count = 0;
		boolean flag = false;
		//jdbc校验
		String[] dbInfo = db.split(";");
		String sourceName = dbInfo[0];
		String tableName = dbInfo[1];
		Connection con = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		PostGisConnectEntity entity = mapServer.queryPostGisBySourceName(sourceName);
		if (entity.getDbStatus()==1) {
			String url = "jdbc:postgresql://" + entity.getDatabaseIp() + ":" + entity.getPort() + "/" + entity.getDatabaseName();
			String username = entity.getUserName();
			String passwordEncoder = entity.getPassword();
			byte[] bytes = passwordEncoder.getBytes();
			Base64.Decoder decoder = Base64.getDecoder();
			try {
				String password = new String(decoder.decode(bytes),"UTF-8");
				Class.forName("org.postgresql.Driver");
				con = DriverManager.getConnection(url, username, password);
				preparedStatement = con.prepareStatement(sql);
				preparedStatement.setString(1, tableName);
				resultSet = preparedStatement.executeQuery();
				while (resultSet.next()) {
					count = resultSet.getInt("count");
				}
				if (count==1){
					flag = true;
				}
			} catch (Exception e) {
				log.error("MapServerServiceImpl checkServerDataStatus has error:" + ExceptionsUtil.stackTraceToStr(e));
//				throw new ResourceException(ExceptionsUtil.ERROR_500, ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_NOT_FOUND, "SQLite驱动类"));
                return false;
			} finally {
			    if (resultSet != null) {
                    try {
                        resultSet.close();
                    } catch (SQLException e) {
                        log.error("MapServerServiceImpl checkDBServerDataStatus has error:" + ExceptionsUtil.stackTraceToStr(e));
                    }
                    try {
                        preparedStatement.close();
                    } catch (SQLException e) {
                        log.error("MapServerServiceImpl checkDBServerDataStatus has error:" + ExceptionsUtil.stackTraceToStr(e));
                    }
                    //最后关闭connection
                    try {
                        con.close();
                    } catch (SQLException e) {
                        log.error("MapServerServiceImpl checkDBServerDataStatus has error:" + ExceptionsUtil.stackTraceToStr(e));
                    }
                }
			}
		}
		return flag;
	}

	@Override
	public List<DataServerEntity> queryServerDataList(RequestQueryCondition queryCondition,String reqIP) {
		List<DataServerEntity> serverDatas = mapServer.queryServerData(queryCondition);
		systemService.checkPostGISList();
		serverDatas.stream().forEach((dataServer) -> {
			if (dataServer.getFeatureInfo() == null) {
				return;
			}
			//将已经发布但是地图数据不存在的服务置为异常状态
			if(!SystemServiceImpl.isPostGIS(dataServer.getDataUrl())) {
				File file = new File(dataServer.getDataUrl());
				if (dataServer.getLayerGroupFlag() != 1 && dataServer.getLayerGroupFlag() != 2 && DataTypeEnum.ONLINE.getFileCode() != dataServer.getDataType()) {
					if (!file.exists()) {
						//修改不存在的服务状态为异常
						dataServer.setStatus(ServerStatusEnum.ABNORMAL.getCode());
						if (!isContainsInAbnormal(dataServer)) {
							abnormalServer.put(dataServer.getServerName(), dataServer);
						}
						systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, ExceptionsUtil.formatInfo(ExceptionsUtil.FILE_LOST,
								dataServer.getServerName() + "服务"), UserUtil.getUserName(),"文件管理", reqIP));
					} else {
						// 如果文件存在，并且该服务之前因为路径而异常，则将状态修改回去
						if (isContainsInAbnormal(dataServer)) {
							dataServer.setStatus(ServerStatusEnum.PUBLISHED.getCode());
							abnormalServer.remove(dataServer.getServerName());
						}
					}
				}
			} else {
				if (!checkDBServerDataStatus(dataServer.getDataUrl())) {
					dataServer.setStatus(ServerStatusEnum.ABNORMAL.getCode());
					if (!isContainsInAbnormal(dataServer)) {
						abnormalServer.put(dataServer.getServerName(), dataServer);
					}
					systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, ExceptionsUtil.formatInfo(ExceptionsUtil.DATABASE_TABLE_LOST,
							dataServer.getServerName() + "服务"), UserUtil.getUserName(), "数据库文件异常", reqIP));
				}
			}
			dataServer.setServerProtocols(getMapServerFormat(dataServer));

			// 当是第一次访问且服务状态为5(金字塔缓存生成中)，判定为服务异常重启
			if (isFirst && dataServer.getStatus() == 5) {
				Future future = statsticService.buildOverviews(dataServer);
				overViewsCache.put(dataServer.getServerName(), future);
			}
		});
		serverDatas.parallelStream().forEach((dataServer) -> {
			//将已经发布但是地图数据不存在的服务置为异常状态
			boolean serverIsOk = true;
			if (dataServer.getLayerGroupFlag() == 1 || dataServer.getLayerGroupFlag() == 2) {
				List<LayerGroup> groups = dataServer.getLayerGroups();
				if (groups != null && groups.size() > 0) {
					for (LayerGroup flg : groups) {
						String serverName = flg.getLayerName();
						if (abnormalServer.containsKey(serverName)) {
							if (serverIsOk != false) {
								serverIsOk = false;
							}
						}
					}
					if (!serverIsOk) {
						dataServer.setStatus(ServerStatusEnum.ABNORMAL.getCode());
					}
				}
			}
		});
		if (queryCondition != null) {
			if (queryCondition.getServerStatus() != -2) {
				if (queryCondition.getServerStatus() == ServerStatusEnum.ABNORMAL.getCode()) {
					clearSameServer(serverDatas);
					serverDatas.addAll(abnormalServer.values());
				} else {
					Iterator<DataServerEntity> iterator = serverDatas.iterator();
					while (iterator.hasNext()) {
						DataServerEntity entity = iterator.next();
						if (isContainsInAbnormal(entity)) {
							iterator.remove();
						}
					}
				}
			}
			//TODO 	代码解释
			if (!StringUtils.isBlank(queryCondition.getServerName())) {
				clearServerByCondition(serverDatas, "SERVER:" + queryCondition.getServerName());
			} else if (!StringUtils.isBlank(queryCondition.getPublishor())) {
				clearServerByCondition(serverDatas, "PUBLISHER:" + queryCondition.getPublishor());
			}
			if (!StringUtils.isBlank(queryCondition.getServerProtocol())) {
				clearServerByCondition(serverDatas, "PROTOCAL:" + queryCondition.getServerProtocol());
			}
		}
		isFirst = false;
		return serverDatas;
	}
	public List<DataServerEntity> queryRestDataList(RequestQueryCondition queryCondition,String reqIP) {
		List<DataServerEntity> serverDatas = mapServer.queryRestData(queryCondition);
		systemService.checkPostGISList();
		serverDatas.stream().forEach((dataServer) -> {
			if (dataServer.getFeatureInfo() == null) {
				return;
			}
			//将已经发布但是地图数据不存在的服务置为异常状态
			if(!SystemServiceImpl.isPostGIS(dataServer.getDataUrl())) {
				File file = new File(dataServer.getDataUrl());
				if (dataServer.getLayerGroupFlag() != 1 && dataServer.getLayerGroupFlag() != 2 && DataTypeEnum.ONLINE.getFileCode() != dataServer.getDataType()) {
					if (!file.exists()) {
						//修改不存在的服务状态为异常
						dataServer.setStatus(ServerStatusEnum.ABNORMAL.getCode());
						if (!isContainsInAbnormal(dataServer)) {
							abnormalServer.put(dataServer.getServerName(), dataServer);
						}
						systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, ExceptionsUtil.formatInfo(ExceptionsUtil.FILE_LOST,
								dataServer.getServerName() + "服务"), UserUtil.getUserName(),"文件管理", reqIP));
					} else {
						// 如果文件存在，并且该服务之前因为路径而异常，则将状态修改回去
						if (isContainsInAbnormal(dataServer)) {
							dataServer.setStatus(ServerStatusEnum.PUBLISHED.getCode());
							abnormalServer.remove(dataServer.getServerName());
						}
					}
				}
			} else {
				if (!checkDBServerDataStatus(dataServer.getDataUrl())) {
					dataServer.setStatus(ServerStatusEnum.ABNORMAL.getCode());
					if (!isContainsInAbnormal(dataServer)) {
						abnormalServer.put(dataServer.getServerName(), dataServer);
					}
					systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, ExceptionsUtil.formatInfo(ExceptionsUtil.DATABASE_TABLE_LOST,
							dataServer.getServerName() + "服务"), UserUtil.getUserName(), "数据库文件异常", reqIP));
				}
			}
			dataServer.setServerProtocols(getMapServerFormat(dataServer));

		});
//		serverDatas.parallelStream().forEach((dataServer) -> {
//			//将已经发布但是地图数据不存在的服务置为异常状态
//			boolean serverIsOk = true;
//			if (dataServer.getLayerGroupFlag() == 1 || dataServer.getLayerGroupFlag() == 2) {
//				List<LayerGroup> groups = dataServer.getLayerGroups();
//				if (groups != null && groups.size() > 0) {
//					for (LayerGroup flg : groups) {
//						String serverName = flg.getLayerName();
//						if (abnormalRestServer.containsKey(serverName)) {
//							if (serverIsOk != false) {
//								serverIsOk = false;
//							}
//						}
//					}
//					if (!serverIsOk) {
//						dataServer.setStatus(ServerStatusEnum.ABNORMAL.getCode());
//					}
//				}
//			}
//		});
//		if (queryCondition != null) {
//			if (queryCondition.getServerStatus() != -2) {
//				if (queryCondition.getServerStatus() == ServerStatusEnum.ABNORMAL.getCode()) {
//					clearSameServer(serverDatas);
//					serverDatas.addAll(abnormalRestServer.values());
//				} else {
//					Iterator<DataServerEntity> iterator = serverDatas.iterator();
//					while (iterator.hasNext()) {
//						DataServerEntity entity = iterator.next();
//						if (isContainsInAbnormal(entity)) {
//							iterator.remove();
//						}
//					}
//				}
//			}
//			//TODO 	代码解释
//			if (!StringUtils.isBlank(queryCondition.getServerName())) {
//				clearServerByCondition(serverDatas, "SERVER:" + queryCondition.getServerName());
//			} else if (!StringUtils.isBlank(queryCondition.getPublishor())) {
//				clearServerByCondition(serverDatas, "PUBLISHER:" + queryCondition.getPublishor());
//			}
//			if (!StringUtils.isBlank(queryCondition.getServerProtocol())) {
//				clearServerByCondition(serverDatas, "PROTOCAL:" + queryCondition.getServerProtocol());
//			}
//		}
		isFirst = false;
		return serverDatas;
	}

	private void clearServerByCondition(List<DataServerEntity> entity, String condition) {
		Iterator<DataServerEntity> iterator = entity.iterator();
		while (iterator.hasNext()) {
			DataServerEntity dataServerEntity = iterator.next();
			if (condition.split(":")[0].equals("SERVER")) {
				String serverName = condition.split(":")[1].toUpperCase(Locale.US);
				String name = dataServerEntity.getServerName().toUpperCase(Locale.US);
				if (!name.contains(serverName)) {
					iterator.remove();
				}
			} else if (condition.split(":")[0].equals("PUBLISHER")) {
				String user = condition.split(":")[1];
				String userName = dataServerEntity.getPublishor();
				if (!userName.contains(user)) {
					iterator.remove();
				}
			} else if (condition.split(":")[0].equals("PROTOCAL")) {
				String protocal = condition.split(":")[1];
				String[] protocals = dataServerEntity.getPublisedProctol().split(";");
				boolean isContains = false;
				for (String version : protocals) {
					if (protocal.equalsIgnoreCase(version)) {
						isContains = true;
					}
				}
				if (!isContains) {
					iterator.remove();
				}
			}
		}
	}

	private void clearSameServer(List<DataServerEntity> entity) {
		Iterator<DataServerEntity> iterator = entity.iterator();
		while (iterator.hasNext()) {
			DataServerEntity dataServerEntity = iterator.next();
			if (isContainsInAbnormal(dataServerEntity)) {
				iterator.remove();
			}

		}
	}

	private boolean isContainsInAbnormal(DataServerEntity entity) {
		Enumeration<String> iterator = abnormalServer.keys();
		while (iterator.hasMoreElements()) {
			String serverName = iterator.nextElement();
			if (serverName.equals(entity.getServerName())) {
				return true;
			}
		}
		return false;
	}

	private void publishedData(LayerInfo layerInfo,DataServerEntity dataServer) {
		if (layerInfo != null) {
			// 设置图层名称
			layerInfo.setName(dataServer.getLayerName());
			// 读取文件获取当前的bbox
			ResourceInfo resourceInfo = layerInfo.getResource();
			try {
				if (resourceInfo.getNativeBoundingBox() == null) {
					FeatureInfo featureInfo = dataServer.getFeatureInfo();
					if (featureInfo == null) {
						return;
					}
					String[] nativeArray = featureInfo.getNativeLatLon().split(";");
					String[] resourceArray = featureInfo.getResourceBbox().split(";");
					ReferencedEnvelope nativeEnvelope = new ReferencedEnvelope(Double.parseDouble(nativeArray[0]),
							Double.parseDouble(nativeArray[2]), Double.parseDouble(nativeArray[1]),
							Double.parseDouble(nativeArray[3]), CRS.decode("EPSG:4326", true));
					ReferencedEnvelope resourceEnvelope = new ReferencedEnvelope(Double.parseDouble(resourceArray[0]),
							Double.parseDouble(resourceArray[2]), Double.parseDouble(resourceArray[1]),
							Double.parseDouble(resourceArray[3]), CRS.decode(featureInfo.getResourceSrs()));
					resourceInfo.setLatLonBoundingBox(resourceEnvelope);
					resourceInfo.setNativeBoundingBox(nativeEnvelope);
				}
			} catch (FactoryException e) {
				log.error("MapServerServiceImpl publishedData has error:" + ExceptionsUtil.stackTraceToStr(e));
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, dataServer.getServerName() + "服务坐标系信息"));
			}
			try {
				if (resourceInfo instanceof CoverageInfo) {
					CoverageInfo cinfo = (CoverageInfo) resourceInfo;
					GridCoverage2DReader reader = (GridCoverage2DReader) cinfo.getGridCoverageReader(null,
							GeoTools.getDefaultHints());
					final ReferencedEnvelope bounds = new ReferencedEnvelope(reader.getOriginalEnvelope());
					final ProjectionPolicy projectionPolicy = resourceInfo.getProjectionPolicy();
					if (projectionPolicy != ProjectionPolicy.NONE && bounds != null) {
						final GridGeometry grid = cinfo.getGrid();
						cinfo.setGrid(new GridGeometry2D(grid.getGridRange(), grid.getGridToCRS(), resourceInfo.getCRS() == null ? resourceInfo.getNativeCRS() : resourceInfo.getCRS()));
					}
				}
			} catch (IOException e) {
				log.error("MapServerServiceImpl publishedData has error:" + ExceptionsUtil.stackTraceToStr(e));
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, dataServer.getServerName() + "服务GridCoverage2DReader信息"));
			}

			if (DataTypeEnum.TILED.getFileCode() != dataServer.getDataType()) {
				//如果当前有相同资源未删除
				NamespaceInfo namespace = resourceInfo.getNamespace();
				ResourceInfo existing = catalog.getResourceByName( namespace, resourceInfo.getName(), ResourceInfo.class);
				if ( existing != null) {
					if (existing.getStore() != null) {
						catalog.remove(existing.getStore());
						if (catalog.getLayers(existing) != null && !catalog.getLayers(existing).isEmpty()) {
							catalog.remove(catalog.getLayers(existing).get(0));
						}
						catalog.remove(existing);
					}
				}
				catalog.add(resourceInfo);
				catalog.add(layerInfo);
			}
		}
	}

	private StoreInfo dealDataStoreInfo(DataStoreInfo info) {
		DataStoreInfo savedStore = catalog.getResourcePool().clone(info, true);
		catalog.validate(savedStore, true).throwIfInvalid();
		savedStore = catalog.getResourcePool().clone(info, false);
		catalog.add(savedStore);
		storeInfoId = savedStore.getId();
		return savedStore;
	}

	/**
	 * 处理栅格数据存储信息
	 *
	 * @param info
	 */
	private StoreInfo dealCoverageStoreInfo(CoverageStoreInfo info) {
		CoverageStoreInfo expandedStore = catalog.getResourcePool().clone(info, true);
		CoverageStoreInfo savedStore = catalog.getFactory().createCoverageStore();
		catalog.validate(expandedStore, false).throwIfInvalid();
		savedStore = catalog.getResourcePool().clone(info, false);
		catalog.save(savedStore);
		storeInfoId = savedStore.getId();
		return savedStore;
	}

	private StoreInfo dealWmsStoreInfo(WMSStoreInfo info) {
		WMSStoreInfo expandedStore = catalog.getResourcePool().clone(info, true);
		WMSStoreInfo savedStore = catalog.getFactory().createWebMapServer();
		catalog.validate(expandedStore, false).throwIfInvalid();
		savedStore = catalog.getResourcePool().clone(info, false);
		catalog.save(savedStore);
		storeInfoId = savedStore.getId();
		return savedStore;
	}


//	private StoreInfo dealTiledStoreInfo(TiledStoreInfo info) {
//		TiledStoreInfo expandedStore = catalog.getResourcePool().clone(info);
//		TiledStoreInfo savedStore = catalog.getFactory().createTiledStore();
//		catalog.validate(expandedStore, false).throwIfInvalid();
//		savedStore = catalog.getResourcePool().clone(info);
//		catalog.save(savedStore);
//		storeInfoId = savedStore.getId();
//		return savedStore;
//	}

	/**
	 * 根据数据类型获取存储对象信息
	 *
	 * @param fileType 数据类型
	 * @param fileUrl 文件url
	 * @param encoding 编码方式
	 * @return
	 */
	private StoreInfo getStoreInfoByDataType(String fileType, String fileUrl, String encoding) {
		StoreInfo info = null;
		switch (fileType) {
			case "Shape":
			case "kml":
			case "PostGIS":
				info = getShapeStoreInfo(fileUrl, encoding);
				break;
			case "GeoTIFF":
			case "ImageMosaic":
			case "ERDASImg":
				info = getCoverageStoreInfo(fileUrl, fileType);
				break;
//			case "TDB":
//			case "TMS":
//			case "FlatGis":
//			case "MBTiles":
//				info = getTiledStoreInfo(fileUrl, fileType);
//				break;
			case "wmsremote":
				info = constructWMSStoreInfo(fileUrl);
				break;
//			case "rest":
//				info = constructWMSStoreInfo(fileUrl);
//				break;
			default:
				break;
		}
		return info;
	}

	/**
	 * 获取texx数据的存储信息
	 *
	 * @param fileUrl
	 * @return
	 */
//	private TiledStoreInfo getTiledStoreInfo(String fileUrl, String dataType) {
//		TiledStoreInfoImpl store = new TiledStoreInfoImpl(catalog);
//		store.setName(MapServerUtils.createIdentiCode());
//		store.setType(dataType.toLowerCase());
//		store.setWorkspace(MapServerUtils.getDefaultWorkspace());
//		store.setEnabled(true);
//		store.setUrl(fileUrl);
//		return store;
//	}

	/**
	 * 获取tif数据的存储信息
	 *
	 * @param fileUrl
	 * @return
	 */
	private CoverageStoreInfo getCoverageStoreInfo(String fileUrl, String type) {
		CoverageStoreInfoImpl store = new CoverageStoreInfoImpl(catalog);
		store.setName(MapServerUtils.createIdentiCode());
		store.setType(type);
		store.setWorkspace(MapServerUtils.getDefaultWorkspace());
		store.setEnabled(true);
		store.setURL(fileUrl);
		return store;
	}

	/**
	 * 获取矢量数据信息（shape数据）
	 *
	 * @return
	 */
	private DataStoreInfo getShapeStoreInfo(String fileUrl, String encoding) {
		DataStoreInfoImpl store = new DataStoreInfoImpl(catalog);
		// 创建默认的连接信息
		Map<String, Serializable> connectionParameters = new HashMap<String, Serializable>();
		store.setWorkspace(MapServerUtils.getDefaultWorkspace());
		store.setEnabled(true);
		if (fileUrl.contains(";")) {
			PostGisConnectEntity entity = mapServer.queryPostGisBySourceName(fileUrl.split(";")[0]);
			store.setType("PostGIS");
			store.setName(MapServerUtils.createIdentiCode());
			connectionParameters.put("host", entity.getDatabaseIp());
			connectionParameters.put("port", entity.getPort());
			connectionParameters.put("database", entity.getDatabaseName());
			connectionParameters.put("user", entity.getUserName());
			connectionParameters.put("passwd", PostGisStoreUtils.getDecodePass(entity.getPassword()));
			connectionParameters.put("dbtype", "postgis");
			WorkspaceInfo wsInfo = store.getWorkspace();
			NamespaceInfo nsInfo = catalog.getNamespaceByPrefix(wsInfo.getName());
			if (nsInfo == null) {
				nsInfo = catalog.getDefaultNamespace();
			}
			if (nsInfo != null) {
				connectionParameters.put("namespace", nsInfo.getURI());
			}
		} else {
			store.setName(MapServerUtils.createIdentiCode());
			store.setType("Shapefile");
			connectionParameters.put("charset", encoding);
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
		}
		store.setConnectionParameters(connectionParameters);
		return store;
	}

	/**
	 * 获取远程wms服务的操作
	 * @return
	 */
	private WMSStoreInfo constructWMSStoreInfo(String fileUrl) {
		WMSStoreInfoImpl store = new WMSStoreInfoImpl(catalog);
		store.setName(MapServerUtils.createIdentiCode());
		store.setType("WMS");
		store.setWorkspace(MapServerUtils.getDefaultWorkspace());
		if (fileUrl.startsWith("http://")) {
			store.setCapabilitiesURL(fileUrl);
		}else {
			store.setCapabilitiesURL("http://"+fileUrl);
		}
		store.setMaxConnections(6);    //最大链接数量
		store.setConnectTimeout(30);   //连接超时
		store.setReadTimeout(60);      //读超时
		store.setUseConnectionPooling(true);  //使用连接池
		store.setEnabled(true);
		return store;
	}

	/**
	 * 获取已选择的存储id
	 *
	 * @return
	 */
	private String getSelectedStoreId() {
		return storeInfoId;
	}

	/**
	 * 获取图层信息
	 *
	 * @return
	 */
	private LayerInfo getLayerInfo(String name, DataServerEntity dataServer, String reqIp, boolean isEdit) {
		LayerInfo layerInfo = null;
		try {
			if (DataTypeEnum.TILED.getFileCode() == dataServer.getDataType()) {
				return layerInfo;
			}
			StoreInfo info = catalog.getStore(getSelectedStoreId(), StoreInfo.class);
			CatalogBuilder builder = new CatalogBuilder(catalog);
			builder.setStore(info);
			if (info instanceof CoverageStoreInfo) {
				CoverageStoreInfo storeInfo = (CoverageStoreInfo) info;
				GridCoverage2DReader reader =
		                (GridCoverage2DReader)
		                        catalog.getResourcePool()
		                                .getGridCoverageReader(storeInfo, GeoTools.getDefaultHints());
				
				Map<String,Serializable> customParam = null;
				if (StringUtils.isNotBlank(dataServer.getInputTransparentColor())) {
					customParam = new HashMap<String,Serializable>();
					customParam.put("InputTransparentColor", dataServer.getInputTransparentColor());
				}
				
				if (StringUtils.isNotBlank(dataServer.getOutputTransparentColor())) {
					if (customParam == null) {
						customParam = new HashMap<String,Serializable>();
					}
					customParam.put("OutputTransparentColor", dataServer.getInputTransparentColor());
				}
				CoverageInfo ci = builder.buildCoverage(reader,name,customParam);
				if (customParam != null) {
					ci.getParameters().putAll(customParam);
				}
				layerInfo = builder.buildLayer(ci);
			} else if (info instanceof DataStoreInfo) {
				FeatureTypeInfo fti = null;
				if (name.contains(";")) {
					fti = builder.buildFeatureType(new NameImpl(name.split(";")[1]));
				} else {
					fti = builder.buildFeatureType(new NameImpl(name));
				}
				layerInfo = builder.buildLayer(fti);
			}
//			else if (info instanceof TiledStoreInfo) {
//				TiledInfo tileInfo = buildTiledLayer(name, dataServer);
////				layerInfo = builder.buildLayer(tileInfo);
//				layerInfo = buildTileLayer(tileInfo);
//			}
			else if (info instanceof WMSStoreInfo) {
				WMSLayerInfo wli = builder.buildWMSLayer(name);
				layerInfo = builder.buildLayer(wli);
			}
			//设置样式
			String fileType = dataServer.getFileType().equalsIgnoreCase("PostGis") ? "Shape" : dataServer.getFileType();
			if (!"wmsremote".equals(fileType) && DataTypeEnum.TILED.getFileCode() != dataServer.getDataType()) {
				resetStyleInfo(dataServer, layerInfo,fileType,reqIp,isEdit);
			}
			return layerInfo;
		} catch (Exception e) {
			log.error("MapServerServiceImpl getLayerInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_CREATE, dataServer.getServerName() + "服务图层信息"));
		}
	}

//	private LayerInfo buildTileLayer(TiledInfo tileInfo) {
//		LayerInfo layer = new LayerInfoImpl();
//		layer.setResource((ResourceInfo)tileInfo);
//		layer.setName(tileInfo.getName());
//		layer.setEnabled(true);
//		// setup the layer type
//		if (layer.getResource() instanceof FeatureTypeInfo) {
//			layer.setType(PublishedType.VECTOR);
//		} else if (layer.getResource() instanceof CoverageInfo) {
//			layer.setType(PublishedType.RASTER);
//		} else if (layer.getResource() instanceof WMTSLayerInfo) {
//			layer.setType(PublishedType.WMTS);
//		} else if (layer.getResource() instanceof WMSLayerInfo) {
//			layer.setType(PublishedType.WMS);
//		} else if (layer.getResource() instanceof TiledInfo) {
//			layer.setType(PublishedType.TILED);
//		}
//		return layer;
//	}

	private void resetStyleInfo(DataServerEntity dataServer, LayerInfo layerInfo, String fileType, String reqIP, boolean isEdit) {
		//如果dataServer有样式，则直接处理
		final StyleInfo defaultStyle;
		org.geoserver.catalog.StyleInfo styleInfo;
		if (dataServer.getStyleInfo() != null && StringUtils.isNotBlank(dataServer.getStyleInfo().getStyleId())) {
			defaultStyle = dataServer.getStyleInfo();
		} else {
			List<StyleInfo> styleInfos = styleInfoServiceImpl.queryStyleInfoList(dataServer.getFileType(),dataServer.getDataUrl(),reqIP);
			styleInfo = layerInfo.getDefaultStyle();
			if (styleInfo == null) {
				defaultStyle = styleInfoServiceImpl.getDataDefaultStyle(styleInfos, fileType,null);
			} else {
				defaultStyle = styleInfoServiceImpl.getDataDefaultStyle(styleInfos, fileType, styleInfo.getName());
			}
		}
		styleInfo = styleInfoServiceImpl.getDefaultStyle(fileType, defaultStyle, reqIP);
		layerInfo.setDefaultStyle(styleInfo);
		if (!isEdit) {
			defaultStyle.setServerId(dataServer.getServerId());
			dataServer.setStyleInfo(defaultStyle);
			mapServer.saveStyleInfo(defaultStyle);
		}
	}

//	private TiledInfo buildTiledLayer(StoreInfo store, String tiledName, DataServerEntity dataServer) {
//		if (store == null || !(store instanceof TiledStoreInfo)) {
//			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_NOT_FOUND, dataServer.getServerName() + "的StoreInfo"));
//		}
//		FeatureInfo fearture = featureInfo == null ? dataServer.getFeatureInfo() : featureInfo;
////		TiledInfo tiledInfo = catalog.getFactory().createTiledLayer();
//		TiledInfo tiledInfo = new TiledInfoImpl();
//		tiledInfo.setName(tiledName);
//		tiledInfo.setNativeName(tiledName);
//		tiledInfo.setStore(store);
//		tiledInfo.setEnabled(true);
//		WorkspaceInfo workspace = store.getWorkspace();
//		NamespaceInfo namespace = catalog.getNamespaceByPrefix(workspace.getName());
//		if (namespace == null) {
//			namespace = catalog.getDefaultNamespace();
//		}
//		tiledInfo.setNamespace(namespace);
//		try {
//			// keywords
//			tiledInfo.getKeywords().add(new Keyword(tiledName));
//			tiledInfo.getKeywords().add(new Keyword("wmts"));
//			if(fearture != null) {
//				tiledInfo.setSRS(fearture.getResourceSrs());
//				tiledInfo.setNativeCRS(CRS.decode(fearture.getNativeSrs()));
//				String lonLatNative = fearture.getNativeLatLon();
//				String[] lonLatNativeArray = lonLatNative.split(";");
//				ReferencedEnvelope nativeEnvelope = new ReferencedEnvelope(Double.parseDouble(lonLatNativeArray[0]), Double.parseDouble(lonLatNativeArray[2]), Double.parseDouble(lonLatNativeArray[1]), Double.parseDouble(lonLatNativeArray[3]), CRS.decode(fearture.getNativeSrs(),true));
//				tiledInfo.setNativeBoundingBox(nativeEnvelope);
//				String resourceBbox = fearture.getResourceBbox();
//				String[] resourceBboxArray = resourceBbox.split(";");
//				ReferencedEnvelope resEnvelope = new ReferencedEnvelope(Double.parseDouble(resourceBboxArray[0]), Double.parseDouble(resourceBboxArray[2]), Double.parseDouble(resourceBboxArray[1]), Double.parseDouble(resourceBboxArray[3]), CRS.decode(fearture.getResourceSrs(),true));
//				tiledInfo.setLatLonBoundingBox(resEnvelope);
//			} else {
//				tiledInfo.setSRS(null);
//				tiledInfo.setNativeCRS(null);
//				tiledInfo.setNativeBoundingBox(null);
//				tiledInfo.setLatLonBoundingBox(null);
//			}
//		} catch (FactoryException e) {
//			log.error("MapServerServiceImpl buildTiledLayer has error:" + ExceptionsUtil.stackTraceToStr(e));
//			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, dataServer.getServerName() + "坐标系"));
//		}
//		return tiledInfo;
//	}

	/**
	 *  解析文件获取bbox
	 *  @param url 文件的url
	 *  @param fileType 文件类型
	 */
	@Override
	public BboxInfoEntity queryBboxFromFile(String url, String fileType, DataServerEntity serverEntity, String reqIP) {
		//将海图解析单独处理，如果不是海图服务，则依然沿用原有的解析逻辑
		if ("ImageMosaic".equalsIgnoreCase(fileType)) {
			BboxInfoEntity bboxInfoEntity = new BboxInfoEntity("-180;-90;180;90", false, null);
			bboxInfoEntity.setResourceBbox("-180;-90;180;90");
			return bboxInfoEntity;
		} else {
			return queryServerBboxByFile(url, fileType, serverEntity, reqIP);
		}
	}

	/**
	 * 生成ImageMosaic遥感影像数据的footprint的shp文件并从shp文件中获取bbox
	 *
	 * @param url
	 * @param serverEntity
	 * @param reqIP
	 * @return
	 */
	private BboxInfoEntity queryBboxForImageMosaic(String url, DataServerEntity serverEntity, String reqIP) {
		File source = new File(url);
		try {
			ImageMosaicReader reader = new ImageMosaicReader(source, null);
			reader.harvest(null, source, null);
		} catch (IOException e) {
			log.error("生成footprint的shp文件异常!");
		}

		url = url + File.separator + source.getName() + ".shp";
		return queryServerBboxByFile(url, "Shape", serverEntity, reqIP);
	}

	/**
	 * 海图以外的地图数据解析
	 * @param url
	 * @param fileType
	 * @param serverEntity
	 * @return
	 */
	public BboxInfoEntity queryServerBboxByFile(String url, String fileType,DataServerEntity serverEntity, String reqIP) {
		// 根据类型获取到对对应的GridCoverageReader或SimpleFeatureSource
		ReadDataInfo dataInfo = null;
		DataStore store = null;
		try {
			//如果是从postgis发布服务，暂时是将数据源名称和具体数据名称以";"拼接并传递处理的
			if (SystemServiceImpl.isPostGIS(url)) {
				store = PostGisStoreUtils.getPostGisFromCache(url.split(";")[0]);
				if (store == null) {
					PostGisConnectEntity entity = mapServer.queryPostGisBySourceName(url.split(";")[0]);
					store = PostGisStoreUtils.getPostGiDataStore(entity);
				}
			}
			dataInfo = FileDataInfoFactory.getDataInfo(url, fileType, store);
			DataInfoStrategy strategy = new DataInfoStrategy(dataInfo);
			FeatureInfo featureInfo = new FeatureInfo();
			if (CommonUtil.isTiledServer(fileType)) {
				featureInfo.setTileExtension(strategy.getTileExtension());
			}
			featureInfo.setTileWidth(strategy.getImageWidth());
			featureInfo.setTileHeight(strategy.getImgHeight());
			CoordinateReferenceSystem resourceCrs = strategy.getResourceSrs();
			//先处理下srs,主要处理不是标准投影数据的web墨卡托
			String srsName = getSrs(resourceCrs);
			ReferencedEnvelope resourceEnvelope = strategy.getEnvelope();
			if (OgcEpsg.getOgcEpsgCodeType(srsName) == OgcEpsg.MERCATOR_CODE
					&& !"EPSG:3857".equalsIgnoreCase(srsName)) {
				resourceCrs = CRS.parseWKT(Constant.WEB_Mercator);
				srsName = "EPSG:3857";
				resourceEnvelope = new ReferencedEnvelope(resourceEnvelope, resourceCrs);
			}
			//增加直接记录CRS的wkt格式
			String resourceCrsWkt = resourceCrs.toWKT();
			featureInfo.setResourceCrsWkt(resourceCrsWkt);
			featureInfo.setResourceSrs(srsName);
			featureInfo.setResourceBbox(convertBboxStr(resourceEnvelope,srsName));
			//本地投影默认设置为 wgs84 4326,如果元数据不是该投影，则需要转化
			ReferencedEnvelope nativeBbox = resourceEnvelope;
			if (!OgcEpsg.DEGREE.getOgcCode().equalsIgnoreCase(srsName)) {
				nativeBbox = GeometryUtil.transformCoordinateSystem(resourceEnvelope, "EPSG:4326");
			}

			ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(-182, 182, -92, 92, CRS.decode("EPSG:4326", true));
			if (!referencedEnvelope.contains((Envelope) nativeBbox)){
				//过滤维度超出4326投影坐标范围的服务(有2的误差)
				return null;
			}
			String nativeBboxstr = convertBboxStr(nativeBbox,"EPSG:4326");
			featureInfo.setNativeLatLon(nativeBboxstr);
			//矢量数据的特征总数
			featureInfo.setTotalFeatures(strategy.getTotalFeatures());
			featureInfo.setNativeSrs("EPSG:4326");
			featureInfo.setZoomLevel(strategy.getMaxLevel(nativeBbox));
			featureInfo.setSeaChartIdentifier(strategy.getSeaChartIdentifier());
			featureInfo.setOrigin(strategy.getOrigin());
			if (serverEntity != null) {
				serverEntity.setFeatureInfo(featureInfo);
			}else {
				this.featureInfo = featureInfo;
			}
			//封装Bbox相关信息返回给前台
			BboxInfoEntity entity = new BboxInfoEntity();
			entity.setBboxInfo(nativeBboxstr);
			boolean isDem = isDemServer(url,featureInfo);
			if (isDem) {
				featureInfo.setDataType(StyleInfo.RASTER_dem);
			} else {
				featureInfo.setDataType(strategy.getDataType());
			}
			entity.setResourceBbox(featureInfo.getResourceBbox());
			entity.setResourceCrs(featureInfo.getResourceSrs());
			entity.setDemServer(isDem);
			entity.setExtension(strategy.getTileExtension());
			return entity;
		} catch (FactoryException e) {
			log.error("MapServerServiceImpl queryBboxFromFile has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, "EPSG:3857坐标系"));
		} catch (BaseException e) {
			if(e.getErrorMsg().equals("Sqlite error")) {
				File file = new File(url);
				String fileName = file.exists() ? file.getName() : "";
				systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, ExceptionsUtil.formatInfo(ExceptionsUtil.SQLITE_DB_INIT_FAIL,fileName),
						UserUtil.getUserName(),"Sqlite服务", reqIP));
				throw new SqlException(e.getErrorCode(),e.getErrorMsg());
			} else {
				throw new ResourceException(e.getErrorCode(),e.getErrorMsg());
			}
		} catch (ResourceException e) {
			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, e.getErrorMsg(), UserUtil.getUserName(),"Sqlite服务", reqIP));
			throw new ResourceException(e.getErrorCode(),e.getErrorMsg());
		}
	}

	/**
	 * 如果有EPSG就记录，没有可以不记录
	 * @param resourceCrs
	 * @return String
	 * @throws FactoryException
	 */
	public String getSrs(CoordinateReferenceSystem resourceCrs) throws FactoryException {
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

	private String convertBboxStr(org.opengis.geometry.Envelope envelope,String srs) {
		if (envelope != null) {
			StringBuilder sb = new StringBuilder();
			if ("EPSG:4326".equals(srs)){
				//4326 bbox校准,若resourceCrs也是4326也校准
				return sb.append(Math.max(envelope.getMinimum(0), -180.0)).append(";").append(Math.max(envelope.getMinimum(1), -90.0))
						.append(";").append(Math.min(envelope.getMaximum(0), 180.0)).append(";").append(Math.min(envelope.getMaximum(1), 90.0)).toString();
			}
			return sb.append(envelope.getMinimum(0)).append(";").append(envelope.getMinimum(1))
					.append(";").append(envelope.getMaximum(0)).append(";").append(envelope.getMaximum(1)).toString();
		}
		return null;
	}

	@Override
	public int queryServerDataCount(RequestQueryCondition queryCondition) {
		try {
			//如果是根据异常状态查询服务，则需要将因为移动数据造成的异常算进去
			int count = mapServer.queryServerDataCount(queryCondition);
			if (queryCondition != null && queryCondition.getServerStatus() == ServerStatusEnum.ABNORMAL.getCode()) {
				count += abnormalServer.size();
			}
			return count;
		} catch (Exception e) {
			log.error("MapServerServiceImpl queryServerDataCount has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,e.getMessage(),e.getCause());
		}
	}

	@Override
	public int queryRestDataCount(RequestQueryCondition queryCondition) {
		try {
			//如果是根据异常状态查询服务，则需要将因为移动数据造成的异常算进去
			int count = mapServer.queryRestDataCount(queryCondition);
			//暂时屏蔽
//			if (queryCondition != null && queryCondition.getServerStatus() == ServerStatusEnum.ABNORMAL.getCode()) {
//				count += abnormalRestServer.size();
//			}
			return count;
		} catch (Exception e) {
			log.error("MapServerServiceImpl queryServerDataCount has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,e.getMessage(),e.getCause());
		}
	}

	/**
	 * 编辑服务 特殊情况，如果修改了原有服务的文件位置，则对应的特征信息数据需要修改 否则只需要修改服务和版本信息数据即可
	 */
	@Override
	public void editServerData(DataServerEntity dataServer,String reqIP) {
		LayerInfo layerInfo = null;
		StoreInfo info = null;
		boolean isBigSeaChartServer = false;
		try {
			checkServerDataStatus(dataServer.getDataUrl());
			DataServerEntity oldEntity = mapServer.queryServerInfoById(dataServer.getServerId());
			// 设置编辑更新时间
			dataServer.setUpdateTime(dataServer.getUpdateTime());
			String dataType = dataServer.getFileType();
			//进行两次编辑金字塔缓存
//			if (dataServer.getIsPyamid() == 1) {
//				dataServer.setStatus(ServerStatusEnum.OVERVIEWS.getCode());
//			}


			String outputColorNew = dataServer.getOutputTransparentColor();
			String outputColorOld = oldEntity.getOutputTransparentColor();
			String inputColorNew = dataServer.getInputTransparentColor();
			String inputColorOld = oldEntity.getInputTransparentColor();
			// 如果是ImageMosaic数据，比较输入和输出头名色是否修改
			if (!StringUtils.equalsAny(outputColorNew, outputColorOld) || !StringUtils.equalsAny(inputColorNew, inputColorOld)) {
				mapServer.editTransparentColor(dataServer.getInputTransparentColor(), dataServer.getOutputTransparentColor(), dataServer.getServerId());
			}
			// 对象不相等，则进行编辑相关内容
			if (!dataServer.equals(oldEntity)) {
				dataServer.setLayerName(dataServer.getServerName());
				// 存储storeInfo
				info = getStoreInfoByDataType(dataType, dataServer.getDataUrl(), dataServer.getEncoding());
				info = dealStoreInfo(info);
				// 构建图层信息
				layerInfo = getLayerInfo(getDefaultResource(dataServer,info),dataServer,reqIP,true);
				// 处理styleInfo
				LayerBaseInfo layerBaseInfo = getLayerBaseInfo(oldEntity.getServerName(), reqIP);
				if (DataTypeEnum.TILED.getFileCode() != dataServer.getDataType()) {
					if (dataServer.getStyleInfo() != null
							&& StringUtils.isNotBlank(dataServer.getStyleInfo().getStyleId())
							&& !dataServer.getStyleInfo().getStyleId().equals(oldEntity.getStyleInfo().getStyleId())) {
						layerBaseInfo.setStyleName(dataServer.getStyleInfo().getSldName());
						org.geoserver.catalog.StyleInfo styleInfo = catalog.getStyle(dataServer.getStyleInfo().getStyleId());
						layerInfo.setDefaultStyle(styleInfo);
						// 保存服务样式
						dataServer.getStyleInfo().setServerId(dataServer.getServerId());
						mapServer.editStyleInfo(dataServer.getStyleInfo());
						try {
							//如果样式改变，需要清空之前的缓存记录
							renderTileService.flushTileTask(dataServer.getServerName(),false,true);
							//清空之前的样式缓存
							delTempFile(oldEntity.getServerName());
//							clearCacheTiles(oldEntity.getServerName(),reqIP);
						} catch (Exception e) {
							if(ExceptionsUtil.isPytcError(ExceptionsUtil.stackTraceToStr(e))) {
								systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR,
										ExceptionsUtil.formatInfo(ExceptionsUtil.PYTC_SERVER_ERROR,
												dataServer.getServerName() + "服务缓存清理"), UserUtil.getUserName(),"Pytc服务", reqIP));
							}
							log.error("MapServerServiceImpl editServerData has error:" + ExceptionsUtil.stackTraceToStr(e));
							throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.STATUS_PYTC,  dataServer.getServerName() + "服务"));
						}
					}
					// 栅格色带
					if (dataServer.getRasterColor() != null && dataServer.getRasterColor().getColorId() != 0) {
						dataServer.getRasterColor().setServerId(dataServer.getServerId());
						mapServer.saveOrUpdateColorModel(dataServer.getRasterColor());
					}
				}
				//如果编码格式改变，需要清空之前的缓存记录
				if (StringUtils.isNotBlank(dataServer.getEncoding()) || !dataServer.getEncoding().equals(oldEntity.getEncoding())
						|| !StringUtils.equalsAny(outputColorNew, outputColorOld) || !StringUtils.equalsAny(inputColorNew, inputColorOld)) {
					renderTileService.flushTileTask(dataServer.getServerName(),false,true);
					//清空之前的样式缓存
					delTempFile(oldEntity.getServerName());
				}

				// 编辑成功后删除原有的resource文件
				List<String> layerNameList = new ArrayList<String>();
				layerNameList.add(oldEntity.getLayerName());
				if (DataTypeEnum.TILED.getFileCode() != getDataType(dataServer.getFileType())) {
					removeOldResource(layerNameList);
				}
				if (dataServer.getIsPyamid() != oldEntity.getIsPyamid() && dataServer.getIsPyamid() == 1) {
					if (isBigSeaChartServer == false) {
						List<Integer> servers = new ArrayList<Integer>(1);
						int serverStatus = dataServer.getIsPyamid() == 1 ? ServerStatusEnum.OVERVIEWS.getCode() : ServerStatusEnum.PUBLISHED.getCode();
						servers.add(dataServer.getServerId());
						mapServer.updateServerStatusByIds(serverStatus, servers);
					}
					dataServer.setStatus(ServerStatusEnum.OVERVIEWS.getCode());
					publishedData(layerInfo, dataServer);
				} else {
					dataServer.setStatus(ServerStatusEnum.PUBLISHED.getCode());
				}
//				mapServer.editServerData(dataServer);
				//修改服务名称时 先删除原有记录 然后保存新的值 同步修改缓存文件名称
				if (!dataServer.getServerName().equals(oldEntity.getServerName())) {
					//原内存缓存的代码，暂时屏蔽
					//clearCacheTiles(oldEntity.getServerName(),reqIP);
					MapServerUtils.renameLocalCacheFileNames(oldEntity.getServerName(), dataServer.getServerName());
					layerBaseInfo.setLayerName(dataServer.getServerName());
					//名称不相同，并且当前的图层是位于某几个图层组中的，因此需要修改该图层组的数据
					modifyLayerGroupInfo(dataServer, oldEntity, reqIP);
					//编辑tile_task_info和dynamic_data_source和layer_store_info
					mapServer.updateTileTaskInfo(oldEntity.getServerName(), dataServer.getServerName());
					mapServer.updateDynamicInfo(oldEntity.getServerName(), dataServer.getServerName());
					mapServer.updateLayerStoreInfo(oldEntity.getServerName(), dataServer.getServerName());
					mapServer.updateTileEntityInfo(oldEntity.getServerName(), dataServer.getServerName());
				}
				if (layerBaseInfo != null) {
					redisUtils.deleteKey(reqIP,oldEntity.getServerName());
					layerBaseInfo.setEncoding(dataServer.getEncoding());
					redisUtils.storeValue(reqIP,dataServer.getServerName(), layerBaseInfo);
					//更新切图模块缓存信息
					renderTileService.updateCacheInfo(oldEntity.getLayerName());
				} else {
					cacheLayerInfo(dataServer,reqIP);
				}
				// 编辑存储信息和图层信息
//				StoreLayerInfo storeLayerInfo = new StoreLayerInfo(storeInfoId, dataServer.getServerId(),layerInfo.getId());
				cacheLayers.clear();
				//栅格数据
				if (DataTypeEnum.RASTER.getFileCode() == dataServer.getDataType()) {
					FeatureInfo featureInfo = oldEntity.getFeatureInfo();
					featureInfo.setInputTransParentColor(dataServer.getInputTransparentColor());
					featureInfo.setOutPutTransParentColor(dataServer.getOutputTransparentColor());
					mapServer.editFeatureInfo(featureInfo);
				}
				mapServer.editServerData(dataServer);
				
			}

			// 编辑后服务是否生成金字塔缓存
//			File overViewFile = new File(dataServer.getDataUrl() + ".ovr");
//			// && !isFirst && !overViewFile.exists()
//			if (dataServer.getIsPyamid() == 1 && !overViewFile.exists()) {
//				Future future = statsticService.buildOverviews(dataServer);
//				overViewsCache.put(dataServer.getServerName(), future);
//			} else if (dataServer.getIsPyamid() == 1 && overViewFile.exists()) {
//				// 当发布服务勾选生成金字塔，但此服务已有缓存文件时不再生成金字塔缓存，修改服务状态
//				mapServer.updateStatusAndOverview(dataServer.getServerId());
//				dataServer.setStatus(ServerStatusEnum.PUBLISHED.getCode());
//				dataServer.setIsPyamid(0);
//				JSONObject jsonObject = new JSONObject();
//				jsonObject.put("status", 200);
//				jsonObject.put("msgKey", "mapServer");
//				jsonObject.put("msg", dataServer.getServerName() + "服务已存在金字塔缓存!");
//				webSocket.sendMessage(jsonObject.toJSONString());
//			}
			
			if (dataServer.getOpenTiled() == 1) {
				excutorService.submit(() -> {
					try {
						renderTileService.seedTaskByLayerName(dataServer.getLayerName(), dataServer.getServerId(), reqIP, dataServer.getIsPyamid());
					} catch (Exception e) {
						if(ExceptionsUtil.isPytcError(ExceptionsUtil.stackTraceToStr(e))) {
							systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR,
									ExceptionsUtil.formatInfo(ExceptionsUtil.PYTC_SERVER_ERROR,
											dataServer.getServerName() + "服务开启切图任务"), UserUtil.getUserName(),"Pytc服务", reqIP));
						}
						log.error("MapServerServiceImpl editServerData has error:" + ExceptionsUtil.stackTraceToStr(e));
						throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.STATUS_PYTC,  dataServer.getServerName() + "服务开启切图任务"));
					}
				});
			}
			
			//用户在修改服务名称时，同步修改缩略图名称
			File tempFile = new File(new StringBuilder(MapServerUtils.getThumpath()).append(File.separator).append(oldEntity.getServerName()).append(".png").toString());
			String fileName = tempFile.getName();
			if ((oldEntity.getStyleInfo() == null || dataServer.getStyleInfo().getStyleId().equals(oldEntity.getStyleInfo().getStyleId()))
					&& dataServer.getEncoding().equals(oldEntity.getEncoding())) {
				if(!fileName.equals(dataServer.getServerName() + ".png")) {
					tempFile.renameTo(new File(new StringBuilder(MapServerUtils.getThumpath()).append(File.separator).append(dataServer.getServerName()).append(".png").toString()));
				}
				if (!StringUtils.equalsAny(outputColorNew, outputColorOld) || !StringUtils.equalsAny(inputColorNew, inputColorOld)) {
                    if (tempFile.exists()) {
                        tempFile.delete();
                    }
					// 修改问题--带黑边数据发布后编辑黑边还在
					redisUtils.deleteKey(reqIP, dataServer.getLayerName());
                    dealThumbal(reqIP,dataServer);
                }
			}else {
				//删除之前老数据
				if (tempFile.exists()) {
					tempFile.delete();
				}
				dealThumbal(reqIP,dataServer);
			}

			// 如果服务名称有修改，需更新请求表和请求统计表中的服务名称
			if (!oldEntity.getServerName().equals(dataServer.getServerName())) {
				statsticService.updateRequestServerName(dataServer.getServerId(), dataServer.getServerName());
				statsticService.updateVisitServerName(dataServer.getServerId(), dataServer.getServerName());
			}
		} catch (StatusException e) {
			closeInfo(info, layerInfo);
			throw e;
		} catch (BaseException e) {
			closeInfo(info, layerInfo);
			throw e;
		} catch (ResourceException e) {
			closeInfo(info, layerInfo);
			throw e;
		} finally {
			//编辑服务完成后,如果服务状态正常且服务数据文件存在,就将其从异常服务缓存中清除
			File tempServerFile = new File(dataServer.getDataUrl());
			if (tempServerFile.exists() && (dataServer.getStatus() == ServerStatusEnum.PUBLISHED.getCode())) {
				if (isContainsInAbnormal(dataServer)) {
					abnormalServer.remove(dataServer.getServerName());
				}
			}
		}
	}

	/**
	 * 编辑rest服务 特殊情况，如果修改了原有服务的文件位置，则对应的特征信息数据需要修改 否则只需要修改服务和版本信息数据即可
	 */
	@Override
	public void editRestData(DataServerEntity dataServer,String reqIP) {
		LayerInfo layerInfo = null;
		StoreInfo info = null;
		boolean isBigSeaChartServer = false;
		try {
			checkServerDataStatus(dataServer.getDataUrl());
			DataServerEntity oldEntity = mapServer.queryServerInfoById(dataServer.getServerId());
			// 设置编辑更新时间
			dataServer.setUpdateTime(dataServer.getUpdateTime());
			String dataType = dataServer.getFileType();
			if (dataServer.getIsPyamid() == 1) {
				dataServer.setStatus(ServerStatusEnum.OVERVIEWS.getCode());
			}
			mapServer.editRestData(dataServer);
			// 如果服务名称有修改，需更新请求表和请求统计表中的服务名称
//			if (!oldEntity.getServerName().equals(dataServer.getServerName())) {
//				statsticService.updateRequestServerName(dataServer.getServerId(), dataServer.getServerName());
//				statsticService.updateVisitServerName(dataServer.getServerId(), dataServer.getServerName());
//			}
		} catch (StatusException e) {
			closeInfo(info, layerInfo);
			throw e;
		} catch (BaseException e) {
			closeInfo(info, layerInfo);
			throw e;
		} catch (ResourceException e) {
			closeInfo(info, layerInfo);
			throw e;
		} finally {
			//编辑服务完成后,如果服务状态正常且服务数据文件存在,就将其从异常服务缓存中清除
//			File tempServerFile = new File(dataServer.getDataUrl());
//			if (tempServerFile.exists() && (dataServer.getStatus() == ServerStatusEnum.PUBLISHED.getCode())) {
//				if (isContainsInAbnormal(dataServer)) {
//					abnormalRestServer.remove(dataServer.getServerName());
//				}
//			}
		}
	}


	public void modifyLayerGroupInfo(DataServerEntity dataServer, DataServerEntity oldEntity,String reqIP) {
		Map<String,Object> mapParam = new HashMap<>();
		mapParam.put("oldLayerName", oldEntity.getServerName());
		mapParam.put("newLayerName", dataServer.getServerName());
		List<LayerGroup> layerGroups = mapServer.queryLayerGroup(mapParam);
		if (layerGroups != null && !layerGroups.isEmpty()) {
			//删除之前的图层组，删除缓存图层组，删除对应的
			for (LayerGroup layerGroup2 : layerGroups) {
				if (catalog.getLayerGroupByName(layerGroup2.getLayerGroupName()) != null) {
					catalog.remove(catalog.getLayerGroupByName(layerGroup2.getLayerGroupName()));
				}
				//获取到图层组信息
				List<LayerGroup> list = layerBaseInfoUtil.getLayerGroups(layerGroup2.getLayerGroupName());
				//遍历修改图层组中各个图层信息，将修改过的图层组名称替换
				for (LayerGroup layerGroup : list) {
					if (layerGroup.getLayerName().equals(oldEntity.getServerName())) {
						layerGroup.setLayerName(dataServer.getServerName());
					}
				}
				layerGroupServerService.createLayerGroup(layerGroup2.getLayerGroupName(), list, layerGroup2.getLayerGroupId(),null,reqIP);
			}
			mapParam.put("status", dataServer.getStatus());
			mapParam.put("overView", dataServer.getIsPyamid());
			mapServer.editLayerGroup(oldEntity.getServerName(),dataServer.getServerName());
		}
	}



	@Override
	@Transactional
	public void deleteServerData(List<Integer> ids, String reqIP) {
		// 如果删除的服务中包含批量发布的编组服务，需要对未展示的原始服务也进行删除
		List<DataServerEntity> dataServerEntities = queryDeleteServers(ids);
		if (dataServerEntities == null || dataServerEntities.isEmpty()) {
			return;
		}
		ArrayList<Integer> serverIds = new ArrayList<>();
		dataServerEntities.forEach(dataServerEntity -> {
			serverIds.add(dataServerEntity.getServerId());
		});
		List<Integer> layerList = new ArrayList<Integer>();
		List<Integer> groupList = null;
		List<String> allLayerName = new ArrayList<String>();
		List<String> layerNames = null;
		List<String> layerGroupNames = null;
		File tempFile =  null;
		for (DataServerEntity dataServerEntity : dataServerEntities) {
			//非瓦片数据，删除服务时，需要清除其缓存信息
			if (dataServerEntity.getDataType() != DataTypeEnum.TILED.getFileCode()) {
				layerList.add(dataServerEntity.getServerId());
			}
			String serverName = dataServerEntity.getServerName();
			//清除访问时缓存的TDB数据读取相关缓存
			delTempFile(dataServerEntity.getServerName());
			//缓存瓦片时，tdb存储路径名称包含服务名称；切图时，tdb存储路径包含切图任务的任务名称
			RequestQueryCondition condition = new RequestQueryCondition();
			condition.setServerName(serverName);
			File file = new File(dataServerEntity.getDataUrl());
//			List<TileTaskEntity> list = renderTileService.queryTileTaskInfo(condition);
//			if(list.size() > 0) {
//				list.forEach(item -> {
//					String taskName = item.getTaskName();
//					renderTileService.deleteTdbCacheInfo(taskName,serverName);
//				});
//			} else {
			CustomDataLoadImpl.clearTDBCacheInfo(file.getName(),serverName);
//			}
			//删除保存在文件中的缩略图
			tempFile = new File(new StringBuilder(MapServerUtils.getThumpath()).append(File.separator).append(serverName).append(".png").toString());
			if (tempFile.exists()) {
				tempFile.delete();
			}
			// 删除异常服务时，清理缓存的异常信息，防止在条件查询时返回已经删除的服务信息
			if (isContainsInAbnormal(dataServerEntity)) {
				abnormalServer.remove(serverName);
			}
			//删除瓦片类型服务时同步删除Redis中的记录
			LayerBaseInfo tiledBaseInfo = (LayerBaseInfo)redisUtils.getValue(serverName);
			redisUtils.deleteKey(reqIP,serverName,serverName+"scale");
			allLayerName.add(dataServerEntity.getServerName());
			if (dataServerEntity.getLayerGroupFlag() == 0) {
				//图层名称
				if (layerNames == null) {
					layerNames = new ArrayList<String>();
				}
				layerNames.add(dataServerEntity.getServerName());
				LayerInfo layerInfo = catalog.getLayerByName(serverName);

				if (layerInfo != null) {
					ResourceInfo resourceInfo = layerInfo.getResource();
					if (resourceInfo != null) {
						catalog.remove(resourceInfo.getStore());
					}
					catalog.remove(layerInfo);
					catalog.remove(layerInfo.getResource());
				}
			} else {
				if (groupList == null) {
					groupList = new ArrayList<>();
				}
				//图层名称
				if (layerGroupNames == null) {
					layerGroupNames = new ArrayList<>();
				}
				layerGroupNames.add(dataServerEntity.getServerName());
				LayerGroupInfo layerGroup = catalog.getLayerGroupByName(serverName);
				if (layerGroup != null) {
					catalog.remove(layerGroup);
				}
				groupList.add(dataServerEntity.getServerId());
			}
			if (dataServerEntity.getFeatureInfo() != null && dataServerEntity.getFeatureInfo().getDataType() == StyleInfo.RASTER_dem){
				redisUtils.deleteKey(reqIP, dataServerEntity.getDataUrl());
			}

			// 删除金字塔缓存数据
			// 如果是正在发布的ImageMosaic数据，需要终止正在发布的异步线程
			if (dataServerEntity.getIsPyamid() == 1
					|| (dataServerEntity.getStatus() == 3 && "ImageMosaic".equals(dataServerEntity.getDataType()))) {
				// 正在生成金字塔缓存的服务先停掉
				if (dataServerEntity.getStatus() == ServerStatusEnum.OVERVIEWS.getCode()) {
					Future future = overViewsCache.get(dataServerEntity.getServerName());
					if (future != null) {
						future.cancel(true);
						overViewsCache.remove(dataServerEntity.getServerName());
					}
				}
				if (dataServerEntity.getIsPyamid() == 1) {
					deleteOverViewsFile(dataServerEntity.getDataUrl());
				}
			}
		}
		if (groupList != null && !groupList.isEmpty()) {
			mapServer.deleteLayerGroupData(groupList);
			layerGroupNames = layerGroupNames.stream().map(name -> name +"-group").collect(Collectors.toList());
			//删除redis中缓存的数据
			redisUtils.deleteKey(reqIP,layerGroupNames.toArray(new String[] {}));
		}
		//查看删除的单个图层中  是否包含在某些图层组中，如果包含，则需要调整图层组的数据
		if (layerNames != null && !layerNames.isEmpty()) {
			//获取删除的图层组信息
			List<String> layerGroups = mapServer.queryLayerGroups(layerNames);
			//删除图层组中的图层数据信息
			mapServer.deleteLayerGroupByLayerName(layerNames);
			List<LayerGroup> layerGroup = layerGroupServerService.createLayerGroup(layerGroups,reqIP,layerNames);
			layerGroupServerService.updateLayerGroupImg(layerGroups, layerGroup, reqIP);
		}
		//先清除相关服务监控信息，然后在删除服务记录
		statsticService.deleteServerReqInfoByName(allLayerName);
		statsticService.deleteServerStatstic(allLayerName);
		//当前数据不是瓦片数据时，需要对其切片进行删除
		if (!layerList.isEmpty()) {
			CompletableFuture.runAsync(() ->{
				try {
					renderTileService.stopSeedTaskByIds(layerList);
				} catch (Exception e) {
					if(ExceptionsUtil.isPytcError(ExceptionsUtil.stackTraceToStr(e))) {
						systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, ExceptionsUtil.formatInfo(ExceptionsUtil.PYTC_SERVER_ERROR,
								"服务刪除信息"), UserUtil.getUserName(),"Pytc服务", reqIP));
					}
					log.error("MapServerServiceImpl dealServerData has error:" + ExceptionsUtil.stackTraceToStr(e));
					throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.STATUS_PYTC, "服务刪除"));
				}
			}, excutorService);	
		}
		mapServer.deleteServerData(serverIds);
		cacheLayers.clear();
		//清除对应的地形分析缓存的数据，只用在高程数据部分
		mapLayerAnalysisAdvices.delteLayerCache(allLayerName);
	}

	@Override
	@Transactional
	public void deleteRestData(List<Integer> ids, String reqIP) {
		// 如果删除的服务中包含批量发布的编组服务，需要对未展示的原始服务也进行删除
//		List<DataServerEntity> dataServerEntities = queryDeleteServers(ids);
//		if (dataServerEntities == null || dataServerEntities.isEmpty()) {
//			return;
//		}
//		ArrayList<Integer> serverIds = new ArrayList<>();
//		dataServerEntities.forEach(dataServerEntity -> {
//			serverIds.add(dataServerEntity.getServerId());
//		});
//		List<Integer> layerList = new ArrayList<Integer>();
//		List<Integer> groupList = null;
//		List<String> allLayerName = new ArrayList<String>();
//		List<String> layerNames = null;
//		List<String> layerGroupNames = null;
//		File tempFile =  null;
//		for (Integer id : ids) {
//			//非瓦片数据，删除服务时，需要清除其缓存信息
//			if (dataServerEntity.getDataType() != DataTypeEnum.TILED.getFileCode()) {
//				layerList.add(dataServerEntity.getServerId());
//			}
//			String serverName = dataServerEntity.getServerName();
//			// 删除异常服务时，清理缓存的异常信息，防止在条件查询时返回已经删除的服务信息
//			if (isContainsInAbnormal(dataServerEntity)) {
//				abnormalRestServer.remove(serverName);
//			}
//		}

		//先清除相关服务监控信息，然后在删除服务记录
//		statsticService.deleteServerReqInfoByName(allLayerName);
//		statsticService.deleteServerStatstic(allLayerName);

		mapServer.deleteRestData(ids);
		cacheLayers.clear();

	}

	private List<DataServerEntity> queryDeleteServers(List<Integer> ids) {
		List<Map<String, Integer>> groupServerShow = mapServer.queryGroupServerShow(ids);
		ArrayList<Integer> serverIds = new ArrayList<>();
		groupServerShow.forEach(doc ->{
			int serverid = doc.get("serverid");
			if (doc.get("layergroup") == 1 || doc.get("servershow") == 1 || ids.contains(serverid)) {
				serverIds.add(serverid);
			}
		});
		return mapServer.queryServerDataByIds(serverIds.stream().distinct().collect(Collectors.toList()));
	}

	/**
	 * 删除服务信息时，同步清除内存缓存。清除策略是只要key中包含服务名称，就将其清除
	 * @param key
	 */
	private void clearCacheTiles(String key,String reqIP) {
		Jedis jedis =  null;
		String redisKey  = key + "_EPSG";
		try {
			jedis = JedisConfig.getInstance().getJedis();
			Set<String> sets = jedis.keys("*");
			Iterator<String> it = sets.iterator();
			while (it.hasNext()) {
				String str = it.next();
				if(str.contains(redisKey)) {
					str = str.substring(str.indexOf(key));
					redisUtils.deleteKey(reqIP,str);
				}
			}
			log.info("服务{}内存缓存清理完毕！",key);
		} finally {
			if(jedis != null) {
				jedis.close();
			}
		}
	}

	/**
	 * 删除目录时调用FileUtil.deleteFile方法，如果目录不为空，需要先清空目录在删除该目录
	 *
	 * @param serverName serverName
	 */
	public void delTempFile(String serverName) {
		//先重命名，然后再异步删除
		String cachePath = System.getProperty(TdbUtils.USER_VISIT_CACHE_DIR);
		if (StringUtils.isBlank(cachePath)) {
			return;
		}
		File parentFile = new File(cachePath);
		File[] files = parentFile.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.contains(serverName);
			}
		});
		if (files == null || files.length == 0) {
			return;
		}
		for (File filePath : files) {
			new Thread(() -> {
				FileUtil.deleteFile(filePath.getAbsolutePath());
				filePath.delete();
			}).start();
		}
		//若不清空缓存策略，会导致读取同类型同名服务缓存的偏移量错误
		CustomDataLoadImpl.indexCache.cleanUp(serverName);
		log.info("服务{}访问缓存清理完毕！",serverName);
	}


	private void removeOldResource(List<String> layerNameList) {
		for (String layerName : layerNameList) {
			LayerInfo layerInfo = catalog.getLayerByName(layerName);
			if(layerInfo!=null){
				StoreInfo storeInfo = layerInfo.getResource().getStore();
				// 删除集群缓存信息
				// synchorCache(layerInfo, false);
				if (storeInfo != null) {
					catalog.remove(storeInfo);
				}
				if (layerInfo != null) {
					catalog.remove(layerInfo);
				}
				if (layerInfo != null) {
					ResourceInfo resourceInfo = layerInfo.getResource();
					if (resourceInfo != null) {
						catalog.remove(resourceInfo);
					}
				}
			}

		}
	}

	/**
	 * 启动和暂停服务
	 */
	@Override
	public void updateServerStatus(int serverId,String reqIP) {
		mapServer.updateServerStatus(serverId);
		//更新缓存的数据
		List<Integer> ids = new ArrayList<Integer>();
		ids.add(serverId);
		List<DataServerEntity> dataServerEntitys = mapServer.queryServerDataByIds(ids);
		DataServerEntity serverEntity = dataServerEntitys.get(0);
		String layerName = serverEntity.getServerName();
		LayerBaseInfo baseInfo = getLayerBaseInfo(layerName, reqIP);
		baseInfo.setStatus(baseInfo.getStatus() == 0 ? 1 : 0);
		redisUtils.storeValue(reqIP,layerName, baseInfo);
		//如果当前暂停的图层属于某一个图层组中的图层，则对应的该图层组的缓存需要清除掉
		if (serverEntity.getLayerGroupFlag() == 0) {
			List<String> layerGroups = mapServer.queryLayerGroups(Arrays.asList(new String[] {layerName}));
			//清除对应的
			if (layerGroups != null && !layerGroups.isEmpty()) {
				for (String layer : layerGroups) {
					try {
						filterPaesedServer(layerName,layer,serverEntity.getStatus(),reqIP);
						delTempFile(layer);
						renderTileService.flushTileTask(layer,false,true);
					} catch (Exception e) {
						if(ExceptionsUtil.isPytcError(ExceptionsUtil.stackTraceToStr(e))) {
							systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, ExceptionsUtil.formatInfo(ExceptionsUtil.PYTC_SERVER_ERROR,
									layer + "服务缓存清理"), UserUtil.getUserName(),"Pytc服务", reqIP));
						}
						log.error("MapServerServiceImpl updateServerStatus has error:" + ExceptionsUtil.stackTraceToStr(e));
						throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.STATUS_PYTC,  layer + "服务"));
					}
				}
			}
		}
	}

	/**
	 * 启动和暂停rest服务
	 */
	@Override
	public void updateRestStatus(int serverId,String reqIP) {
		mapServer.updateRestStatus(serverId);
	}
	
	//更新某个服务状态时如果该服务状态为1就整体更新缓存，否则将暂停服务剔除后更新redis缓存
	private void filterPaesedServer(String layerName,String groupName,int serverStatus,String reqIP) {
		DataServerEntity group = mapServer.queryServerInfoByServerName(groupName);
		List<LayerGroup> layers =  group.getLayerGroups();
		if (layers == null || layers.size() == 0) {
			return;
		}
		if (serverStatus != 1) {
			Iterator<LayerGroup> it = layers.iterator();
			while(it.hasNext()) {
				LayerGroup fe = it.next();
				if (fe.getLayerName().equals(layerName)) {
					it.remove();
				}
			}
		}
		redisUtils.deleteKey(reqIP, groupName);
		cacheLayerInfo(group,reqIP);
	}

	@Override              
	public List<ServerProtocolInfo> queryServerProtocolInfo() {
		return mapServer.queryServerProtocolInfo();
	}

	@Override
	public void modifyCacheInfo(LayerInfo layerInfo, boolean flag) {
		StyleInfoImpl styleInfo = (StyleInfoImpl) layerInfo.getDefaultStyle();
		styleInfo.setCatalog(catalog);
		ResourceInfo resourceInfo = layerInfo.getResource();
		if (flag) {
			// 保存图层，保存资源信息
			catalog.add(resourceInfo.getStore());
			catalog.add(resourceInfo);
			catalog.add(layerInfo);
		} else {
			catalog.remove(resourceInfo.getStore());
			catalog.remove(resourceInfo);
			catalog.remove(layerInfo);
		}
	}

	@Override
	public DataServerEntity queryServerInfoById(int serverId) {
		return mapServer.queryServerInfoById(serverId);
	}

	@Override
	public DataServerEntity queryRestInfoById(int serverId) {
		return mapServer.queryRestInfoById(serverId);
	}




	@Override
	public int queryZoomLevel(String layer) {
		int zoomLevel = -1;
		if (zoomCache.containsKey(layer)) {
			zoomLevel = zoomCache.get(layer);
		}else {
			zoomLevel = mapServer.queryZoomLevel(layer);
			zoomCache.put(layer, zoomLevel);
		}
		return zoomLevel;
	}

	@Override
	public String getLayerNameByToken(String token) {
		return mapServer.queryLayerNameByToken(token);
	}

	@Override
	public List<Integer> getServerStatusByIds(List<Integer> ids) {
		return mapServer.getServerStatusByIds(ids);
	}

	@Override
	public Map<String, Date> queryServerName(List<String> serverName) {
		List<Map<String, Object>> existsServer = mapServer.queryExistsServerName(serverName);
		if (existsServer != null && !existsServer.isEmpty()) {
			Map<String, Date> serverExistsStaus = new HashMap<>();
			for (Map<String, Object> map : existsServer) {
				serverExistsStaus.put(map.get("server_name").toString(), DateUtil.covertStringToDateHour(map.get("publish_time").toString()));
			}
			return serverExistsStaus;
		}
		return null;
	}

	@Override
	public String batchPublishServerData(MapServerBatchInfoEntity infoEntity, String reqIP) {
		List<MapServerBatchInfo> dataServer = infoEntity.getDataServer();
		boolean editGroup = infoEntity.isEditGroup();
		List<DataServerEntity> list = new ArrayList<>();
		List<String> servers = new ArrayList<>();
		for (MapServerBatchInfo mapServerBatchInfo : dataServer) {
			String serverName = MapServerUtils.getUTF8FilePath(mapServerBatchInfo.getServerName());
			String filePath = MapServerUtils.getUTF8FilePath(mapServerBatchInfo.getFileUrl());
			mapServerBatchInfo.setServerName(serverName);
			mapServerBatchInfo.setFileUrl(filePath);
			DataServerEntity serverEntity = null;
			try {
				serverEntity = createMapServerEntity(mapServerBatchInfo,reqIP);
			} catch (Exception e) {
				log.error("服务{}发布失败，原因{}",mapServerBatchInfo.getServerName(),e.getLocalizedMessage());
				continue;
			}
			if (editGroup) {
				serverEntity.setServerShow(1);
			} else {
				serverEntity.setServerShow(0);
			}
			if (serverEntity.getDataUrl().length() > 200) {  //?
				servers.add(serverEntity.getServerName());
			} else {
				list.add(serverEntity);
			}
		}
		if (list.size() == 0) {
			if (servers.size() > 0) {
				sendSocketInfo(servers,reqIP);
				return "发布失败";
			}
		}
		//保存数据
		mapServer.saveMapServerDatas(list);
		List<Integer> serverIds = new ArrayList<>();
		List<DataServerEntity> failMapData = new ArrayList<>();
		List<DataServerEntity> successMapData = new ArrayList<>();
		Map<Integer,String> failMsg = null;
		List<Integer> deleteServers = new ArrayList<>();
		for (DataServerEntity mapData : list) {
			try {
				String msg = publishServerData(mapData,reqIP);
				if (!"OK".equals(msg)) {
					if (failMsg == null) {
						failMsg = new HashMap<>();
					}
					failMsg.put(mapData.getServerId(), msg);
					mapData.setFailReason(msg);
					mapData.setStatus(ServerStatusEnum.ABNORMAL.getCode());
					failMapData.add(mapData);
				} else {
					serverIds.add(mapData.getServerId());
					if (mapData.getIsPyamid() == 1) {
						mapData.setStatus(ServerStatusEnum.OVERVIEWS.getCode());
					} else {
						mapData.setStatus(ServerStatusEnum.PUBLISHED.getCode());
					}
					successMapData.add(mapData);
				}
				//将瓦片服务基本信息存入Redis,需要将最大图层级别存入Redis
				cacheLayerInfo(mapData,reqIP);
			} catch (SqlException e) {
				systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR,
						"服务发布失败:" + e.getErrorMsg(), UserUtil.getUserName(),"服务发布", reqIP));
				deleteServers.add(mapData.getServerId());
			} catch (ResourceException e) {
				systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR,
						"服务发布失败:" + e.getErrorMsg(), UserUtil.getUserName(),"服务发布", reqIP));
				deleteServers.add(mapData.getServerId());
			} catch (StatusException e) {
				systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR,
						"服务发布失败:" + e.getErrorMsg(), UserUtil.getUserName(),"服务发布", reqIP));
				deleteServers.add(mapData.getServerId());
			} catch (Exception e) {
				log.error("SeaCharts is error!");
				deleteServers.add(mapData.getServerId());
			}
		}
		if (!serverIds.isEmpty()) {
			mapServer.updateServerStatusByIds(ServerStatusEnum.PUBLISHED.getCode(), serverIds);
		}
		if (!deleteServers.isEmpty()) {
			mapServer.deleteServerData(deleteServers);
		}
		// 批量发布编组的部分原始服务发布失败，无法进行编组业务
		if (failMsg != null && !failMsg.isEmpty() && editGroup) {
			return "部分原始服务发布失败";
		}
		if (failMsg != null && !failMsg.isEmpty() && !editGroup) {
			mapServer.updateMapServerData(failMapData);
			dealThumbal(reqIP,successMapData.toArray(new DataServerEntity[] {}));
			return JSON.toJSONString(failMsg);
		}

		// 批量发布的编组任务进行编组、切图(如有)
		if (editGroup) {
			layerGroupServerService.batchPublishAndEditGroup(reqIP, list, infoEntity);
		} else {	// 批量发布不编组对原始服务进行渲染
			dealThumbal(reqIP, list.toArray(new DataServerEntity[] {}));
		}
		if (!servers.isEmpty()) {
			sendSocketInfo(servers, reqIP);
			return "发布部分成功";
		}
		return "发布成功";
	}

	@Override
	public String batchPublishRestData(MapServerBatchInfoEntity infoEntity, String reqIP) {
		List<MapServerBatchInfo> dataServer = infoEntity.getDataServer();
		boolean editGroup = infoEntity.isEditGroup();
		List<DataServerEntity> list = new ArrayList<>();
		List<String> servers = new ArrayList<>();
		for (MapServerBatchInfo mapServerBatchInfo : dataServer) {
			String serverName = MapServerUtils.getUTF8FilePath(mapServerBatchInfo.getServerName());
			String filePath = MapServerUtils.getUTF8FilePath(mapServerBatchInfo.getFileUrl());
			mapServerBatchInfo.setServerName(serverName);
			mapServerBatchInfo.setFileUrl(filePath);
			DataServerEntity serverEntity = null;
			try {
				serverEntity = createMapRestEntity(mapServerBatchInfo,reqIP);
			} catch (Exception e) {
				log.error("服务{}发布失败，原因{}",mapServerBatchInfo.getServerName(),e.getLocalizedMessage());
				continue;
			}
			if (editGroup) {
				serverEntity.setServerShow(1);
			} else {
				serverEntity.setServerShow(0);
			}
			if (serverEntity.getDataUrl().length() > 200) {  //?
				servers.add(serverEntity.getServerName());
			} else {
				list.add(serverEntity);
			}
		}
		if (list.size() == 0) {
			if (servers.size() > 0) {
				sendSocketInfo(servers,reqIP);
				return "发布失败";
			}
		}
		//保存数据
		mapServer.saveRestServerDatas(list);
		List<Integer> serverIds = new ArrayList<>();
		List<DataServerEntity> failMapData = new ArrayList<>();
		List<DataServerEntity> successMapData = new ArrayList<>();
		Map<Integer,String> failMsg = null;
		List<Integer> deleteServers = new ArrayList<>();
		for (DataServerEntity mapData : list) {
			try {
				String msg ="OK";
				if (!"OK".equals(msg)) {
					if (failMsg == null) {
						failMsg = new HashMap<>();
					}
					failMsg.put(mapData.getServerId(), msg);
					mapData.setFailReason(msg);
					mapData.setStatus(ServerStatusEnum.ABNORMAL.getCode());
					failMapData.add(mapData);
				} else {
					serverIds.add(mapData.getServerId());
					if (mapData.getIsPyamid() == 1) {
						mapData.setStatus(ServerStatusEnum.OVERVIEWS.getCode());
					} else {
						mapData.setStatus(ServerStatusEnum.PUBLISHED.getCode());
					}
					successMapData.add(mapData);
				}
			} catch (SqlException e) {
				systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR,
						"服务发布失败:" + e.getErrorMsg(), UserUtil.getUserName(),"服务发布", reqIP));
				deleteServers.add(mapData.getServerId());
			} catch (ResourceException e) {
				systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR,
						"服务发布失败:" + e.getErrorMsg(), UserUtil.getUserName(),"服务发布", reqIP));
				deleteServers.add(mapData.getServerId());
			} catch (StatusException e) {
				systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR,
						"服务发布失败:" + e.getErrorMsg(), UserUtil.getUserName(),"服务发布", reqIP));
				deleteServers.add(mapData.getServerId());
			} catch (Exception e) {
				log.error("SeaCharts is error!");
				deleteServers.add(mapData.getServerId());
			}
		}
		if (!serverIds.isEmpty()) {
			mapServer.updateRestStatusByIds(ServerStatusEnum.PUBLISHED.getCode(), serverIds);
		}
		if (!deleteServers.isEmpty()) {
			mapServer.deleteRestData(deleteServers);
		}
		// 批量发布编组的部分原始服务发布失败，无法进行编组业务
		if (failMsg != null && !failMsg.isEmpty() && editGroup) {
			return "部分原始服务发布失败";
		}
		if (failMsg != null && !failMsg.isEmpty() && !editGroup) {
			mapServer.updateRestServerData(failMapData);
//			dealThumbal(reqIP,successMapData.toArray(new DataServerEntity[] {}));
			return JSON.toJSONString(failMsg);
		}


		if (!servers.isEmpty()) {
			sendSocketInfo(servers, reqIP);
			return "发布部分成功";
		}
		return "发布成功";
	}

	private void sendSocketInfo(List<String> servers, String reqIP) {
		JSONObject jsonObject = new JSONObject();
		StringBuilder sBuilder = new StringBuilder();
		for (int i = 0; i < servers.size(); i++) {
			sBuilder.append(servers.get(i)).append(";");
		}
		String serverNames = sBuilder.toString().substring(0, sBuilder.toString().length() -1);
		systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR,
				"服务发布失败:" + serverNames + "服务实体构建失败", UserUtil.getUserName(),"服务发布", reqIP));
		String errMsg = serverNames + ":" + ErrorCode.FILE_PATH_TOO_LONG.getErrorMsg();
		jsonObject.put("status", ReponseStatus.ERROR.getCode());
		jsonObject.put("msg", errMsg);
		jsonObject.put("msgKey", "mapServer");
		webSocket.sendMessage(jsonObject.toJSONString());
	}

	/**
	 *
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private DataServerEntity createMapServerEntity(MapServerBatchInfo mapServerBatchInfo, String reqIP) {
		String serverName = mapServerBatchInfo.getServerName();
		if (isPublished(serverName)) {
			//服务重命名
			serverName = getNewServerName(serverName);
		}
		String fileUrl = mapServerBatchInfo.getFileUrl();
		try {
			fileUrl = URLDecoder.decode(fileUrl, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.error("MapServerServiceImpl createMapServerEntity has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_DECODE, fileUrl));
		}
		String fileName = null;
		if (SystemServiceImpl.isPostGIS(fileUrl)) {
			fileName = "postgis";
		} else {
			fileName = new File(fileUrl).getName();
			if (fileName.equalsIgnoreCase("tms.xml")) {
				fileName = "tms";
			} else if (fileName.equalsIgnoreCase("config.xml")) {
				fileName = "flatgis";
			} else {
				fileName = fileName.split("\\.")[1];
			}
		}
		DataServerEntity serverEntity = new DataServerEntity(serverName, serverName, fileUrl);
		String fileType = FileType.getFileType("." + fileName);
		serverEntity.setFileType(fileType);
		serverEntity.setIsPyamid(mapServerBatchInfo.getIsPyamid());
		serverEntity.setEncoding("GBK");
		serverEntity.setOpenTiled(mapServerBatchInfo.getOpenTiled());
		serverEntity.setPublisedProctol(mapServerBatchInfo.getServerProctol());
		serverEntity.setUpdateTime(new Date());
		serverEntity.setPublishTime(new Date());
		serverEntity.setIsPublic(1);
		serverEntity.setPublishor(UserUtil.getUserName());
		// 设置数据文件类型
		serverEntity.setDataType(getDataType(serverEntity.getFileType()));
		// 目前无数据图层概念，暂时图层名与服务名一样
		serverEntity.setLayerName(serverEntity.getServerName());
		serverEntity.setStatus(ServerStatusEnum.PROCESSING.getCode());
		if (!"MBTiles".equalsIgnoreCase(serverEntity.getFileType()) && CommonUtil.isTiledServer(serverEntity.getFileType())) {
			File file = new File(serverEntity.getDataUrl());
			serverEntity.setDataUrl(file.getParentFile().getAbsolutePath() + File.separator);
		}
		//设置featureinfo
		queryBboxFromFile(mapServerBatchInfo.getFileUrl(),fileType,serverEntity,reqIP);
		return serverEntity;
	}

	/**
	 * 创建rest实体
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private DataServerEntity createMapRestEntity(MapServerBatchInfo mapServerBatchInfo, String reqIP) {
		String serverName = mapServerBatchInfo.getServerName();
		if (isPublished(serverName)) {
			//服务重命名
			serverName = getNewRestName(serverName);
		}
		String fileUrl = mapServerBatchInfo.getFileUrl();
		try {
			fileUrl = URLDecoder.decode(fileUrl, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.error("MapServerServiceImpl createMapServerEntity has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_DECODE, fileUrl));
		}
		String fileName  = "rest";


		DataServerEntity serverEntity = new DataServerEntity(serverName, serverName, fileUrl);
		String fileType = FileType.getFileType("." + fileName);
		serverEntity.setFileType(fileType);
		serverEntity.setIsPyamid(mapServerBatchInfo.getIsPyamid());
		serverEntity.setEncoding("GBK");
		serverEntity.setOpenTiled(mapServerBatchInfo.getOpenTiled());
		serverEntity.setPublisedProctol(mapServerBatchInfo.getServerProctol());
		serverEntity.setUpdateTime(new Date());
		serverEntity.setPublishTime(new Date());
		serverEntity.setIsPublic(1);
		serverEntity.setPublishor(UserUtil.getUserName());
		// 设置数据文件类型
		serverEntity.setDataType(getDataType(serverEntity.getFileType()));
		// 目前无数据图层概念，暂时图层名与服务名一样
		serverEntity.setLayerName(serverEntity.getServerName());
		serverEntity.setStatus(ServerStatusEnum.PROCESSING.getCode());
		if (!"MBTiles".equalsIgnoreCase(serverEntity.getFileType()) && CommonUtil.isTiledServer(serverEntity.getFileType())) {
			File file = new File(serverEntity.getDataUrl());
			serverEntity.setDataUrl(file.getParentFile().getAbsolutePath() + File.separator);
		}
		//设置featureinfo
//		queryBboxFromFile(mapServerBatchInfo.getFileUrl(),fileType,serverEntity,reqIP);
		return serverEntity;
	}

	@Override
	public int getServerIdByServerUrl(String fileUrl) {
		try {
			return mapServer.queryServerIdByFileUrl(fileUrl);
		} catch (Exception e) {
			log.error("MapServerServiceImpl getServerIdByServerUrl has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "服务ID"));
		}
	}

	@Override
	public int calcuCurrentLevel(int defaultLevel, int tileWidth,String srs) {
		if (OgcEpsg.getOgcEpsgCodeType(srs) == OgcEpsg.MERCATOR_CODE) {
			defaultLevel = defaultLevel + 1;
		}
		int ration = 0;
		if (tileWidth > 256) {
			ration = tileWidth / 256;
			return defaultLevel - (ration/2);
		}else {
			ration = 256 / tileWidth;
//			return defaultLevel + (ration/2);
			return defaultLevel + (int)(Math.log(ration) / Math.log(2));
		}

	}

	@Override
	public LayerBaseInfo getLayerBaseInfo(String layerName,String reqIP) {
//		if (CACHE_LAYERBASE_IFNO.containsKey(layerName)) {
//			return CACHE_LAYERBASE_IFNO.get(layerName);
//		}
		Object value = redisUtils.getValue(layerName);
		LayerBaseInfo baseInfo = null;
		if (value != null) {
			if (value instanceof LayerBaseInfo){
				return (LayerBaseInfo)value ;
			} else if (value instanceof List){
				return null;
			}
		} else {
			//查数据库
			DataServerEntity serverEntity = mapServer.queryServerInfoByServerName(layerName);
			if (serverEntity == null) {
				return null;
			}
			return cacheLayerInfo(serverEntity,reqIP);
		}
		return baseInfo;
	}

	@Override
	public List<String> getLayerNameByDataType(int dataType) {
        //sql更改，地形分析过滤已经暂停的服务
        List<String> infos = mapServer.queryLayerNamesByDataType(dataType);
        //修复地形分析异常服务过滤问题，服务异常状态没有持久化(只有开启、暂停0/1)，故要查表校验文件
        Iterator<String> iterator = infos.iterator();
        while (iterator.hasNext()){
            String next = iterator.next();
            String serverName = next.split(";")[0];
            DataServerEntity dataServerEntity = mapServer.queryServerInfoByServerName(serverName);
            if (!new File(dataServerEntity.getDataUrl()).exists()) iterator.remove();
        }
        return infos;
	}

	@Override
	public void stopOverView (int serverId, String reqIP) {
		// 根据服务名停掉线程并删除金字塔缓存数据数据
		DataServerEntity dataServerEntity = mapServer.queryServerInfoById(serverId);
		if (dataServerEntity == null) {
			throw new BaseException("参数异常,服务不存在!");
		}
		Future future = overViewsCache.get(dataServerEntity.getServerName());
		if (future != null) {
			future.cancel(true);
			overViewsCache.remove(dataServerEntity.getServerName());
		}

		// 删除金字塔缓存文件
		deleteOverViewsFile(dataServerEntity.getDataUrl());

		// 如果有切图任务,将切图任务从预等待队列添加到等待队列
		if (dataServerEntity.getOpenTiled() == 1) {
			renderTileService.addWaitTileTask(dataServerEntity.getServerName());
		}

		// 修改数据库中服务状态以便预览
		mapServer.updateServerStatu(serverId);

		// 更新状态后删除redis中的缓存，否则预览时服务状态为发布中，无法预览
		redisUtils.deleteKey("system", dataServerEntity.getServerName());
	}

	private void deleteOverViewsFile(String fileUrl) {
		File file = new File(fileUrl + ".ovr");
		if (file.exists()) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			boolean delete = file.delete();
			if (!delete) {
				log.error("金字塔缓存数据删除失败!");
			}
		}
	}

	@Override
	public Map<String, Object> queryPreviewParameter(String resourceSrs, int maxlevel){
		Map<String, Map<String, List<String>>> tileParam = guassCoordinates.getTileParam();
		Map<String, List<String>> coordinatesParam = tileParam.get(resourceSrs.replace(":", "").toUpperCase());
		List<String> tilecount = coordinatesParam.get("tilecount");
		if (MapUtils.isNotEmpty(coordinatesParam) && maxlevel <= tilecount.size() - 1) {
			List<String> bbox = coordinatesParam.get("bbox");
			double bBoxMinY = Double.parseDouble(bbox.get(1));
			double bBoxMaxY = Double.parseDouble(bbox.get(3));
			double perY = (bBoxMaxY - bBoxMinY) / 256;
			List<String> perYs = new ArrayList<>();
			for (int index = 0; index <= maxlevel; index++) {
				String[] split = tilecount.get(index).split("\\*");
				perYs.add(Double.toString(perY / Double.parseDouble(split[1])));
			}
			StringBuilder bBoxBuilder = new StringBuilder();
			bbox.stream().forEach(doc -> {
				bBoxBuilder.append(doc).append(";");
			});
			String bboxString = bBoxBuilder.deleteCharAt(bBoxBuilder.lastIndexOf(";")).toString();
			Map<String, Object> resultMap = new HashMap<>(2);
			resultMap.put("tileSize", perYs);
			resultMap.put("bbox", bboxString);
			return resultMap;
		} else {
			return Collections.emptyMap();
		}
	}

	@Override
	public Map<String, Object> queryOgcProtocolInfo(String reqIp, String protocol, String serverName) {
		List<String> interfaces = new ArrayList<>();
		Map<String, Map<String, List<String>>> interfacesParameter = new HashMap<>();
		Map<String, Object> resultMap = new HashMap<>();
		LayerBaseInfo layerBaseInfo = getLayerBaseInfo(serverName, reqIp);
		String resourceSrs = layerBaseInfo.getResourceSrs();
		if ("WMS".equalsIgnoreCase(protocol)) {
			interfaces.addAll(ogcProtocol.getWmsInterface());
			interfacesParameter.putAll(ogcProtocol.getWmsParameter());
			Map<String, List<String>> getMap = interfacesParameter.get("GetMap");
			List<String> formatList = new ArrayList<>();
			List<String> outPutFormat = getMap.get("outPutFormat");
			formatList.addAll(outPutFormat);
			if (StringUtils.equalsAnyIgnoreCase(layerBaseInfo.getResourceFormat(), "geojson", "protobuf")
					&& layerBaseInfo.getDataType() == 6) {
				// 矢量瓦片数据目前只支持自己的格式
				formatList.clear();
				formatList.add(ResponseFormat.getFormatByPrefix(layerBaseInfo.getResourceFormat()));
			}

			// Crs处理---4326、3857和原坐标的并集
			List<String> strings = getCrsList(resourceSrs, getMap);
			Map<String, Object> parameterMap = new HashMap<>();
			parameterMap.put("GetCapabilities", interfacesParameter.get("GetCapabilities"));
			HashMap<String, Object> getMapMap = new HashMap<>();
			getMapMap.put("crs", strings);
			getMapMap.put("outPutFormat", formatList);

			// 如果是矢量栅格化数据，有ECOL过滤器.暂不支持矢量图层组
			if (layerBaseInfo.getDataType() == DataTypeEnum.VECTOR.getFileCode() && layerBaseInfo.getIsLayerGroup() == 0) {
				getShapeServerName(layerBaseInfo, getMapMap);
			}

			// 瓦片数据暂时屏蔽GetFeatureInfo接口
			if (layerBaseInfo.getDataType() == 6) {
				interfaces.remove("GetFeatureInfo");
			}
			parameterMap.put("GetMap", getMapMap);
			resultMap.put("interface", interfaces);
			resultMap.put("parameter", parameterMap);
		} else if ("WMTS".equalsIgnoreCase(protocol)) {
			interfaces.addAll(ogcProtocol.getWmtsInterface());
			interfacesParameter.putAll(ogcProtocol.getWmtsParameter());
			Map<String, List<String>> getTile = interfacesParameter.get("GetTile");
			List<String> strings = new ArrayList<>();

			//矢量瓦片数据只支持原坐标预览
			if (StringUtils.equalsAnyIgnoreCase(layerBaseInfo.getResourceFormat(), "geojson", "protobuf")
					&& layerBaseInfo.getDataType() == 6) {
				strings.add(layerBaseInfo.getResourceSrs());
			} else {
				strings = getCrsList(resourceSrs, getTile);
			}
			List<String> outPutFormat = getTile.get("outPutFormat");
			List<String> formatList = new ArrayList<>();
			formatList.addAll(outPutFormat);
			if (StringUtils.equalsAnyIgnoreCase(layerBaseInfo.getResourceFormat(), "geojson", "protobuf")
					&& layerBaseInfo.getDataType() == 6) {
				// 矢量瓦片数据目前只支持自己的格式
				formatList.clear();
				formatList.add(ResponseFormat.getFormatByPrefix(layerBaseInfo.getResourceFormat()));
			} else if (layerBaseInfo.getDataType() == DataTypeEnum.VECTOR.getFileCode()) {
				formatList.add(ResponseFormat.PROTOBUF.getFormat());
				formatList.add(ResponseFormat.GEOJSON.getFormat());
			}

			Map<String, List<String>> getTileMap = new HashMap<>();
			getTileMap.put("outPutFormat", formatList);
			getTileMap.put("crs", strings);
			interfacesParameter.put("GetTile", getTileMap);
			resultMap.put("interface", interfaces);
			resultMap.put("parameter", interfacesParameter);
		} else if ("WFS".equalsIgnoreCase(protocol)) {
			interfaces.addAll(ogcProtocol.getWfsInterface());
			interfacesParameter.putAll(ogcProtocol.getWfsParameter());
			Map<String, List<String>> getFeature = interfacesParameter.get("GetFeature");
			List<String> strings = getCrsList(resourceSrs, getFeature);
			HashMap<String, Object> parameterMap = new HashMap<>();
			parameterMap.put("GetCapabilities", interfacesParameter.get("GetCapabilities"));
			parameterMap.put("GetFeatureType", interfacesParameter.get("GetFeatureType"));
			HashMap<String, Object> getFeatureMap = new HashMap<>();
            getFeatureMap.put("crs", strings);
            getFeatureMap.put("outPutFormat", getFeature.get("outPutFormat"));

			// 如果是矢量栅格化数据，有ECOL过滤器
			if (layerBaseInfo.getDataType() == DataTypeEnum.VECTOR.getFileCode() && layerBaseInfo.getIsLayerGroup() == 0) {
				getShapeServerName(layerBaseInfo, getFeatureMap);
			}
			parameterMap.put("GetFeature", getFeatureMap);
			resultMap.put("interface", interfaces);
			resultMap.put("parameter", parameterMap);
		} else if ("WCS".equalsIgnoreCase(protocol)) {
//			interfaces.addAll(ogcProtocol.getWcsInterface());
//			interfacesParameter.putAll(ogcProtocol.getWcsParameter());
//			Map<String, List<String>> describeCoverage = interfacesParameter.get("DescribeCoverage");
//			ArrayList<String> coverageId = new ArrayList<>();
//			coverageId.add(serverName);
//			describeCoverage.put("coverageId", coverageId);
//			resultMap.put("interface", interfaces);
//			resultMap.put("parameter", interfacesParameter);
			resultMap.put("msg","当前只支持WMTS、TMS、WMS、WFS协议预览。");
		}
		return  resultMap;
	}

	private List<String> getCrsList(String resourceSrs, Map<String, List<String>> getFeature) {
		List<String> crsList = getFeature.get("crs");
		List<String> crsListAll = new ArrayList<>();
		crsListAll.addAll(crsList);
		if (!StringUtils.equalsAnyIgnoreCase(resourceSrs, "EPSG:4326", "EPSG:3857")) {
			crsListAll.add(resourceSrs);
		}
		return crsListAll;
	}

	private void getShapeServerName(LayerBaseInfo layerBaseInfo, HashMap<String, Object> getMapMap) {
		List<String> eColfFilter = ogcProtocol.getEColfFilter();
		Map<String, Map<String, String>> eColGeom = ogcProtocol.getEColGeom();
		Map<String, Object> eColMap = new HashMap<>();
		eColMap.put("filter", eColfFilter);
		eColMap.put("geom", eColGeom);
		String filePath = layerBaseInfo.getFilePath();
		List<String> nameList = new ArrayList<>();
		if ("Shape".equals(layerBaseInfo.getFileType())) {
            try {
                ShapefileDataStore shapefileDataStore = new ShapefileDataStore(new File(filePath).toURI().toURL());
                shapefileDataStore.setCharset(Charset.forName("GBK"));
                ContentFeatureSource featureSource = shapefileDataStore.getFeatureSource();
                List<AttributeDescriptor> attributeDescriptors = featureSource.getSchema().getAttributeDescriptors();
                attributeDescriptors.stream().forEach(doc -> {
                    nameList.add(doc.getLocalName());
                });
            } catch (IOException e) {
				log.error("ShapFileAnalysis simpleFeatureSource has error {}",e.getLocalizedMessage());
				throw new BaseException(ErrorCode.RUNTIME_EXCEPTION.getErrorCode(),
						ExceptionsUtil.formatInfo(ExceptionsUtil.ANALYSIS_SHAPE_FILE, layerBaseInfo.getLayerName()));
            }
        } else if ("PostGIS".equals(layerBaseInfo.getFileType())) {
			String sourceName = layerBaseInfo.getFilePath().split(";")[0];
			DataStore store = PostGisStoreUtils.getPostGisFromCache(sourceName);
			if (store == null) {
				store = PostGisStoreUtils.getPostGiDataStore(systemService.queryPostGis(sourceName));
			}
			// 查询数据库发布服务的表格字段
			try {
				Connection connection = ((JDBCDataStore) store).getDataSource().getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement(QUERY_FILED_SQL);
				preparedStatement.setString(1, layerBaseInfo.getFilePath().split(";")[1]);
				ResultSet resultSet = preparedStatement.executeQuery();
				while (resultSet.next()) {
					nameList.add(resultSet.getString("field_name"));
				}
			} catch (SQLException e) {
				log.error("MapServerServiceImpl has error:" + ExceptionsUtil.stackTraceToStr(e));
				throw new ResourceException(ExceptionsUtil.ERROR_500, ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_NOT_FOUND, "SQLite驱动类"));
			}
		}
		eColMap.put("name", nameList);
		getMapMap.put("ECQL", eColMap);
	}

	public Catalog getCataLogData() {
		return catalog;
	}

	public List<String> getCacheLayerList() {
		return cacheLayers;
	}

	public Map<String, Future> getOverViews() {
		return overViewsCache;
	}
}
