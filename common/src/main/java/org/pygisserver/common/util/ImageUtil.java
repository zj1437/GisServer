package org.pygisserver.common.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.Interpolation;

import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.geotools.image.ImageWorker;
import org.geotools.image.io.ImageIOExt;
import org.geotools.image.util.ImageUtilities;
import org.geotools.styling.AbstractStyleVisitor;
import org.geotools.styling.ColorMap;
import org.geotools.styling.Style;

import ar.com.hjg.pngj.FilterType;
import it.geosolutions.imageio.plugins.png.PNGWriter;
import lombok.extern.slf4j.Slf4j;

/**
 * 图像处理工具类,主要用于图像的合并，裁剪，格式转换等
 * @author
 */
@Slf4j
public class ImageUtil {

    /**
     * 压缩率
     */
    private static final float COMPRESS_RATION = 0.75f;
    
    
    private static final ImageWriterSpi writerSPI =
            new it.geosolutions.imageioimpl.plugins.tiff.TIFFImageWriterSpi();
    
	/**
	 * 将图片数据格式化为jpeg
	 * @param image
	 * @return
	 * @throws IOException
	 */
    public static byte[] formatImage2Jpeg(RenderedImage image) {
         ImageWorker iw = new ImageWorker(image);
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         try {
			iw.writeJPEG(bos, "JPEG", COMPRESS_RATION, false);
		} catch (IOException e) {
			log.error("ImageUtil formatImage2Jpeg has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_TRANSFOR, "瓦片压缩类型"));
		}finally {
			FileUtil.closeStream(bos);
			ImageUtilities.disposeImage(image);
		}
         return bos.toByteArray();
    }
    
    /**
	 * 将图片数据格式化为png
	 * @param image
	 * @return
     * @throws Exception 
	 * @throws IOException
	 */
    public static byte[] formatImage2Png(RenderedImage image,Style...styles) {
    	boolean hasAlpha = image.getColorModel().hasAlpha();
    	RenderedImage pngImage = null;
    	if (!hasAlpha) {
    		//TODO TYPE_4BYTE_ABGR与TYPE_INT_ARGB区别
    		pngImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
    		if (image instanceof BufferedImage) {
    			((Image) pngImage).getGraphics().drawImage((Image) image, 0, 0, null);
    		}
    	}else {
    		pngImage = image;
    	}
    	
    	//默认使用PNGJENCODER
		PNGWriter writer = new PNGWriter();
		FilterType filterType = getFilterType(styles);
		boolean isScanlineSupported = writer.isScanlineSupported(pngImage);
		if (!isScanlineSupported) {
            image = new ImageWorker(pngImage).rescaleToBytes().forceComponentColorModel().getRenderedImage();
        }
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            writer.writePNG(pngImage, bos, COMPRESS_RATION, filterType);
        } catch (Exception e) {
        	log.error("ImageUtil formatImage2Png has error:" + ExceptionsUtil.stackTraceToStr(e));
        }finally {
			FileUtil.closeStream(bos);
			ImageUtilities.disposeImage(image);
		}
        return bos.toByteArray();
    }
    
    /**
     * 将图片转化为tIFF格式
     * @param image
     * @return
     */
	public static byte[] formatImage2Tiff(RenderedImage image) {
    	ImageWriter writer = null;
    	ImageOutputStream ioutstream = null;
    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
        	writer = writerSPI.createWriterInstance();
        	ioutstream = ImageIOExt.createImageOutputStream(image,bos);
			writer.setOutput(ioutstream);
			writer.write(image);
			ioutstream.flush();
			return bos.toByteArray();
		} catch (IOException e) {
			log.error("ImageUtil formatImage2Tiff has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_TRANSFOR, "瓦片压缩类型"));
		} finally {
			FileUtil.closeStream(ioutstream);
			writer.dispose();
			ImageUtilities.disposeImage(image);
		}
    }
    
    private static BufferedImage dealSingleBand(int type, BufferedImage...image) {
    	float[][] floatArray = new float[image.length][]; 
    	int index = 0; 
    	for (BufferedImage bufferedImage : image) {
    		DataBuffer dataBuffer = bufferedImage.getRaster().getDataBuffer();
    		int dataType = dataBuffer.getDataType();
    		if (dataType == DataBuffer.TYPE_FLOAT) {
    			DataBufferFloat buf = (DataBufferFloat) dataBuffer;
    			floatArray[index++] = buf.getData();
    		}
		}
    	return combineArrayData(type,image[0].getWidth(),image[0].getHeight(),floatArray);
    }
    
    /**
     * 
     * @param type
     * @param width
     * @param height
     * @param dataArray
     * @return
     */
    private static BufferedImage combineArrayData(int type,int width,int height,float[]...dataArray) {
    	int length = 0,index = 0;
    	for (float[] fs : dataArray) {
			length+=fs.length;
			index++;
		}
    	float[] combineFloat = new float[length];
    	final int combineWidth,combineHeight;
    	if (type == 1) {
    		combineWidth = width*index;
    		combineHeight = height;
    	}else {
    		combineWidth = width;
    		combineHeight = height*index;
    	}
    	for (int i = 0; i < combineFloat.length; i++) {
    		int n,stride,arryNo;
    		if (type == 1) {
    			n = i / combineWidth;
    			stride = i % width;
    			arryNo = i / (width * (n * 2 + 1));
    		}else {
    			n = i / combineWidth;
    			if (n >= height) {
    				n = n - height;
    			}
    			stride = i % combineWidth;
    			arryNo = i / combineWidth / height;
    		}
			combineFloat[i] = dataArray[arryNo][stride+n*width];
			
		}
    	return constructGrayImg(combineFloat, combineWidth, combineHeight);
	}

	private static BufferedImage constructGrayImg(float[] combineFloat, int width, int height) {
		DataBufferFloat dataBuffer = new DataBufferFloat(combineFloat, width*height);
 		ColorSpace space = ColorSpace.getInstance(ColorSpace.CS_GRAY);
     	ComponentSampleModel sampleModle = new ComponentSampleModel(DataBuffer.TYPE_FLOAT,width , height, 1, width, new int[] {0});
     	int[] bits = {32};
     	ComponentColorModel color1 = new ComponentColorModel(space, bits, false, false, Transparency.TRANSLUCENT,DataBuffer.TYPE_FLOAT);
     	WritableRaster raster = Raster.createWritableRaster(sampleModle,dataBuffer,new Point(0,0));
     	BufferedImage image = new BufferedImage(color1,raster,false,null);
     	return image;
	}

    
    /**
     * 合并图片
     * @param image 待合并的图片
     * @param type 1 是横向  2 是纵向
     * @return
     * @throws IOException 
     */
    public static BufferedImage mergeImageByImage(int type,String requestFormat,BufferedImage...image){
    	try {
    		int numBands = image[0].getColorModel().getNumColorComponents();
        	if (numBands == 1) {
        		return dealSingleBand(type,image);
        	} else {
        		return dealMultiBand(type, requestFormat,image);
        	}
		} finally {
			//清除掉原image数据
			for (BufferedImage bufferedImage : image) {
				ImageUtilities.disposeImage(bufferedImage);
			}
		}
    	
    }

	private static BufferedImage dealMultiBand(int type, String requestFormat,BufferedImage... image) {
		int mergeWidth = 0;
    	int mergeHeight = 0;
    	int[][] imageArray = new int[image.length][];
    	int i = 0;
    	for (BufferedImage bufferedImage : image) {
    		int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            imageArray[i] = new int[width * height];
            // 从图片中读取RGB
            imageArray[i] = bufferedImage.getRGB(0, 0, width, height, imageArray[i], 0, width);
            if (type == 1) 
            {
            	mergeHeight = bufferedImage.getHeight();
                mergeWidth += bufferedImage.getWidth();
            } 
            else if (type == 2) 
            { 
            	mergeWidth = bufferedImage.getWidth();
            	mergeHeight += bufferedImage.getHeight();  
            }
            i++;
		}
    	BufferedImage imageNew = null;
    	if (requestFormat.equalsIgnoreCase("png")) {
    		imageNew = createTransparentImg(mergeWidth, mergeHeight);
    	}else{
    		imageNew = createWhiteBgcolorImg(mergeWidth, mergeHeight);
    	} 
        int height_i = 0;  
        int width_i = 0;
        int index = 0;
        for (BufferedImage bufferedImage : image)
        { 
            if (type == 1) 
            {  
            	imageNew.setRGB(width_i, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), imageArray[index], 0,  
                                bufferedImage.getWidth());  
                width_i += bufferedImage.getWidth();  
            } 
            else if (type == 2) 
            {  
            	imageNew.setRGB(0, height_i, bufferedImage.getWidth(), bufferedImage.getHeight(), imageArray[index], 0, bufferedImage.getWidth());  
                height_i += bufferedImage.getHeight();  
            }
            index++;
        }
        return imageNew;
	}
    
    /**
     * 将原始图片压缩成指定宽和高的图片
     * @param image  原始图片
     * @param width  压缩后的图片宽度
     * @param height 压缩后的图片高度
     * @return
     */
    public static BufferedImage compressImage(BufferedImage image,int width,int height) {
    	//新的图片
    	int numBands = image.getColorModel().getNumColorComponents();
    	BufferedImage newImage = null;
    	if (numBands == 1) {
    		ImageWorker iw = new ImageWorker(image);
    		iw.scale(width*1.0 / image.getWidth(), height*1.0 / image.getHeight(), 0d, 0d, Interpolation.getInstance(Interpolation.INTERP_NEAREST));
    		return iw.getBufferedImage();
    	}else {
    		newImage = new BufferedImage(width, height, image.getType());
    		//绘制缩放后的图片
    		newImage.getGraphics().drawImage(image.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);
    		return newImage;
    	}
    }
    
    /**
     * 将图片读取为指定格式的buffer
     * @param image
     * @param format
     * @return
     * @throws IOException
     */
    public static byte[] readImgToBuffer(RenderedImage image,String format) throws IOException {
    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
    	ImageIO.write(image, format, bos);
    	return bos.toByteArray();
    }
    
	 /**
     * 将高程单通道转成rgb的通道
     * @param grayColorImage
     * @return
     */
    public static BufferedImage forceComponet(BufferedImage grayColorImage) {
		ImageWorker imageWorker = new ImageWorker(grayColorImage);
		if (grayColorImage.getWidth() >= 256) {
			imageWorker = imageWorker.rescaleToBytes();
		}
		imageWorker = imageWorker.forceColorSpaceRGB();
		return imageWorker.getBufferedImage();		
    }
    
    
    /**
     * 创建白色背景透明图片
     * @param width 图片宽度
     * @param height 图片高度
     * @return
     */
    public static BufferedImage createTransparentImg(int width,int height) {
    	BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				Color rgb = new Color(0,0,0,1);
				image.setRGB(j, i, rgb.getRGB());
			}
		}
		return image;
    }
    
    
    public static BufferedImage createWhiteBgcolorImg(int width,int height) {
    	BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    	//设置白色背景
		Graphics2D grdphics = (Graphics2D) image.getGraphics();
		grdphics.setColor(new Color(255,255,255));
		grdphics.fillRect(0, 0, width, height);
		return image;
    }
    
    
    static class RasterSymbolizerVisitor extends AbstractStyleVisitor {
        boolean highChangeRasterSymbolizer;
        public void visit(org.geotools.styling.RasterSymbolizer raster) {
            if (raster.getColorMap() == null) {
                highChangeRasterSymbolizer = true;
                return;
            }
            int cmType = raster.getColorMap().getType();
            if (cmType != ColorMap.TYPE_INTERVALS && cmType != ColorMap.TYPE_VALUES) {
                highChangeRasterSymbolizer = true;
            }
        }
    }
    
    
    /**
     * @param styles
     * @return
     */
    private static FilterType getFilterType(Style...styles) {
    	if (styles == null || styles.length == 0) {
    		return FilterType.FILTER_NONE;
    	}
        RasterSymbolizerVisitor visitor = new RasterSymbolizerVisitor();
        for (Style style : styles) {
            if (style != null) {
                style.accept(visitor);
                if (visitor.highChangeRasterSymbolizer) {
                    return FilterType.FILTER_SUB;
                }
            }
        }
        return FilterType.FILTER_NONE;
    }
    
    public static RenderedImage makeTransparent(RenderedImage image,String color) {
    	ImageWorker iw = new ImageWorker(image);
    	iw.makeColorTransparent(new Color(Integer.parseInt(color, 16)));
    	return iw.getBufferedImage();
    }
}
