package org.pygisserver.render.tiled;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.enumm.OgcEpsg;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.pytc.entity.TileRequestInfo;
import org.pygisserver.common.util.FileUtil;
import org.pygisserver.common.util.GeometryUtil;
import org.pygisserver.common.util.ImageUtil;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.geometry.Envelope;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 在线资源瓦片访问 
 * @author: 刘洋 
 * @date: 2020-09-08
 */
@Slf4j
public class OnlineTileData extends TiledInfoReadAbstract implements TiledInfoRead {
	
	public static final String GOOGLE_RASTER = "google_raster";
	
	public static final String GOOGLE_HYBRID = "google_combine";
	
	public static final String GOOGLE_MAP = "google_map";
	
	public static final String GAODE_RASTER = "gaode_raster";
	
	public static final String GAODE_MAP = "gaode_map";
	
	public static final String MAPBOX = "mapbox";
	
	public static final String OPENSTREETMAP = "osm";

	public static final String TIANDITU = "tianditu";
	
	private String reqSrs;
	
	private String reqFormat;
	
	private String resFormat;

	private int tileWidth;
	
	private int tileHeight;

	private int level;

	private int realRow;

	private int realCol;

	private String remoteUrl;
	
	private String fileType;
	
	private String layerName;
	
	public OnlineTileData(TileRequestInfo requestInfo,LayerBaseInfo baseInfo) {
		super(requestInfo, baseInfo);
		this.tileWidth = baseInfo.getTileWidth();
		this.tileHeight = baseInfo.getTileHeight();
		if (tileWidth == 0) {
			tileWidth = 256;
		}
		if (tileHeight == 0) {
			tileHeight = 256;
		}
		this.reqSrs = requestInfo.getGridSet();
		String mineType = requestInfo.getMineType();
		if (mineType.indexOf("/") > -1) {
			this.reqFormat = mineType.split("/")[1];
		} else {
			this.reqFormat = mineType;
		}
		this.resFormat = baseInfo.getResourceFormat();
		if (StringUtils.isBlank(resFormat) || "jpg".equals(resFormat)) {
			resFormat = "jpeg";
		}
		this.realRow = requestInfo.getTileRow();
		this.realCol = requestInfo.getTileCol();
		this.level = requestInfo.getZoomLevel();
		this.remoteUrl = baseInfo.getFilePath();
		this.fileType = baseInfo.getFileType();
		this.layerName = requestInfo.getLayerName();
	}
	
	private int[] calcuteValidTile(int[] tileRange) {
		//有高德数据0级没有，则使用
		if (fileType.equals(OnlineTileData.GAODE_RASTER) && tileRange[4] == 0) {
			tileRange[0] = tileRange[0] * 2;
			tileRange[1] = tileRange[1] * 2;
			tileRange[2] = tileRange[2] * 2 + 1;
			tileRange[3] = tileRange[3] * 2 + 1;
			tileRange[4] = 1;
		}
		if (fileType.equals(OnlineTileData.GAODE_MAP) && tileRange[4] < 3) {
			tileRange[0] = (int) (tileRange[0] * Math.pow(2, (3 - tileRange[4])));
			tileRange[1] = (int) (tileRange[1] * Math.pow(2, (3 - tileRange[4])));
			tileRange[2] = (int) ((tileRange[2] + 1) * Math.pow(2, (3 - tileRange[4])) - 1);
			tileRange[3] = (int) ((tileRange[3] + 1) * Math.pow(2, (3 - tileRange[4])) - 1);
			tileRange[4] = 3;
		}
		return tileRange;
	}
	
	public BufferedImage compressImg(int[] tileRanage, int realLevel) {
		tileRanage = calcuteValidTile(tileRanage);
		realLevel = tileRanage[4];
		List<BufferedImage> merageImages = new ArrayList<BufferedImage>();
		for (int j = tileRanage[1]; j <= tileRanage[3]; j++) { // row(y)
			List<BufferedImage> images = new ArrayList<BufferedImage>();
			for (int i = tileRanage[0]; i <= tileRanage[2]; i++) { // col(x)
				//先去缓存找，如果没有，再去远程请求
				BufferedImage image = getRemoteImg(realLevel, i, j);
				if (image != null) {
					images.add(image);
				}
			}
			// 横向
			if (images.size() > 0) {
				BufferedImage merageImage = null;
				merageImage = ImageUtil.mergeImageByImage(1, reqFormat,images.toArray(new BufferedImage[] {}));
				merageImages.add(merageImage);
			}
		}
		BufferedImage image = null; 
		try {
			if (merageImages.size() > 0) {
				image = ImageUtil.mergeImageByImage(2, reqFormat,merageImages.toArray(new BufferedImage[] {}));
			}
		} catch (Exception e) {
			log.error("OnlineTileData compressImg has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_MERGE, "瓦片图层"));
		}
		return image;
	}

	public BufferedImage getRemoteImg(int realLevel, int i, int row) {
		String httpUrl = getHttpUrl(realLevel,row,i);
		HttpEntity entity = null;
		BufferedImage image = null;
		try {
			entity = HttpUtils.httpGet(httpUrl, null);
			if (entity == null || entity.getContent() == null || entity.getContentLength() == 0) {
				if(this.reqFormat.equals("png")) {
					image = new BufferedImage(tileWidth, tileWidth, BufferedImage.TYPE_INT_ARGB);
				} else {
					image = new BufferedImage(tileWidth, tileWidth, BufferedImage.TYPE_INT_RGB);
				}	
			} else {
				image = formatResource2Image(entity,realLevel,row,i);
			}
		} catch (IOException e) {
			log.error("OnlineTileData getRemoteImg has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, "瓦片数据"));
		} 
		return image;
	}
	
	
	/**
	 * 
	 * @param level
	 * @param row
	 * @param col
	 * @return
	 */
	private String getHttpUrl(int level,int row,int col) {
		String url = null;
		if (fileType.equals(GOOGLE_RASTER)) {
			url = String.format(remoteUrl, col,row,level);
		}
		if (fileType.equals(GOOGLE_HYBRID)) {
			url = String.format(remoteUrl, col,row,level);
		}
		if (fileType.equals(GOOGLE_MAP)) {
			url = String.format(remoteUrl, col,row,level);
		}
		if (fileType.equals(GAODE_RASTER)) {
			url = String.format(remoteUrl, col,row,level);
		}
		if (fileType.equals(GAODE_MAP)) {
			url = String.format(remoteUrl, col,row,level);
		}
		if (fileType.equals(MAPBOX)) {
			url = String.format(remoteUrl, level,col,row);
		}
		if (fileType.equals(OPENSTREETMAP)) {
			url = String.format(remoteUrl, level,col,row);
		}
		if (fileType.equals(TIANDITU)) {
			url = String.format(remoteUrl, level+1,row,col);
		}
		return url;
	}
	
	/**
	 * 返回图片
	 * @param resource
	 * @return
	 */
	private BufferedImage formatResource2Image(HttpEntity entity,int level,int row,int col) {
		InputStream is = null;
		FileOutputStream fos = null;
		try {
			is = entity.getContent();
			BufferedImage image = ImageIO.read(is);
			return image;
		} catch (IOException e) {
			log.error("OnlineTileData formatResource2Image has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_TRANSFOR, "瓦片数据类型"));
		}finally {
			FileUtil.closeStream(fos,is);
		}
	}
	
	
	/**
	 * 
	 * @param httpEntity 响应数据
	 * @param file 写入的文件流
	 * @return
	 */
	private byte[] convertHttpEntityToByte(HttpEntity httpEntity) {
		ByteArrayOutputStream bos = null;
		try {
			InputStream is = httpEntity.getContent();
			bos = new ByteArrayOutputStream();
			int len = 0;
			byte[] buffer = new byte[1024];
			while((len = is.read(buffer)) != -1) {
				bos.write(buffer, 0, len);
			}
		} catch (UnsupportedOperationException | IOException e) {
			log.error("OnlineTileData convertHttpEntityToByte has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, "请求数据"));
		}finally {
			FileUtil.closeStream(bos);
		}
		return bos.toByteArray();
	}
	

	@Override
	public byte[] getTmsResource() {
		return null;
	}

	@Override
	public byte[] getRealSource(String parentPath) {
		byte[] result = null;
		int row = realRow;
		int col = -1;
		if (realCol < 0) {
			col = (int) (Math.pow(2, level + 1) - Math.abs(realCol));
		} else {
			col = realCol;
		}
		//远程获取
		if (parentPath.equals(remoteUrl)) {
			String cachePath = getReqDirect(layerName,reqSrs,reqFormat,tileWidth);
			File file = new File(cachePath);
			if (file.exists()) {
				result = getTdbFileResource(realRow,realCol,level,cachePath,layerName);
				int size = result == null ? 0 : result.length;
				//代码调试以及测试验证
//				log.info("getRealSource+++++++online++++++++" + size);
				if(result != null) return result;
			}
			final byte[] resource;
			ByteArrayOutputStream bos = null;
			int[] ranageTiled = null;
			if (isNeedToTransform()) {
				if ("EPSG:4326".equalsIgnoreCase(reqSrs)) {
					ReferencedEnvelope envelope = (ReferencedEnvelope) GeometryUtil.calcute4326bbox(level, realRow, realCol);
					ranageTiled = GeometryUtil.calcuteTileRange(envelope,level);
				} else if (OgcEpsg.getOgcEpsgCodeType(reqSrs) == OgcEpsg.MERCATOR_CODE) {
					ReferencedEnvelope bbox = null;
					bbox = GeometryUtil.calcute3857To4326LonLatBbox(level, realRow, realCol);
					//计算出火星坐标的经纬度范围
					Coordinate leftUpper = GeometryUtil.gps2Gcj02(bbox.getMinX(), bbox.getMaxY());
					Coordinate rightLower = GeometryUtil.gps2Gcj02(bbox.getMaxX(), bbox.getMinY());
					ranageTiled = GeometryUtil.convert4326To3857Tiled(leftUpper,rightLower,level);
				}
				
			}else {
				ranageTiled = new int[5];
				ranageTiled[0] = col;
				ranageTiled[1] = row;
				ranageTiled[2] = col;
				ranageTiled[3] = row;
				ranageTiled[4] = level;
			}
			try {
				BufferedImage image = cropImage(ranageTiled);
				bos = new ByteArrayOutputStream();
				ImageIO.write(image, resFormat, bos);
				resource = bos.toByteArray();
			} catch (IOException e) {
				log.error("OnlineTileData getRealSource has error:" + ExceptionsUtil.stackTraceToStr(e));
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_TRANSFOR, "图片格式化"));
			} finally {
				try {
					bos.close();
				} catch (Exception e) {
					log.error("OnlineTileData getRealSource has error:" + ExceptionsUtil.stackTraceToStr(e));
				}
			}
			//异步缓存
			writeCacheFile(cachePath,resource,col,row,level);
			return resource;
		}else {
			File file = new File(parentPath);
			if (file.exists() && file.isDirectory()) {
				result = getTdbFileResource(realRow, realCol, level, parentPath, layerName);
				int size = result == null ? 0 : result.length;
				//代码调试以及测试验证
				log.info("getRealSource+++++++online++++++++" + size);
				if(result != null) return result;
			}
			String resPath = getReqDirect(layerName,resFormat,resFormat,tileWidth);
			File resFormatFile = new File(resPath);
			if (resFormatFile.exists()) {
				result = getTdbFileResource(realRow, realCol, level, resPath, layerName);
				int size = result == null ? 0 : result.length;
				//代码调试以及测试验证
				log.info("getRealSource+++++++online++++++++" + size);
				if(result != null) return result;
			}
			return null;
		}
	}

	private BufferedImage cropImage(int[] tileRanage) {
		BufferedImage image = compressImg(tileRanage,level);
		final ReferencedEnvelope envelope;
		final Envelope upperEnvelope;
		final Envelope lowerEnvelope;
		if (OgcEpsg.getOgcEpsgCodeType(reqSrs) == OgcEpsg.MERCATOR_CODE) {
			envelope = (ReferencedEnvelope) GeometryUtil.calcute3857bbox(level, realRow, realCol, "EPSG:3857");
			upperEnvelope = GeometryUtil.calcute3857bbox(tileRanage[4], tileRanage[1], tileRanage[0], "EPSG:3857");
			lowerEnvelope = GeometryUtil.calcute3857bbox(tileRanage[4], tileRanage[3], tileRanage[2], "EPSG:3857");
		}else {
			envelope = (ReferencedEnvelope) GeometryUtil.calcute4326bbox(level, realRow, realCol);
			upperEnvelope = GeometryUtil.calcute4326bbox(tileRanage[4], tileRanage[1], tileRanage[0]);
			lowerEnvelope = GeometryUtil.calcute4326bbox(tileRanage[4], tileRanage[3], tileRanage[2]);
		}
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
		return image;
	}
	
	

	public byte[] getCurrentTile(int row, int col) {
		String httpUrl = getHttpUrl(level,row,col);
		HttpEntity entity = null;
		entity = HttpUtils.httpGet(httpUrl, null);
		return convertHttpEntityToByte(entity);
	}

	@Override
	public byte[] getDemSource(String filePath) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File getRealFile(String parentPath, String format) {
		return null;
	}

	@Override
	public byte[] getFormatSource(String parentPath) {
		return getRealSource(parentPath);
	}
}
