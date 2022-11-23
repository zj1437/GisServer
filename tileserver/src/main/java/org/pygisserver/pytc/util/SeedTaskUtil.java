package org.pygisserver.pytc.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Driver;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.ZipOutputStream;

import javax.annotation.Resource;
import javax.imageio.ImageIO;

import org.apache.dubbo.config.annotation.DubboReference;
import org.pygisserver.common.entity.Constant;
import org.pygisserver.common.entity.PyRenderRequestParam;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.entity.PostGisConnectEntity;
import org.pygisserver.common.entity.SeedTaskInstance;
import org.pygisserver.common.entity.SeedTaskParam;
import org.pygisserver.common.entity.TDBDataConfig;
import org.pygisserver.common.entity.TileTaskEntity;
import org.pygisserver.common.enumm.OgcEpsg;
import org.pygisserver.common.enumm.ResponseFormat;
import org.pygisserver.common.enumm.StoreType;
import org.pygisserver.common.enumm.TileTaskStatus;
import org.pygisserver.common.exception.ArgumentException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.exception.StatusException;
import org.pygisserver.common.pytc.entity.DataSourceEntity;
import org.pygisserver.common.pytc.entity.LayerStoreTypeInfo;
import org.pygisserver.common.pytc.entity.RenderResouce;
import org.pygisserver.common.pytc.entity.TableSource;
import org.pygisserver.common.pytc.entity.TileRequestInfo;
import org.pygisserver.common.pytc.entity.TileResourceInfo;
import org.pygisserver.common.redis.util.RedisUtils;
import org.pygisserver.common.service.MapServerDataService;
import org.pygisserver.common.service.SystemLogService;
import org.pygisserver.common.sqlite.util.SQLitePoolManage;
import org.pygisserver.common.util.CommonUtil;
import org.pygisserver.common.util.FileUtil;
import org.pygisserver.common.util.GeometryUtil;
import org.pygisserver.common.util.ImageUtil;
import org.pygisserver.pytc.configuration.DataSourceContextHolder;
import org.pygisserver.pytc.configuration.DataSourceUtil;
import org.pygisserver.pytc.configuration.DynamicDataSource;
import org.pygisserver.pytc.dao.CommonMapper;
import org.pygisserver.pytc.dao.TileCacheMapper;
import org.pygisserver.pytc.entity.GuassCoordinates;
import org.pygisserver.pytc.entity.SeedTask;
import org.pygisserver.pytc.entity.TileTask;
import org.pygisserver.pytc.entity.TileTask.TILESTATUS;
import org.pygisserver.pytc.service.impl.RenderTileServiceImpl;
import org.pygisserver.pytc.thread.SeedTaskThreadPool;
import org.pygisserver.pytc.thread.SeedTaskTileQueue;
import org.pygisserver.mapserver.MapService;
import org.pygisserver.render.config.TdbConfigFileGenerate;
import org.pygisserver.render.service.CustomDataLoad;
import org.pygisserver.render.service.RenderService;
import org.pygisserver.render.service.impl.CustomDataLoadImpl;
import org.pygisserver.render.util.TdbUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.util.ImageUtilities;
import org.geotools.referencing.CRS;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.GridSubsetFactory;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.alibaba.druid.pool.DruidDataSource;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 切图任务工具类
 * @author: 刘洋
 * @date: 2021-04-01
 */
@Component
@Slf4j
public class SeedTaskUtil {
	
	@Autowired
	private GridSetBroker gridSetBroker;
	
	@Autowired
	private TileCacheMapper tileCacheMapper;
	
	@Autowired
	private CommonMapper commonMapper;

	@Autowired
	private SeedTaskThreadPool threadPool;
	
	@Autowired
	private RedisUtils redisUtils;
	
	@Autowired
	private MongoTemplate mongoTemplate;
	
	@Autowired
	private ExceptionsUtil exceptionsUtil;
	
	@Autowired
	private GuassCoordinates guassCoordinates;
	
	@Autowired
	private RenderTileServiceImpl renderTileServiceImpl;

	@Autowired
	private MapServerDataService mapServerDataService;
	
	@Autowired
	private SystemLogService systemLogService;
	
	//使用dubbo的注解
//	@DubboReference(version="3.0.0")
	@Resource(name="renderService")
	private RenderService renderService;
	
	@DubboReference(version="3.0.0",check=false,lazy=true)
	private MapService mapService;

	private Map<Integer, TileTask> currentPool = new TreeMap<Integer, TileTask>();
	
	@Resource
	private DynamicDataSource dynamicDataSource;

	private AtomicInteger currentId = new AtomicInteger();
	
	//切图任务实例
	private static Map<String, SeedTaskInstance> cacheTileTask = new ConcurrentHashMap<String, SeedTaskInstance>();

	private static Map<String,List<LayerStoreTypeInfo>> layerStoreTypes = new ConcurrentHashMap<String,List<LayerStoreTypeInfo>>();
	
	// 已经提交为清理的任务
	private int dispatchesWithoutDrain = 0;

	// 最大可容纳未清理任务数
	private static final int MAX_DISPATCHES_WITHOUT_DRAIN = 50;
	
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	
	//瓦片队列单次容纳的数目
	private static final int QUEUE_SIZE = 200000;
	
	//mongodb集合缓存
	private volatile static CopyOnWriteArrayList<String> mongdoCollection = new CopyOnWriteArrayList<String>();
	
	//数据库和对应的表
	private volatile static Map<String,List<String>> existsDbAndTable = new ConcurrentHashMap<String,List<String>>();
	
	private static Map<String, GridSubset> gridSubSets = new HashMap<String, GridSubset>();
	
	private static final Lock dbLock = new ReentrantLock();
	
	private static final Lock TBLlock = new ReentrantLock();

	private static final Lock sqllock = new ReentrantLock();
	
	@Value("${spring.datasource.url}")
	private String jdbcUrl;

	private static CustomDataLoad customDataLoad = new CustomDataLoadImpl();
	
	private static CopyOnWriteArrayList<String> cacheFinshedTiles = new CopyOnWriteArrayList<String>();
	
	public static boolean zosIsFlag;

	/**
	 * 获取全局切图配置参数
	 * @return
	 */
	public SeedTaskParam getGlobalSeedParam() {
		DataSourceContextHolder.setDataSource("default");
		return tileCacheMapper.queryTileCacheConfig();
	}

	/**
	 * 获取切图任务实例
	 * @param layerName
	 * @return
	 */
	public SeedTaskInstance getTaskInstance(String taskName) {
		if (cacheTileTask.containsKey(taskName)) {
			return cacheTileTask.get(taskName);
		}
		DataSourceContextHolder.setDataSource("default");
		SeedTaskInstance taskInstance = tileCacheMapper.queryTaskInstance(taskName);
		if (taskInstance != null) {
			cacheTileTask.put(taskName, taskInstance);
		}
		return taskInstance;
	}
	
	/**
	 * 获取图层的基础信息
	 * @return
	 */
	public LayerBaseInfo queryServerBaseInfo(String layerName) {
		LayerBaseInfo baseInfo = (LayerBaseInfo) redisUtils.getValue(layerName);
		if (baseInfo != null) {
			return baseInfo;
		}else{
			return mapServerDataService.queryLayerBaseInfo(layerName);
		}
	}
	
	/**
	 * 下发切图任务
	 * @param seedTaskInstance
	 */
	public void dispatchTask(SeedTaskInstance seedTaskInstance) {
		
		// 计算当前的切图任务的瓦片总数
		try {
			String taskName = seedTaskInstance.getTaskName();
			if ("reseed".equalsIgnoreCase(seedTaskInstance.getSeedType())){
				clearFinshedTiles(taskName);
			}
			//如果是影像数据，则预留一个线程用于数据的拼接（导出不拼接）
			LayerBaseInfo layerBaseInfo = queryServerBaseInfo(seedTaskInstance.getLayerName());
			boolean isRaster = layerBaseInfo.getDataType() == 1 
					&& seedTaskInstance.getStoreType() != 5
					&& !ResponseFormat.TERRAIN.getFormat().equalsIgnoreCase(seedTaskInstance.getTileFormat())
					&& !ResponseFormat.TIFF.getFormat().equalsIgnoreCase(seedTaskInstance.getTileFormat());
			int mediaZoomLevel = - 1;
			if (isRaster) {
				//计算中间级别
				mediaZoomLevel = seedTaskInstance.getZoomEnd() - 1;
			}
			//计算总瓦片数
			int[] total = calcuteTilesNum(seedTaskInstance,mediaZoomLevel);
			int totalSize = total[1];
			// 更新下当前任务的总瓦片数,并把切图开始的时间写入表格中
			tileCacheMapper.updateTileTaskTiles(totalSize, taskName, TileTaskStatus.RUNNING.getStatus(), seedTaskInstance.getStartTime());
			tileCacheMapper.updateTaskInstance(taskName, seedTaskInstance.getStartTime());
			//构造切图任务瓦片队列
			int treadCount = seedTaskInstance.getThreadNum();
			SeedTaskTileQueue seedTaskTileQueue = new SeedTaskTileQueue(taskName, totalSize < QUEUE_SIZE ? totalSize : QUEUE_SIZE,treadCount);
			//开启线程计算瓦片
			final int mediaLevel = mediaZoomLevel;
			threadPool.submit(() -> {
				produceSeedTaskTile(seedTaskInstance,seedTaskTileQueue,isRaster,mediaLevel);
			});
			//构造瓦片任务，主要用于切图任务的基本信息
			TileTask tileTask = new TileTask(seedTaskTileQueue,taskName, seedTaskInstance.getLayerName());
			tileTask.setTotalSize(totalSize);
			tileTask.setThreadCount(treadCount);
			int taskId = currentId.incrementAndGet();
			tileTask.setTaskId(taskId);
			tileTask.setMediaLevel(mediaZoomLevel);
			tileTask.setMediaLvelTileCount(total[0]);
			SeedTask seedTask = new SeedTask(tileTask,this, guassCoordinates);
			//开启消费线程
			treadCount = isRaster ? (treadCount - 1) : treadCount;
			for (int i = 0; i < treadCount; i++) {
				final int index = i;
				if (tileTask.getThreadState() != TILESTATUS.DONE) {
					CompletableFuture.supplyAsync(() -> {
						return seedTask.begin(index);
					},threadPool);
				}
				dispatchesWithoutDrain++;
			}
			currentPool.put(taskId, tileTask);
			
			if (dispatchesWithoutDrain > MAX_DISPATCHES_WITHOUT_DRAIN) {
				drain();
			}
		} catch (MismatchedDimensionException | FactoryException e) {
			log.error("SeedTaskUtil dispatchTask has error {}", e.getLocalizedMessage());
		}
	}
	
	private void produceSeedTaskTile(SeedTaskInstance seedTaskInstance,SeedTaskTileQueue seedTaskTileQueue,boolean isRaster,int mediaZoomLevel)  {
		String srs = seedTaskInstance.getSrs();
		ReferencedEnvelope envelope;
		boolean isGuassCoordinate = guassCoordinates.getGuassCoordinates().contains(srs);
		try {
			if (isGuassCoordinate) {
				Map<String, List<String>> guassCoordinatePara = guassCoordinates.getTileParam().get(srs.replace(":", ""));
				List<String> bBox = guassCoordinatePara.get("bbox");
				// 源数据Bbox与坐标系Bbox取交集---最小X,最小Y,最大X,最大Y
				double minX = Double.parseDouble(bBox.get(0)) > seedTaskInstance.getMinX() ? Double.parseDouble(bBox.get(0)) : seedTaskInstance.getMinX();
				double maxX = Double.parseDouble(bBox.get(2)) > seedTaskInstance.getMaxX() ? seedTaskInstance.getMaxX() : Double.parseDouble(bBox.get(2));
				double minY = Double.parseDouble(bBox.get(1)) > seedTaskInstance.getMinY() ? Double.parseDouble(bBox.get(1)) : seedTaskInstance.getMinY();
				double maxY = Double.parseDouble(bBox.get(3)) > seedTaskInstance.getMaxY() ? seedTaskInstance.getMaxY() : Double.parseDouble(bBox.get(3));
				envelope = new ReferencedEnvelope(minX, maxX, minY, maxY, CRS.decode(srs));
			} else {
				envelope = new ReferencedEnvelope(seedTaskInstance.getMinX(), seedTaskInstance.getMaxX(),
						seedTaskInstance.getMinY(), seedTaskInstance.getMaxY(), CRS.decode(OgcEpsg.getOgcEpsgCodeType(srs) == OgcEpsg.MERCATOR_CODE ? "EPSG:3857" : srs, true));
			}
			int startZoom = seedTaskInstance.getZoomStart();
			int endZoom = seedTaskInstance.getZoomEnd();
			//判断是否为矢量数据请求，矢量数据一个瓦片一个请求，栅格数据按照4*4的规则切图后再裁剪
			boolean flag = iSMetaTileSize(seedTaskInstance.getTileFormat());
			if (flag) {
				for (int i = endZoom; i >=startZoom; i--) {
					int[] tilesRange = calcutePerLevelTile(srs, envelope, i);
					calcuteVectorTile(seedTaskTileQueue, i, tilesRange);
				}
			}else {
				if (isRaster) {
					for (int j = mediaZoomLevel; j <= endZoom; j++) {
						produceVectorTile(seedTaskTileQueue, srs, envelope, j,true,false);
					}
					for (int m = (mediaZoomLevel - 1); m >= startZoom; m--) {
						produceVectorTile(seedTaskTileQueue, srs, envelope, m,true,true);
					}
				}else {
					//矢量栅格化处理
					for (int n= endZoom; n >= startZoom; n--) {
						produceVectorTile(seedTaskTileQueue, srs, envelope, n,false,false);
					}
				}
				
			}
		} catch (MismatchedDimensionException | FactoryException e) {
			log.error("SeedTaskUtil produceSeedTaskTile has error {}", e.getLocalizedMessage());
		}
	}

	
	private void produceVectorTile(SeedTaskTileQueue seedTaskTileQueue, String srs, ReferencedEnvelope envelope,
			int j,boolean isRaster,boolean isStart) {
		int[] tilesRange = calcutePerLevelTile(srs, envelope, j);
		int levelTotalSize = (tilesRange[3] - tilesRange[1] + 1) * (tilesRange[2] - tilesRange[0] + 1);
		//如果当前级瓦片别总数大于16，则每个4*4的瓦片为一个渲染单位，小于16则，则当前级别直接为一个渲染单位 
		if (levelTotalSize < 16) {
			seedTaskTileQueue.addTileTask(new int[]{tilesRange[0],tilesRange[1],tilesRange[2],tilesRange[3],j},isRaster,isStart); 
			return;			
		}else {
			calcuteRasterTile(seedTaskTileQueue, j, tilesRange,isRaster,isStart);
		}
	}
	
	
	private void calcuteVectorTile(SeedTaskTileQueue seedTaskTileQueue, int i, int[] tilesRange) {
		for (int m = tilesRange[1]; m <= tilesRange[3]; m++) {
			for (int n = tilesRange[0]; n <= tilesRange[2]; n++) {
				//如果是补全切片，则需要查看当前切片是否已经且过，如果切过，则不加入
				int[] tile = { n, m, n, m, i };
				seedTaskTileQueue.addTileTask(tile,false,false);
			}
		}
	}
	
	/**
	 * 获取制定状态的切片任务集合
	 * @param filter
	 * @return
	 */
	public List<TileTask> filterTileTask(TILESTATUS...filter){
		List<TILESTATUS> states = Arrays.asList(filter);
		List<TileTask> list = new ArrayList<TileTask>(currentPool.size());
		Collection<TileTask> tileTasks = currentPool.values();
		for (TileTask tileTask : tileTasks) {
			TILESTATUS state = tileTask.getThreadState();
			if (states.contains(state)) {
				list.add(tileTask);
			}
		}
		return list;
	}

	public void stoptTileTask(String taskName) {
		Iterator<TileTask> it = currentPool.values().iterator();
		while(it.hasNext()) {
			TileTask tileTask = it.next();
			if (taskName.equals(tileTask.getTaskName())) {
				tileTask.setTerminate(true);
				it.remove();
//				submitTask.getFuture().cancel(true);
			}
		}
	}
	
	/**
	 * 计算当前请求的瓦片总数
	 * @param seedTaskInstance
	 * @throws FactoryException
	 * @throws NoSuchAuthorityCodeException
	 * @throws MismatchedDimensionException
	 */
	private int[] calcuteTilesNum(SeedTaskInstance seedTaskInstance,int mediaZoomLevel) throws MismatchedDimensionException, NoSuchAuthorityCodeException, FactoryException {
		String srs = seedTaskInstance.getSrs();
		ReferencedEnvelope envelope;
		boolean isGuassCoordinate = guassCoordinates.getGuassCoordinates().contains(srs);

		if (isGuassCoordinate) {
			Map<String, List<String>> guassCoordinatePara = guassCoordinates.getTileParam().get(srs.replace(":", ""));
			List<String> bBox = guassCoordinatePara.get("bbox");
			// 源数据Bbox与坐标系Bbox取交集---最小X,最小Y,最大X,最大Y
			double minX = Double.parseDouble(bBox.get(0)) > seedTaskInstance.getMinX() ? Double.parseDouble(bBox.get(0)) : seedTaskInstance.getMinX();
			double maxX = Double.parseDouble(bBox.get(2)) > seedTaskInstance.getMaxX() ? seedTaskInstance.getMaxX() : Double.parseDouble(bBox.get(2));
			double minY = Double.parseDouble(bBox.get(1)) > seedTaskInstance.getMinY() ? Double.parseDouble(bBox.get(1)) : seedTaskInstance.getMinY();
			double maxY = Double.parseDouble(bBox.get(3)) > seedTaskInstance.getMaxY() ? seedTaskInstance.getMaxY() : Double.parseDouble(bBox.get(3));
			envelope = new ReferencedEnvelope(minX, maxX, minY, maxY, CRS.decode(srs));
		} else {
			envelope = new ReferencedEnvelope(seedTaskInstance.getMinX(), seedTaskInstance.getMaxX(),
					seedTaskInstance.getMinY(), seedTaskInstance.getMaxY(), CRS.decode(OgcEpsg.getOgcEpsgCodeType(srs) == OgcEpsg.MERCATOR_CODE ? "EPSG:3857" : srs, true));
		}
		int startZoom = seedTaskInstance.getZoomStart();
		int endZoom = seedTaskInstance.getZoomEnd();
		int totalSize = 0;
		int[] values = new int[2];
		for (int i = startZoom; i <= endZoom; i++) {
			int[] tilesRange = calcutePerLevelTile(srs, envelope, i);
			int levelTotalSize = (tilesRange[3] - tilesRange[1] + 1) * (tilesRange[2] - tilesRange[0] + 1);
			totalSize+=levelTotalSize;
			if (mediaZoomLevel == i) {
				//记录下当前级别
				values[0] = levelTotalSize;
			}
		}
		values[1] = totalSize;
		return values;
	}
	

	public int[] calcutePerLevelTile(String srs, ReferencedEnvelope envelope, int i) {
		int[] tilesRange = null;
		if (OgcEpsg.MERCATOR_CODE == OgcEpsg.getOgcEpsgCodeType(srs)) {
			tilesRange = GeometryUtil.convert3857ToTiledRange(i, envelope);
		} else if (guassCoordinates.getGuassCoordinates().contains(srs)) {
				List<String> tilecount = guassCoordinates.getTileParam().get(srs.replace(":", "")).get("tilecount");
				List<String> guassCoordinateBbox = guassCoordinates.getTileParam().get(srs.replace(":", "")).get("bbox");
				tilesRange = GeometryUtil.convertGuassToTiledRange(tilecount.get(i), guassCoordinateBbox, envelope, i);
		} else if (OgcEpsg.DEGREE_CODE == OgcEpsg.getOgcEpsgCodeType(srs)) {
			tilesRange = GeometryUtil.convert4326ToTiledRange(i, envelope);
		}
		return tilesRange;
	}
	
	
	private boolean iSMetaTileSize(String mimeType) {
		if (ResponseFormat.GEOJSON.getFormat().equalsIgnoreCase(mimeType)
				|| ResponseFormat.TOPOJSON.getFormat().equalsIgnoreCase(mimeType)
				|| ResponseFormat.PBF.getFormat().equalsIgnoreCase(mimeType)
				|| ResponseFormat.TERRAIN.getFormat().equalsIgnoreCase(mimeType)
				/*|| ResponseFormat.TIFF.getFormat().equalsIgnoreCase(mimeType)*/) {
			return true;
		}
		return false;
	}
	
	private void calcuteRasterTile(SeedTaskTileQueue seedTaskTileQueue, int i, int[] tilesRange,boolean isRaster,boolean isStart) {
		for (int m = tilesRange[1]; m <= tilesRange[3]; m+=4) {
			for (int n = tilesRange[0]; n <= tilesRange[2]; n+=4) {
				int[] tile = new int[5];
				if (n+4 > tilesRange[2] && m+4 < tilesRange[3]) {
					tile[0] = n;
					tile[1] = m;
					tile[2] = tilesRange[2];
					tile[3] = m+3;
				} else if (m+4 > tilesRange[3] && n+4 < tilesRange[2]) {
					tile[0] = n;
					tile[1] = m;
					tile[2] = n+3;
					tile[3] = tilesRange[3];
				} else if (m+4 > tilesRange[3] && m+4 > tilesRange[3]) {
					tile[0] = n;
					tile[1] = m;
					tile[2] = tilesRange[2];
					tile[3] = tilesRange[3];
				} else {
					tile[0] = n;
					tile[1] = m;
					tile[2] = n+3;
					tile[3] = m+3;
				}
				tile[4] = i;
				seedTaskTileQueue.addTileTask(tile,isRaster,isStart);
			}
		}
	}

	/**
	 * 清理未关闭的线程数
	 */
	public void drain() {
		lock.writeLock().lock();
		try {
			dispatchesWithoutDrain = 0;
			threadPool.purge();
			for (Iterator<Entry<Integer, TileTask>> it = this.currentPool.entrySet().iterator(); it.hasNext();) {
				if (it.next().getValue().getThreadState() == TILESTATUS.DONE) {
					it.remove();
				}
			}
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	/**
	 * 开始执行渲染请求
	 * @param renderRequestParam
	 * @param tileIndex 瓦片索引
	 */
	public void startSeedTileTask(PyRenderRequestParam renderRequestParam,
								  int[] tileIndex,
								  String taskName) {
		byte[] buf = renderService.renderImageByRequestParam(renderRequestParam);
		if (buf == null) {
			return;
		}
		
		SeedTaskInstance taskInstance = getTaskInstance(taskName);
		RenderResouce resource = new RenderResouce(taskInstance,buf,tileIndex,
				renderRequestParam.getStyleName(),renderRequestParam.getFormat());
		//异步写数据
//		CompletableFuture.supplyAsync(() -> {
			saveTileResource(resource);
//			return "文件保存成功";
//		}, threadPool);
	}
	
	private void saveTileResource(RenderResouce resource) {
		String styleName = resource.getStyleName();
		SeedTaskInstance taskInstance = resource.getTaskInstance();
		int[] tileIndex = resource.getTileIndex();
		byte[] buf = resource.getBuf();
		if (iSMetaTileSize(resource.getFormat())) {
			TileResourceInfo tileResource = new TileResourceInfo(buf, tileIndex[0],tileIndex[1],tileIndex[4]);
			saveTileResource(tileResource,taskInstance,styleName);
		}else {
			//解析为bufferImage,顺序读取，裁剪
			ByteArrayInputStream bais = new ByteArrayInputStream(buf);
			BufferedImage image = null;
			try {
				image = ImageIO.read(bais);
			} catch (IOException e) {
				e.printStackTrace();
			}
			List<TileResourceInfo> list = createTiles(image,
													  tileIndex,
													  taskInstance.getTileHeight(),
													  taskInstance.getTileHeight(),
													  taskInstance.getTileFormat());
			for (TileResourceInfo tileResourceInfo : list) {
				saveTileResource(tileResourceInfo,taskInstance,styleName);
			}
		}
	}
	
	
	/**
	 * 根据瓦片的位置切分每一个图片
	 * @param image
	 * @param tileIndex
	 * @return
	 */
	private List<TileResourceInfo> createTiles(BufferedImage image,
											   int[] tileIndex,
											   int tileWidth,
											   int tileHeight,
											   String format){
		//i 是y的变换    j 是x的变换
		int m = 0;
		int n = 0;
		List<TileResourceInfo> list = new ArrayList<TileResourceInfo>();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (long i =  tileIndex[1]; i <= tileIndex[3]; i++) {
			for (long j = tileIndex[0]; j <= tileIndex[2]; j++) {
 				BufferedImage tileImage = image.getSubimage(n*tileWidth, m*tileHeight, tileWidth, tileHeight);
				try {
					ImageIO.write(tileImage, simpleMineType(format), baos);
					TileResourceInfo tileResource = new TileResourceInfo(baos.toByteArray(), j,i,tileIndex[4]);
					list.add(tileResource);
					baos.reset();
				} catch (IOException e) {
					e.printStackTrace();
				}
				n++;
			}
			n=0;
			m++;
		}
		return list;
	}
	

	public void saveTileResource(TileResourceInfo resourceInfo,SeedTaskInstance taskInstance,String styleName) {
		int storeType = taskInstance.getStoreType();
		int level = (int)resourceInfo.getZ();
		int col = (int)resourceInfo.getX();
		int row = (int)resourceInfo.getY();
		String format = taskInstance.getTileFormat();
		String gridSet = taskInstance.getSrs();
		int tileSize = taskInstance.getTileWidth();
		String layerName = taskInstance.getLayerName();
		String taskName = taskInstance.getTaskName();
		LayerStoreTypeInfo storeInfo = null;
		if(StoreType.getStoreType(storeType).equals(StoreType.MONGODB)) {
			//创建mongodb的集合
			String collectionName = createMongodbOrSqlLiteName(level,row,col,taskName,gridSet,format,taskInstance.getTileWidth());
			//如果不包含，则需要创建索引
			if (!mongdoCollection.contains(collectionName)) {
				mongdoCollection.add(collectionName);
				storeInfo = new LayerStoreTypeInfo(collectionName,layerName,storeType,simpleMineType(format));
//				commonMapper.saveLayerStoreInfo(storeInfo);
				Index indexDefinition = new Index();
				indexDefinition.on("x", Sort.Direction.ASC).on("y", Sort.Direction.ASC).on("z", Sort.Direction.ASC);
				try {
					mongoTemplate.indexOps(collectionName).ensureIndex(indexDefinition);
				} catch (Exception e) {
					log.error("SeedTaskUtil saveTileResource has error {}", ExceptionsUtil.stackTraceToStr(e));
					if(ExceptionsUtil.isMongoDBError(ExceptionsUtil.stackTraceToStr(e))) {
						systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, 
								ExceptionsUtil.STATUS_MONGODB, "System","MongoDB服务",CommonUtil.getLocalHost()));
						throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.STATUS_MONGODB);
					} else {
						throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.STATUS_MONGODB));
					}
				}
			}
			//插入
			try {
				mongoTemplate.insert(resourceInfo, collectionName);
			} catch (Exception e) {
				log.error("SeedTaskUtil saveTileResource has error {}", ExceptionsUtil.stackTraceToStr(e));
				if(ExceptionsUtil.isMongoDBError(ExceptionsUtil.stackTraceToStr(e))) {
					systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, 
							ExceptionsUtil.STATUS_MONGODB, "System","MongoDB服务",CommonUtil.getLocalHost()));
					throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.STATUS_MONGODB);
				} else {
					throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.STATUS_MONGODB));
				}
			}
//		}else if (StoreType.getStoreType(storeType).equals(StoreType.POSTGRRSQL)) {
//			//创建数据库和表
//			String dbName = createDBName(taskName,level,col,format,gridSet,taskInstance.getTileWidth());
//			String tblName = createTableName(level,row,col);
//			if (existsDbAndTable.isEmpty()) {
//				initDbName();
//			}
//			if (existsDbAndTable.containsKey(dbName)) {
//				List<String> existsTblNames = existsDbAndTable.get(dbName);
//				if (!existsTblNames.contains(tblName)) {
//					TBLlock.lock();
//					try {
//						DataSourceContextHolder.setDataSource(dbName);
//						createNewTable(tblName);
//						DataSourceContextHolder.setDataSource("default");
//						int sourceId = commonMapper.querySourceId(dbName);
//						commonMapper.savaTableSource(new TableSource(sourceId,tblName));
//						existsTblNames.add(tblName);
//						existsDbAndTable.put(dbName, existsTblNames);
//					} finally {
//						TBLlock.unlock();
//					}
//				}
//			}else {
//				dbLock.lock();
//				try {
//					//创建数据库
//					if (!existsDbAndTable.containsKey(dbName)) {
//						DruidDataSource dataSource = createNewDB(dbName,storeType,layerName,null);
//						DataSourceContextHolder.setDataSource(dbName);
//						createNewTable(tblName);
//						storeInfo = new LayerStoreTypeInfo(dbName,layerName,storeType,simpleMineType(format));
//						saveTileStoreInfo(layerName, dbName, tblName, dataSource);
//					}
//				} catch (CloneNotSupportedException e) {
//					//TODO 记录日志
//					dbLock.unlock();
//				} finally {
//					dbLock.unlock();
//				}
//			}
//			//存储数据
//			DataSourceContextHolder.setDataSource(dbName);
//			tileCacheMapper.saveTileResourceInfo(resourceInfo,tblName);
		}else if (StoreType.getStoreType(storeType).equals(StoreType.SQLITE)) {
			//创建数据库
			String sqliteDb = createMongodbOrSqlLiteName(level,row,col,taskName,gridSet,format,taskInstance.getTileWidth());
			String storePath = taskInstance.getStorePath();
			if (existsDbAndTable.isEmpty()) {
				initDbName();
			}
			if (!existsDbAndTable.containsKey(sqliteDb)) {
				sqllock.lock();
				try {
					//创建数据库
					if (!existsDbAndTable.containsKey(sqliteDb)) {
						DruidDataSource dataSource = createNewDB(sqliteDb,storeType,layerName,storePath);
						DataSourceEntity dataSouceEntity = new DataSourceEntity(layerName,sqliteDb, dataSource.getUsername(), dataSource.getPassword(), 
								dataSource.getDriverClassName(), dataSource.getUrl());
						DataSourceContextHolder.setDataSource(sqliteDb);
						//修改模式
						tileCacheMapper.openSqliteWALMode();
						createSqliteNewTable("tiles");
						DataSourceContextHolder.setDataSource("default");
						commonMapper.saveDataSource(dataSouceEntity);
						storeInfo = new LayerStoreTypeInfo(sqliteDb,layerName,storeType,storePath,simpleMineType(format));
//						commonMapper.saveLayerStoreInfo(storeInfo);
						existsDbAndTable.put(sqliteDb,new ArrayList<String>());
					}
				} catch (CloneNotSupportedException e) {
					//TODO 记录日志
					sqllock.unlock();
				} finally {
					sqllock.unlock();
				}
			}
			saveTileResourceToSqlite(resourceInfo, layerName, sqliteDb, storePath);
		} else if (StoreType.getStoreType(storeType).equals(StoreType.TDB)) {
			//创建对应文件夹
			String tdbStorePath = taskInstance.getStorePath();
			String tdbDbName = createTdbName(format,taskName,gridSet,tileSize);
//			String tdbDbName = getExistTdbSavedFile(layerName,format,taskName,gridSet,tileSize,tdbStorePath);
			File file = new File(tdbStorePath,tdbDbName);
			File cFile = new File(file,"config.tdb");
			//TDB配置文件实体信息，保存切图数据时需要该实体中的信息，因此需要确保该信息的存在
			TDBDataConfig config = null;
			if(TdbUtils.tdbMap.containsKey(tdbDbName)) {
				config = TdbUtils.tdbMap.get(tdbDbName);
			} else {
				//TDB配置实体信息部分，有部分功能尚未适配，高程和法线待补全
				TileRequestInfo tileInfo = new TileRequestInfo();
				tileInfo.setMineType(format);
				tileInfo.setGridSet(gridSet);
				tileInfo.setTileWidth(tileSize);
				tileInfo.setStyle(styleName);
				config = TdbUtils.getTDBDataConfig(tileInfo,taskInstance);
				if (!TdbUtils.tdbMap.containsKey(tdbDbName)) TdbUtils.tdbMap.put(tdbDbName, config);
			}
			if (taskInstance.getZoomEnd() > config.getNMaxLod()) {
				config.setNMaxLod(taskInstance.getZoomEnd());
				File configFile = new File(file,"config.tdb");
				if (configFile.exists()) configFile.delete();
				
				File readFile = new File(file,"readme.xml");
				if (readFile.exists()) readFile.delete();
				//写readme.xml文件
				TdbConfigFileGenerate.writeReadmeFile(file.getAbsolutePath(),config);
				//写config.tab文件
				TdbConfigFileGenerate.writeTDBConfigFile(file.getAbsolutePath(), config);
			}
			if (!file.exists()) {
				storeInfo = new LayerStoreTypeInfo(tdbDbName,layerName,storeType,tdbStorePath,simpleMineType(format));
				//设置默认数据源
				DataSourceContextHolder.setDataSource("default");
//				commonMapper.saveLayerStoreInfo(storeInfo);
				//创建切图缓存目录
				file.mkdirs();
				//写readme.xml文件
				TdbConfigFileGenerate.writeReadmeFile(file.getAbsolutePath(),config);
				//写config.tab文件
				TdbConfigFileGenerate.writeTDBConfigFile(file.getAbsolutePath(), config);
			}
			if (!cFile.exists()){
				//写readme.xml文件
				TdbConfigFileGenerate.writeReadmeFile(file.getAbsolutePath(),config);
				//写config.tab文件
				TdbConfigFileGenerate.writeTDBConfigFile(file.getAbsolutePath(), config);
			}
			Map<String,String> tdbParams = new HashMap<String, String>();
			tdbParams.put("destPath", file.getAbsolutePath() + File.separator);
			tdbParams.put("serverName", layerName);
			//为了兼容studio的瓦片存储而修改
//			int tileRow = (int) (Math.pow(2, level)) - row - 1;
//			resourceInfo.setY(tileRow);
            String rowNum = "";
            if(guassCoordinates.getGuassCoordinates().contains(gridSet)) {
                List<String> tilecount = guassCoordinates.getTileParam().get(gridSet.replace(":", "")).get("tilecount");
                rowNum = tilecount.get(level);
            }
			customDataLoad.insertTileImage(resourceInfo, config,tdbParams, rowNum);
		} else if (StoreType.getStoreType(storeType).equals(StoreType.TMS)) {
			List<TileTaskEntity> taskEntity = renderTileServiceImpl.statisticSeedTaskInfo(taskName);
			if(taskEntity.get(0).getTaskStatus() != TileTaskStatus.DEAD.getStatus() ) {
				zosIsFlag = true;
			}
			//创建对应文件夹
			int tileRow = (int) (Math.pow(2, level)) - row - 1;
			String tmsStorePath = taskInstance.getStorePath();
			String tmsDirName = ExportTaskUtils.getExportDataName(taskInstance);
			String tmpPath = Constant.TILE_CACHE_TMP + "exportDir" + File.separator + System.currentTimeMillis();
			File file = new File(tmsStorePath);
			if (!file.exists()) file.mkdirs();
			try {
				dbLock.lock();
				ZipOutputStream zos = null;
				try {
					//获取文件压缩流
					if (ExportTaskUtils.containsZos(tmsDirName)) {
						zos = ExportTaskUtils.zosStreamMap.get(tmsDirName);
					} else {
						File zipFile = new File(tmsStorePath,tmsDirName + ".zip");
//						if(!zipFile.exists()) {
//							storeInfo = new LayerStoreTypeInfo(tmsDirName,layerName,storeType,tmsStorePath,simpleMineType(format));
//						}
						zos = new ZipOutputStream(new FileOutputStream(zipFile),StandardCharsets.UTF_8);
						ExportTaskUtils.addZos2Map(tmsDirName, zos);
					}
					if (!ExportTaskUtils.containsTmsConfigFile(tmsDirName)) {
						File config = ExportTaskUtils.buildTmsConfigFile(taskInstance,ExportTaskUtils.getUTF8FilePath(tmpPath));
						ExportTaskUtils.addTmsConfigFile2Map(tmsDirName, config);
						ExportTaskUtils.writeTmsZipFile(config,null,tmsDirName + File.separator + "tms.xml",zos);
					}
					StringBuilder sb = new StringBuilder(tmsDirName);
					sb.append(File.separator).append(level).append(File.separator)
					.append(col).append(File.separator)
					.append(tileRow).append(".").append(simpleMineType(format));
					if(zosIsFlag) {
						ExportTaskUtils.writeTmsZipFile(null,resourceInfo.getByteResource(),sb.toString(),zos);
					}				
				} catch (IOException e) {
//					FileUtil.closeStream(zos);
					e.printStackTrace();
				}
			} finally {
				dbLock.unlock();
			}
		}
		if (storeInfo != null) {
			saveStoreType(gridSet, tileSize, layerName, storeInfo);
		}
	}

	public synchronized void saveTileResourceToSqlite(TileResourceInfo resourceInfo, String layerName, String sqliteDb,
			String storePath) {
		createSqliteFile(sqliteDb, layerName,storePath);
		DataSourceContextHolder.setDataSource(sqliteDb);
		tileCacheMapper.saveTileResourceInfo(resourceInfo,"tiles");
	}

	public void saveStoreType(String gridSet, int tileSize, String layerName, LayerStoreTypeInfo storeInfo) {
		storeInfo.setSrs(gridSet);
		storeInfo.setTileSize(tileSize);
		DataSourceContextHolder.setDataSource("default");
		commonMapper.saveLayerStoreInfo(storeInfo);
		if (storeInfo != null) {
			List<LayerStoreTypeInfo> list = null;
			if (layerStoreTypes.containsKey(layerName)) {
				list = layerStoreTypes.get(layerName);
			}else{
				list = new ArrayList<LayerStoreTypeInfo>();
			}
			layerStoreTypes.put(layerName, list);
		}
	}
	
	public String getExistTdbSavedFile(String layerName,String format,String taskName,String gridSet,int tileWidth,String tdbStorePath) {
		String tdbFileName = null;
		List<SeedTaskInstance> tasks =  tileCacheMapper.queryTaskInstancesByLayerName(layerName);
		Map<String,String> tdbFiles = new HashMap<String, String>();
		if (tasks.size() > 0) {
			File tempF = new File(tdbStorePath);
			tempF.list(new FilenameFilter() {
				
				@Override
				public boolean accept(File dir, String name) {
					String taskName = name.substring(0,name.indexOf("_"));
					if (!tdbFiles.containsKey(taskName)) {
						tdbFiles.put(taskName, name);
					}
					return false;
				}
			});
			
			for (SeedTaskInstance seedTaskInstance : tasks) {
				String taskName1 = seedTaskInstance.getTaskName();
				if (tdbFiles.containsKey(taskName1)) {
					File file = new File(tdbStorePath,tdbFiles.get(taskName1));
					if (file.exists() && file.isDirectory()) {
						tdbFileName = tdbFiles.get(taskName1);
						break;
					}
				}
			}
		}
		if (tdbFileName == null) {
			tdbFileName = createTdbName(format,taskName,gridSet,tileWidth);
		}
		return tdbFileName;
	}

	/**
	 * 创建tdb的名称
	 * @param tileInfo 请求信息
	 * @return
	 */
	public static String createTdbName(String format,
							    String layerName,
							    String gridSet,
							    int width) {
		String mimeType = simpleMineType(format);
		String dbName = new StringBuilder(layerName).append("_").append(gridSet)
				.append("_").append(mimeType).append("_").append(width).toString();
		return dbName.replace(":", "_");
	}
	
	/**
	 * 保存瓦片存储信息
	 * @param storeType
	 * @param format
	 * @param layerName
	 * @param dbName
	 * @param tblName
	 * @param dataSource
	 */
	private void saveTileStoreInfo(String layerName, String dbName, String tblName,
			DruidDataSource dataSource) {
		DataSourceEntity dataSouceEntity = new DataSourceEntity(layerName,dbName, dataSource.getUsername(), dataSource.getPassword(), 
				dataSource.getDriverClassName(), dataSource.getUrl());
		//保存到缓存中
		List<String> tableList = new ArrayList<String>();
		tableList.add(tblName);
		//数据库表信息存储到dynamic_data_source和table_source
		DataSourceContextHolder.setDataSource("default");
		commonMapper.saveDataSource(dataSouceEntity);
		commonMapper.savaTableSource(new TableSource(dataSouceEntity.getId(), tblName));
		existsDbAndTable.put(dbName,tableList);
	}
	
	/**
	 * 创建sqlite的数据表
	 * @param tableName
	 */
	private void createSqliteNewTable(String tableName) {
		//捕获下异常，如果表名应存在，则不需要创建
		try {
			commonMapper.createSqliteTableForDB(tableName);
			commonMapper.createIndex(tableName+"_index",tableName);
		} catch (Exception e) {
			log.warn("SeedTaskUtil createSqliteNewTable has warn:{}", "tiles 表已经存在，无需重复创建");
		}
	}
	
	/**
	 * 创建数据表
	 * @param tableName
	 */
	private void createNewTable(String tableName) {
		commonMapper.createTableForDB(tableName);
		commonMapper.createIndex(tableName+"_index",tableName);
	}
	
	private DruidDataSource createNewDB(String dbName, 
									   int storeTypeId, 
									   String layerName,
									   String storePath) throws CloneNotSupportedException {
		DruidDataSource dataSource = createDataSource(dbName,storeTypeId,layerName,storePath);
		DataSourceUtil.addDataSource(dbName, dataSource);
		return dataSource;
	}
	
	/**
	 * 
	 * @return
	 * @throws CloneNotSupportedException 
	 * @throws IOException 
	 */
	public DruidDataSource createDataSource(String dbName,
											int storeType,
											String layerName,
											String storePath) throws CloneNotSupportedException {
		DruidDataSource dataSource = (DruidDataSource) dynamicDataSource.getDataSouceMap().get("default");
		DruidDataSource baseSource = (DruidDataSource) dataSource.clone();
//		if (StoreType.getStoreType(storeType).equals(StoreType.POSTGRRSQL)) {
//			baseSource.setDriver(createDriver(Constant.PSQL_DRIVER_CLASS));
//			jdbcUrl = jdbcUrl.replace("fs_data", "");
//			baseSource.setUrl(jdbcUrl + dbName);
//			//设置psql数据库连接的若干配置
//			baseSource.setInitialSize(0);
//			baseSource.setMaxActive(500);
//			baseSource.setMaxWait(60000);
//			baseSource.setTimeBetweenEvictionRunsMillis(60000);
//			baseSource.setMinEvictableIdleTimeMillis(60000);
//			//通过如下两个配置解决数据库连接失败时不断重复连接问题
//			baseSource.setBreakAfterAcquireFailure(true);
//			baseSource.setConnectionErrorRetryAttempts(3);
//			DataSourceContextHolder.setDataSource("default");
//			commonMapper.createDBSource("\"" + dbName + "\"");
//			return baseSource;
//		}else
		if (StoreType.getStoreType(storeType).equals(StoreType.SQLITE)) {
			createSqliteFile(dbName,layerName,storePath);
			baseSource.setDriverClassName(Constant.SQLITE_DRIVER_CLASS);
			baseSource.setDriver(createDriver(Constant.SQLITE_DRIVER_CLASS));
			baseSource.setValidationQuery(null);
			baseSource.setDbType("sqlite");
			String sqlitePath = new StringBuilder(storePath).append(File.separator).append(layerName).append(File.separator).append(dbName).append(".db").toString();
			baseSource.setUrl(String.format("jdbc:sqlite:"+sqlitePath));
		}
		return baseSource;
	}
	
	/**
	 * sqlite的文件夹路径在
	 * @param dbName
	 * @param layerName
	 * @throws IOException
	 */
	private void createSqliteFile(String dbName,String layerName,String storePath) {
		StringBuilder sb = new StringBuilder(storePath);
		sb.append(File.separator).append(layerName).append(File.separator).append(dbName).append(".db");
		File file = new File(sb.toString());
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
//				systemLogUtil.synchSysLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, ExceptionsUtil.formatInfo(ExceptionsUtil.FILE_CREATE_FAIL,layerName + "服务Sqlite瓦片存储文件"), 
//						"System","Sqlite服务", ExceptionsUtil.getServerIp(null)));
				log.error("PytcUtil createSqliteFile has error:" + ExceptionsUtil.stackTraceToStr(e));
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_CREATE, layerName + "服务Sqlite瓦片存储文件"));
			}
		}
	}
	
	/**
	 * 获取切换后的数据库驱动
	 * @param driverClassName
	 * @return
	 * @throws ClassNotFoundException 
	 */
	public static Driver createDriver(String driverClassName) {
		Driver dirver = null;
		try {
			dirver = (Driver) Class.forName(driverClassName).newInstance();
		} catch (ClassNotFoundException e) {
			log.error("PytcUtil createDriver has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_NOT_FOUND, 
					driverClassName + "驱动类"));
		} catch (InstantiationException e) {
			log.error("PytcUtil createDriver has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_CREATE, 
					driverClassName + "驱动类实例"));
		} catch (IllegalAccessException e) {
			log.error("PytcUtil createDriver has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ArgumentException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.ARGGUMENT_ILLEGAL, 
					driverClassName + "驱动类字段"));
		}
		return dirver;
	}
	
	/**
	 * 分库策略为当前0——15级为一个库
	 * 16级以上以第8级分块存储，每一列分块作为一个数据库，数据库命名
	 * @return
	 */
	private String createDBName(String layerName,
							    int level,
							    int tileCol,
							    String format,
							    String gridSet,
							    int tileWidth) {
		format = simpleMineType(format); 
		if (gridSet.indexOf(":") > -1) {
			gridSet = gridSet.split(":")[1];
		}
		String dbName = null;
		if (level < 16) {
			dbName = MessageFormat.format("{0}_0_15_{1}_{2}_{3}",layerName,gridSet,format,tileWidth);
		}
		if (level > 15) {
			int col8 = (int) (tileCol / Math.pow(2, (level - 8)));
			dbName = MessageFormat.format("{0}_16_col{1}_{2}_{3}_{4}",layerName,col8,gridSet,format,tileWidth);
		}
		return dbName;
	}
	
	/**
	 * 创建表规则  0-10级为一张表 表的命名规则为0_10
	 * @return
	 */
	public String createTableName(int level, int tileRow, int tileCol) {
		if (level < 11) {
			return "tile_0_10";
		}
		if (level > 10 && level < 16) {
			int row3 = (int) (tileRow / Math.pow(2, (level - 3)));
			int col3 = (int) (tileCol / Math.pow(2, (level - 3)));
			return MessageFormat.format("tile_11_15_row{0}_col{1}",row3,col3);
		}
		if (level > 15) {
			int row8 = (int) (tileRow / Math.pow(2, (level - 8)));
			return MessageFormat.format("tile_16_row{0}",row8);
		}
		return null;
	}
	
	/**
	 * 获取请求格式的缩写
	 * @param tileInfo
	 * @return
	 */
	public static String simpleMineType(String mineType) {
		if (mineType.indexOf(";") > -1) {
			if (mineType.contains("type=mapbox-vector")) {
				mineType = "protobuf";
			}else if(mineType.contains("type=geojson")){
				mineType = "geojson";
			} else if (mineType.contains("type=topojson")) {
				mineType = "topojson";
			}
		}
		if (mineType.indexOf("/") > -1) {
			mineType = mineType.split("/")[1];
		}
		return mineType;
	}
	
	/**
	 * 创建mongodb与sqlite的数据库名称（mongodb是集合）
	 * @param level     级别
	 * @param tileRow   行
	 * @param tileCol   列
	 * @param layerName 图层
	 * @param gridSet   投影
	 * @param format    格式
	 * @return
	 */
	private String createMongodbOrSqlLiteName(int level,
											  int tileRow,
											  int tileCol,
											  String layerName,
											  String gridSet,
											  String format,
											  int tileWidth) {
		format = simpleMineType(format);
		if (gridSet.indexOf(":") > -1) {
			gridSet = gridSet.split(":")[1];
		}
		String dbName = null;
		if (level < 11) {
			dbName =  MessageFormat.format("{0}_0_10_{1}_{2}_{3}",layerName,gridSet,format,tileWidth);
		} else if (10 < level && level < 17) {
			//按照第6级分块存储
			int row6 = (int) (tileRow / Math.pow(2, (level - 6)));
			int col6 = (int) (tileCol / Math.pow(2, (level - 6)));
			dbName =  MessageFormat.format("{0}_11_15_row{1}_col{2}_{3}_{4}_{5}",layerName,row6,col6,gridSet,format,tileWidth);
		} else if (16 < level && level < 20) {
			int row10 = (int) (tileRow / Math.pow(2, (level - 10)));
			int col10 = (int) (tileCol / Math.pow(2, (level - 10)));
			dbName =  MessageFormat.format("{0}_16_20_row{1}_col{2}_{3}_{4}_{5}",layerName,row10,col10,gridSet,format,tileWidth);
		} else if (level > 20) {
			//TODO 设计库表能力
		}
		return dbName;
	}
	
	/**
	 * 初始化Db以及db对应的表
	 */
	private void initDbName() {
		DataSourceContextHolder.setDataSource("default");
		List<DataSourceEntity> DataSources = commonMapper.queryDataSourceInfo();
		for (DataSourceEntity dataSourceEntity : DataSources) {
			existsDbAndTable.put(dataSourceEntity.getSourceKey(),convertTableNames(dataSourceEntity.getTableSources()));
		}
	}
	
	public List<String> getTblNames(String dbSource){
		if (existsDbAndTable.isEmpty()) {
			initDbName();
		}
		if (existsDbAndTable.containsKey(dbSource)) {
			return existsDbAndTable.get(dbSource);
		}
		return null;
	}
	
	private List<String> convertTableNames(List<TableSource> list) {
		List<String> tableSet = new ArrayList<String>();
		if (list != null && !list.isEmpty()) {
			list.stream().forEach((tableSource) -> {
				tableSet.add(tableSource.getTableName());
			});
		}
		return tableSet;
	}
	
	public void removeDbAndTables(String dbName){
		Iterator<Entry<String, List<String>>> it = existsDbAndTable.entrySet().iterator();
		while(it.hasNext()) {
			String sourceName = it.next().getKey();
			if (sourceName.equals(dbName)) {
				it.remove();
			}
		}
	}
	
	/**
	 * 修改服务状态为切图进行整
	 * @param layerName
	 */
	public void updateServerStatus(String layerName,int status) {
		try {
			tileCacheMapper.updateLayerStatus(layerName, status);
		} catch (Exception e) {
			log.error("SeedTaskUtil updateServerStatus has error : {}",ExceptionsUtil.stackTraceToStr(e));
		}
	}
	
	public void updateTileEntity(TileTaskEntity tileTaskEntity) {
		try {
			List<TileTaskEntity> list = new ArrayList<TileTaskEntity>();
			list.add(tileTaskEntity);
			tileCacheMapper.updateTileTaskEntity(list);
		} catch (Exception e) {
			log.error("SeedTaskUtil updateTileEntity has error : {}",ExceptionsUtil.stackTraceToStr(e));
		}
	}
	
	/**
	 * 切图完成推送消息
	 * @param message
	 */
	public void noticeWebSocketTileFinshed(String message) {
//		Map<String,Object> msgMap = new HashMap<String,Object>();
//		msgMap.put("msg", message);
//		JSONObject jsonObject = new JSONObject(msgMap);
		mapService.noticeWebSocketTileFinshed(message);
//		webSocketManager.sendMessage(jsonObject.toJSONString());
		
	}
	
	
	public List<LayerStoreTypeInfo> getLayerStoreInfo(String layerName){
		if (layerStoreTypes.containsKey(layerName)) {
			List<LayerStoreTypeInfo> list = layerStoreTypes.get(layerName);
			if (list != null && !list.isEmpty()) {
				return list;
			}
		}
		DataSourceContextHolder.setDataSource("default");
		return commonMapper.queryLayerStore(layerName);
	}

	public void removeLayerStoreType(String layerName) {
		Iterator<Entry<String, List<LayerStoreTypeInfo>>> it = layerStoreTypes.entrySet().iterator();
		while(it.hasNext()) {
			Entry<String, List<LayerStoreTypeInfo>> entry = it.next();
			if (entry.getKey().equals(layerName)) {
				it.remove();
			}
		}
	}
	
	public String getDbName(TileRequestInfo tileInfo, int storeTypeId)  {
		String objectDbName = null;
		switch (storeTypeId) {
//		//psql
//		case 1:
//			objectDbName = createDBName(tileInfo.getLayerName(),
//										tileInfo.getZoomLevel(),
//										tileInfo.getTileCol(),
//										tileInfo.getMineType(),
//										tileInfo.getGridSet(),
//										tileInfo.getTileWidth());
//			break;
		//mongo和sqlite
		case 2:
		case 3:
			objectDbName = createMongodbOrSqlLiteName(tileInfo.getZoomLevel(),
													  tileInfo.getTileRow(),
													  tileInfo.getTileCol(),
													  tileInfo.getLayerName(),
													  tileInfo.getGridSet(),
													  tileInfo.getMineType(),
													  tileInfo.getTileWidth());
			break;
		case 4:
			objectDbName = createTdbName(tileInfo.getMineType(),tileInfo.getLayerName(),tileInfo.getGridSet(),tileInfo.getTileWidth());
			break;
		case 5:
			SeedTaskInstance taskInstance = new SeedTaskInstance();
			taskInstance.setSrs(tileInfo.getGridSet());
			taskInstance.setTileFormat(tileInfo.getMineType());
			taskInstance.setLayerName(tileInfo.getLayerName());
			taskInstance.setTileHeight(tileInfo.getTileHeight());
			taskInstance.setTileWidth(tileInfo.getTileWidth());
			taskInstance.setZoomEnd(tileInfo.getZoomLevel());
			objectDbName = ExportTaskUtils.getExportDataName(taskInstance);
			break;
		default:
			break;
		}
		return objectDbName;
	}
	
	
	public GridSubset getGridSubSet(String layerName,String gridSet,LayerBaseInfo layerBaseInfo) throws MismatchedDimensionException, NumberFormatException, NoSuchAuthorityCodeException, FactoryException {
		GridSubset gridSubSet = null;
		String gridSetKey = layerName + "-" + gridSet;
		if (gridSubSets.containsKey(gridSetKey)) {
			gridSubSet = gridSubSets.get(gridSetKey);
		} else {
			gridSubSet = GridSubsetFactory.createGridSubSet(gridSetBroker.get(gridSet),getBoundingBoxFromLayer(layerBaseInfo,gridSet), 0, layerBaseInfo.getSupportMaxLevel());
			gridSubSets.put(gridSetKey, gridSubSet);
		}
		return gridSubSet;
	}
	
	/**
	 * 获取当前图层的bbox
	 * @param layerName
	 * @return
	 * @throws FactoryException 
	 * @throws NoSuchAuthorityCodeException 
	 * @throws NumberFormatException 
	 * @throws MismatchedDimensionException 
	 */
	private BoundingBox getBoundingBoxFromLayer(LayerBaseInfo layerBaseInfo,String gridSet) throws MismatchedDimensionException, NumberFormatException, NoSuchAuthorityCodeException, FactoryException {
		//获取当前的bbox
		String nativeBBox = layerBaseInfo.getNativaBbox();
		String[] bboxArray = nativeBBox.split(";");
		ReferencedEnvelope envelope = new ReferencedEnvelope(Double.parseDouble(bboxArray[0]), Double.parseDouble(bboxArray[2]),
															 Double.parseDouble(bboxArray[1]), Double.parseDouble(bboxArray[3]), 
															 CRS.decode("EPSG:4326",true));
		if (!"EPSG:4326".equalsIgnoreCase(gridSet)) {
			envelope = (ReferencedEnvelope) GeometryUtil.transformCoordinateSystem(envelope, gridSet);
		}
		
		return new BoundingBox(envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY());
	}
	
	private String createTileIndex(SeedTaskInstance taskInstance,int[] tiledIndex) {
		StringBuilder sb = new StringBuilder(taskInstance.getLayerName());
		sb.append("_").append(taskInstance.getSrs().split(":")[1]).append("_").append(simpleMineType(taskInstance.getTileFormat())).append("_").append(taskInstance.getTileWidth())
		.append(tiledIndex[2]).append(tiledIndex[1]).append(tiledIndex[0]);
		return sb.toString();
	}
	
	private String createPreTileIndex(SeedTaskInstance taskInstance) {
		StringBuilder sb = new StringBuilder(taskInstance.getLayerName());
		sb.append("_").append(taskInstance.getSrs().split(":")[1]).append("_").append(simpleMineType(taskInstance.getTileFormat())).append("_").append(taskInstance.getTileWidth());
		return sb.toString();
	}
	
	public void clearFinshedTiles(String taskName) {
		SeedTaskInstance taskInstance = getTaskInstance(taskName);
		String tileUniqueCode = createPreTileIndex(taskInstance);
		for (String tileIndex : cacheFinshedTiles) {
			if (tileIndex.startsWith(tileUniqueCode)) {
				cacheFinshedTiles.remove(tileIndex);
			}
		}
	}
	
	public boolean isExists(int[] tiledIndex,String taskName) {
		SeedTaskInstance taskInstance = getTaskInstance(taskName);
		//重新切图直接跳过
		if ("reseed".equalsIgnoreCase(taskInstance.getSeedType())) {
			return false;
		}
		List<LayerStoreTypeInfo> storeTypes = getLayerStoreInfo(taskInstance.getLayerName());
		if (storeTypes == null || storeTypes.isEmpty()) {
			return false;
		}
		boolean flag = false;
		LayerStoreTypeInfo storeTypeInfo = null;
		for (LayerStoreTypeInfo layerStoreTypeInfo : storeTypes) {
			//如果存在格式大小投影都一样的存储数据，且存储空间不一样，则直接返回false 进行重新切
			if (layerStoreTypeInfo.getSrs().equals(taskInstance.getSrs()) && 
					layerStoreTypeInfo.getTileFormat().equals(simpleMineType(taskInstance.getTileFormat()))
					&& layerStoreTypeInfo.getTileSize() == taskInstance.getTileWidth() 
					&& layerStoreTypeInfo.getStoreTypeId() != taskInstance.getStoreType()) {
				continue;
			}
			if (layerStoreTypeInfo.getSrs().equals(taskInstance.getSrs()) && 
					layerStoreTypeInfo.getTileFormat().equals(simpleMineType(taskInstance.getTileFormat()))
					&& layerStoreTypeInfo.getTileSize() == taskInstance.getTileWidth() 
					&& layerStoreTypeInfo.getStoreTypeId() == taskInstance.getStoreType()) {
				storeTypeInfo = layerStoreTypeInfo;
				flag = true;
			}
		}
		//如果存在，则查看后续的，不存在，则直接返回false
		if (!flag) {
			return false;
		}
		String tileUniqueCode = createTileIndex(taskInstance,tiledIndex);
		if (cacheFinshedTiles.contains(tileUniqueCode)) {
			return true;
		}
		if(inspectTileExists(tiledIndex, taskInstance, storeTypeInfo)) {
			cacheFinshedTiles.add(tileUniqueCode);
			return true;
		}
		return false;
	}

	public boolean inspectTileExists(int[] tiledIndex, SeedTaskInstance taskInstance,
			LayerStoreTypeInfo layerStoreTypeInfo) {
		int storeTypeId = layerStoreTypeInfo.getStoreTypeId();
		String sourceName = layerStoreTypeInfo.getDataSource();
		String tableName = null;
//		if (StoreType.getStoreType(storeTypeId).equals(StoreType.POSTGRRSQL)) {
//			tableName = createTableName((int)tiledIndex[2], (int)tiledIndex[1], (int)tiledIndex[0]);
//			// 如果当前数据库下有这个表，在处理，没有则不处理
//			List<String> tableNames = getTblNames(sourceName);
//			if (tableNames == null || !tableNames.contains(tableName)) {
//				return false;
//			}
//		}
		byte[] buf = getResourceFromDB(sourceName,
									   tableName,
									   storeTypeId,
									   taskInstance.getStorePath(),
									   taskInstance.getLayerName(),
									   layerStoreTypeInfo.getTileFormat(),
									   tiledIndex,
									   layerStoreTypeInfo.getSrs(),
									   layerStoreTypeInfo.getTileSize());
		if (buf != null) {
			return true;
		}
		return false;
	}
	
	
	public byte[] getResourceFromDB(String sourceName, 
								     String tableName, 
								     int storeType, 
								     String storePath,
								     String layerName, 
								     String format, 
								     int[] tileIndex,
								     String gridSet,
								     int tileWidth) {
		if (StoreType.getStoreType(storeType).equals(StoreType.MONGODB)) {
			String collectionName = sourceName;
			Query query = new Query();
			query.addCriteria(Criteria.where("x").is(tileIndex[0]).and("y").is(tileIndex[1]).and("z").is(tileIndex[2]));
			TileResourceInfo tileResourceInfo = null;
			try {
				tileResourceInfo= mongoTemplate.findOne(query, TileResourceInfo.class, collectionName);
			} catch (Exception e) {
				log.error("SeedTaskUtil getResourceFromDB has error {}", ExceptionsUtil.stackTraceToStr(e));
				if(ExceptionsUtil.isMongoDBError(ExceptionsUtil.stackTraceToStr(e))) {
					systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, 
							ExceptionsUtil.STATUS_MONGODB, "System","MongoDB服务",CommonUtil.getLocalHost()));
					throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.STATUS_MONGODB);
				} else {
					throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.STATUS_MONGODB));
				}
			}
			if (tileResourceInfo != null) {
				return tileResourceInfo.getByteResource();
			}
			return null;
//		} else if (StoreType.getStoreType(storeType).equals(StoreType.POSTGRRSQL)) {
//			// 获取数据库,需要将数据库名称转化为小写,数据库默认将大写转化为小写并创建库,如果此处不转化,会导致库不存在错误出现
//			String dataSourceName = sourceName;
//			DataSourceContextHolder.setDataSource(dataSourceName);
//			TileResourceInfo resourceInfo = tileCacheMapper.queryTileImageInfo(tableName, (int) tileIndex[0],
//					(int) tileIndex[1], (int) tileIndex[2]);
//			if (resourceInfo == null) {
//				return null;
//			}
//			return resourceInfo.getByteResource();
		} else if (StoreType.getStoreType(storeType).equals(StoreType.SQLITE)) {
			String sqlDb = sourceName;
			StringBuilder sb = new StringBuilder(storePath);
			sb.append(File.separator).append(layerName).append(File.separator).append(sqlDb).append(".db");
			File file = new File(sb.toString());
			if (!file.exists() || (file.exists() && file.length() == 0)) {
				return null;
			}
			Map<Object, Object> dataSourceMap = DynamicDataSource.getInstance().getDataSouceMap();
			DruidDataSource dataSource = (DruidDataSource) dataSourceMap.get(sqlDb);
			if (dataSource == null) {
				return null;
			}
			SQLitePoolManage.createConnectionPool(dataSource.getUrl());
			String sql = String.format(Constant.SQL, (int) tileIndex[0], (int) tileIndex[1], (int) tileIndex[2]);
			return SQLitePoolManage.getMbTilesImage(sql, dataSource.getUrl());
		} else if (StoreType.getStoreType(storeType).equals(StoreType.TDB)) {
			byte[] result = null;
			DataSourceContextHolder.setDataSource("default");
			List<SeedTaskInstance> tasks = tileCacheMapper.queryTaskInstancesByLayerName(layerName);
			if (tasks == null || tasks.size() == 0) return null;
			for (SeedTaskInstance seedTaskInstance : tasks) {
				String tdbPath = storePath + File.separator + createTdbName(format, seedTaskInstance.getTaskName(), gridSet, tileWidth);
				File tdbFile = new File(tdbPath);
				if (!tdbFile.exists()) {
					continue;
				}
				File configFile = new File(tdbPath, "config.tdb");
				if (!configFile.exists()) {
					continue;
				}
//				int tileRow = (int) (Math.pow(2, (int) tileIndex[2])) - (int) tileIndex[1] - 1;
				result = customDataLoad.getImageInfo((int) tileIndex[1], (int) tileIndex[0], (int) tileIndex[2], tdbPath,
						layerName);
				if (result != null) return result;
			}
		}
//			else if (StoreType.getStoreType(storeType).equals(StoreType.TMS)) {
//			byte[] result = null;
//			int tileRow = (int) (Math.pow(2, tileIndex[2])) - (int)tileIndex[1] - 1;
//			String tmsDirName = sourceName;
//			StringBuilder tmsPath = new StringBuilder(storePath);
//			tmsPath.append(File.separator).append(tmsDirName);
//			String zipPath = tmsPath.toString() + ".zip";
//			StringBuilder sb = new StringBuilder(tmsDirName);
//			sb.append(File.separator).append(tileIndex[2]).append(File.separator)
//			.append(tileIndex[0]).append(File.separator)
//			.append(tileRow).append(".").append(simpleMineType(format));
////			ZipFile zipFile = null;
//			try {
////				dbLock.lock();
////				zipFile = new ZipFile(zipPath);
//				ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipPath));
//				ZipEntry entry = null;
//				while ((entry = zipInputStream.getNextEntry()) != null) {
//					if(entry.getName().equals(sb.toString())) {
//						return entry.getName().getBytes();
//					}
//				}
//				if(entry == null) {
//					return null;
//				}
//			} catch (IOException e) {
//			}finally {
////				dbLock.unlock();
//			}
//			return result;
//		
//		}
		return null;
	}
	
	/**
	 * 根据瓦片的位置使用下一级合成当前瓦片
	 * @param tileIndex
	 * @param taskName
	 * @return BufferedImage
	 */
	public TileResourceInfo composeTile(int[] tileIndex,SeedTaskInstance taskInstance){
		//根据任务名称、投影、大小、格式确定对应的资源
		TileRequestInfo requestInfo = new TileRequestInfo();
		requestInfo.setLayerName(taskInstance.getLayerName());
		requestInfo.setZoomLevel((int)tileIndex[2]);
		requestInfo.setTileCol((int)tileIndex[0]);
		requestInfo.setTileRow((int)tileIndex[1]);
		requestInfo.setMineType(taskInstance.getTileFormat());
		requestInfo.setTileWidth(taskInstance.getTileWidth());
		requestInfo.setGridSet(taskInstance.getSrs());
		int storeTypeId = taskInstance.getStoreType();
		String sourceName = getDbName(requestInfo,storeTypeId);
		String tableName = null;
//		if (storeTypeId == 1){  //postgresql
//			tableName = createTableName((int)tileIndex[2], (int)tileIndex[1], (int)tileIndex[0]);
//		}
		List<BufferedImage> images = new ArrayList<BufferedImage>();
		for(int j = tileIndex[1]*2;j <= tileIndex[1]*2+1;j++) {
			for (int i = tileIndex[0]*2; i <= tileIndex[0]*2+1; i++) {
				int[] index = new int[3];
				index[0] = i;  //x
				index[1] = j;  //y
				index[2] = tileIndex[2]+1;  //z
				byte[] buf = getResourceFromDB(sourceName,
											   tableName,
											   storeTypeId,
											   taskInstance.getStorePath(),
											   taskInstance.getLayerName(),
						                       taskInstance.getTileFormat(),
											   index,
						                       taskInstance.getSrs(),
						                       taskInstance.getTileWidth());
				BufferedImage image = null;
				if (buf != null) {
					ByteArrayInputStream in = new ByteArrayInputStream(buf);
					try {
						image = ImageIO.read(in);
					} catch (IOException e) {
						log.error("SeedTaskUtil composeTile has error {}",e.getLocalizedMessage());
					}
				}else{
					if (taskInstance.getTileFormat().endsWith("png")){
						image = ImageUtil.createTransparentImg(taskInstance.getTileWidth(), taskInstance.getTileWidth());
					}else{
						image = ImageUtil.createWhiteBgcolorImg(taskInstance.getTileWidth(), taskInstance.getTileWidth());
					}
				}	
				images.add(image);
			}
		}
		String simpleFormat = simpleMineType(taskInstance.getTileFormat());
		//由于是wmts瓦片从左上角开始，因此初始是上面
		BufferedImage upImage = ImageUtil.mergeImageByImage(1, simpleFormat, images.get(0),images.get(1));
		BufferedImage downImage = ImageUtil.mergeImageByImage(1, simpleFormat, images.get(2),images.get(3));
		//图像是从左上角开始记录像素，方便后面处理
		BufferedImage image = ImageUtil.mergeImageByImage(2, simpleFormat, upImage,downImage);
		//压缩数据为请求的大小
		image = ImageUtil.compressImage(image,taskInstance.getTileWidth(),taskInstance.getTileWidth());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ImageIO.write(image, simpleFormat, baos);
			TileResourceInfo tileResource = new TileResourceInfo(baos.toByteArray(), tileIndex[0],tileIndex[1],tileIndex[2]);
			baos.reset();
			return tileResource;
		} catch (IOException e) {
			log.error("SeedTaskUtil composeTile has error {}",e.getLocalizedMessage());
		}finally {
			ImageUtilities.disposeImage(upImage);
			ImageUtilities.disposeImage(downImage);
			ImageUtilities.disposeImage(image);
			FileUtil.closeStream(baos);
		}
		return null;
	}

	/**
	 * 查询数据库实例
	 *
	 * @param sourceName sourceName
	 * @return PostGisConnectEntity
	 */
	public PostGisConnectEntity queryPostGisInfos(String sourceName) {
		return tileCacheMapper.queryPostGisBySourceName(sourceName);
	}
}
