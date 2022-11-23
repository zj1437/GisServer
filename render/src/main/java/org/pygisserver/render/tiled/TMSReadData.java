package org.pygisserver.render.tiled;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.pytc.entity.TileRequestInfo;
import org.pygisserver.common.util.ImageUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: FlatGis数据读取  
 * flatgis  数据不支持投影转换  也就是当前数据是什么投影就直接返回
 * 不考虑投影的转化
 * @author: 刘洋
 * @date: 2020-08-24
 */
@Slf4j
public class TMSReadData extends TiledInfoReadAbstract implements TiledInfoRead {

	private String fileUrl;

	private String resFormat;

	private int tileWidth;
	
	private String layerName;

	private int level;

	private int realRow;

	private int realCol;
	
	private String reqFormat;
	
	private int maxLevel;
	
	private int origin;

	public TMSReadData(TileRequestInfo requestInfo,LayerBaseInfo tileInfo) {
		super(requestInfo, tileInfo);
		this.resFormat = tileInfo.getResourceFormat();
		this.tileWidth = tileInfo.getTileWidth();
		this.realRow = requestInfo.getTileRow();
		this.realCol = requestInfo.getTileCol();
		this.layerName = requestInfo.getLayerName();
		this.level = requestInfo.getZoomLevel();
		this.fileUrl = tileInfo.getFilePath();
		String mineType = requestInfo.getMineType();
		this.reqFormat = createReqFormat(mineType);
		this.origin = tileInfo.getOrigin();
		this.maxLevel = tileInfo.getSupportMaxLevel();
	}
	

	@Override
	public BufferedImage compressImg(int[] tileRanage,int realLevel)  {
		/**
		 * 获取当前存在的数据，并将其合并后，并按照，请求行列号进行裁剪
		 * 然后再缩放为请求的宽高 默认256*256
		 * TODO 当请求的瓦片不存在时，new 一个空白图
		 */
		List<BufferedImage> merageImages = new ArrayList<BufferedImage>();
		int num = 0;
		for (int j = tileRanage[1]; j <= tileRanage[3]; j++) { //row(y)
			List<BufferedImage> images = new ArrayList<BufferedImage>();
			for (int i = tileRanage[0]; i <= tileRanage[2]; i++) {  //col(x)
				int row;
				if (origin == 0) {
					row = (int) (Math.pow(2, realLevel)) - j - 1;
				}else {
					row = j;
				}
				File file = new File(constructImageFilePath(tileRanage[4],row,i));
				BufferedImage image = null;
				if (!file.exists()) {
					image = dealTiledImgExists(tileRanage, j, i, image);
					if (image == null){
						image = constructDefaultImage();
						num++;
					}
				} else {
					try {
						image = ImageIO.read(file);
					} catch (IOException e) {
						log.error("TMSReadData compressImg has error:" + ExceptionsUtil.stackTraceToStr(e));
						throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, "瓦片数据"));
					}
				}
				//如果是单通道的tif，则需要转化
				if (("tiff".equals(resFormat) || "tif".equals(resFormat)) && image.getColorModel().getNumComponents() == 1) {
					//请求格式不是tiff时，需要处理通道
					if (!"tiff".equals(reqFormat) ) {
						image = ImageUtil.forceComponet(image);
					}
				}
				images.add(image);
			}
			//横向
			if (images.size() > 0) {
				BufferedImage merageImage = null;
				merageImage = ImageUtil.mergeImageByImage(1,reqFormat,images.toArray(new BufferedImage[] {}));
				merageImages.add(merageImage);
			}
		}
		//如果所有请求瓦片都不存在，则直接返回空
		if (num == ((tileRanage[3] - tileRanage[1] + 1) * (tileRanage[2] - tileRanage[0] + 1))) {
			return null;
		}
		//纵向
		try {
			BufferedImage image = ImageUtil.mergeImageByImage(2, reqFormat,merageImages.toArray(new BufferedImage[] {}));
			return image;
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
			if (origin == 0) {
				currentRow = (int) (Math.pow(2, maxLevel)) - currentRow - 1;
			}
			try {
				File file = new File(constructImageFilePath(maxLevel, currentRow, currentCol));
				if (!file.exists()) {
					return null;
				}
				image = ImageIO.read(file);
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
		}
		return image;
	}


	//TMS瓦片存放格式为 level/col/row.png or row.jpg
	private String constructImageFilePath(int level,int realRow,int realCol) {
		StringBuilder sb = new StringBuilder(fileUrl);
		sb.append(File.separator).append(level).append(File.separator).append(realCol).
		append(File.separator).append(realRow).append(".").append(resFormat);
		return sb.toString();
	}
	
	
	public byte[] getRealSource(String parentPath) {
		File resFormatFile = getRealFile(parentPath,resFormat);
		return getCacheFileResource(resFormatFile);
	}


	/**
	 * 直接返回tif数据
	 */
	@Override
	public byte[] getDemSource(String filePath) {
		File file = getRealFile(filePath,resFormat);
		return getCacheFileResource(file);
	}


	@Override
	public File getRealFile(String parentPath, String format) {
		//行列转换
		if (origin == 0) {
			realRow = (int) (Math.pow(2, level)) - realRow - 1;
		}
		StringBuilder sb = new StringBuilder(parentPath);
		sb.append(File.separator).append(level).append(File.separator).append(realCol).
		append(File.separator).append(realRow).append(".").append(format);
		return new File(sb.toString());
	}


	@Override
	public byte[] getFormatSource(String parentPath) {
		return getRealSource(parentPath);
	}
}
