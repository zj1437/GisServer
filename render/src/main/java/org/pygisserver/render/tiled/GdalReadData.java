package org.pygisserver.render.tiled;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.pygisserver.common.enumm.OgcEpsg;
import org.pygisserver.common.exception.ArgumentException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.pytc.entity.TileRequestInfo;
import org.pygisserver.common.util.GeometryUtil;
import org.pygisserver.common.util.ImageUtil;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Description: gdal数据获取
 * @author: 刘洋 
 * @date: 2020-12-03
 */
@Slf4j
public class GdalReadData {

	private static volatile Map<String,Dataset> DATASETCACHE = new ConcurrentHashMap<String,Dataset>();

	private static volatile Map<String,GdalDataInfo> ENVELOPECACHE = new ConcurrentHashMap<String,GdalDataInfo>();
	
	private static final Lock lock = new ReentrantLock();
	
	private int level;
	
	private int row;
	
	private int col;

	private String srs;
	
	private String layerName;
	
	private String fileUrl;
	
	private int tileWidth;
	
	private int tileHeight;
	
	private boolean isUpperLeft;
	
	private BoundingBox boundingBox;

	private String sourcesSrs;
	
	private static final float PIXEL_NO_USE_DATA_32767 = -32767f;
	
	static {
		gdal.AllRegister();
	}
	
	public GdalReadData(TileRequestInfo requestInfo,
						String fileUrl,
					    boolean isUpperLeft,
					    int tileWidth,
					    int tileHeight,
						String sourcesSrs) {
		this.boundingBox = requestInfo.getBbox();
		if (boundingBox == null) {
			this.level = requestInfo.getZoomLevel();
			this.row = requestInfo.getTileRow();
			this.col = requestInfo.getTileCol();
		}
		// 900913预览时坐标处理--不处理会在求交集时因坐标不一致报错
		this.srs = OgcEpsg.getOgcEpsgCodeType(requestInfo.getGridSet()) == OgcEpsg.MERCATOR_CODE
				? "EPSG:3857" : requestInfo.getGridSet();
		this.layerName = requestInfo.getLayerName();
		this.fileUrl = fileUrl;
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		if (requestInfo.getMineType().endsWith("terrain")) {
			this.tileWidth = 65;
			this.tileHeight = 65;
		}
		this.isUpperLeft = isUpperLeft;
		this.sourcesSrs = sourcesSrs;
	}

	public byte[] getTifResource() {
		float[] rasterBuffer = getDataRasterPiexl();
		if (rasterBuffer == null) {
			return new byte[0];
		}
		DataBufferFloat dataBuffer = new DataBufferFloat(rasterBuffer, tileWidth*tileHeight);
		ColorSpace space = ColorSpace.getInstance(ColorSpace.CS_GRAY);
    	ComponentSampleModel sampleModle = new ComponentSampleModel(DataBuffer.TYPE_FLOAT,tileWidth , tileHeight, 1, tileWidth, new int[] {0});
    	int[] bits = {32};
    	ComponentColorModel color1 = new ComponentColorModel(space, bits, false, false, Transparency.TRANSLUCENT,DataBuffer.TYPE_FLOAT);
    	WritableRaster raster = Raster.createWritableRaster(sampleModle,dataBuffer,new Point(0,0));
    	BufferedImage image = new BufferedImage(color1,raster,false,null);
		return ImageUtil.formatImage2Tiff(image);
	}
	
	
	/**
	 * 获取像素
	 * @return
	 */
	public float[] getDataRasterPiexl() {
		Dataset dataset = null;
		if (DATASETCACHE.containsKey(layerName)) {
			dataset =  DATASETCACHE.get(layerName);
		} else {
			dataset = gdal.Open(fileUrl, gdalconstConstants.GA_ReadOnly);
			DATASETCACHE.put(layerName, dataset);
		}
		GdalDataInfo gdalInfo = null;
		if (ENVELOPECACHE.containsKey(layerName)) {
			gdalInfo =  ENVELOPECACHE.get(layerName);
		} else {
			gdalInfo = getFileData(dataset);
			ENVELOPECACHE.put(layerName, gdalInfo);
		}
		if(dataset == null || gdalInfo == null) return null;
		ReferencedEnvelope originalEnvelope = gdalInfo.getReferencedEnvelope();
		double resoultionX = gdalInfo.getResoultionX();
		double resoultionY = gdalInfo.getResoultionY();
		if(!sourcesSrs.equals(srs)) {
			//数据原始坐标转换为切图请求投影坐标
			originalEnvelope = GeometryUtil.transformCoordinateSystem(originalEnvelope, srs);
			resoultionX = originalEnvelope.getWidth() / gdalInfo.getXCount();
			resoultionY = originalEnvelope.getHeight() / gdalInfo.getYCount();
		}
		//请求的envelope
		final ReferencedEnvelope reqEnvelope;
		if (boundingBox == null) {
			reqEnvelope = (ReferencedEnvelope) getReqEnvelope();
		}else {
			reqEnvelope = (ReferencedEnvelope) boundingBox;
		}
		org.locationtech.jts.geom.Envelope envelope = new org.locationtech.jts.geom.Envelope(reqEnvelope.getMinX(),reqEnvelope.getMaxX(),reqEnvelope.getMinY(),reqEnvelope.getMaxY());
		//请求和原始数据的交集
		if (!originalEnvelope.intersects(envelope)) {
			return null;
		}
		double reqResolutionX = reqEnvelope.getWidth() / tileWidth;
		double reqResolutionY = reqEnvelope.getHeight() / tileHeight;
		ReferencedEnvelope interEnvelope = reqEnvelope.intersection(originalEnvelope);
		//计算在交集像像素上的偏移量
		int imageOffX = (int) ((interEnvelope.getMinX() - reqEnvelope.getMinX()) / reqResolutionX);
		int imageOffY = (int) Math.abs((interEnvelope.getMaxY() - reqEnvelope.getMaxY()) / reqResolutionY);
		//计算需要多少个
		int image_Xbuf = (int) Math.ceil((interEnvelope.getMaxX() - interEnvelope.getMinX()) / reqResolutionX);
		int image_Ybuf =  (int) Math.ceil(Math.abs((interEnvelope.getMaxY() - interEnvelope.getMinY()) / reqResolutionY));
		imageOffX = imageOffX > 0 ?  imageOffX : 0;
		imageOffY = imageOffY > 0 ?  imageOffY : 0;
		//获取单通道
		float[] floatArray = new float[tileWidth * tileHeight];
		
		Band band1 = dataset.GetRasterBand(1);
		double tileX = (reqEnvelope.getMaxX() - reqEnvelope.getMinX()) / (tileWidth - 1);
		double tileY = (reqEnvelope.getMaxY() - reqEnvelope.getMinY()) / (tileHeight - 1);
		float[] tempArray = new float[1];
		for (int i = imageOffY; i < imageOffY + image_Ybuf; i++) {
			for (int j = imageOffX; j < imageOffX + image_Xbuf; j++) {
				//获取每一个点的经纬度
				double lon = reqEnvelope.getMinX() + j*tileX;
				double lat = reqEnvelope.getMaxY() - i*tileY;
				int x = (int) Math.round((lon - originalEnvelope.getMinX()) / resoultionX);
				int y = (int) Math.abs(Math.round((originalEnvelope.getMaxY() - lat) / resoultionY));
				lock.lock();
				try {
					band1.ReadRaster(x, y, 1, 1, tempArray);
				} finally {
					lock.unlock();
				}
				float piexl = tempArray[0] <= band1.SetNoDataValue(tempArray[0])? 0 : tempArray[0];
				floatArray[i*tileWidth+j] = piexl;
			}
		}
		return floatArray;
	}
	
	private GdalDataInfo getFileData(Dataset dataset) {
		int xCount = dataset.getRasterXSize();
		int yCount = dataset.getRasterYSize();
		double[] ori_transform = dataset.GetGeoTransform();
		double minX = ori_transform[0];
		double maxX = ori_transform[0] + xCount*ori_transform[1];
		double maxY = ori_transform[3];
		double minY = ori_transform[3] + yCount*ori_transform[5];
		double resoultionX = ori_transform[1];
		double resoultionY = ori_transform[5];
		ReferencedEnvelope resEnvelope = null;
		try {
			resEnvelope = new ReferencedEnvelope(minX, maxX, minY, maxY, CRS.decode(sourcesSrs,true));
		} catch (MismatchedDimensionException | FactoryException e) {
			log.error("GdalReadData getFileData has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, "坐标系"));
		}
		GdalDataInfo gdalInfo = new GdalDataInfo();
		gdalInfo.setSrs(/*dataset.GetProjection()*/ sourcesSrs);
		gdalInfo.setResoultionX(resoultionX);
		gdalInfo.setResoultionY(resoultionY);
		gdalInfo.setXCount(xCount);
		gdalInfo.setYCount(yCount);
		gdalInfo.setReferencedEnvelope(resEnvelope);
		return gdalInfo;
	}
	
	public void isValidReqParameter(long[] gridCov) {
		if (col < gridCov[0] || col > gridCov[2]) {
			String str = "column" + col + " is out of range min: " + gridCov[0] + " max:" + gridCov[2];
			throw new ArgumentException(ExceptionsUtil.ERROR_400,ExceptionsUtil.formatInfo(ExceptionsUtil.ARGGUMENT_EMPTY, str));
		}
		row = (int) (Math.pow(2, level) - row - 1);
		if (row < gridCov[1] || row > gridCov[3]) {
			long minRow = (long) (Math.pow(2, level) - gridCov[3] - 1);
			long maxRow = (long) (Math.pow(2, level) - gridCov[1] - 1);
			String str = "row " + row + " is out of range, min: " + minRow + " max:" + maxRow;
			throw new ArgumentException(ExceptionsUtil.ERROR_400,ExceptionsUtil.formatInfo(ExceptionsUtil.ARGGUMENT_EMPTY, str));
		}
	}
	
	
	public long[] createCoverage(Envelope originalEnvelope) {
		double tileWidth = 360 / Math.pow(2, level + 1);
		double tileHeight = 180 / Math.pow(2, level);
		long minX = (long) Math.floor((originalEnvelope.getMinimum(0) - (-180)) / tileWidth);
        long maxX = (long) Math.ceil(((originalEnvelope.getMaximum(0) - (-180)) / tileWidth));
        long minY = (long) Math.floor((originalEnvelope.getMinimum(1) - (-90)) / tileHeight);
        long maxY = (long) Math.ceil(((originalEnvelope.getMaximum(1) - (-90)) / tileHeight));
        long[] ret = {minX, minY, maxX - 1, maxY - 1, level};
        return ret;
	}
	
	
	public Dataset getDataset() {
		if (DATASETCACHE.containsKey(layerName)) {
			return DATASETCACHE.get(layerName);
		}
		Dataset dataset = gdal.Open(fileUrl, gdalconstConstants.GA_ReadOnly);
		DATASETCACHE.put(layerName, dataset);
		return dataset;
	}
	
	
	/**
	 * 根据投影信息获取bbox
	 * @param srs
	 * @return
	 */
	public Envelope getReqEnvelope() {
		int realRow = row;
		if (OgcEpsg.getOgcEpsgCodeType(srs) == OgcEpsg.MERCATOR_CODE) {
			return GeometryUtil.calcute3857bbox(level, realRow, col, srs);
		}else {
			return GeometryUtil.calcute4326bbox(level, realRow, col);
		}
	}
}

@Data
class GdalDataInfo{
	
	/**
	 * 投影格式
	 */
	private String srs;
	
	/**
	 * x方向像素数量
	 */
	private int xCount;
	
	/**
	 * y方向像素数量
	 */
	private int yCount;
	
	/**
	 * bbox
	 */
	private ReferencedEnvelope referencedEnvelope;

	/**
	 * X方向的分辨率
	 */
	private double resoultionX;
	
	/**
	 * Y方向的分辨率
	 */
	private double resoultionY;
}

