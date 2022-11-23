package org.pygisserver.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.pygisserver.common.enumm.OgcEpsg;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.ImageWorker;
import org.geotools.referencing.CRS;
import org.geotools.renderer.crs.ProjectionHandler;
import org.geotools.renderer.lite.gridcoverage2d.GridCoverageRendererUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.RenderedOp;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Description: 地理信息工具类
 * @author: 刘洋 
 * @date: 2019-07-18
 */
@Slf4j
public class GeometryUtil {

	public static final double DPI = 25.4 / 0.28;
	
	public static final double DEFAULT_TILE_WIDTH = 256;

	public static final double DEFAULT_TILE_HEIGHT = 256;

	public static final double RADIUS = 6378137.0;
	
	public static final double DEGREE = 360;
	
	public static final double EARTHRADIUS = 6371.393;
	
	public static final double GAUSSIANDEGREE = 2692922.478199121;
	
	public final static double OGC_DEGREE_TO_METERS = RADIUS * 2.0 * Math.PI / 360;
	
	public static final double MERCATOR_UNITS = 20037508.34;
	
	public static final double MERCATOR_UNITS_NEGITAVE = -20037508.34;
	
	public final static double OGC_METERS_TO_DEGREES = 1 / OGC_DEGREE_TO_METERS;
	
	public final static double EE = 0.00669342162296594323;
	
	public static ReferencedEnvelope WHOLE_ENVELOPE_4326 = null;
	
	public static ReferencedEnvelope WHOLE_ENVELOPE_3857 = null;
	
	public final static double PI = Math.PI;

	public final static double R = 6378245.0;
	
	private static final Lock lock = new ReentrantLock();

	public final static List<String> supportSrs = new ArrayList<>();

	public static final CoordinateReferenceSystem SPHERICAL_MERCATOR;

    static {
        try {
            SPHERICAL_MERCATOR = CRS.decode("EPSG:3857", true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    
    public static final CoordinateReferenceSystem SPHERICAL_WGS;

    static {
        try {
        	SPHERICAL_WGS = CRS.decode("EPSG:4326", true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
	static {
		supportSrs.add("EPSG:4326");
		supportSrs.add("EPSG:3857");
	}
	/**
	 * 默认插值法
	 */
	public static final Interpolation interpolation = new InterpolationNearest();
    
    private static Set<Double> epsg4326 = new TreeSet<Double>(new Comparator<Double>() {
		@Override
		public int compare(Double o1, Double o2) {
			return Double.compare(o2, o1);
		}
	});

    /**
     * 按照256*256瓦片大小的分辨率
     */
	static {
		epsg4326.add(0.703125);
		epsg4326.add(0.3515625);
		epsg4326.add(0.17578125);
		epsg4326.add(0.087890625);
		epsg4326.add(0.0439453125);
		epsg4326.add(0.02197265625);
		epsg4326.add(0.010986328125);
		epsg4326.add(0.0054931640625);
		epsg4326.add(0.00274658203125);
		epsg4326.add(0.001373291015625);
		epsg4326.add(6.866455078125E-4);
		epsg4326.add(3.4332275390625E-4);
		epsg4326.add(1.71661376953125E-4);
		epsg4326.add(8.58306884765625E-5);
		epsg4326.add(4.291534423828125E-5);
		epsg4326.add(2.1457672119140625E-5);
		epsg4326.add(1.0728836059570312E-5);
		epsg4326.add(5.364418029785156E-6);
		epsg4326.add(2.682209014892578E-6);
		epsg4326.add(1.341104507446289E-6);
		epsg4326.add(6.705522537231445E-7);
		epsg4326.add(3.3527612686157227E-7);
		try {
			WHOLE_ENVELOPE_4326 = new ReferencedEnvelope(-180,180,-90,90,CRS.decode("EPSG:4326",true));
		} catch (MismatchedDimensionException | FactoryException e) {
			e.printStackTrace();
		}
	}
	  private static Set<Double> epsg3857 = new TreeSet<Double>(new Comparator<Double>() {
			@Override
			public int compare(Double o1, Double o2) {
				return Double.compare(o2, o1);
			}
		});

		static {
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 1f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 2f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 4f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 8f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 16f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 32f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 64f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 128f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 256f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 512f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 1024f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 2048f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 4096f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 8192f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 16384f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 32768f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 65536f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 131072f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 262144f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 524288f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 1048576f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 2097152f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 4194304f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 8388608f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 16777216f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 33554432f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 67108864f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 134217728f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 268435456f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 536870912f));
			epsg3857.add(MERCATOR_UNITS * 2 / (256 * 1073741824f));
			try {
				WHOLE_ENVELOPE_3857 = new ReferencedEnvelope(MERCATOR_UNITS_NEGITAVE,MERCATOR_UNITS,MERCATOR_UNITS_NEGITAVE,MERCATOR_UNITS,CRS.decode("EPSG:3857",true));
			} catch (MismatchedDimensionException | FactoryException e) {
				e.printStackTrace();
			}
		}
	    
	
	/**
	 * 计算当前级别的比例尺
	 * @param level 当前级别
	 * @param srs 投影信息
	 * @param tileWidth
	 * @return
	 */
	public static double calculateScale(int level,String srs,boolean isMinLevel) {
		//计算分辨率，即就是一个像素有多少米
		if (StringUtils.isBlank(srs)) {
			srs = "EPSG:4326";
		}
		if (isMinLevel) {
			level = level - 1;
		}
		//当前投影转为米的参数
		double resolution = 0;
		double scale = 0;
		if ("EPSG:4326".equalsIgnoreCase(srs)) {
			resolution = DEGREE / (Math.pow(2, level) * DEFAULT_TILE_WIDTH);
		}else if("EPSG:900913".equalsIgnoreCase(srs) || "EPSG:3857".equalsIgnoreCase(srs)) {
			resolution = DEGREE / (Math.pow(2, level) * DEFAULT_TILE_WIDTH);
		}
		//其中39.37是1/0.0254(1英寸等于0.0254米)
		scale = resolution * DPI * 39.37 * OGC_DEGREE_TO_METERS;
//		return Math.ceil(scale);
		return scale;
	}

	/**
	 * 计算当前级别的比例尺
	 * @param level 当前级别
	 * @param srs 投影信息
	 * @param tileWidth
	 * @return
	 */
	public static double calculateScale(int level,String srs,boolean isMinLevel, List<String> tilecount) {
		//计算分辨率，即就是一个像素有多少米
		if (StringUtils.isBlank(srs)) {
			srs = "EPSG:4326";
		}
		if (isMinLevel) {
			level = level - 1;
		}
		//当前投影转为米的参数
		double resolution = 0;
		double scale = 0;
		if ("EPSG:4326".equalsIgnoreCase(srs)) {
			resolution = DEGREE / (Math.pow(2, level + 1) * DEFAULT_TILE_WIDTH);
		}else if("EPSG:900913".equalsIgnoreCase(srs) || "EPSG:3857".equalsIgnoreCase(srs)) {
			resolution = DEGREE / (Math.pow(2, level) * DEFAULT_TILE_WIDTH);
		}
		if(tilecount != null && !tilecount.isEmpty()){
			String[] split = tilecount.get(level+1).split("\\*");
			resolution = GAUSSIANDEGREE / (Integer.parseInt(split[1]) * DEFAULT_TILE_WIDTH);
			scale = resolution * DPI * 39.37 ;
		}else {
			//其中39.37是1/0.0254(1英寸等于0.0254米)
			scale = resolution * DPI * 39.37 * OGC_DEGREE_TO_METERS;
		}
//		return Math.ceil(scale);
		return scale;
	}
	
	public static double calculateScale(double resolution) {
		return resolution * DPI * 39.37 * OGC_DEGREE_TO_METERS;
	}
	
	
	public static BoundingBox transformCoordinateSystem(BoundingBox sourceEnvelope,String targetSrs) {
		try {
			CoordinateReferenceSystem sourceCoordinate = sourceEnvelope.getCoordinateReferenceSystem();
			if (sourceCoordinate == null) {
			    return sourceEnvelope;
		    }
			ReferencedEnvelope envelope = null;
			double maxY = 0.0;
			double minY = 0.0;
			if (OgcEpsg.getOgcEpsgCodeType(targetSrs) == OgcEpsg.MERCATOR_CODE) {
				//如果纬度大于85则需要处理
				if (sourceEnvelope.getMaximum(1) > mercatorY2lat(MERCATOR_UNITS)) {
					maxY = mercatorY2lat(MERCATOR_UNITS);
				}else {
					maxY = sourceEnvelope.getMaximum(1);
				}
				if (sourceEnvelope.getMinimum(1) < mercatorY2lat(MERCATOR_UNITS_NEGITAVE)) {
					minY = mercatorY2lat(MERCATOR_UNITS_NEGITAVE);
				}else {
					minY = sourceEnvelope.getMinimum(1);
				}
				envelope = new ReferencedEnvelope(sourceEnvelope.getMinimum(0),sourceEnvelope.getMaximum(0),
						minY,maxY,sourceEnvelope.getCoordinateReferenceSystem());
			} else {
				envelope = new ReferencedEnvelope(sourceEnvelope);
			}
			CoordinateReferenceSystem targetCoordinate = CRS.decode(targetSrs,true);
			MathTransform transform = CRS.findMathTransform(sourceCoordinate, targetCoordinate, true);
			Envelope result = JTS.transform(envelope, transform);
			return new ReferencedEnvelope(result, targetCoordinate);
		} catch (FactoryException | TransformException e) {
			log.error("GeometryUtil transformCoordinateSystem has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_TRANSFOR, "坐标系"));
		} 
		
	}
	
	/**
	 * 
	 * @param envelope
	 * @param epsg  目标投影
	 * @return
	 * @throws FactoryException 
	 * @throws TransformException 
	 * @throws NoSuchAuthorityCodeException 
	 */
	public static ReferencedEnvelope transformCoordinateSystem(ReferencedEnvelope sourceEnvelope,String targetSrs) {
		try {
			CoordinateReferenceSystem sourceCoordinate = sourceEnvelope.getCoordinateReferenceSystem();
			if (sourceCoordinate == null) {
			    return sourceEnvelope;
		    }
			ReferencedEnvelope envelope = null;
			double maxY = 0.0;
			double minY = 0.0;
			if (OgcEpsg.getOgcEpsgCodeType(targetSrs) == OgcEpsg.MERCATOR_CODE) {
				//赋值为3857投影
				targetSrs = "EPSG:3857";
				//如果纬度大于85则需要处理
				if (sourceEnvelope.getMaximum(1) > mercatorY2lat(MERCATOR_UNITS)) {
					maxY = mercatorY2lat(MERCATOR_UNITS);
				}else {
					maxY = sourceEnvelope.getMaximum(1);
				}
				if (sourceEnvelope.getMinimum(1) < mercatorY2lat(MERCATOR_UNITS_NEGITAVE)) {
					minY = mercatorY2lat(MERCATOR_UNITS_NEGITAVE);
				}else {
					minY = sourceEnvelope.getMinimum(1);
				}
				envelope = new ReferencedEnvelope(sourceEnvelope.getMinimum(0),sourceEnvelope.getMaximum(0),
						minY,maxY,sourceEnvelope.getCoordinateReferenceSystem());
			} else {
				envelope = new ReferencedEnvelope(sourceEnvelope);
			}
			CoordinateReferenceSystem targetCoordinate = CRS.decode(targetSrs,true);
			return envelope.transform(targetCoordinate, true);
		} catch (FactoryException | TransformException e) {
			log.error("GeometryUtil transformCoordinateSystem has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_TRANSFOR, "坐标系"));
		} 
		
	}
	

	/**
	 * 获取当前在4326，且瓦片是256*256的大小时的等级
	 * @param targetNum
	 * @return
	 */
	public static int get4326CurrentLevel(double targetNum,int tileWidth) {
		if (tileWidth <= 0) {
			tileWidth = 256;
		}
		Map<Double, Integer> map = new TreeMap<Double, Integer>();
		int index = 0;
		try {
			lock.lock();
			epsg4326.add(targetNum);
			for (Double resolution : epsg4326) {
				map.put(resolution, index);
				index++;
			}
			try {
				index = map.get(targetNum);
			} catch (Exception e) {
				log.error(e.getLocalizedMessage());
			}
			if (epsg4326.size() > 22) {
				Iterator<Double> it = epsg4326.iterator();
				while(it.hasNext()) {
					Double target = it.next();
					if(targetNum == target) {
						it.remove();
					}
				}
			}
		} finally {
			lock.unlock();
		}
		int ration = 0;
		if (tileWidth > 256) {
			ration = tileWidth / 256;
			index = index - (ration/2);
		}else {
			ration = 256 / tileWidth;
			index = index + (ration/2);
		}
		return index;
	}
	
	/**
	 * 墨卡托投影下的级别计算
	 * @param targetNum
	 * @return
	 */
	public static int get3857CurrentLevel(double targetNum,int tileWidth) {
		Map<Double, Integer> map = new TreeMap<Double, Integer>();
		epsg3857.add(targetNum);
		int index = 0;
		try {
			lock.lock();
			for (Double resolution : epsg3857) {
				map.put(resolution, index);
				index++;
			}
			index = map.get(targetNum);
			if (epsg3857.size() > 31) {
				Iterator<Double> it = epsg3857.iterator();
				while(it.hasNext()) {
					Double target = it.next();
					if(targetNum == target) {
						it.remove();
					}
				}
			} 
		}finally {
			lock.unlock();
		}
		int ration = 0;
		if (tileWidth > 256) {
			ration = tileWidth / 256;
			index = index - (ration/2);
		}else {
			ration = 256 / tileWidth;
			index = index + (ration/2);
		}
		return index;
	}
	
	/**
	 * 墨卡托坐标转化为经纬度
	 * @return 
	 */
	public static Coordinate mercator2Lonlat(double mercatorX,double mercatorY) {
		double x = mercatorX / MERCATOR_UNITS * 180;
		double y = mercatorY / MERCATOR_UNITS * 180;
		y = 180 / Math.PI * (2 * Math.atan(Math.exp(y * Math.PI / 180)) - Math.PI / 2);
		Coordinate coord = new Coordinate(x, y);
		return coord;
	}
	
	/**
	 * 墨卡托的y值转化为纬度
	 * @return 
	 */
	public static double mercatorY2lat(double mercatorY) {
		double y = mercatorY / MERCATOR_UNITS * 180;
		y = 180 / Math.PI * (2 * Math.atan(Math.exp(y * Math.PI / 180)) - Math.PI / 2);
		return y;
	}
	
	
	public static Coordinate lonLat2Mercator(double lon,double lat) {
		if (lat > 85.0511287798066) {
			lat = 85.0511287798066;
		}
		if (lat < -85.0511287798066) {
			lat = -85.0511287798066;
		}
		double x = lon * MERCATOR_UNITS / 180;
		double y = Math.log(Math.tan((90+lat) * Math.PI / 360 )) / (Math.PI / 180);
		y = y * MERCATOR_UNITS / 180;
		y = CommonUtil.format2Decimal(y, 2);
		Coordinate coord = new Coordinate(x, y);
		return coord;
	}
	
	
	
	
	/**
	 * 等经纬值转化为墨卡托
	 * @return 
	 */
	public static double lat2MercatorY(double lat) {
		if (lat > 85.0511287798066) {
			lat = 85.0511287798066;
		}
		if (lat < -85.05112877980659) {
			lat = -85.05112877980659;
		}
		double y = Math.log(Math.tan((90+lat) * Math.PI / 360 )) / (Math.PI / 180);
		y = y * MERCATOR_UNITS / 180;
		//保留两位小数
		return CommonUtil.format2Decimal(y, 2);
	}
	
	/**
	 * 将3857的请求行列号转化为4326所需要的行列号
	 * @param level
	 * @param row
	 * @param col
	 * @return
	 */
	public static int[] convert3857To4326Tiled(int level,int row,int col) {
		int[] tileRange = new int[5];
		/**
		 * 由于x方向上是均匀变化的，因此
		 * 最小为 2*col  最大为2*col + 1
		 * 然后根据请求的y,z计算出当前mercator下的y的变化值
		 * 然后根据mercator与经纬度的转化，计算出当前纬度的变化范围
		 */
		tileRange[0] = 2*col;
		tileRange[2] = 2*col + 1;
		//计算
		double minMercatorY = GeometryUtil.MERCATOR_UNITS - (GeometryUtil.MERCATOR_UNITS / Math.pow(2, level - 1) * (row + 1));
		double maxMercatorY = GeometryUtil.MERCATOR_UNITS - (GeometryUtil.MERCATOR_UNITS / Math.pow(2, level - 1) * row);
		double minLat = GeometryUtil.mercatorY2lat(minMercatorY);
		double maxLat = GeometryUtil.mercatorY2lat(maxMercatorY);
		tileRange[1] = (int) Math.floor((90 - maxLat) / (180 / Math.pow(2, level)));
		tileRange[3] = (int) Math.floor((90 - minLat) / (180 / Math.pow(2, level)));
		tileRange[4] = level;
		return tileRange;
	}
	
	/**
	 * 根bbox计算当前
	 * @param envelope
	 * @return
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 */
	public static int[] calcuteTileRange(ReferencedEnvelope envelope,int currentLevel) {
		//计算左上角和右下角的火星坐标
		Coordinate upperLeft = GeometryUtil.gps2Gcj02(envelope.getMinX(), envelope.getMaxY());
		Coordinate lowerRight = GeometryUtil.gps2Gcj02(envelope.getMaxX(), envelope.getMinY());
		ReferencedEnvelope gcjEnvelope = new ReferencedEnvelope(upperLeft.getX(), lowerRight.getX(), lowerRight.getY(), upperLeft.getY(), envelope.getCoordinateReferenceSystem());
		return convert4326ToTiledRange(currentLevel,gcjEnvelope);
	}
	
	/**
	 * 将4326的请求行列号转化为3857所需要的行列号
	 * @param level 级别
	 * @param row
	 * @param col
	 * @return
	 */
	public static int[] convert4326To3857Tiled(int level,int row,int col) {
		int[] tileRange = new int[5];
		//列方向不变
		tileRange[0] = col / 2;
		tileRange[2] = col / 2;
		//将4326的行列号转化为3857的bbox
		double minLat = 90 - (180 / Math.pow(2, level) * (row + 1));
		double maxLat = 90 - (180 / Math.pow(2, level) * row);
		double minMercatorY = lat2MercatorY(minLat);
		double maxMercatorY = lat2MercatorY(maxLat);
		tileRange[1] = (int) Math.floor((MERCATOR_UNITS - maxMercatorY) / (GeometryUtil.MERCATOR_UNITS / Math.pow(2, level - 1)));
		tileRange[3] = (int) Math.floor((MERCATOR_UNITS - minMercatorY) / (GeometryUtil.MERCATOR_UNITS / Math.pow(2, level - 1)));
		if (tileRange[3] >= Math.pow(2, level)) {
			tileRange[3] = (int) (Math.pow(2, level) - 1);
		}
		tileRange[4] = level;
		return tileRange;
	}
	
	public static int[] convert4326To3857Tiled(Coordinate upperLeft,
											   Coordinate lowerRight,
											   int level) {
		int[] tileRange = new int[5];
		Coordinate leftUpper = lonLat2Mercator(upperLeft.getX(),upperLeft.getY());
		Coordinate rightLower = lonLat2Mercator(lowerRight.getX(),lowerRight.getY());
		tileRange[0] = (int) Math.floor((leftUpper.getX() - MERCATOR_UNITS_NEGITAVE) / (GeometryUtil.MERCATOR_UNITS / Math.pow(2, level - 1)));
		tileRange[2] = (int) Math.floor((rightLower.getX() - MERCATOR_UNITS_NEGITAVE) / (GeometryUtil.MERCATOR_UNITS / Math.pow(2, level - 1)));
		tileRange[1] = (int) Math.floor((MERCATOR_UNITS - leftUpper.getY()) / (GeometryUtil.MERCATOR_UNITS / Math.pow(2, level - 1)));
		tileRange[3] = (int) Math.floor((MERCATOR_UNITS - rightLower.getY()) / (GeometryUtil.MERCATOR_UNITS / Math.pow(2, level - 1)));
		if (tileRange[3] >= Math.pow(2, level)) {
			tileRange[3] = (int) (Math.pow(2, level) - 1);
		}
		tileRange[4] = level;
		return tileRange;
	}
	
	
	/**
	 * 计算当前求的的3857对应的纬度的最大、最小值
	 * @param level
	 * @param row
	 * @param col
	 * @return
	 * @throws FactoryException 
	 * @throws NoSuchAuthorityCodeException 
	 * @throws MismatchedDimensionException 
	 */
	public static ReferencedEnvelope calcute3857To4326LonLatBbox(int level,int row,int col) {
		try {
			double minMercatorY = GeometryUtil.MERCATOR_UNITS - (GeometryUtil.MERCATOR_UNITS / Math.pow(2, level - 1) * (row + 1));  
			double maxMercatorY = GeometryUtil.MERCATOR_UNITS - (GeometryUtil.MERCATOR_UNITS / Math.pow(2, level - 1) * row); 
			double minMercatorX = GeometryUtil.MERCATOR_UNITS_NEGITAVE + (GeometryUtil.MERCATOR_UNITS / Math.pow(2, level - 1) * col);  
			double maxMercatorX = GeometryUtil.MERCATOR_UNITS_NEGITAVE + (GeometryUtil.MERCATOR_UNITS / Math.pow(2, level - 1) * (col + 1)); 
			Coordinate upperLeft = mercator2Lonlat(minMercatorX,maxMercatorY);
			Coordinate lowerRight = mercator2Lonlat(maxMercatorX,minMercatorY);
			return new ReferencedEnvelope(upperLeft.getX(), lowerRight.getX(), lowerRight.getY(), upperLeft.getY(), CRS.decode("EPSG:4326",true));
		} catch (MismatchedDimensionException | FactoryException e) {
			log.error("GeometryUtil calcute3857To4326LonLatBbox has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_TRANSFOR, "坐标系"));
		}
	}
	
	
	/**
	 * 将4326的请求行列号转化为3857投影
	 * @param level
	 * @param row
	 * @param col
	 * @return
	 * @throws FactoryException 
	 * @throws NoSuchAuthorityCodeException 
	 * @throws MismatchedDimensionException 
	 */
	public static ReferencedEnvelope calcute4326To3857Mercator(int level,int row,int col,String reqSrs,boolean isTransform) {
		try {
			double minLon = -180 + (180 / Math.pow(2, level) * col);  
			double maxLon = -180 + (180 / Math.pow(2, level) * (col + 1));  
			double minLat = 90 - (90 / Math.pow(2, level - 1) * (row + 1));
			double maxLat = 90 - (90 / Math.pow(2, level - 1) * row);
			//如果是onlien数据，则需要朱换为火星坐标再计算
			if (isTransform) {
				Coordinate upperLeft = gps2Gcj02(minLon, maxLat);
				Coordinate lowerRight = gps2Gcj02(maxLon, minLat);
				minLon = upperLeft.getX();
				maxLat = upperLeft.getY();
				minLat = lowerRight.getY();
				maxLon = lowerRight.getX();
			}
			Coordinate upperLeft = lonLat2Mercator(minLon,maxLat);
			Coordinate lowerRight = lonLat2Mercator(maxLon,minLat);
			return new ReferencedEnvelope(upperLeft.getX(), lowerRight.getX(), lowerRight.getY(), upperLeft.getY(), CRS.decode(reqSrs,true));
		} catch (MismatchedDimensionException | FactoryException e) {
			log.error("GeometryUtil calcute4326To3857Mercator has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_TRANSFOR, "坐标系"));
		}
	}
	
	/**
	 * 计算当前求的的3857对应的bbox
	 * @param level
	 * @param row
	 * @param col
	 * @return
	 * @throws FactoryException 
	 * @throws NoSuchAuthorityCodeException 
	 * @throws MismatchedDimensionException 
	 */
	public static ReferencedEnvelope calcute3857bbox(int level, int row, int col, String srs) {
		try {
			double minMercatorY = GeometryUtil.MERCATOR_UNITS - (GeometryUtil.MERCATOR_UNITS / Math.pow(2, level - 1) * (row + 1));  
			double maxMercatorY = GeometryUtil.MERCATOR_UNITS - (GeometryUtil.MERCATOR_UNITS / Math.pow(2, level - 1) * row); 
			double minMercatorX = GeometryUtil.MERCATOR_UNITS_NEGITAVE + (GeometryUtil.MERCATOR_UNITS / Math.pow(2, level - 1) * col);  
			double maxMercatorX = GeometryUtil.MERCATOR_UNITS_NEGITAVE + (GeometryUtil.MERCATOR_UNITS / Math.pow(2, level - 1) * (col + 1)); 
			return new ReferencedEnvelope(minMercatorX, maxMercatorX, minMercatorY, maxMercatorY, CRS.decode(srs,true));
		} catch (MismatchedDimensionException | FactoryException e) {
			log.error("GeometryUtil calcute3857To4326LonLatBbox has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, "坐标系"));
		}
	}
	
	
	/**
	 * 计算当前求的的4326bbox
	 * @param level
	 * @param row
	 * @param col
	 * @return
	 * @throws FactoryException 
	 * @throws NoSuchAuthorityCodeException 
	 * @throws MismatchedDimensionException 
	 */
	public static ReferencedEnvelope calcute4326bbox(int level, int row, int col) {
		try {
			double minLon = -180 + (360 / Math.pow(2, level + 1)) * col;
			double maxLon = -180 + (360 / Math.pow(2, level + 1)) * (col + 1);
			double minLat = 90 - (180 / Math.pow(2, level)) * (row + 1);
			double maxLat = 90 - (180 / Math.pow(2, level)) * row;
			return new ReferencedEnvelope(minLon, maxLon, minLat, maxLat, CRS.decode("EPSG:4326",true));
		} catch (MismatchedDimensionException | FactoryException e) {
			log.error("GeometryUtil calcute3857To4326LonLatBbox has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, "坐标系"));
		}
	}

	/**
	 * 计算当前求的的guassCoordinate的bbox
	 *
	 * @param level 请求级别
	 * @param row row
	 * @param col col
	 * @param srs 坐标系
	 * @param guassCoordinatePara 坐标系参数
	 * @return ReferencedEnvelope
	 */
	public static ReferencedEnvelope calcuteGuassBbox(int level, int row, int col, String srs, Map<String, List<String>> guassCoordinatePara) {
		String[] tileCounts = ((List) guassCoordinatePara.get("tilecount")).get(level).toString().split("\\*");
		List guassCoordinateBbox = guassCoordinatePara.get("bbox");
		double minX = Double.parseDouble(guassCoordinateBbox.get(0).toString());
		double minY = Double.parseDouble(guassCoordinateBbox.get(1).toString());
		double maxX = Double.parseDouble(guassCoordinateBbox.get(2).toString());
		double maxY = Double.parseDouble(guassCoordinateBbox.get(3).toString());
		double perX = (maxX - minX) / Double.parseDouble(tileCounts[0]);
		double perY = (maxY - minY) / Double.parseDouble(tileCounts[1]);
		try {
			double minLon = minX + perX * col;
			double maxLon = minX + perX * (col + 1);
			double minLat = maxY - perY * (row + 1);
			double maxLat = maxY - perY * row;
			return new ReferencedEnvelope(minLon, maxLon, minLat, maxLat, CRS.decode(srs,true));
		} catch (MismatchedDimensionException | FactoryException e) {
			log.error("GeometryUtil calcuteGuassBbox has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, "坐标系"));
		}
	}
	
	/**
	 * 
	 * @param image 需要投影的图片
	 * @param destSrs 目标投影
	 * @param resourceEnvelope 原数据的bbox
	 * @param interpolation  插值算法
	 * @param destinationEnvelope 目标bbox
	 * @param layerName 图层名称
	 * @return
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 */
	public static BufferedImage reprojectImage(BufferedImage image,
												String destSrs,
												ReferencedEnvelope resourceEnvelope,
												Interpolation interpolation,
												GeneralEnvelope destinationEnvelope,
												String layerName) {
		try {
			GridCoverageFactory gridCoverageFactory = CoverageFactoryFinder.getGridCoverageFactory(null);
			GridCoverage2D gridCoverage2D = gridCoverageFactory.create(layerName, image, resourceEnvelope);
			GridCoverageRendererUtilities gridCoverageRenderUtil = new GridCoverageRendererUtilities();
			gridCoverage2D = gridCoverageRenderUtil.reproject(gridCoverage2D, CRS.decode(destSrs,true), interpolation, destinationEnvelope, null, gridCoverageFactory, null);
			RenderedOp renderedOp = (RenderedOp) gridCoverage2D.getRenderedImage();
			ImageWorker iw = new ImageWorker(renderedOp);
			return iw.getBufferedImage();
		} catch (FactoryException e) {
			log.error("GeometryUtil calcute3857To4326LonLatBbox has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_TRANSFOR, "瓦片数据投影"));
		}
	}
	
	/**
	 * 
	 * @param image 需要处理的图片
	 * @param reqSrs 请求的投影
	 * @param resSrs 原始数据投影
	 * @return
	 * @throws FactoryException 
	 * @throws NoSuchAuthorityCodeException 
	 * @throws TransformException 
	 */
	public static RenderedImage disPlaceCoverage(List<GridCoverage2D> coverages,
												 GridCoverageFactory gridCoverageFactory,
												 ProjectionHandler handler,
												 CoordinateReferenceSystem reqSrs,
												 CoordinateReferenceSystem resSrs,
												 AffineTransform finalWorldToGrid,
												 ReferencedEnvelope reqEnvelope) {
		try {
			GeneralEnvelope destinationEnvelope = new GeneralEnvelope(reqEnvelope);
			List<GridCoverage2D> displaceCoverages = GridCoverageRendererUtilities.displace(coverages, handler, destinationEnvelope, resSrs, reqSrs, gridCoverageFactory);
			List<GridCoverage2D> symbolizedCoverages = new ArrayList<GridCoverage2D>();
	        for (GridCoverage2D displaced : displaceCoverages) {
	        	GridCoverage2D finalCoverage2d = GridCoverageRendererUtilities.affine(displaced, interpolation, finalWorldToGrid, null, false, gridCoverageFactory, null);
	        	symbolizedCoverages.add(finalCoverage2d);
	        }
			GridCoverage2D coverage2d = GridCoverageRendererUtilities.mosaicSorted(symbolizedCoverages, destinationEnvelope, null, null);
			if (coverage2d == null) {
				return coverages.get(0).getRenderedImage();
			}
			coverage2d = GridCoverageRendererUtilities.crop(coverage2d, destinationEnvelope, false, null, null);
			return coverage2d.getRenderedImage();
		} catch (FactoryException | TransformException e) {
			log.error("GeometryUtil calcute3857To4326LonLatBbox has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_TRANSFOR, "坐标系"));
		}
	}

	/**
	 * 将4326的bbox转化为对应的行列号
	 *
	 * @param bbox 请求bbox
	 * @param currentlevel 请求级别
	 * @return int[]
	 */
	public static int[] convert4326ToTiledRange(int currentlevel, BoundingBox bbox) {
		if (currentlevel < 0) {
			currentlevel = 0;
		}
		int minX = (int) Math.floor((bbox.getMinX() - (-180)) / 360 * Math.pow(2, currentlevel + 1));
        if (minX < 0) {
            minX = 0;
        }
		double x = (bbox.getMaxX() - (-180)) / 360 * Math.pow(2, currentlevel + 1);
		//判断x方向是否为瓦片边界值，若为边界值则-1处理
		int maxX = x % 1 == 0.0 && x != 0.0 ? (int) Math.floor(x) - 1 : (int) Math.floor(x);
		if (maxX >= Math.pow(2, currentlevel + 1)) {
			maxX = maxX - 1;
		}
		int miny = (int) Math.floor((90 - bbox.getMaxY()) / 180 * Math.pow(2, currentlevel));
        if (miny < 0) {
            miny = 0;
        }
		double y = (90 - bbox.getMinY()) / 180 * Math.pow(2, currentlevel);
		//判断y方向是否为瓦片边界值，若为边界值则-1处理
		int maxy = y % 1 == 0.0 && y != 0.0 ? (int) Math.floor(y) - 1 : (int) Math.floor(y);
		if (maxy >= Math.pow(2, currentlevel)) {
			maxy = maxy -1;
		}
		int[] tileRange = new int[5];
		tileRange[0] = minX;
		tileRange[1] = miny;
		tileRange[2] = maxX;
		tileRange[3] = maxy;
		tileRange[4] = currentlevel;
		return tileRange;
	}

	/**
	 * 将3857的bbox转化为对应的行列号
	 *
	 * @param bbox 请求bbox
	 * @param currentlevel 请求级别
	 * @return int[]
	 */
	public static int[] convert3857ToTiledRange(int currentlevel, BoundingBox bbox) {
		if (currentlevel < 0) {
			currentlevel = 0;
		}
		int minX = (int) Math.floor((CommonUtil.format2Decimal(bbox.getMinX(), 2) - MERCATOR_UNITS_NEGITAVE) / (MERCATOR_UNITS * 2) * Math.pow(2, currentlevel));
		if (minX < 0) {
			minX = 0;
		}
		double x = (CommonUtil.format2Decimal(bbox.getMaxX(), 2) - MERCATOR_UNITS_NEGITAVE) / (MERCATOR_UNITS * 2) * Math.pow(2, currentlevel);
		int maxX = x % 1 == 0.0 && x != 0.0 ? (int) Math.floor(x) - 1 : (int) Math.floor(x);
		if (maxX == Math.pow(2, currentlevel)) {
			maxX = maxX - 1;
		}
		int miny = (int) Math.floor((MERCATOR_UNITS - CommonUtil.format2Decimal(bbox.getMaxY(), 2)) / (MERCATOR_UNITS * 2) * Math.pow(2, currentlevel));
		if (miny < 0) {
			miny = 0;
		}
		double y = (MERCATOR_UNITS - CommonUtil.format2Decimal(bbox.getMinY(), 2)) / (MERCATOR_UNITS * 2) * Math.pow(2, currentlevel);
		int maxy = y % 1 == 0.0 && y != 0.0 ? (int) Math.floor(y) - 1 : (int) Math.floor(y);
		if (maxy == Math.pow(2, currentlevel)) {
			maxy = maxy - 1;
		}
		int[] tileRange = new int[5];
		tileRange[0] = minX;
		tileRange[1] = miny;
		tileRange[2] = maxX;
		tileRange[3] = maxy;
		tileRange[4] = currentlevel;
		return tileRange;
	}

	/**
	 * 将guass投影坐标的bbox转化为对应的行列号
	 *
	 * @param levelTileNum 请求级别的切图行列数
	 * @param guassCoordinateBbox 高斯坐标的Bbox---顺序为:最小X,最小Y,最大X,最大Y
	 * @param resourceBbox 请求bbox
	 * @param currentlevel 请求级别
	 * @return int[]
	 */
	public static int[] convertGuassToTiledRange(String levelTileNum, List<String> guassCoordinateBbox, BoundingBox resourceBbox, int currentlevel) {
		if (currentlevel < 0) {
			currentlevel = 0;
		}
		String[] split = levelTileNum.split("\\*");
		double bBoxMinX = Double.parseDouble(guassCoordinateBbox.get(0));
		double bBoxMinY = Double.parseDouble(guassCoordinateBbox.get(1));
		double bBoxMaxX = Double.parseDouble(guassCoordinateBbox.get(2));
		double bBoxMaxY = Double.parseDouble(guassCoordinateBbox.get(3));
		double perX = (bBoxMaxX - bBoxMinX) / Double.parseDouble(split[0]);
		double perY = (bBoxMaxY - bBoxMinY) / Double.parseDouble(split[1]);

		int minX = (int) Math.floor((resourceBbox.getMinX() - bBoxMinX) / perX);
		int maxX = (int) Math.floor((resourceBbox.getMaxX() - bBoxMinX) / perX);
		int minY = (int) Math.floor((bBoxMaxY - resourceBbox.getMaxY() ) / perY);
		int maxY = (int) Math.floor((bBoxMaxY - resourceBbox.getMinY() ) / perY);

		int[] tileRange = new int[5];
		tileRange[0] = minX;
		tileRange[1] = minY;
		tileRange[2] = maxX;
		tileRange[3] = maxY;
		tileRange[4] = currentlevel;
		return tileRange;
	}
	 
	/**
	 * wgs84转火星坐标系
	 * @param lat
	 * @param lon
	 * @return
	 */
    public static Coordinate gps2Gcj02(double lon,double lat) {
        if (outOfChina(lat, lon)) {
            return new Coordinate(lon,lat);
        }
        double dLat = transformLat(lon - 105.0, lat - 35.0);
        double dLon = transformLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * PI;
        double magic = Math.sin(radLat);
        magic = 1 - EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((R * (1 - EE)) / (magic * sqrtMagic) * PI);
        dLon = (dLon * 180.0) / (R / sqrtMagic * Math.cos(radLat) * PI);
        double mgLat = lat + dLat;
        double mgLon = lon + dLon;
        return new Coordinate(mgLon,mgLat);
    }
    
    /**
     * 火星坐标系转wgs84坐标
     * @param lat
     * @param lon
     * @return
     */
    public static Coordinate gcj2Gps84(double lon, double lat) {
    	Coordinate gps = transform(lon, lat);
        double lontitude = lon * 2 - gps.getX();
        double latitude = lat * 2 - gps.getY();
        return new Coordinate(latitude, lontitude);
    }

    /**
     * 火星坐标转百度坐标
     * @param gg_lat
     * @param gg_lon
     * @return
     */
    public static Coordinate gcj2Bd09(double lon, double lat) {
        double x = lon, y = lat;
        double z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * PI);
        double theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * PI);
        double bd_lon = z * Math.cos(theta) + 0.0065;
        double bd_lat = z * Math.sin(theta) + 0.006;
        return new Coordinate(bd_lon, bd_lat);
    }

    /**
     * 百度坐标转火星坐标
     * @param bd_lat
     * @param bd_lon
     * @return
     */
    public static Coordinate bd2Gcj02(double bd_lat, double bd_lon) {
        double x = bd_lon - 0.0065, y = bd_lat - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * PI);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * PI);
        double gg_lon = z * Math.cos(theta);
        double gg_lat = z * Math.sin(theta);
        return new Coordinate(gg_lon,gg_lat);
    }

    /**
     * 百度坐标转wgs84
     * @param bd_lat
     * @param bd_lon
     * @return
     */
    public static Coordinate bd092Gps84(double bd_lat, double bd_lon) {

    	Coordinate gcj02 = bd2Gcj02(bd_lat, bd_lon);
    	Coordinate map84 = gcj2Gps84(gcj02.getX(),gcj02.getY());
        return map84;
    }

    public static boolean outOfChina(double lat, double lon) {
        if (lon < 72.004 || lon > 137.8347)
            return true;
        if (lat < 0.8293 || lat > 55.8271)
            return true;
        return false;
    }

    public static Coordinate transform(double lat, double lon) {
        if (outOfChina(lon, lat)) {
            return new Coordinate(lon, lat);
        }
        double dLat = transformLat(lon - 105.0, lat - 35.0);
        double dLon = transformLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * PI;
        double magic = Math.sin(radLat);
        magic = 1 - EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((R * (1 - EE)) / (magic * sqrtMagic) * PI);
        dLon = (dLon * 180.0) / (R / sqrtMagic * Math.cos(radLat) * PI);
        double mgLat = lat + dLat;
        double mgLon = lon + dLon;
        return new Coordinate(mgLon,mgLat);
    }

    public static double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y
                + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * PI) + 320 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    public static double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1
                * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0
                * PI)) * 2.0 / 3.0;
        return ret;
    }
	
    public static double getInstance(double startLon,double startLat,double endLon,double endLat) {
		//转弧度
		double lat1 = convertLonOrLatToRad(startLat);
		double lat2 = convertLonOrLatToRad(endLat);
		double a = lat1 -lat2;
		double b = convertLonOrLatToRad(startLon - endLon);
		double distance = 2*RADIUS *Math.asin(Math.sqrt(Math.sin(a/2.0)*Math.sin(a/2.0)+Math.cos(lat1)*Math.cos(lat2)*Math.sin(b/2.0)*Math.sin(b/2.0)));
		return distance;
	}
    
    
    /**
	 * 经纬度转化为弧度
	 * @param lonOrLat
	 * @return
	 */
	public static double convertLonOrLatToRad(double lonOrLat) {
		return (Math.PI / 180) * lonOrLat; 
	}
	
	/**
	 * 弧度转经纬度
	 * @param rad
	 * @return
	 */
	public static double convertRadToLon(double rad) {
		return (180 / Math.PI) * rad;
	}
	
	
	/**
	 * 根据起点和终点计算偏向角（正北）
	 * @param startLonRad 起点经度（弧度）
	 * @param startLatRad 起点纬度（弧度）
	 * @param endLonRad  终点经度（弧度）
	 * @param endLatRad  终点经度（弧度）
	 * @return
	 */
	public static double calcuteBearing(double startLonRad,double startLatRad,double endLonRad,double endLatRad) {
		double dLon = (endLonRad - startLonRad); 
	    double y = Math.sin(dLon) * Math.cos(endLatRad);
	    double x = Math.cos(startLatRad)*Math.sin(endLatRad) - Math.sin(startLatRad)*Math.cos(endLatRad)*Math.cos(dLon);
	    double bear = Math.atan2(y, x);
	    if (bear < 0) {
	    	bear+=360;
	    }
	    return bear;
	}
	
	
	/**
	 * 已知起点的经纬度，以及偏向角，距离  计算终点坐标
	 * @param startLonRad  起点经度（弧度）
	 * @param startLatRad  起点纬度 （弧度）
	 * @param bearingRad 增量角度（偏向角）
	 * @param distance  距离
	 * @return
	 */
	public static Coordinate getEndPoint(double startLonRad,double startLatRad,double bearingRad,double distance) {
		  double dR = distance / RADIUS;
		  double endLat = Math.asin(Math.sin(startLatRad)*Math.cos(dR) + Math.cos(startLatRad)*Math.sin(dR)*Math.cos(bearingRad));
		  double endLon = startLonRad + Math.atan2(Math.sin(bearingRad)*Math.sin(dR)*Math.cos(startLatRad), 
				  								Math.cos(dR) - Math.sin(startLatRad)*Math.sin(endLat));
		  Coordinate coordinate = new Coordinate(convertRadToLon(endLon), convertRadToLon(endLat));
		  return coordinate;
	}
	
	
	
	public static Coordinate[] calcuteCoordinates(String startPos, String endPos, double stride) {
		String[] startPointArray = startPos.split(",");
		String[] endPointArray = endPos.split(",");
		double startLon = Double.parseDouble(startPointArray[0]);
		double startLat = Double.parseDouble(startPointArray[1]);
		double endLon = Double.parseDouble(endPointArray[0]);
		double endLat = Double.parseDouble(endPointArray[1]);
		double distance = GeometryUtil.getInstance(startLon, startLat, endLon, endLat);
		double bearad = GeometryUtil.calcuteBearing(GeometryUtil.convertLonOrLatToRad(startLon), 
													GeometryUtil.convertLonOrLatToRad(startLat),
													GeometryUtil.convertLonOrLatToRad(endLon),
													GeometryUtil.convertLonOrLatToRad(endLat));
		Coordinate coordinate = GeometryUtil.getEndPoint(GeometryUtil.convertLonOrLatToRad(startLon), 
														 GeometryUtil.convertLonOrLatToRad(startLat), 
														 bearad, 
														 stride);
		double resX = coordinate.getX() - startLon;
		double resY = coordinate.getY() - startLat;
		int loopNum = (int) (distance / stride);
		Coordinate[] coordinates = new Coordinate[loopNum+2];
		for (int i = 0; i <= loopNum; i++) {
			coordinates[i] = new Coordinate(startLon+resX*i,startLat+resY*i);
		}
		//不能整除，需要计算最后一个点
		if (distance % stride != 0) {
			coordinates[coordinates.length - 1] = new Coordinate(endLon, endLat);
		}
		return coordinates;
	}

	//获取指定等级的瓦片数
	public static int[] getNumMatrixTile(String srs , int level) {
		int matrixRow = 0;
		int matrixCol = 0;
		int[] matrix = new int[2];
		if (supportSrs.contains(srs)) {
			if (StringUtils.equals(srs, "EPSG:4326")) {
				matrix[0] = (int) Math.pow(2, level + 1);
				matrix[1] = (int) Math.pow(2, level);
			}else if (StringUtils.equalsAny(srs, "EPSG:900913","EPSG:3857")){
				matrix[0] = (int) Math.pow(2, level);
				matrix[1] = (int) Math.pow(2, level);
			}
		}
		return matrix;
	}

	//获取指定投影坐标支持的最大像素级别
	public static int getMaxSupportLevel(String srs){
		int maxLevel = 0;
		if (supportSrs.contains(srs)) {
			if (StringUtils.equals(srs, "EPSG:4326")) {
				maxLevel = epsg4326.size();
			}else if (StringUtils.equalsAny(srs, "EPSG:900913","EPSG:3857")){
				maxLevel = epsg3857.size();
			}
		}
		return maxLevel;
	}

	//获取指定投影坐标系坐标原点(地理坐标系采用YX,投影坐标系采用XY)
	public static double[] getOrderedTopLeftCorner(String srs){
		double[] leftCorner = new double[2];
		if (supportSrs.contains(srs)) {
			if (StringUtils.equals(srs, "EPSG:4326")) {
				leftCorner[0] = 90.0d;
				leftCorner[1] = -180.0d;
			}else if (StringUtils.equalsAny(srs, "EPSG:900913","EPSG:3857")){
				leftCorner[0] = MERCATOR_UNITS_NEGITAVE;
				leftCorner[1] = MERCATOR_UNITS;
			}
		}
		return leftCorner;
	}

	public static Double[] getResolutions(String srs,int maxSupportLevel,int tileWidth){
		Double[] resolutions = new Double[maxSupportLevel + 1];
		int n = 0;
		if (StringUtils.equals(srs, "EPSG:4326")) {
			if (tileWidth == DEFAULT_TILE_WIDTH){
				Iterator<Double> iterator = epsg4326.iterator();
				while (iterator.hasNext()){
					if (n > maxSupportLevel){
						break;
					}
					resolutions[n] = iterator.next();
					n++;
				}
			} else {
				for (int i = 0; i <= maxSupportLevel ; i++) {
					resolutions[i] = DEGREE / (Math.pow(2, i + 1) * tileWidth);
				}
			}
		} else if (StringUtils.equalsAny(srs, "EPSG:900913","EPSG:3857")){
			if (tileWidth == DEFAULT_TILE_WIDTH){
				Iterator<Double> iterator = epsg3857.iterator();
				while (iterator.hasNext()){
					if (n > maxSupportLevel){
						break;
					}
					resolutions[n] = iterator.next();
					n++;
				}
			} else {
				for (int i = 0; i <= maxSupportLevel ; i++) {
					resolutions[i] = DEGREE / (Math.pow(2, i) * tileWidth);
				}
			}
		}
		return resolutions;
	}

	/**
	 * 获取高斯投影下要素点角度间隔
	 *
	 */
	public static Double[] getResolutions(List<String> tileCount, int maxSupportLevel, int tileWidth){
		Double[] resolutions = new Double[maxSupportLevel + 1];
		for (int i = 0; i <= maxSupportLevel ; i++) {
			resolutions[i] = DEGREE / (Integer.parseInt(tileCount.get(i).split("\\*")[0]) * tileWidth);
		}
		return resolutions;
	}

	/**
	 * 将4326的行列号转换为4326的bbox
	 *
	 * @param level
	 * @param row
	 * @param col
	 * @return
	 */
	public static String convertRangeToBbox (int level, int row, int col) {
		// 将行列号转换为bbox
		double minLat = 90 - (180 / Math.pow(2, level) * (row + 1));
		double maxLat = 90 - (180 / Math.pow(2, level) * row);

		double minLon = (360 / Math.pow(2, level + 1) * (col + 1)) - 180;
		double maxLon = (360 / Math.pow(2, level + 1) * col) - 180;
		StringBuilder bbox = new StringBuilder();
		bbox.append(minLon)
				.append(",")
				.append(minLat)
				.append(",")
				.append(maxLon)
				.append(",")
				.append(maxLat);

		return bbox.toString();
	}
	
	/**
	 * km转度
	 */
	public static Double kmToGegree(Double l) {
		Double degree = (180 / EARTHRADIUS / Math.PI) * l;
		return degree;
	}
	
	/**
	 * 度转km
	 */
	public static Double gegreeToKm(Double degree) {
		Double l = EARTHRADIUS / 180 * Math.PI * degree;
		return l;
	}

	/**
	 * 将4326范围缩小至3857接受的范围
	 * @return
	 */
	public static ReferencedEnvelope checkEnvelope(ReferencedEnvelope sourceEnvelope) {
		ReferencedEnvelope envelope = null;
		double maxY = 0.0;
		double minY = 0.0;
		//如果纬度大于85则需要处理
		if (sourceEnvelope.getMaximum(1) > mercatorY2lat(MERCATOR_UNITS)) {
			maxY = mercatorY2lat(MERCATOR_UNITS);
		}else {
			maxY = sourceEnvelope.getMaximum(1);
		}
		if (sourceEnvelope.getMinimum(1) < mercatorY2lat(MERCATOR_UNITS_NEGITAVE)) {
			minY = mercatorY2lat(MERCATOR_UNITS_NEGITAVE);
		}else {
			minY = sourceEnvelope.getMinimum(1);
		}
		envelope = new ReferencedEnvelope(sourceEnvelope.getMinimum(0),sourceEnvelope.getMaximum(0),
				minY,maxY,sourceEnvelope.getCoordinateReferenceSystem());
		return envelope;
	}

	public static Double geodesicDistance(Geometry a,Geometry b){
		 return a.distance(b);
	}

	public static void main(String[] args) {
		String startPoint="108.935182,34.342859";
		String entPoint="107.019257,33.069495";

//		测地距离
		// 84坐标系构造GeodeticCalculator
		GeodeticCalculator geodeticCalculator = new GeodeticCalculator(DefaultGeographicCRS.WGS84);
		// 起点经纬度
		geodeticCalculator.setStartingGeographicPoint(108.935182,34.342859);
		// 末点经纬度
		geodeticCalculator.setDestinationGeographicPoint(107.019257,33.069495);
		// 计算距离，单位：米
		double orthodromicDistance = geodeticCalculator.getOrthodromicDistance();
		System.out.println(orthodromicDistance/1000);


	}
}
