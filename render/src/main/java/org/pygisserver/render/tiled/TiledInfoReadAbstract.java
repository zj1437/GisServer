package org.pygisserver.render.tiled;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;
import javax.media.jai.PlanarImage;

import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.entity.SeedTaskInstance;
import org.pygisserver.common.entity.TDBDataConfig;
import org.pygisserver.common.enumm.DataTypeEnum;
import org.pygisserver.common.enumm.OgcEpsg;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.pytc.entity.TileRequestInfo;
import org.pygisserver.common.pytc.entity.TileResourceInfo;
import org.pygisserver.common.util.FileUtil;
import org.pygisserver.common.util.GeometryUtil;
import org.pygisserver.common.util.ImageUtil;
import org.pygisserver.render.cache.VisitCacheClearRunnable;
import org.pygisserver.render.config.TdbConfigFileGenerate;
import org.pygisserver.render.service.CustomDataLoad;
import org.pygisserver.render.service.impl.CustomDataLoadImpl;
import org.pygisserver.render.util.TdbUtils;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.ImageWorker;
import org.geotools.image.util.ImageUtilities;
import org.geotools.referencing.CRS;
import org.geotools.renderer.crs.ProjectionHandler;
import org.geotools.renderer.crs.ProjectionHandlerFinder;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.renderer.lite.gridcoverage2d.GridCoverageRendererUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class TiledInfoReadAbstract implements TiledInfoRead {

	private TileRequestInfo tiledRequestInfo;

	private String reqSrs;

	private String reqFormat;

	private String resourceSrs;

	private String fileUrl;

	private String resFormat;

	private int tileWidth;

	private int tileHeight;
	
	private boolean isCreateThumbal = false;

	private int level;

	private int realRow;

	private int realCol;

	private BoundingBox bbox;

	private AffineTransform finalWorldToGrid;

	private ReferencedEnvelope mapEnvelope;

	private Rectangle mapRasterArea;

	private int mapWidth;

	private int mapHeight;

	private ProjectionHandler handler;

	private GridCoverageFactory gridCoverageFactory = CoverageFactoryFinder.getGridCoverageFactory(null);

	private boolean transparent = true;

	private String fileType;

	private int dataType;

	private static final int DEFAULT_PADDING = 10;

	private CustomDataLoad customDataLoad = new CustomDataLoadImpl();
	
	public static volatile long curDirSize = 0;
	
	public static long maxCacheSize = 0;
	
	private static final Lock lock = new ReentrantLock();

	public TiledInfoReadAbstract() {
		
	}
	
	public TiledInfoReadAbstract(TileRequestInfo requestInfo, LayerBaseInfo tileInfo) {

		this.fileType = tileInfo.getFileType();
		this.tiledRequestInfo = requestInfo;
		this.reqSrs = requestInfo.getGridSet();
		String mineType = requestInfo.getMineType();
		this.reqFormat = createReqFormat(mineType);
		this.isCreateThumbal = requestInfo.isCreateThumbal();
		this.resourceSrs = tileInfo.getResourceSrs();
		this.fileUrl = tileInfo.getFilePath();
		this.resFormat = tileInfo.getResourceFormat();
		if (StringUtils.isBlank(resFormat) || "jpg".equals(resFormat)) {
			resFormat = "jpeg";
		}
		this.tileWidth = 256;
		this.tileHeight = 256;
		this.realRow = tiledRequestInfo.getTileRow();
		this.realCol = tiledRequestInfo.getTileCol();
		this.level = tiledRequestInfo.getZoomLevel();
		bbox = tiledRequestInfo.getBbox();
		if (bbox != null) {
			this.transparent = isSupportTransparent(requestInfo.isTransparent());
			this.mapWidth = requestInfo.getImageWidth();
			this.mapHeight = requestInfo.getImageHeight();
			if (mapWidth > 0 && mapHeight > 0){
				this.mapRasterArea = new Rectangle(0, 0, mapWidth, mapHeight);
				affineTransform();
			}
		}
		this.dataType = tileInfo.getDataType();
	}

	public String createReqFormat(String mineType) {
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
	
	private boolean isSupportTransparent(boolean transparent) {
		return transparent && reqFormat.endsWith("png");
	}

	private void affineTransform() {
		mapEnvelope = new ReferencedEnvelope(bbox);
		mapRasterArea = new Rectangle(0, 0, mapWidth, mapHeight);
		finalWorldToGrid = RendererUtilities.worldToScreenTransform(mapEnvelope, mapRasterArea);
		if (OgcEpsg.getOgcEpsgCodeType(resourceSrs) != OgcEpsg.getOgcEpsgCodeType(reqSrs)) {
			GridGeometry2D requestedGridGeometry = new GridGeometry2D(new GridEnvelope2D(mapRasterArea), mapEnvelope);
			MathTransform2D worldToScreen = requestedGridGeometry.getCRSToGrid2D(PixelOrientation.UPPER_LEFT);
			GridEnvelope2D requestedGridEnvelope = new GridEnvelope2D(mapRasterArea);
			applyReadGutter(requestedGridEnvelope);
			try {
				requestedGridGeometry = new GridGeometry2D(requestedGridEnvelope, PixelInCell.CELL_CORNER,
						worldToScreen.inverse(), mapEnvelope.getCoordinateReferenceSystem(), null);
			} catch (IllegalArgumentException | NoninvertibleTransformException e) {
				log.error("TiledInfoReadAbstract affineTransform has error:" + ExceptionsUtil.stackTraceToStr(e));
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_CREATE, "GridGeometry"));
			}
			mapEnvelope = ReferencedEnvelope.reference(requestedGridGeometry.getEnvelope2D());
			mapRasterArea = requestedGridGeometry.getGridRange2D().getBounds();
		}
		try {
			this.handler = ProjectionHandlerFinder.getHandler(mapEnvelope, CRS.decode(resourceSrs, true), true);
		} catch (FactoryException e) {
			log.error("TiledInfoReadAbstract affineTransform has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_CREATE, "ProjectionHandler"));
		}
	}

	private void applyReadGutter(GridEnvelope2D gridRange) {
		gridRange.setBounds(gridRange.x - DEFAULT_PADDING, gridRange.y - DEFAULT_PADDING,
				gridRange.width + DEFAULT_PADDING * 2, gridRange.height + DEFAULT_PADDING * 2);
	}

	/**
	 * 计算可用的交集
	 * 
	 * @return
	 */
	private ReferencedEnvelope calcuteIntersectionEnvelope(ReferencedEnvelope queryEnvelope) {
		ReferencedEnvelope intersections = null;
		org.locationtech.jts.geom.Envelope originalEnvelope;
		if ("EPSG:4326".equalsIgnoreCase(resourceSrs)) {
			originalEnvelope = new org.locationtech.jts.geom.Envelope(-180, 180, -90, 90);
		} else {
			originalEnvelope = new org.locationtech.jts.geom.Envelope(GeometryUtil.MERCATOR_UNITS_NEGITAVE,
					GeometryUtil.MERCATOR_UNITS, GeometryUtil.MERCATOR_UNITS_NEGITAVE, GeometryUtil.MERCATOR_UNITS);
		}
		final ReferencedEnvelope reqEnvelope;
		try {
			reqEnvelope = new ReferencedEnvelope(queryEnvelope, CRS.decode(resourceSrs, true));
			ReferencedEnvelope resEnvelope = new ReferencedEnvelope(originalEnvelope, CRS.decode(resourceSrs, true));
			if (!queryEnvelope.intersects(originalEnvelope)) {
				return null;
			} else {
				intersections = reqEnvelope.intersection(resEnvelope);
			}
		} catch (MismatchedDimensionException | FactoryException e) {
			log.error("TiledInfoReadAbstract calcuteIntersectionEnvelope has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, "瓦片数据Bbox交集信息"));
		}

		return intersections;
	}

	@Override
	public byte[] getWmsResource() {
		RenderedImage image = getWmsImg();
		if (image == null) {
			return new byte[0];
		}
		try {
			if (!reqFormat.equalsIgnoreCase(resFormat)) {
				// 格式化后返回
				if ("png".equalsIgnoreCase(reqFormat)) {
					return ImageUtil.formatImage2Png(image);
				} else if ("jpeg".equalsIgnoreCase(reqFormat) || "jpg".equalsIgnoreCase(reqFormat)) {
					return ImageUtil.formatImage2Jpeg(image);
				}
			}
			return ImageUtil.readImgToBuffer(image, reqFormat);
		} catch (IOException e) {
			log.error("TiledInfoReadAbstract getWmsResource has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, "WMS数据"));
		}
	}

	@Override
	public RenderedImage getWmsImg() {
		int currentLevel = getCurrentLevel(mapEnvelope, tiledRequestInfo.getImageWidth(), reqSrs);
		// 首先计算出交集
		List<ReferencedEnvelope> intersections = null;
		try {
			intersections = handler.getQueryEnvelopes();
		} catch (TransformException | FactoryException e) {
			log.error("TiledInfoReadAbstract getWmsImg has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, "瓦片数据Bbox交集信息"));
		}

		if (intersections == null || intersections.isEmpty()) {
			return null;
		}
		List<GridCoverage2D> coverages = new ArrayList<GridCoverage2D>();
		// 获取多个coverage
		for (ReferencedEnvelope referencedEnvelope : intersections) {
			referencedEnvelope = getValidEnvelope(referencedEnvelope);
			GridCoverage2D image = createCoverage2D(currentLevel, referencedEnvelope);
			if (image != null) {
				coverages.add(image);
			}
		}
		if (coverages.size() > 0) {
			RenderedImage image = createWmsMap(coverages);
			Color bgColor = new Color(255, 255, 255);
			image = dealImage(image, bgColor);
			ImageWorker iw = new ImageWorker(image);
			return iw.getBufferedImage();
		}
		return null;
	}

	public ReferencedEnvelope getValidEnvelope(ReferencedEnvelope referencedEnvelope) {
		if ("EPSG:4326".equalsIgnoreCase(resourceSrs)) {
			if (referencedEnvelope.getMaxX() > 180 && referencedEnvelope.getMinX() > -180) {
				referencedEnvelope = new ReferencedEnvelope(-180, referencedEnvelope.getMaxX(),
						referencedEnvelope.getMinY(), referencedEnvelope.getMaxY(),
						referencedEnvelope.getCoordinateReferenceSystem());
			}
			if (referencedEnvelope.getMaxX() < 180 && referencedEnvelope.getMinX() < -180) {
				referencedEnvelope = new ReferencedEnvelope(referencedEnvelope.getMinX(), 180,
						referencedEnvelope.getMinY(), referencedEnvelope.getMaxY(),
						referencedEnvelope.getCoordinateReferenceSystem());
			}
		}

		if (OgcEpsg.getOgcEpsgCodeType(resourceSrs) == OgcEpsg.MERCATOR_CODE) {
			if (referencedEnvelope.getMaxX() > GeometryUtil.MERCATOR_UNITS
					&& referencedEnvelope.getMinX() > GeometryUtil.MERCATOR_UNITS_NEGITAVE) {
				referencedEnvelope = new ReferencedEnvelope(GeometryUtil.MERCATOR_UNITS_NEGITAVE,
						referencedEnvelope.getMaxX(), referencedEnvelope.getMinY(), referencedEnvelope.getMaxY(),
						referencedEnvelope.getCoordinateReferenceSystem());
			}
			if (referencedEnvelope.getMaxX() < GeometryUtil.MERCATOR_UNITS
					&& referencedEnvelope.getMinX() < GeometryUtil.MERCATOR_UNITS_NEGITAVE) {
				referencedEnvelope = new ReferencedEnvelope(referencedEnvelope.getMinX(), GeometryUtil.MERCATOR_UNITS,
						referencedEnvelope.getMinY(), referencedEnvelope.getMaxY(),
						referencedEnvelope.getCoordinateReferenceSystem());
			}
		}
		return referencedEnvelope;
	}

	private RenderedImage dealImage(RenderedImage image, Color bgColor) {
		if (image == null) {
			return null;
		}
		final Rectangle imageBounds = PlanarImage.wrapRenderedImage(image).getBounds();
		Rectangle intersection = imageBounds.intersection(mapRasterArea);
		if (intersection.isEmpty()) {
			return null;
		}
		ImageWorker iw = new ImageWorker(image);
		BufferedImage bufferimage = null;
		if (reqFormat.equalsIgnoreCase("jpeg")) {
			bufferimage = new BufferedImage(mapWidth, mapHeight, BufferedImage.TYPE_INT_RGB);
		}else {
			bufferimage = new BufferedImage(mapWidth, mapHeight, BufferedImage.TYPE_INT_ARGB);
			bufferimage = bufferimage.createGraphics().getDeviceConfiguration().createCompatibleImage(mapWidth, mapHeight, Transparency.TRANSLUCENT);
		}
		Graphics g2d = bufferimage.createGraphics();
		g2d.drawImage(iw.getBufferedImage(), (int) imageBounds.getX(), (int) imageBounds.getY(), null);
		return bufferimage;
	}

	public GridCoverage2D createCoverage2D(int currentLevel, ReferencedEnvelope refrenceBbox) {
		ReferencedEnvelope intersection = calcuteIntersectionEnvelope(refrenceBbox);
		// 如果投影不相同，则根据投影转化，计算出需要合并的瓦片数量
		int[] tileRange = null;
		if ("epsg:4326".equalsIgnoreCase(resourceSrs)) {
			// 获取需要请求的行列号
			tileRange = GeometryUtil.convert4326ToTiledRange(currentLevel, intersection);
		} else if (OgcEpsg.getOgcEpsgCodeType(resourceSrs) == OgcEpsg.MERCATOR_CODE) {
			tileRange = GeometryUtil.convert3857ToTiledRange(currentLevel, intersection);
		}
		// }
		return getCoverage(tileRange, currentLevel, intersection);
	}

	private int getCurrentLevel(BoundingBox bbox, int imageWidth, String reqSrs) {
		double resolution = bbox.getSpan(0) / imageWidth;
		int currentlevel = 0;
		if ("EPSG:4326".equalsIgnoreCase(reqSrs)) {
			currentlevel = GeometryUtil.get4326CurrentLevel(resolution, tileWidth);
		} else if (OgcEpsg.getOgcEpsgCodeType(reqSrs) == OgcEpsg.MERCATOR_CODE) {
			currentlevel = GeometryUtil.get3857CurrentLevel(resolution, tileWidth);
		}
		return currentlevel;
	}

	private GridCoverage2D getCoverage(int[] tileRange, int currentLevel, ReferencedEnvelope imageEnvelope) {
		BufferedImage image = null;
		image = compressImg(tileRange, tileRange[4]);
		if (image == null) {
			return null;
		}
		if (tileWidth > 256 && currentLevel < 0) {
			// 压缩对应的数据
			int ration = tileWidth / 256;
			image = ImageUtil.compressImage(image, image.getWidth() / ration, image.getHeight() / ration);
		}
		Envelope leftUpperBbox = null;
		Envelope rightLowerBbox = null;
		Envelope resourceEnvelope = null;
		if (OgcEpsg.getOgcEpsgCodeType(resourceSrs) == OgcEpsg.MERCATOR_CODE) {
			leftUpperBbox = GeometryUtil.calcute3857bbox(tileRange[4], tileRange[1], tileRange[0], resourceSrs); // 默认全部是3857
			rightLowerBbox = GeometryUtil.calcute3857bbox(tileRange[4], tileRange[3], tileRange[2], resourceSrs); // 默认全部是3857
		} else {
			leftUpperBbox = GeometryUtil.calcute4326bbox(tileRange[4], tileRange[1], tileRange[0]);
			rightLowerBbox = GeometryUtil.calcute4326bbox(tileRange[4], tileRange[3], tileRange[2]);
		}
		resourceEnvelope = imageEnvelope;
		double resolution = (rightLowerBbox.getMaximum(0) - leftUpperBbox.getMinimum(0)) / image.getWidth();
		int minXOffSet = (int) Math.floor((resourceEnvelope.getMinimum(0) - leftUpperBbox.getMinimum(0)) / resolution);
		int maxXOffSet = (int) Math.ceil((rightLowerBbox.getMaximum(0) - resourceEnvelope.getMaximum(0)) / resolution);
		int minYOffSet = (int) Math.ceil((leftUpperBbox.getMaximum(1) - resourceEnvelope.getMaximum(1)) / resolution);
		int maxYOffSet = (int) Math.floor((resourceEnvelope.getMinimum(1) - rightLowerBbox.getMinimum(1)) / resolution);
		int strideX = minXOffSet + maxXOffSet < 0 ? 0 : minXOffSet + maxXOffSet;
		int strideY = minYOffSet + maxYOffSet < 0 ? 0 : minYOffSet + maxYOffSet;
		// 裁剪
		ImageWorker iw = new ImageWorker(image);
		iw.crop(minXOffSet < 0 ? 0 : minXOffSet, minYOffSet < 0 ? 0 : minYOffSet, image.getWidth() - strideX,
				image.getHeight() - strideY);
		return gridCoverageFactory.create("config", iw.getRenderedImage(), imageEnvelope);
	}

	private RenderedImage createWmsMap(List<GridCoverage2D> coverages) {
		// 重投影
		RenderedImage image = null;
		try {
			GeneralEnvelope generalEnvelope = new GeneralEnvelope(mapEnvelope);
			coverages = GridCoverageRendererUtilities.reproject(coverages, CRS.decode(reqSrs, true),
					GeometryUtil.interpolation, generalEnvelope, null, gridCoverageFactory, null);
			image = GeometryUtil.disPlaceCoverage(coverages, gridCoverageFactory, handler, CRS.decode(reqSrs, true),
					CRS.decode(resourceSrs, true), finalWorldToGrid, mapEnvelope);
		} catch (FactoryException e) {
			log.error("TiledInfoReadAbstract createWmsMap has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_CREATE, "WMS数据重投影"));
		}
		return image;
	}
	
	public byte[] getTdbFileResource(int tileRow, int tileCol, int zoomLevel,String dataPath,String serverName) {
		try {
			return customDataLoad.getImageInfo(tileRow, tileCol, zoomLevel, dataPath, serverName);
		} catch (Exception e) {
			log.error("TiledInfoReadAbstract getFileResource has error:{}",ExceptionsUtil.stackTraceToStr(e));
		}
		return null;
	}

	public byte[] getCacheFileResource(File file) {
		return FileUtil.getFileResource(file);
	}

	@Override
	public byte[] getWmtsResource() {
		// 如果是高程直接获取数据
		if ("osgb".equals(reqFormat)) {
			return getDemSource(fileUrl);
		}
		if (("tiff".equals(reqFormat) || TdbUtils.TDB_DEM_TYPE.equalsIgnoreCase(reqFormat)) 
				&& ("tiff".equals(resFormat) || "tif".equals(resFormat) || TdbUtils.TDB_DEM_TYPE.equals(resFormat)) || "terrain".equals(reqFormat)) {
			//高程处理其投影格式
			if (OgcEpsg.getOgcEpsgCodeType(resourceSrs) != OgcEpsg.getOgcEpsgCodeType(reqSrs) && !"terrain".equals(reqFormat)) {
			    //terrain不走此处获取image
				String tifPath = getReqDirect(tiledRequestInfo.getLayerName(),reqSrs,reqFormat,tileWidth);
				return getResourceImage(tifPath);
			}
			return getDemSource(fileUrl);
		}
		// 投影相同，格式也相同---矢量瓦片数据由前端渲染，只需要根据url返回文件内容
		if ((OgcEpsg.getOgcEpsgCodeType(resourceSrs) == OgcEpsg.getOgcEpsgCodeType(reqSrs)
				|| StringUtils.equalsAnyIgnoreCase(reqFormat, "geojson", "protobuf"))
				&& resFormat.equals(reqFormat) ) {
			return getRealSource(fileUrl);
		}
		// 投影不相同
		String reqCachePath = getReqDirect(tiledRequestInfo.getLayerName(),reqSrs,reqFormat,tileWidth);
		//只有海图、在线地图、请求样式和坐标系不同时才会读取访问缓存
		if ((DataTypeEnum.ONLINE.getFileCode() == dataType || !resFormat.equals(reqFormat)
				|| !resourceSrs.equals(reqSrs)) && isCreateThumbal == false && !"geojson".equals(reqFormat)) {
			checkTdbConfig(reqCachePath);
			byte[] result = getTdbFileResource(realRow,realCol,level,reqCachePath,tiledRequestInfo.getLayerName());
			if(result != null) return result;
		}
		if (OgcEpsg.getOgcEpsgCodeType(resourceSrs) != OgcEpsg.getOgcEpsgCodeType(reqSrs)) {
			return getResourceImage(reqCachePath);              
		}
		// 格式不相同
		if (!resFormat.equals(reqFormat)) {
			return formatFileInfo(reqCachePath, fileUrl);

		}
		return null;
	}
	
	private TDBDataConfig checkTdbConfig(String cachaPath) {
		File temp = new File(cachaPath);
		String cacheKey = temp.getName();
		TDBDataConfig tdbConfig = null;
		if(TdbUtils.tdbMap.containsKey(cacheKey)) {
			tdbConfig = TdbUtils.tdbMap.get(cacheKey);
		} else {
			SeedTaskInstance tileTask = new SeedTaskInstance();
			tileTask.setTileWidth(tileWidth);
			tileTask.setTileHeight(tileHeight);
			tileTask.setZoomStart(0);
			tileTask.setZoomEnd(23);
			tileTask.setMinX(-180.0);
			tileTask.setMinY(-90.0);
			tileTask.setMaxX(180.0);
			tileTask.setMaxX(90.0);
			tdbConfig = TdbUtils.getTDBDataConfig(tiledRequestInfo, tileTask);
			if (!TdbUtils.tdbMap.containsKey(cacheKey)) TdbUtils.tdbMap.put(cacheKey, tdbConfig);
		}
		//写config.tab文件
		if(isCreateThumbal == false) {
			File configFile = new File(cachaPath,"config.tdb");
			if (!configFile.exists()) {
				TdbConfigFileGenerate.writeTDBConfigFile(cachaPath, tdbConfig);
			}
		}
		return tdbConfig;
	}

	//瓦片数据访问缓存目录创建入口，如果目录不存在则先创建，防止由于目录不存在导致的异常
	public String getReqDirect(String serverName,String crs,String format,int width) {
		String visitCachePath = TdbUtils.getVistitCacheDirName(serverName, crs, format, width);
		if(isCreateThumbal == false) {
			File file = new File(visitCachePath);
			if(!file.exists()) file.mkdirs();
		}
		return visitCachePath;
	}

	public byte[] getResourceImage(String cachePath) {
		BufferedImage image = null;
		try {
			int[] ranageTiled = null;
			if (OgcEpsg.getOgcEpsgCodeType(reqSrs) == OgcEpsg.MERCATOR_CODE) {
				// 将当前的行列号转化为经纬度后，并计算出火星坐标系的范围
				if (isNeedToTransform()) {
					ReferencedEnvelope envelope = (ReferencedEnvelope) GeometryUtil.calcute3857To4326LonLatBbox(level,
							realRow, realCol);
					ranageTiled = calcuteGcj02Range(envelope);
				} else {
					ranageTiled = GeometryUtil.convert3857To4326Tiled(level, realRow, realCol);
				}
				// 在线数据，通过qu下一级数据来使用相同的分辨率
				if (level == 0 && fileType.equals(OnlineTileData.GAODE_RASTER)) {
					ranageTiled[0] = ranageTiled[0] * 2;
					ranageTiled[1] = ranageTiled[1] * 2;
					ranageTiled[2] = ranageTiled[2] * 2 + 1;
					ranageTiled[3] = ranageTiled[3] * 2 + 1;
					ranageTiled[4] = ranageTiled[4] + 1;
				}
			} else {
				// 将当前的行列号转化为经纬度后，并计算出火星坐标系的范围
				if (isNeedToTransform()) {
					ReferencedEnvelope envelope = (ReferencedEnvelope) GeometryUtil.calcute4326bbox(level, realRow,
							realCol);
					ranageTiled = calcuteGcj02Range(envelope);
				} else {
					ranageTiled = GeometryUtil.convert4326To3857Tiled(level, realRow, realCol);
				}
				//如果是在线数据
				if (DataTypeEnum.ONLINE.getFileCode() == dataType) {
					ranageTiled[0] = ranageTiled[0] * 2;
					ranageTiled[1] = ranageTiled[1] * 2;
					ranageTiled[2] = ranageTiled[2] * 2 + 1;
					ranageTiled[3] = ranageTiled[3] * 2 + 1;
					ranageTiled[4] = ranageTiled[4] + 1;
				}
			}
			if (OgcEpsg.getOgcEpsgCodeType(reqSrs) == OgcEpsg.MERCATOR_CODE) {
				image = project3857Image(ranageTiled, reqSrs);
			} else {
				image = project4326Image(ranageTiled, reqSrs);
			}
			if (image == null) {
				return null;
			}
			byte[] dataArray = null;
			if (!resFormat.equals(reqFormat) && !reqFormat.contains(resFormat)) {
				if (("tif".equals(resFormat) || "tiff".equals(resFormat))
						&& image.getColorModel().getNumComponents() == 1) {
					image = ImageUtil.forceComponet(image);
				}
				if ("png".equalsIgnoreCase(reqFormat)) {
					dataArray = ImageUtil.formatImage2Png(image);
				} else if ("jpeg".equalsIgnoreCase(reqFormat) || "jpg".equalsIgnoreCase(reqFormat)) {
					dataArray = ImageUtil.formatImage2Jpeg(image);
				} else if ("tiff".equalsIgnoreCase(reqFormat)){
					dataArray = ImageUtil.formatImage2Tiff(image);
				}
			} else {
				//如果是高程，则需要将数据处理为单通道
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ImageIO.write(image, resFormat, bos);
				dataArray = bos.toByteArray();
			}
			if (cachePath != null && dataArray != null) {
				writeCacheFile(cachePath, dataArray,realCol,realRow,level);
			}
			return dataArray;
		} catch (IOException e) {
			log.error("TiledInfoReadAbstract createWmsMap has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_TRANSFOR, "瓦片压缩类型"));
		} finally {
			ImageUtilities.disposeImage(image);
		}
	}

	public int[] calcuteGcj02Range(ReferencedEnvelope envelope) {
		int[] ranageTiled;
		// 计算左上角和右下角的经纬度坐标
		Coordinate upperLeft = GeometryUtil.gps2Gcj02(envelope.getMinX(), envelope.getMaxY());
		Coordinate lowerRight = GeometryUtil.gps2Gcj02(envelope.getMaxX(), envelope.getMinY());
		ranageTiled = GeometryUtil.convert4326To3857Tiled(upperLeft, lowerRight, level);
		return ranageTiled;
	}

	/**
	 * 将获取到数据合成需要的投影格式
	 * 
	 * @param tileRanage
	 * @throws IOException
	 * @throws FactoryException
	 * @throws NoSuchAuthorityCodeException
	 * @throws MismatchedDimensionException
	 */
	public BufferedImage project3857Image(int[] tileRanage, String reqSrs)  {
		BufferedImage image = compressImg(tileRanage, level);
		if (image == null) {
			return null;
		}
		// 计算出当前请求的数据的最小经纬度范围，然后裁剪，在重投影
		ReferencedEnvelope envelope = GeometryUtil.calcute3857To4326LonLatBbox(level, realRow, realCol);
		// 取其最大纬度
		Envelope upperEnvelope = GeometryUtil.calcute4326bbox(tileRanage[4], tileRanage[1], tileRanage[0]);
		// 取其最小纬度
		Envelope lowerEnvelope = GeometryUtil.calcute4326bbox(tileRanage[4], tileRanage[3], tileRanage[2]);
		double resolution = (upperEnvelope.getMaximum(1) - lowerEnvelope.getMinimum(1)) / image.getHeight();
		int minYoffset = (int) Math.floor((envelope.getMinimum(1) - lowerEnvelope.getMinimum(1)) / resolution);
		int maxYoffset = (int) Math.ceil((upperEnvelope.getMaximum(1) - envelope.getMaximum(1)) / resolution);
		// 裁剪
		image = image.getSubimage(0, maxYoffset, image.getWidth(), image.getHeight() - (minYoffset + maxYoffset));
		// 压缩成和原始数据一样宽高的图片
		image = ImageUtil.compressImage(image, tileWidth, tileHeight);
		// 重投影
		Envelope desEnvelope = GeometryUtil.calcute3857bbox(level, realRow, realCol, reqSrs);
		GeneralEnvelope generalEnvelope = new GeneralEnvelope(desEnvelope);
		return GeometryUtil.reprojectImage(image, reqSrs, envelope, GeometryUtil.interpolation, generalEnvelope,
				tiledRequestInfo.getLayerName());
	}

	public boolean isNeedToTransform() {
		return DataTypeEnum.ONLINE.getFileCode() == dataType && !fileType.equals(OnlineTileData.GOOGLE_HYBRID) &&
				!fileType.equals(OnlineTileData.MAPBOX) && !fileType.equals(OnlineTileData.OPENSTREETMAP)
				&& !fileType.equals(OnlineTileData.TIANDITU) && level > 8;
	}

	/**
	 * 将获取到数据合成需要的投影格式
	 * 
	 * @param tileRanage
	 * @throws IOException
	 * @throws FactoryException
	 * @throws NoSuchAuthorityCodeException
	 * @throws MismatchedDimensionException
	 */
	public BufferedImage project4326Image(int[] tileRanage, String reqSrs) {
		BufferedImage image = compressImg(tileRanage, level);
		if (image == null) {
			return null;
		}
		// 计算出当前请求的数据的最小经纬度范围，然后裁剪，在重投影
		ReferencedEnvelope envelope = GeometryUtil.calcute4326To3857Mercator(level, realRow, realCol, "EPSG:3857",
				isNeedToTransform());
		// 取其最大纬度
		Envelope upperEnvelope = GeometryUtil.calcute3857bbox(tileRanage[4], tileRanage[1], tileRanage[0], "EPSG:3857");
		// 取其最小纬度
		Envelope lowerEnvelope = GeometryUtil.calcute3857bbox(tileRanage[4], tileRanage[3], tileRanage[2], "EPSG:3857");
		double resolution = (upperEnvelope.getMaximum(1) - lowerEnvelope.getMinimum(1)) / image.getHeight();
		int minYoffset = (int) Math.floor((envelope.getMinimum(1) - lowerEnvelope.getMinimum(1)) / resolution);
		int maxYoffset = (int) Math.ceil((upperEnvelope.getMaximum(1) - envelope.getMaximum(1)) / resolution);

		int minXoffset = (int) Math.floor((envelope.getMinimum(0) - upperEnvelope.getMinimum(0)) / resolution);
		int maxXoffset = (int) Math.ceil((lowerEnvelope.getMaximum(0) - envelope.getMaximum(0)) / resolution);
		// 裁剪
		image = image.getSubimage(minXoffset < 0 ? 0 : minXoffset, maxYoffset < 0 ? 0 : maxYoffset,
				image.getWidth() - (minXoffset + maxXoffset), image.getHeight() - (minYoffset + maxYoffset));
		// 压缩成和原始数据一样宽高的图片
		image = ImageUtil.compressImage(image, tileWidth, tileHeight);
		// 重投影
		Envelope desEnvelope = GeometryUtil.calcute4326bbox(level, realRow, realCol);
		GeneralEnvelope generalEnvelope = new GeneralEnvelope(desEnvelope);
		return GeometryUtil.reprojectImage(image, reqSrs, envelope, GeometryUtil.interpolation, generalEnvelope,
				tiledRequestInfo.getLayerName());
	}

	public abstract BufferedImage compressImg(int[] tileRanage, int realLevel);

	private byte[] formatFileInfo(String cachePath, String parentUrl) {
		// 获取后缓存数据
		byte[] resource = null;
		if("TDB".equals(fileType) && ("tif".equals(resFormat) || "tiff".equals(resFormat) || TdbUtils.TDB_DEM_TYPE.equals(resFormat))) {
			resource = getDemSource(parentUrl);
		} else {
			resource = getFormatSource(parentUrl);
		}
		//如果是矢量瓦片数据，则不需要格式化
		if(StringUtils.equalsAnyIgnoreCase(reqFormat, "geojson", "protobuf")) return resource;
		if (resource != null && (!resFormat.equals(reqFormat) || !resourceSrs.equals(reqSrs))) {
			return formatImage(cachePath, resource);
		} else {
			return resource;
		}
	}

	public byte[] formatImage(String path, byte[] resource) {
		byte[] dataArray = null;
		try {
			ByteArrayInputStream bis = null;
			if (resource != null) {
				bis = new ByteArrayInputStream(resource);
				BufferedImage image = ImageIO.read(bis);	
				if (("tif".equals(resFormat) || "tiff".equals(resFormat) || TdbUtils.TDB_DEM_TYPE.equals(resFormat))
						&& image.getColorModel().getNumComponents() == 1) {
					image = ImageUtil.forceComponet(image);
				}
				
				if ("png".equalsIgnoreCase(reqFormat)) {
					dataArray = ImageUtil.formatImage2Png(image);
				} else if ("jpeg".equalsIgnoreCase(reqFormat) || "jpg".equalsIgnoreCase(reqFormat)) {
					dataArray = ImageUtil.formatImage2Jpeg(image);
				} else if ("tiff".equalsIgnoreCase(reqFormat)) {
					dataArray = ImageUtil.formatImage2Tiff(image);
				}
				final byte[] bufSource = dataArray;
				writeCacheFile(path, bufSource,realCol,realRow,level);
				FileUtil.closeStream(bis);
			}
			return dataArray;
		} catch (IOException e) {
			log.error("TiledInfoReadAbstract formatImage has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_FORMAT, "瓦片数据"));
		}
	}
	
	private void checkVisitCacheDirSize(long tileSize) {
		long curTotalSize = curDirSize + tileSize;
		if((maxCacheSize - curTotalSize) < 0) {
			Thread t = new Thread(new VisitCacheClearRunnable(System.getProperty(TdbUtils.USER_VISIT_CACHE_DIR),null,false,0,false));
			t.start();
		} else {
			lock.lock();
			try {
				curDirSize = curTotalSize;
			} finally {
				lock.unlock();
			}
		}
	}

	public void writeCacheFile(String path, byte[] dataArray,int col,int row,int level) {
		if ((DataTypeEnum.ONLINE.getFileCode() == dataType || !resFormat.equals(reqFormat)
				|| !resourceSrs.equals(reqSrs)) && isCreateThumbal == false) {
			TileResourceInfo tileResourceInfo = new TileResourceInfo(dataArray,col,row,level);
//			int tileRow = (int) (Math.pow(2, level)) - row - 1;
			tileResourceInfo.setY(row);
			TDBDataConfig tdbConfig = checkTdbConfig(path);
			Map<String,String> tdbParams = new HashMap<String, String>();
			tdbParams.put("destPath", path + File.separator);
			tdbParams.put("serverName", tiledRequestInfo.getLayerName());
			if(dataArray != null) {
				checkVisitCacheDirSize(dataArray.length);
				customDataLoad.insertTileImage(tileResourceInfo, tdbConfig, tdbParams,"");
			}
		}
	}

	@Override
	public byte[] getTmsResource() {
		return null;
	}
	
	
	public BufferedImage constructDefaultImage() {
		BufferedImage image;
		//根据请求的格式创建不同的空白图片
		if(this.reqFormat.equals("png")) {
			image = ImageUtil.createTransparentImg(tileWidth, tileWidth);
		} else {
			image = ImageUtil.createWhiteBgcolorImg(tileWidth, tileHeight);
		}
		return image;
	}

	public abstract File getRealFile(String parentPath, String format);

	/**
	 * 直接获取瓦片数据
	 * @param parentPath
	 * @return
	 */
	public abstract byte[] getRealSource(String parentPath);

	/**
	 * 获取格式化之前的数据
	 * @param parentPath
	 * @return
	 */
	public abstract byte[] getFormatSource(String parentPath);
	
	public abstract byte[] getDemSource(String filePath);
	
	public byte[] convetDemPixelData(float[] pixelArray) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[65 * 65 * 2 + 2]);
		for (int j = 0; j < 65; j++) {
			for (int i = 0; i < 65; i++) {
				short pixel = (short) pixelArray[j * 65 + i];
				if (pixel < 0) {
					pixel = 0;
				}
				short demHigh = (short) ((pixel + 1000) * 5);
				byte[] buf = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(demHigh).array();
				byteBuffer.put(buf);
			}
		}
		byte[] buf = ByteBuffer.allocate(1).order(ByteOrder.LITTLE_ENDIAN).put((byte) 15).array();
		byteBuffer.put(buf);
		byte[] land = ByteBuffer.allocate(1).order(ByteOrder.LITTLE_ENDIAN).put((byte) 255).array();
		byteBuffer.put(land);
		return byteBuffer.array();
	}
}
