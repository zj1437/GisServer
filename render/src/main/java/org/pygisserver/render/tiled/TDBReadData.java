package org.pygisserver.render.tiled;

import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.enumm.OgcEpsg;
import org.pygisserver.common.exception.ArgumentException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.pytc.entity.TileRequestInfo;
import org.pygisserver.common.util.CommonUtil;
import org.pygisserver.common.util.GeometryUtil;
import org.pygisserver.common.util.ImageUtil;
import org.pygisserver.render.service.CustomDataLoad;
import org.pygisserver.render.service.impl.CustomDataLoadImpl;
import org.pygisserver.render.util.TdbUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: FlatGis数据读取  
 * flatgis  数据不支持投影转换  也就是当前数据是什么投影就直接返回
 * 不考虑投影的转化
 * @author: 刘洋
 * @date: 2020-08-24
 */
@Slf4j
public class TDBReadData extends TiledInfoReadAbstract implements TiledInfoRead {

	private String fileUrl;
	
	private String serverName;

	private int tileWidth;

	private int level;

	private int realRow;

	private int realCol;
	
	private String resFormat;

	private String reqFormat;
	
	private CustomDataLoad customDataLoad = new CustomDataLoadImpl();
	
	private int maxLevel;

	private String reqSrs;
	
	public TDBReadData(TileRequestInfo requestInfo,LayerBaseInfo tileInfo) {
		super(requestInfo, tileInfo);
		this.fileUrl = tileInfo.getFilePath();
		this.tileWidth = tileInfo.getTileWidth();
		this.realRow = requestInfo.getTileRow();
		this.realCol = requestInfo.getTileCol();
		this.serverName = tileInfo.getLayerName();
		this.reqSrs = requestInfo.getGridSet();
		String mineType = requestInfo.getMineType();
		if (mineType.indexOf("/") > -1) {
			this.reqFormat = mineType.split("/")[1];
		} else {
			this.reqFormat = mineType;
		}
		this.level = requestInfo.getZoomLevel();
		this.resFormat = tileInfo.getResourceFormat();
		if (StringUtils.isBlank(resFormat)) {
			resFormat = "jpeg";
		}
		this.maxLevel = tileInfo.getSupportMaxLevel();
	}
	
	@Override
	public BufferedImage compressImg(int[] tileRanage,int realLevel) {
		/**
		 * 获取当前存在的数据，并将其合并后，并按照，请求行列号进行裁剪
		 * 然后再缩放为请求的宽高 默认256*256
		 * TODO 当请求的瓦片不存在时，new 一个空白图
		 */
		int num = 0;
		List<BufferedImage> merageImages = new ArrayList<BufferedImage>();
		for (int j = tileRanage[1]; j <= tileRanage[3]; j++) { //row(y)
			List<BufferedImage> images = new ArrayList<BufferedImage>();
			for (int i = tileRanage[0]; i <= tileRanage[2]; i++) {  //col(x)
//				int row = (int) (Math.pow(2, realLevel)) - j - 1;
				int row = j;
				BufferedImage image = null;
				//根据请求的格式创建不同的空白图片
				try {
					String tdbType = TdbUtils.getTdbFileType(fileUrl,serverName);
					byte[] imageInfo = null;
					if(tdbType.equals(TdbUtils.TDB_FILE_DEM_SUFFIX)) {
						imageInfo = customDataLoad.getElvationInfo(row, i, tileRanage[4], fileUrl, serverName);
						if(imageInfo != null) {
							image = TdbUtils.getTDBDemImage(imageInfo);
						}
					} else {
						imageInfo = customDataLoad.getImageInfo(row, i, tileRanage[4], fileUrl, serverName);
						if(imageInfo != null) {
							InputStream iStream = new ByteArrayInputStream(imageInfo);
							image = ImageIO.read(iStream);
						}else {
							// 解决空白的问题
							image = dealTiledImgExists(tileRanage, j, i, image);
							num++;
						}
					}
					
				} catch (ArgumentException e) {
					throw new ArgumentException(e.getErrorCode(),e.getMessage());
				} catch (ResourceException e) {
					throw new ResourceException(e.getErrorCode(),e.getMessage());
				} catch (IOException e) {
					log.error("TDBReadData compressImg has error:" + ExceptionsUtil.stackTraceToStr(e));
					throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, serverName + "数据"));
				}
				if (image != null) {
					images.add(image);
				}
			}
			//横向
			if (images.size() > 0) {
				BufferedImage merageImage = null;
				merageImage = ImageUtil.mergeImageByImage(1, reqFormat,images.toArray(new BufferedImage[] {}));
				merageImages.add(merageImage);
			}
		}
		if (num == ((tileRanage[3] - tileRanage[1] + 1) * (tileRanage[2] - tileRanage[0] + 1))) {
			return null;
		}
		//纵向
		try {
			return ImageUtil.mergeImageByImage(2, reqFormat,merageImages.toArray(new BufferedImage[] {}));
		} catch (Exception e) {
			log.error("FlatGisReadData compressImg has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_MERGE, "图层数据"));
		}
	}

	
	public BufferedImage dealTiledImgExists(int[] tileRanage, int j, int i, BufferedImage image) {
		/**
		 * 解决空白的问题
		 * 文件不存在，则根据最大级数据来获取指定的数据，否则就会空白图
		 */
		if (tileRanage[4] > maxLevel) {
			int level = (int) Math.pow(2, (tileRanage[4] - maxLevel));
			int currentRow =  j / level;  //不能使用row 来计算 因为  我们的算法都是基于请求的url的，因此行的计算应该基于以前的逻辑
			int currentCol =  i / level;
			currentRow = (int) (Math.pow(2, maxLevel)) - currentRow - 1;
			try {
				byte[] bufferImageInfo = customDataLoad.getImageInfo(currentRow, currentCol, maxLevel, fileUrl, serverName);
				InputStream iStream = new ByteArrayInputStream(bufferImageInfo);
				image = ImageIO.read(iStream);
			} catch (IOException e) {
				e.printStackTrace();
			}
			//取出裁剪缩放
			int width = image.getWidth();
			int height = image.getHeight();
			int offsetX = i % level;
			int offsetY = j % level;
			int widthX = width / level;
			int heightX = height / level;
			image = image.getSubimage(offsetX*widthX, heightX*offsetY, widthX, heightX);
			image = ImageUtil.compressImage(image, width, height);
		}else {
			//根据请求的格式创建不同的空白图片
			if(this.reqFormat.equals("png")) {
				image = ImageUtil.createTransparentImg(tileWidth, tileWidth);
			} else {
				image = ImageUtil.createWhiteBgcolorImg(tileWidth, tileWidth);
			}
		}
		return image;
	}
	
	
	@Override
	public byte[] getTmsResource() {
		return null;
	}

	public byte[] getRealSource(String parentPath) {
		//屏蔽各种行的转换
//		int row = (int) (Math.pow(2, level)) - realRow - 1;
		int row = realRow;
		int col = -1;
		if (realCol < 0) {
			col = (int) (Math.pow(2, level + 1) - Math.abs(realCol));
		}else {
			col = realCol;
		}
		//如果parentPath跟fileurl相等，则去获取对应的数据，否则，去文件缓存中获取
		if (parentPath.equals(fileUrl)) {
			return customDataLoad.getImageInfo(row, col, level, fileUrl, serverName);
		}else {
			File file = new File(parentPath);
			if (file.exists() && file.isDirectory()) {
				return getTdbFileResource(row, col, level, parentPath, serverName);
			}
			//TdbUtils.getVistitCacheDirName(serverName, crs, format, width);
			String resPath = getReqDirect(serverName,resFormat,resFormat,tileWidth);
			File resFormatFile = new File(resPath);
			if (resFormatFile.exists() && resFormatFile.isDirectory()) {
				return getTdbFileResource(row, col, level, resPath, serverName);
			}
		}
		return null;
	}

	@Override
	public byte[] getDemSource(String filePath) {
		//修复适配etdb 16*16的瓦片后 terrain格式的瓦片无法获取高程问题
		if (reqFormat.equalsIgnoreCase("terrain") && tileWidth != 65){
			float[] tdbTerrain = getTdbTerrain();
			return convetDemPixelData(tdbTerrain);
		}
		int row =  (int)Math.pow(2, level) - realRow -1;
		byte[] buffer = customDataLoad.getElvationInfo(row, realCol, level, fileUrl, serverName);
		//当以约定的TDB格式请求数据时，服务只需要将数据读取并返回，否则将其转换为tiff后再返回。
		//该操作主要目的是提高TDB高程的并发效率
		if(buffer == null) return null;
		if (reqFormat.equalsIgnoreCase(TdbUtils.TDB_DEM_TYPE) || buffer.length != tileWidth * tileWidth * 4) {
			return buffer;
		} else {
			float[] rbgArray = new float[tileWidth * tileWidth];
			int index = 0;
			for (int i = 0; i < buffer.length; i+=4) {
			    byte[] demBuf = Arrays.copyOfRange(buffer, i, (index + 1) * 4);
				float demValue = CommonUtil.bytes2float(demBuf);
				rbgArray[index] = demValue;
				index++;
			}
			float[] finalArray = new  float[tileWidth * tileWidth];
			for (int i = 0; i < tileWidth; i++) {
				for(int j = 0; j < tileWidth; j++) {
					finalArray[(tileWidth-1-i)*tileWidth +j] = rbgArray[i*tileWidth +j];
				}
			}
			DataBufferFloat dataBuffer = new DataBufferFloat(finalArray, tileWidth*tileWidth);
	 		ColorSpace space = ColorSpace.getInstance(ColorSpace.CS_GRAY);
	     	ComponentSampleModel sampleModle = new ComponentSampleModel(DataBuffer.TYPE_FLOAT,tileWidth , tileWidth, 1, tileWidth, new int[] {0});
	     	int[] bits = {32};
	     	ComponentColorModel color1 = new ComponentColorModel(space, bits, false, false, Transparency.TRANSLUCENT,DataBuffer.TYPE_FLOAT);
	     	WritableRaster raster = Raster.createWritableRaster(sampleModle,dataBuffer,new Point(0,0));
	     	BufferedImage image = new BufferedImage(color1,raster,false,null);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
	     	try {
				ImageIO.write(image, "tiff", bos);
			} catch (IOException e) {
				log.error("TDBReadData getDemSource has error:" + ExceptionsUtil.stackTraceToStr(e));
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, serverName + "数据"));
			}
	     	return bos.toByteArray();
		}
	}

	private float[] getTdbTerrain(){
		int num = 0;
		List<Float[]> tileArr = new ArrayList<>();
		//获取当前请求行列号对应bbox
		ReferencedEnvelope referencedEnvelope = null;
		int[] tiledRange = new int[5];
		if (reqSrs != null && OgcEpsg.getOgcEpsgCodeType(reqSrs) == 1){
			referencedEnvelope = GeometryUtil.calcute3857bbox(level, realRow, realCol,reqSrs);
			tiledRange = GeometryUtil.convert3857ToTiledRange(level + 2, referencedEnvelope);
		}else {
			referencedEnvelope = GeometryUtil.calcute4326bbox(level, realRow, realCol);
			tiledRange = GeometryUtil.convert4326ToTiledRange(level + 2, referencedEnvelope);
		}
		for (int j = tiledRange[1] ; j <= tiledRange[3] ; j++) {
			int row = (int)Math.pow(2, level + 2) - j -1;
			for (int i = tiledRange[0]; i <= tiledRange[2]; i++) {
				byte[] tdbData = customDataLoad.getElvationInfo(row, i, tiledRange[4], fileUrl, serverName);
				if (tdbData == null){
					tdbData = new byte[tileWidth * tileWidth * 4];
				}
				Float[] finalArray = new Float[tileWidth * tileWidth];
				if (tdbData.length != tileWidth * tileWidth * 4){
                    float[] tempArray = new float[tileWidth * tileWidth];
					try {
						//处理服务切图tiff的etdb数据解析后剥除tiff头问题
						BufferedImage read = ImageIO.read(new ByteArrayInputStream(tdbData));
						read.getSampleModel().getPixels(0,0,tileWidth,tileWidth,tempArray,read.getData().getDataBuffer());
					} catch (IOException e) {
						//read方法错误时，捕获异常，多半是数据错误导致的数组填充问题，此处不抛出异常
						log.error("TDBReadData getTdbTerrain has error:" + ExceptionsUtil.stackTraceToStr(e));
					}
                    finalArray = ArrayUtils.toObject(tempArray);
                }else {
					float[] dataArr = new float[tileWidth * tileWidth];
					int n = 0;
					//byte数组转float数组
					for (int k = 0; k < tdbData.length; k += 4) {
						byte[] demBuf = Arrays.copyOfRange(tdbData, k, (n + 1) * 4);
						float demValue = CommonUtil.bytes2float(demBuf);
						dataArr[n] = demValue == Float.MIN_VALUE ? 0 : demValue;
						n++;
					}
					//处理桌面端切etdb瓦片逻辑坐标原点在左下角,服务端切etdb原点为左上角
					for (int y = 0; y < tileWidth; y++) {
						for(int x = 0; x < tileWidth; x++) {
							finalArray[(tileWidth-1-y)*tileWidth +x] = dataArr[y*tileWidth +x];
						}
					}
				}
				//暂存16张瓦片数组对象
				tileArr.add(finalArray);
			}
		}
		List<Float> result = new ArrayList<>();
		//获取16张etdb瓦片后，再处理拼接
		for (int i = 0; i < tileArr.size() / 4 ; i++) {
			List<Float[]> tempArr = tileArr;
			//一次4张拼接
			List<Float[]> tileList = tempArr.subList(i * 4, (i + 1) * 4);
			for (int j = 0; j < tileWidth ; j++) {
				int size = 0;
				for (int k = 0; k < tileWidth / 4 ; k++) {
					Float[] tileData = tileList.get(k);
					for (int l = tileWidth*j ; l < (j+1)* tileWidth ; l++) {
						result.add(tileData[l]);
						size++;
						if (size == 64){
							//横轴第65个像素值,压边
							result.add(tileData[l]);
						}
					}
				}
			}
			if (i == 3){
				//最后一行裁剪
				for (int k = 0; k < tileWidth / 4 ; k++) {
					Float[] tileData = tileList.get(k);
					for (int l = tileWidth*15 ; l < 16* tileWidth ; l++) {
						result.add(tileData[l]);
					}
				}
			}
		}
		result.add(result.get(result.size() - 1));
		if (num == ((tiledRange[3] - tiledRange[1] + 1) * (tiledRange[2] - tiledRange[0] + 1))) {
			return null;
		}
		return ArrayUtils.toPrimitive(result.toArray(new Float[result.size()]),0.0F);
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
