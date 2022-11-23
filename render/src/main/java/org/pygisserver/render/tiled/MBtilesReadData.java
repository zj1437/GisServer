package org.pygisserver.render.tiled;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.Constant;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.exception.SqlException;
import org.pygisserver.common.pytc.entity.TileRequestInfo;
import org.pygisserver.common.sqlite.util.SQLitePoolManage;
import org.pygisserver.common.util.ImageUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MBtilesReadData extends TiledInfoReadAbstract  implements TiledInfoRead {
	
	private String dbUrl;
	
	private int tileWidth;

	private int tileHeight;
	
	private String layerName;
	
	private int level;
	
	private int realRow;
	
	private int col;
	
	private String reqFormat;
	
	private String resFormat;
	
	private String fileUrl;
	
	private int maxLevel;
	
	public MBtilesReadData(TileRequestInfo requestInfo, LayerBaseInfo tileInfo) {
		super(requestInfo, tileInfo);
		this.tileWidth = 256;
		this.tileHeight = 256;
		String mineType = requestInfo.getMineType();
		if (mineType.indexOf("/") > -1) {
			this.reqFormat = mineType.split("/")[1];
		} else {
			this.reqFormat = mineType;
		}
		this.resFormat = tileInfo.getResourceFormat();
		if (StringUtils.isBlank(resFormat)) {
			resFormat = "jpeg";
		}
		this.level = requestInfo.getZoomLevel();
		this.realRow = (int) (Math.pow(2, level) - requestInfo.getTileRow() - 1);
		this.col = requestInfo.getTileCol();
		this.fileUrl = tileInfo.getFilePath();
		this.layerName = requestInfo.getLayerName();
		this.dbUrl = "jdbc:sqlite:" + tileInfo.getFilePath();
		SQLitePoolManage.createConnectionPool(dbUrl);
		this.maxLevel = tileInfo.getSupportMaxLevel();
	}

	@Override
	public BufferedImage compressImg(int[] tileRanage, int realLevel) {
		realLevel = tileRanage[4];
		List<BufferedImage> merageImages = new ArrayList<BufferedImage>();
		int num = 0;
		for (int j = tileRanage[1]; j <= tileRanage[3]; j++) { // row(y)
			List<BufferedImage> images = new ArrayList<BufferedImage>();
			for (int i = tileRanage[0]; i <= tileRanage[2]; i++) { // col(x)
				int row = (int) (Math.pow(2, realLevel)) - j - 1;
				BufferedImage image = null;
				String sql = String.format(Constant.SQL, i,row,realLevel); 
				try {
					//每次设置当前的连接池
					byte[] buffer = SQLitePoolManage.getMbTilesImage(sql,dbUrl);
					if (buffer == null) {
						image = dealTiledImgExists(tileRanage, j, i, image);
						if (image == null){
							image = constructDefaultImage();
							num++;
						}	
					}else {
						ByteArrayInputStream bis = new ByteArrayInputStream(buffer);
						image = ImageIO.read(bis);
					}
				} catch (SqlException e) {
					throw new SqlException(e.getErrorCode(),e.getMessage());
				} catch (Exception e) {
					log.error("MBtilesReadData compressImg has error:" + ExceptionsUtil.stackTraceToStr(e));
					throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, "MbTiles瓦片数据"));
				}
				if(image != null) images.add(image);
			}
			// 横向
			if (images.size() > 0) {
				BufferedImage merageImage = null;
				merageImage = ImageUtil.mergeImageByImage(1, reqFormat,images.toArray(new BufferedImage[] {}));
				merageImages.add(merageImage);
			}
		}
		if (num == ((tileRanage[3] - tileRanage[1] + 1)*(tileRanage[2] - tileRanage[0] + 1))) {
			return null;
		}
		try {
			BufferedImage image = ImageUtil.mergeImageByImage(2, reqFormat,merageImages.toArray(new BufferedImage[] {}));
			return image;
		} catch (Exception e) {
			log.error("MBtilesReadData compressImg has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_MERGE, "MbTiles瓦片数据"));
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
				String sql = String.format(Constant.SQL, currentCol,currentRow,maxLevel);
				byte[] buffer = SQLitePoolManage.getMbTilesImage(sql,dbUrl);
				ByteArrayInputStream bis = new ByteArrayInputStream(buffer);
				image = ImageIO.read(bis);
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
				image = new BufferedImage(tileWidth, tileWidth, BufferedImage.TYPE_INT_RGB);
			}
		}
		return image;
	}
	
	
	@Override
	public File getRealFile(String parentPath, String format) {
		return null;
	}

	@Override
	public byte[] getRealSource(String parentPath) {
		byte[] result = null;
		
		//如果是请求元数据，直接去数据库查询
		if (fileUrl.equals(parentPath)) {
			String sql = String.format(Constant.SQL, col,realRow,level);
			return SQLitePoolManage.getMbTilesImage(sql,dbUrl);
		}else {
			File file = new File(parentPath);
			if (file.exists() && file.isDirectory()) {
				result = getTdbFileResource(realRow, col, level, parentPath, layerName);
				if(result != null) return result;
			}
			String resPath = getReqDirect(layerName,resFormat,resFormat,tileWidth);
			File resFormatFile = new File(resPath);
			if (resFormatFile.exists() && resFormatFile.isDirectory()) {
				return getTdbFileResource(realRow, col, level, resPath, layerName);
			}
			return null;
		}
	}

	@Override
	public byte[] getDemSource(String filePath) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] getFormatSource(String parentPath) {
		return getRealSource(parentPath);
	}
	
}
