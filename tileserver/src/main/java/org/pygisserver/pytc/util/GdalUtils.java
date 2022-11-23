package org.pygisserver.pytc.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.pytc.entity.TileRequestInfo;
import org.pygisserver.common.util.FileUtil;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.gdalconst.gdalconstConstants;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Envelope;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: gdal的工具类
 * @author:  
 * @date: 2020-09-17
 */
@Component
@Slf4j
public class GdalUtils {
	
	private static final String JAVA_TEMP_DIR = System.getProperty("java.io.tmpdir");
	
	public byte[] getTileResource(TileRequestInfo tileInfo, Dataset dataSet, String resourceSrs, String styleName) {
		//获取请求的bbox
		ReferencedEnvelope reqBbox = (ReferencedEnvelope) tileInfo.getBbox();
		int width = tileInfo.getImageWidth();
		int height = tileInfo.getImageHeight();
		//获取分辨率以及原始bbox
		double[] ori_transform = dataSet.GetGeoTransform();
		int xCount = dataSet.getRasterXSize();
		int yCount = dataSet.getRasterYSize();
		int numband = dataSet.getRasterCount();
		//经度计算
		double minX = ori_transform[0];
		double maxX = ori_transform[0] + xCount*ori_transform[1];
		//纬度计算
		double maxY = ori_transform[3];
		double minY = ori_transform[3] + yCount*ori_transform[5];
		//分辨率
		double resoultionX = ori_transform[1];
		double resoultionY = ori_transform[5];
		Envelope resEnvelope = null;
		try {
			resEnvelope = new ReferencedEnvelope(minX, maxX, minY, maxY, CRS.decode(resourceSrs,true));
		} catch (MismatchedDimensionException | FactoryException e) {
			e.printStackTrace();
		} //这边替换为原始数据投 
		if (!resEnvelope.intersects(reqBbox)) {
			return null;
		}
		//交集数据
		ReferencedEnvelope intesect = (ReferencedEnvelope) resEnvelope.intersection(reqBbox);
		//计算切片分辨率
		double tileResX = (reqBbox.getMaxX() - reqBbox.getMinX()) / width;
		double tileResY = (reqBbox.getMaxY() - reqBbox.getMinY()) / height;
		//计算起始点位置
		int offset_x = (int) ((intesect.getMinX() - minX) / resoultionX);
		int offset_y = (int) ((intesect.getMaxY() - maxY) / resoultionY);
		//计算原始需要多少个像素
		int block_xsize = (int) ((intesect.getMaxX() - intesect.getMinX()) / resoultionX);
		int block_ysize = (int)Math.abs((intesect.getMaxY() - intesect.getMinY()) / resoultionY);
		//计算切片长度
		int image_Xbuf = (int) Math.ceil((intesect.getMaxX() - intesect.getMinX()) / tileResX);
		int image_Ybuf =  (int) Math.ceil(Math.abs((intesect.getMaxY() - intesect.getMinY()) / tileResY));
		//原始图像在切片中的偏移量
		int imageOffSetX = (int) ((intesect.getMinX() - reqBbox.getMinX()) / tileResX);
		int imageOffSetY = (int) Math.abs((intesect.getMaxY() - reqBbox.getMaxY()) / tileResX);
		imageOffSetX = imageOffSetX > 0 ?  imageOffSetX : 0;
		imageOffSetY = imageOffSetY > 0 ?  imageOffSetY : 0;
		//获取通道数
		String format = tileInfo.getMineType(); 
		String outPutFile = null;
		Driver driver = null;
		// 使用PNG驱动将内存中的图像数组写入文件
		if (format.endsWith("png")) {
			outPutFile = JAVA_TEMP_DIR + File.separator + System.currentTimeMillis() + File.separator + "temp.png";
			driver = gdal.GetDriverByName("PNG");
			format = "png";
		}else if(format.endsWith("jpeg")) {
			outPutFile = JAVA_TEMP_DIR + File.separator + System.currentTimeMillis() + File.separator + "temp.jpg";
			driver = gdal.GetDriverByName("JPEG");
			format = "jpeg";
		} else if(format.endsWith("tiff")) {
			outPutFile = JAVA_TEMP_DIR + File.separator + System.currentTimeMillis() + File.separator + "temp.tiff";
			driver = gdal.GetDriverByName("GTiff");
			format = "tiff";
		}
		try {
			if ("高程".equals(styleName) && "tiff".equals(format)) {
				Band band1 = dataSet.GetRasterBand(1);
	    		float[] rasterPiexl = new float[image_Xbuf*image_Ybuf];
	    		band1.ReadRaster(offset_x, offset_y, block_xsize, block_ysize, image_Xbuf, image_Ybuf, gdalconstConstants.GDT_Float32, rasterPiexl, 0, 0);
	    		File file = new File(outPutFile);
				if (!file.getParentFile().exists()) {
					file.getParentFile().mkdirs();
				}
	    		Dataset tifdataset = driver.Create(outPutFile, width, height, 1,gdalconstConstants.GDT_Float32);
	    		Band datBand = tifdataset.GetRasterBand(1);
	    		datBand.WriteRaster(imageOffSetX, imageOffSetY, image_Xbuf, image_Ybuf, rasterPiexl);
	    		datBand.FlushCache();
	    		tifdataset.delete();
			}else {
				Driver memDriver = gdal.GetDriverByName("MEM");
				Dataset msmDS = null;
				if (numband == 3) {
					Band in_band1 = dataSet.GetRasterBand(1);
					Band in_band2 = dataSet.GetRasterBand(2);
					Band in_band3 = dataSet.GetRasterBand(3);
					int[] band1BuffData = new int[image_Xbuf * image_Ybuf];
					int[] band2BuffData = new int[image_Xbuf * image_Ybuf];
					int[] band3BuffData = new int[image_Xbuf * image_Ybuf];
					in_band1.ReadRaster(offset_x, offset_y, block_xsize, block_ysize, image_Xbuf, image_Ybuf,gdalconst.GDT_Int32 , band1BuffData, 0, 0);
					in_band2.ReadRaster(offset_x, offset_y, block_xsize, block_ysize, image_Xbuf, image_Ybuf,gdalconst.GDT_Int32 , band2BuffData, 0, 0);
					in_band3.ReadRaster(offset_x, offset_y, block_xsize, block_ysize, image_Xbuf, image_Ybuf,gdalconst.GDT_Int32 , band3BuffData, 0, 0);
					// 使用gdal的MEM驱动在内存中创建一块区域存储图像数组
					msmDS = memDriver.Create("msmDS", width, height, 4);
					Band dstBand1 = msmDS.GetRasterBand(1);
					Band dstBand2 = msmDS.GetRasterBand(2);
					Band dstBand3 = msmDS.GetRasterBand(3);
					// 设置alpha波段数据,实现背景透明
					Band alphaBand = msmDS.GetRasterBand(4);
					int[] alphaData = new int[width * height];
					for (int index = 0; index < alphaData.length; index++) {
						if (band1BuffData[index] > 0) {
							alphaData[index] = 255;
						}
					}
					// 写各个波段数据
					dstBand1.WriteRaster(imageOffSetX, imageOffSetY, image_Xbuf, image_Ybuf, band1BuffData);
					dstBand2.WriteRaster(imageOffSetX, imageOffSetY, image_Xbuf, image_Ybuf, band2BuffData);
					dstBand3.WriteRaster(imageOffSetX, imageOffSetY, image_Xbuf, image_Ybuf, band3BuffData);
					alphaBand.WriteRaster(imageOffSetX, imageOffSetY, image_Xbuf, image_Ybuf, alphaData);
				}
				if (numband == 4) {
					Band in_band1 = dataSet.GetRasterBand(1);
					Band in_band2 = dataSet.GetRasterBand(2);
					Band in_band3 = dataSet.GetRasterBand(3);
					Band in_band4 = dataSet.GetRasterBand(4);
					int[] band1BuffData = new int[image_Xbuf * image_Ybuf];
					int[] band2BuffData = new int[image_Xbuf * image_Ybuf];
					int[] band3BuffData = new int[image_Xbuf * image_Ybuf];
					int[] band4BuffData = new int[image_Xbuf * image_Ybuf];
					in_band1.ReadRaster(offset_x, offset_y, block_xsize, block_ysize, image_Xbuf, image_Ybuf,gdalconst.GDT_Int32 , band1BuffData, 0, 0);
					in_band2.ReadRaster(offset_x, offset_y, block_xsize, block_ysize, image_Xbuf, image_Ybuf,gdalconst.GDT_Int32 , band2BuffData, 0, 0);
					in_band3.ReadRaster(offset_x, offset_y, block_xsize, block_ysize, image_Xbuf, image_Ybuf,gdalconst.GDT_Int32 , band3BuffData, 0, 0);
					in_band4.ReadRaster(offset_x, offset_y, block_xsize, block_ysize, image_Xbuf, image_Ybuf,gdalconst.GDT_Int32 , band3BuffData, 0, 0);
					// 使用gdal的MEM驱动在内存中创建一块区域存储图像数组
					msmDS = memDriver.Create("msmDS", width, height, 4);
					Band dstBand1 = msmDS.GetRasterBand(1);
					Band dstBand2 = msmDS.GetRasterBand(2);
					Band dstBand3 = msmDS.GetRasterBand(3);
					Band alphaBand = msmDS.GetRasterBand(4);
					// 写各个波段数据
					dstBand1.WriteRaster(imageOffSetX, imageOffSetY, image_Xbuf, image_Ybuf, band1BuffData);
					dstBand2.WriteRaster(imageOffSetX, imageOffSetY, image_Xbuf, image_Ybuf, band2BuffData);
					dstBand3.WriteRaster(imageOffSetX, imageOffSetY, image_Xbuf, image_Ybuf, band3BuffData);
					alphaBand.WriteRaster(imageOffSetX, imageOffSetY, image_Xbuf, image_Ybuf, band4BuffData);
				}
				
				if (numband == 1) {

					int type = dataSet.GetRasterBand(1).GetColorInterpretation();
					//如果是PaletteIndex 则需要将像素的颜色，进行分解
					if (type == gdalconst.GCI_PaletteIndex) {
						Band in_band1 = dataSet.GetRasterBand(1);
						int[] byteBufferData = new int[image_Xbuf*image_Ybuf];
						in_band1.ReadRaster(offset_x, offset_y, block_xsize, block_ysize, image_Xbuf, image_Ybuf,gdalconst.GDT_Int32, byteBufferData, 0, 0);
						BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
						for (int i = 0,image_offset_y = imageOffSetY; i < image_Ybuf; i++,image_offset_y++) {
							for (int j = 0,image_offset_x = imageOffSetX; j < image_Xbuf; j++,image_offset_x++) {
								int indexColor = byteBufferData[j + i*image_Xbuf];
								Color colorEntry = in_band1.GetColorTable().GetColorEntry(indexColor);
								int paletteType = in_band1.GetColorTable().GetPaletteInterpretation();
								Color color = null;
								if (paletteType == gdalconst.GPI_RGB) {
									color = new Color(colorEntry.getRed(),colorEntry.getGreen(),colorEntry.getBlue(),255);
								}else if (paletteType == gdalconst.GPI_CMYK){
									
								}
								else if (paletteType == gdalconst.GPI_HLS){
								}
								else if (paletteType == gdalconst.GPI_Gray){
									int red = (char)colorEntry.getRed() * 255;
									int green = (char)colorEntry.getGreen() * 255;
									int blue = (char)colorEntry.getBlue() * 255;
									int alpha = (char) 255;
									color = new Color(red,green,blue,alpha);
								}
								image.setRGB(image_offset_x, image_offset_y, color.getRGB());
							}
						}
						//TODO 如果请求的投影格式和原始数据不相同，则需要处理投影转化问题
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						ImageIO.write(image, format, bos);
						return bos.toByteArray();
					}
					//单通道图像，直接显示为灰度图像
					else if (type == gdalconst.GCI_GrayIndex) {
						Band in_band1 = dataSet.GetRasterBand(1);
						int[] byteBufferData = new int[image_Xbuf*image_Ybuf];
						in_band1.ReadRaster(offset_x, offset_y, block_xsize, block_ysize, image_Xbuf, image_Ybuf,gdalconst.GDT_Int32, byteBufferData, 0, 0);
						msmDS = memDriver.Create("msmDS", width, height, 1);
						Band dstBand1 = msmDS.GetRasterBand(1);
						dstBand1.WriteRaster(imageOffSetX, imageOffSetY, image_Xbuf, image_Ybuf, byteBufferData);
					}
				}
				
				File file = new File(outPutFile);
				if (!file.getParentFile().exists()) {
					file.getParentFile().mkdirs();
				}
				Dataset pngDs = driver.CreateCopy(outPutFile, msmDS);
				//释放内存
				msmDS.FlushCache();
				pngDs.delete();
			}
			BufferedImage image = ImageIO.read(new File(outPutFile));
			//TODO 如果请求的投影格式和原始数据不相同，则需要处理投影转化问题
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ImageIO.write(image, format, bos);
			FileUtil.deleteFile(outPutFile);
			return bos.toByteArray();
		} catch (IOException e) {
			log.error("GdalUtils getTileResource has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_READ_WRITE, tileInfo.getLayerName()));
		}
	}
}
