package org.pygisserver.terrainanalysis.service.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import org.pygisserver.common.entity.AnalysisResult;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.enumm.OgcEpsg;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.util.CommonUtil;
import org.pygisserver.common.util.GeometryUtil;
import org.pygisserver.render.service.impl.CustomDataLoadImpl;
import org.pygisserver.terrainanalysis.entity.TilePixelEntity;
import org.pygisserver.terrainanalysis.service.MapLayerAnalysis;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MapLayerAnalysisImpl implements MapLayerAnalysis {

	
	private static volatile Map<String,Dataset> DATASETCACHE = new ConcurrentHashMap<String,Dataset>();
	
	//注册gdal
	static {
		gdal.AllRegister();
	}

	/**
	 * 获取单个点的高程值
	 */
	@Override
	public double getDemByPosition(String fileUrl, String nativeBbox, double longitude, double latitude) {
		// 获取图层文件路径
		try {
			return getSinglePointDem(fileUrl, nativeBbox, longitude, latitude);
		} catch (Exception e) {
			log.error("MapLayerAnalysisImpl getDemByPosition has error:" + e.getLocalizedMessage());
			throw new BaseException(ExceptionsUtil.ERROR_500, e.getLocalizedMessage());
		}
	}


	/**
	 * 获取瓦片中的单个像素点的高程值
	 */
    @Override
    public double getDemByPixel(LayerBaseInfo layerBaseInfo, String nativeBbox, double longitude, double latitude) {
        double result = 0.0;
	    if (("TMS").equals(layerBaseInfo.getFileType())){
            result =  tmsAnalysisDemByPoint(layerBaseInfo,nativeBbox,layerBaseInfo.getSupportMaxLevel(),longitude,latitude);
        } else if (("TDB").equals(layerBaseInfo.getFileType())){
            result =  tdbAnalysisDemByPoint(layerBaseInfo,nativeBbox,layerBaseInfo.getSupportMaxLevel(),longitude,latitude);
        }
	    return result;
    }

	/**
	 * 获取瓦片中的单个像素点的坡度值
	 */
    @Override
    public double getSlopeByPixel(LayerBaseInfo layerBaseInfo, String nativeBbox, double longitude, double latitude) {
        double result = 0.0;
        if (layerBaseInfo.getFileType().equals("TMS")){
            result =  tmsAnalysisSlopeByPoint(layerBaseInfo,nativeBbox,layerBaseInfo.getSupportMaxLevel(),longitude,latitude);
        } else if (layerBaseInfo.getFileType().equals("TDB")){
            result =  tdbAnalysisSlopeByPoint(layerBaseInfo,nativeBbox,layerBaseInfo.getSupportMaxLevel(),longitude,latitude);
        }
        return result;
    }

	/**
	 * 获取瓦片中的单个像素点的坡向值
	 */
    @Override
    public double getAspectByPixel(LayerBaseInfo layerBaseInfo, String nativeBbox, double longitude, double latitude) {
        double result = 0.0;
        if (layerBaseInfo.getFileType().equals("TMS")){
            result =  tmsAnalysisAspectByPoint(layerBaseInfo,nativeBbox,layerBaseInfo.getSupportMaxLevel(),longitude,latitude);
        } else if (layerBaseInfo.getFileType().equals("TDB")){
            result =  tdbAnalysisAspectByPoint(layerBaseInfo,nativeBbox,layerBaseInfo.getSupportMaxLevel(),longitude,latitude);
        }
        return result;
    }

	public double[] getCoordinateTileDemValues(LayerBaseInfo layerBaseInfo, String nativeBbox, Coordinate[] coordinates) {
		double[] demArray = new double[coordinates.length];
		int index = 0;
		double value = 0.0;
		if ("TMS".equals(layerBaseInfo.getFileType())) {
			for (Coordinate coordinate2 : coordinates) {
				try {
					value = tmsAnalysisDemByPoint(layerBaseInfo, nativeBbox, layerBaseInfo.getSupportMaxLevel(), coordinate2.getX(), coordinate2.getY());
					demArray[index] = value;
					index++;
				} catch (Exception e) {
					log.error("MapLayerAnalysisImpl getCoordinateTileDemValues has error:" + e.getLocalizedMessage());
				}
			}
		}
		if ("TDB".equals(layerBaseInfo.getFileType())){
			for (Coordinate coordinate2 : coordinates) {
				try {
					value = tdbAnalysisDemByPoint(layerBaseInfo, nativeBbox, layerBaseInfo.getSupportMaxLevel(), coordinate2.getX(), coordinate2.getY());
					demArray[index] = value;
					index++;
				} catch (Exception e) {
					log.error("MapLayerAnalysisImpl getCoordinateDemValues has error:" + e.getLocalizedMessage());
				}
			}
		}
		return demArray;
	}

    // private double getSinglePointDem(String fileUrl, String nativeBbox, double
	// longitude, double latitude)
	// throws IOException {
	// String[] nativeBboxArray = nativeBbox.split(";");
	// double minX = Double.parseDouble(nativeBboxArray[0]);
	// double maxX = Double.parseDouble(nativeBboxArray[2]);
	// double minY = Double.parseDouble(nativeBboxArray[1]);
	// double maxY = Double.parseDouble(nativeBboxArray[3]);
	// if (longitude < minX || longitude > maxX) {
	// return 0.0;
	// }
	// if (latitude < minY || latitude > maxY) {
	// return 0.0;
	// }
	// BufferedImage bufferedImage = null;
	// if (bufferImageCache.containsKey(fileUrl)) {
	// bufferedImage = bufferImageCache.get(fileUrl);
	// } else {
	// bufferedImage = ImageIO.read(new File(fileUrl));
	// bufferImageCache.put(fileUrl, bufferedImage);
	// }
	// double resX = (maxX - minX) / bufferedImage.getWidth();
	// double resY = (maxY - minY) / bufferedImage.getHeight();
	// int x = (int) Math.floor((longitude - minX) / resX);
	// int y = (int) Math.floor((maxY - latitude) / resY);
	// float[] demArray = new float[1];
	// bufferedImage.getRaster().getPixel(x, y, demArray);
	// bufferedImage = null;
	// return demArray[0];
	// }


	/**
	 * 获取单个点的高程值
	 */
	private double getSinglePointDem(String fileUrl, String nativeBbox, double longitude, double latitude)
			//double longitude, double latitude)
			throws IOException {
		//读取gdal
		Dataset dataset = getDataSetByFilePath(fileUrl);
		//获取6参数
		double[] transform = dataset.GetGeoTransform();
		int x = (int) Math.round((longitude - transform[0]) / transform[1]);
		int y = (int) Math.round((latitude - transform[3]) / transform[5]);
		Band band = dataset.GetRasterBand(1);
		float[] demArray = new float[1];
		band.ReadRaster(x, y, 1, 1, demArray);
//		band.ReadRaster(longitude,latitude,1,1,demArray);
		return demArray[0];
	}


	/**
	 * 从文件中获取dataset对象
	 */
	@Override
	public Dataset getDataSetByFilePath(String fileUrl) {
		Dataset dataset = null;
		if (DATASETCACHE.containsKey(fileUrl)) {
			dataset = DATASETCACHE.get(fileUrl);
		}else {
			dataset = gdal.Open(fileUrl, gdalconstConstants.GA_ReadOnly);
			DATASETCACHE.put(fileUrl, dataset);
		}
		return dataset;
	}

	/**
	 * 线性高程分析（采样间距）
	 */
	@Override
	public void getDemByLinePoint(String fileUrl, String nativeBbox, String startPos, String endPos, double stride,
			AnalysisResult analysisResult) {
		Coordinate[] coordinates = GeometryUtil.calcuteCoordinates(startPos, endPos, stride);
		analysisResult.setCoordinates(coordinates);
		double[] demArray = getCoordinateDemValues(fileUrl, nativeBbox, coordinates);
		analysisResult.setDemArray(demArray);
	}


	/**
	 * 线性高程分析（采用点）
	 */
	@Override
	public void getDemByLinePoint(String fileUrl, String nativeBbox, String startPos, String endPos, int sampingNum,
			AnalysisResult analysisResult) {
		// 采样点数包括起点和终点坐标，如果采样一个点，则实际点数是3，如果采样5个点，则实际点数为7
		String[] startPointArray = startPos.split(",");
		String[] endPointArray = endPos.split(",");
		double startLon = Double.parseDouble(startPointArray[0]);
		double startLat = Double.parseDouble(startPointArray[1]);
		double endLon = Double.parseDouble(endPointArray[0]);
		double endLat = Double.parseDouble(endPointArray[1]);
		double rationX = (endLon - startLon) / (sampingNum + 1);
		double rationY = (endLat - startLat) / (sampingNum + 1);
		Coordinate[] coordinates = new Coordinate[sampingNum + 2];
		for (int i = 0; i < sampingNum + 2; i++) {
			coordinates[i] = new Coordinate(startLon + rationX * i, startLat + rationY * i);
		}
		analysisResult.setCoordinates(coordinates);
		double[] demArray = getCoordinateDemValues(fileUrl, nativeBbox, coordinates);
		analysisResult.setDemArray(demArray);
	}

	/**
	 * 矩形高程分析（采用点数）
	 */
	@Override
	public void getRegionDemBySampingNum(String fileUrl, String nativeBbox, String leftUpperPos, String rightLowerPos,
			int sampleXCount, int sampleYCount, AnalysisResult analysis) {
		/**
		 * 相当于已知起点和终点以及采样点数，计算每一个点的高程数据，sampleXCount和左上角以及右上角来计算x方向每隔多少经度取数据
		 * sampleYCount和左上角以及左下角来计算Y方向每隔多少经纬度取数据
		 */
		Coordinate[] coordinates = getValidCoordinates(nativeBbox, leftUpperPos, rightLowerPos, sampleXCount,
				sampleYCount);
		analysis.setCoordinates(coordinates);
		double[] demArray = getCoordinateDemValues(fileUrl, nativeBbox, coordinates);
		analysis.setDemArray(demArray);
		analysis.setImageWidth(sampleXCount + 2);
		analysis.setImageHeight(sampleYCount + 2);
	}

	public Coordinate[] getValidCoordinates(String nativeBbox, String leftUpperPos, String rightLowerPos,
			int sampleXCount, int sampleYCount) {
		ReferencedEnvelope validEnvelope = calcuteValidBbox(nativeBbox, leftUpperPos, rightLowerPos);
		// 按照水平方向从左至右一次计算，每次给定起点和终点，计算当前的数据
		double startX = validEnvelope.getMinX();
		double startY = validEnvelope.getMaxY();
		double endX = validEnvelope.getMaxX();
		double endY = validEnvelope.getMinY();
		double rationX = (endX - startX) / (sampleXCount + 1);
		double rationY = (endY - startY) / (sampleYCount + 1);
		Coordinate[] coordinates = new Coordinate[(sampleXCount + 2) * (sampleYCount + 2)];
		for (int i = 0; i < sampleYCount + 2; i++) {
			for (int j = 0; j < sampleXCount + 2; j++) {
				coordinates[j + i * (sampleXCount + 2)] = new Coordinate(startX + rationX * j, startY + rationY * i);
			}
		}
		return coordinates;
	}

	public ReferencedEnvelope calcuteValidBbox(String nativeBbox, String leftUpperPos, String rightLowerPos) {
		String[] nativeBboxArray = nativeBbox.split(";");
		double minX = Double.parseDouble(nativeBboxArray[0]);
		double maxX = Double.parseDouble(nativeBboxArray[2]);
		double minY = Double.parseDouble(nativeBboxArray[1]);
		double maxY = Double.parseDouble(nativeBboxArray[3]);
		// 求交集获取有效计算范围
		String[] leftUpperArray = leftUpperPos.split(",");
		String[] rightLowerArray = rightLowerPos.split(",");
		double reqMinX = Double.parseDouble(leftUpperArray[0]);
		double reqMaxX = Double.parseDouble(rightLowerArray[0]);
		double reqMinY = Double.parseDouble(rightLowerArray[1]);
		double reqMaxY = Double.parseDouble(leftUpperArray[1]);
		ReferencedEnvelope validEnvelope = null;
		try {
			ReferencedEnvelope envelope = new ReferencedEnvelope(minX, maxX, minY, maxY, CRS.decode("EPSG:4326", true));
			ReferencedEnvelope reqEnvelope = new ReferencedEnvelope(reqMinX, reqMaxX, reqMinY, reqMaxY,
					CRS.decode("EPSG:4326", true));
			validEnvelope = envelope.intersection(reqEnvelope);
		} catch (MismatchedDimensionException | FactoryException e) {
			log.error("MapLayerAnalysisImpl calcuteValidBbox has error:" + e.getLocalizedMessage());
		}
		return validEnvelope;
	}

	/**
	 * 通过坐标对象获取对应范围内的各个单点高程值
	 */
	public double[] getCoordinateDemValues(String fileUrl, String nativeBbox, Coordinate[] coordinates) {
		double[] demArray = new double[coordinates.length];
		int index = 0;
		for (Coordinate coordinate2 : coordinates) {
			try {
				double value = getSinglePointDem(fileUrl, nativeBbox, coordinate2.getX(), coordinate2.getY());
				demArray[index] = value;
				index++;
			} catch (Exception e) {
				log.error("MapLayerAnalysisImpl getCoordinateDemValues has error:" + e.getLocalizedMessage());
			}
		}
		return demArray;
	}

	/**
	 * 瓦片数据矩形高程分析（采样间隔）
	 */
	@Override
	public void getRegionDemBySampingRatio(String fileUrl, String nativeBbox, String leftUpperPos, String rightLowerPos,
			double strideX, double strideY, AnalysisResult analysis) {

		Coordinate[] coordinates = calculteCoordinates(nativeBbox, leftUpperPos, rightLowerPos, strideX, strideY,
				analysis);
		analysis.setCoordinates(coordinates);
		double[] demArray = getCoordinateDemValues(fileUrl, nativeBbox, coordinates);
		analysis.setDemArray(demArray);
	}

	public Coordinate[] calculteCoordinates(String nativeBbox, String leftUpperPos, String rightLowerPos,
			double strideX, double strideY, AnalysisResult analysis) {
		// 以左上角为起点，计算x方向，和Y方向各一条线的点
		ReferencedEnvelope validEnvelope = calcuteValidBbox(nativeBbox, leftUpperPos, rightLowerPos);
		// 计算出起始位置点
		double startLon = validEnvelope.getMinX();
		double startLat = validEnvelope.getMaxY();
		double endLonX = validEnvelope.getMaxX();
		double endLatX = validEnvelope.getMaxY();
		double endLonY = validEnvelope.getMinX();
		double endLatY = validEnvelope.getMinY();
		String startPos = startLon + "," + startLat;
		String endPosX = endLonX + "," + endLatX;
		String endPosY = endLonY + "," + endLatY;

		Coordinate[] coordinatesx = GeometryUtil.calcuteCoordinates(startPos, endPosX, strideX);
		Coordinate[] coordinatesy = GeometryUtil.calcuteCoordinates(startPos, endPosY, strideY);
		analysis.setImageWidth(coordinatesx.length);
		analysis.setImageHeight(coordinatesy.length);
		Coordinate[] coordinates = new Coordinate[coordinatesx.length * coordinatesy.length];
		for (int i = 0; i < coordinatesy.length; i++) {
			for (int j = 0; j < coordinatesx.length; j++) {
				coordinates[j + i * (coordinatesx.length)] = new Coordinate(coordinatesx[j].getX(),
						coordinatesy[i].getY());
			}
		}
		return coordinates;
	}

	/**
	 * 计算单个点的坡度值
	 */
	@Override
	public double getPointSlope(String fileUrl, String nativeBbox, double longitude, double latitude) {
		//读取gdal
		Dataset dataset = getDataSetByFilePath(fileUrl);
		//获取6参数
		double[] transform = dataset.GetGeoTransform();
		int x = (int) Math.round((longitude - transform[0]) / transform[1]);
		int y = (int) Math.round((latitude - transform[3]) / transform[5]);
		Double slopeValue = dealEspecialPoint(dataset, x, y,true);
		if (!slopeValue.equals(Double.NaN)) {
			return slopeValue;
		}
		float[] floatArray = new float[9];
		Band band = dataset.GetRasterBand(1);
		band.ReadRaster(x - 1, y - 1, 3, 3, floatArray);
		return calcutePointSlope(floatArray, transform[1], Math.abs(transform[5]));
	}
	/**
	 * 处理特殊点数据
	 */
	public double dealEspecialPoint(Dataset dataset, int x, int y, boolean isSlope) {
		double dx = 0;
		double dy = 0;
		double[] transform = dataset.GetGeoTransform();
		double rex = transform[1];
		double rey = Math.abs(transform[5]);
		Band band = dataset.GetRasterBand(1);
		if (x == 0 && y == 0) {
			float[] demArray = new float[4];
			band.ReadRaster(x, y, 2, 2, demArray);
			// 计算x+1和y+1的方向变换
			double startPointDem = demArray[0];
			double nextXpointDem = demArray[1];
			double nextYPointDem = demArray[2];
			dx = (startPointDem - nextXpointDem) / rex;
			dy = (nextYPointDem - startPointDem) / rey;
		} else if (x == 0 && y == (dataset.getRasterYSize() - 1)) {
			// 计算x+1和y-1的方向变换
			float[] demArray = new float[4];
			band.ReadRaster(x, y - 1, 2, 2, demArray);
			double startPointDem = demArray[2];
			double nextXpointDem = demArray[3];
			double preYPointDem = demArray[0];
			dx = (startPointDem - nextXpointDem) / rex;
			dy = (startPointDem - preYPointDem) / rey;
		} else if (y == 0 && x == (dataset.getRasterXSize() - 1)) {
			// 计算x-1和y+1的方向变换
			float[] demArray = new float[4];
			band.ReadRaster(x - 1, y, 2, 2, demArray);
			double startPointDem = demArray[1];
			double preXpointDem = demArray[0];
			double nextYPointDem = demArray[3];
			dx = (preXpointDem - startPointDem) / rex;
			dy = (nextYPointDem - startPointDem) / rey;
		} else if (x == (dataset.getRasterXSize() - 1) && y == (dataset.getRasterYSize() - 1)) {
			// 计算x-1和y-1的方向变换
			float[] demArray = new float[4];
			band.ReadRaster(x - 1, y - 1, 2, 2, demArray);
			double endPointDem = demArray[3];
			double preXpointDem = demArray[2];
			double preYPointDem = demArray[1];
			dx = (preXpointDem - endPointDem) / rex;
			dy = (endPointDem - preYPointDem) / rey;
		} else if (x == 0 && y != 0 && y != dataset.getRasterYSize() - 1) {
			float[] demArray = new float[6];
			band.ReadRaster(x, y - 1, 2, 3, demArray);
			// 计算x+1和y-1和y+1
			double startPointDem = demArray[2];
			double nextXPointDem = demArray[3];
			double preYpointDem = demArray[0];
			double nextYpointDem = demArray[4];
			dx = (startPointDem - nextXPointDem) / rex;
			dy = (nextYpointDem - preYpointDem) / 2 * rey;
		} else if (x == (dataset.getRasterXSize() - 1) && y != 0 && y != dataset.getRasterYSize() - 1) {
			// 计算x-1和y-1和y+1
			float[] demArray = new float[6];
			band.ReadRaster(x - 1, y - 1, 2, 3, demArray);
			double startPointDem = demArray[3];
			double preXPointDem = demArray[2];
			double preYpointDem = demArray[1];
			double nextYpointDem = demArray[5];
			dx = (preXPointDem - startPointDem) / rex;
			dy = (nextYpointDem - preYpointDem) / 2 * rey;
		} else if (y == 0 && x != 0 && x != dataset.getRasterXSize() - 1) {
			// 计算y+1和x-1和x+1
			float[] demArray = new float[6];
			band.ReadRaster(x - 1, y, 3, 2, demArray);
			double startPointDem = demArray[1];
			double nextYPointDem = demArray[4];
			double preXpointDem = demArray[0];
			double nextXpointDem = demArray[2];
			dx = (preXpointDem - nextXpointDem) / 2 * rex;
			dy = (nextYPointDem - startPointDem) / rey;
		} else if (y == (dataset.getRasterYSize() - 1) && x != 0 && x != dataset.getRasterXSize() - 1) {
			// 计算y-1和x-1和x+1
			float[] demArray = new float[6];
			band.ReadRaster(x - 1, y - 1, 3, 2, demArray);
			double startPointDem = demArray[4];
			double preYPointDem = demArray[1];
			double preXpointDem = demArray[3];
			double nextXpointDem = demArray[5];
			dx = (preXpointDem - nextXpointDem) / 2 * rex;
			dy = (startPointDem - preYPointDem) / rey;
		} else {
			return Double.NaN;
		}

		if (isSlope) {
			return Math.atan(Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2))) * (180 / GeometryUtil.PI);
		} else {
			if (dx == 0 && dy == 0) {
				return -1;
			}
			return Math.atan2(dy, dx) / GeometryUtil.OGC_DEGREE_TO_METERS;
		}
	}
	/**
	 * 坡度算法
	 */
	private double calcutePointSlope(float[] demArray, double resX, double rexY) {
		float dx = (float) (((demArray[0] + demArray[6] + 2 * demArray[3])
                - (demArray[2] + demArray[8] + 2 * demArray[5])) / (8 * resX * GeometryUtil.OGC_DEGREE_TO_METERS));
		float dy = (float) (((demArray[8] + demArray[6] + 2 * demArray[7])
				- (demArray[2] + demArray[0] + 2 * demArray[1])) / (8 * rexY * GeometryUtil.OGC_DEGREE_TO_METERS));
		return Math.atan(Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2))) * (180 / GeometryUtil.PI);
	}

	/**
	 * 根据采样步幅计算采样线上的坡度值
	 */
	@Override
	public void getSlopeByLinePoint(String fileUrl, String nativeBbox, String startPos, String endPos, double stride,
			AnalysisResult analysis) {
		/**
		 * 计算起点和终点的距离以及偏向角 根据给定的距离计算出第一个点的经纬度，然后计算出采样的间隔，依次计算出所有点，以及其对应的高程数据
		 */
		Coordinate[] coordinates = GeometryUtil.calcuteCoordinates(startPos, endPos, stride);
		analysis.setCoordinates(coordinates);
		double[] slopeArray = new double[coordinates.length];
		for (int i = 0; i < coordinates.length; i++) {
			slopeArray[i] = getPointSlope(fileUrl, nativeBbox, coordinates[i].getX(), coordinates[i].getY());
		}
		analysis.setDemArray(slopeArray);
	}

	/**
	 * 根据采样点数计算坡度值
	 */
	@Override
	public void getSlopeByLinePoint(String fileUrl, String nativeBbox, String startPos, String endPos, int sampingNum,
			AnalysisResult analysis) {
		String[] startPointArray = startPos.split(",");
		String[] endPointArray = endPos.split(",");
		double startLon = Double.parseDouble(startPointArray[0]);
		double startLat = Double.parseDouble(startPointArray[1]);
		double endLon = Double.parseDouble(endPointArray[0]);
		double endLat = Double.parseDouble(endPointArray[1]);
		double rationX = (endLon - startLon) / (sampingNum + 1);
		double rationY = (endLat - startLat) / (sampingNum + 1);
		Coordinate[] coordinates = new Coordinate[sampingNum + 2];
		for (int i = 0; i < sampingNum + 2; i++) {
			coordinates[i] = new Coordinate(startLon + rationX * i, startLat + rationY * i);
		}
		analysis.setCoordinates(coordinates);
		double[] slopeArray = new double[coordinates.length];
		for (int i = 0; i < coordinates.length; i++) {
			slopeArray[i] = getPointSlope(fileUrl, nativeBbox, coordinates[i].getX(), coordinates[i].getY());
		}
		analysis.setDemArray(slopeArray);
	}

	@Override
	public void getSlopeBySampingNum(String fileUrl, String nativeBbox, String leftUpperPos, String rightLowerPos,
			int sampleXCount, int sampleYCount, AnalysisResult analysisResult) {
		/**
		 * 相当于已知起点和终点以及采样点数，计算每一个点的高程数据，sampleXCount和左上角以及右上角来计算x方向每隔多少经度取数据
		 * sampleYCount和左上角以及左下角来计算Y方向每隔多少经纬度取数据
		 */
		Coordinate[] coordinates = getValidCoordinates(nativeBbox, leftUpperPos, rightLowerPos, sampleXCount,
				sampleYCount);
		analysisResult.setCoordinates(coordinates);
		double[] slopeArray = new double[coordinates.length];
		for (int i = 0; i < coordinates.length; i++) {
			slopeArray[i] = getPointSlope(fileUrl, nativeBbox, coordinates[i].getX(), coordinates[i].getY());
		}
		analysisResult.setDemArray(slopeArray);
		analysisResult.setImageWidth(sampleXCount + 2);
		analysisResult.setImageHeight(sampleYCount + 2);
	}

	@Override
	public void getSlopeBySampingRatio(String fileUrl, String nativeBbox, String leftUpperPos, String rightLowerPos,
			double strideX, double strideY, AnalysisResult analysisResult) {
		Coordinate[] coordinates = calculteCoordinates(nativeBbox, leftUpperPos, rightLowerPos, strideX, strideY,
				analysisResult);
		analysisResult.setCoordinates(coordinates);
		double[] slopeArray = new double[coordinates.length];
		for (int i = 0; i < coordinates.length; i++) {
			slopeArray[i] = getPointSlope(fileUrl, nativeBbox, coordinates[i].getX(), coordinates[i].getY());
		}
		analysisResult.setDemArray(slopeArray);
	}

	@Override
	public double getPointAspect(String fileUrl, String nativeBbox, double longitude, double latitude) {
		// 计算出当前数据的平面坐标位置
		Dataset dataset = getDataSetByFilePath(fileUrl);
		//获取6参数
		double[] transform = dataset.GetGeoTransform();
		int x = (int) Math.round((longitude - transform[0]) / transform[1]);
		int y = (int) Math.round((latitude - transform[3]) / transform[5]);
		Double aspect = dealEspecialPoint(dataset, x, y,false);
		// 当前点的坐标位置
		if (!aspect.equals(Double.NaN)) {
			if (aspect < 0) {
				aspect += 360.0d;
			}
			if (aspect == 360.0) {
				aspect = 0.0;
			}
			return aspect;
		}
		float[] floatArray = new float[9];
		Band band = dataset.GetRasterBand(1);
		band.ReadRaster(x - 1, y - 1, 3, 3, floatArray);
		aspect = calcutePointAspect(floatArray, transform[1], Math.abs(transform[5]));
		// 如果用方位角表示
		if (aspect > 90.0) {
			aspect = 450.0d - aspect;
		} else {
			aspect = 90.0d - aspect;
		}
		// 不用方位角表示
		// if (aspect < 0) {
		// aspect+=360.0d;
		// }
		if (aspect == 360.0) {
			aspect = 0.0;
		}
		return aspect;
	}

	/**
	 * 
	 * @param demArray
	 * @param resX
	 * @param rexY
	 * @return
	 */
	private double calcutePointAspect(float[] demArray, double resX, double rexY) {
		float dx = (float) (((demArray[0] + demArray[6] + 2 * demArray[3])
				- (demArray[2] + demArray[8] + 2 * demArray[5])) / (8 * resX * GeometryUtil.OGC_DEGREE_TO_METERS));
		float dy = (float) (((demArray[8] + demArray[6] + 2 * demArray[7])
				- (demArray[2] + demArray[0] + 2 * demArray[1])) / (8 * rexY * GeometryUtil.OGC_DEGREE_TO_METERS));
		double aspect = Math.toDegrees(Math.atan2(dy, dx));
		if (dx == 0 && dy == 0) {
			return -1;
		}
		return aspect;
	}

	@Override
	public void getAspectByLinePoint(String fileUrl, String nativeBbox, String startPos, String endPos, double stride,
			AnalysisResult analysisResult) {
		/**
		 * 计算起点和终点的距离以及偏向角 根据给定的距离计算出第一个点的经纬度，然后计算出采样的间隔，依次计算出所有点，以及其对应的高程数据
		 */
		Coordinate[] coordinates = GeometryUtil.calcuteCoordinates(startPos, endPos, stride);
		analysisResult.setCoordinates(coordinates);
		double[] slopeArray = new double[coordinates.length];
		for (int i = 0; i < coordinates.length; i++) {
			slopeArray[i] = getPointAspect(fileUrl, nativeBbox, coordinates[i].getX(), coordinates[i].getY());
		}
		analysisResult.setDemArray(slopeArray);

	}

	@Override
	public void getAspectByLinePoint(String fileUrl, String nativeBbox, String startPos, String endPos, int sampingNum,
			AnalysisResult analysisResult) {
		String[] startPointArray = startPos.split(",");
		String[] endPointArray = endPos.split(",");
		double startLon = Double.parseDouble(startPointArray[0]);
		double startLat = Double.parseDouble(startPointArray[1]);
		double endLon = Double.parseDouble(endPointArray[0]);
		double endLat = Double.parseDouble(endPointArray[1]);
		double rationX = (endLon - startLon) / (sampingNum + 1);
		double rationY = (endLat - startLat) / (sampingNum + 1);
		Coordinate[] coordinates = new Coordinate[sampingNum + 2];
		for (int i = 0; i < sampingNum + 2; i++) {
			coordinates[i] = new Coordinate(startLon + rationX * i, startLat + rationY * i);
		}
		analysisResult.setCoordinates(coordinates);
		double[] slopeArray = new double[coordinates.length];
		for (int i = 0; i < coordinates.length; i++) {
			slopeArray[i] = getPointAspect(fileUrl, nativeBbox, coordinates[i].getX(), coordinates[i].getY());
		}
		analysisResult.setDemArray(slopeArray);

	}

	@Override
	public void getAspectBySampingNum(String fileUrl, String nativeBbox, String leftUpperPos, String rightLowerPos,
			int sampleXCount, int sampleYCount, AnalysisResult analysisResult) {
		/**
		 * 相当于已知起点和终点以及采样点数，计算每一个点的高程数据，sampleXCount和左上角以及右上角来计算x方向每隔多少经度取数据
		 * sampleYCount和左上角以及左下角来计算Y方向每隔多少经纬度取数据
		 */
		Coordinate[] coordinates = getValidCoordinates(nativeBbox, leftUpperPos, rightLowerPos, sampleXCount,
				sampleYCount);
		analysisResult.setCoordinates(coordinates);
		double[] slopeArray = new double[coordinates.length];
		for (int i = 0; i < coordinates.length; i++) {
			slopeArray[i] = getPointAspect(fileUrl, nativeBbox, coordinates[i].getX(), coordinates[i].getY());
		}
		analysisResult.setDemArray(slopeArray);
		analysisResult.setImageWidth(sampleXCount + 2);
		analysisResult.setImageHeight(sampleYCount + 2);

	}

	@Override
	public void getAspectBySampingRatio(String fileUrl, String nativeBbox, String leftUpperPos, String rightLowerPos,
			double strideX, double strideY, AnalysisResult analysisResult) {
		Coordinate[] coordinates = calculteCoordinates(nativeBbox, leftUpperPos, rightLowerPos, strideX, strideY,
				analysisResult);
		analysisResult.setCoordinates(coordinates);
		double[] slopeArray = new double[coordinates.length];
		for (int i = 0; i < coordinates.length; i++) {
			slopeArray[i] = getPointAspect(fileUrl, nativeBbox, coordinates[i].getX(), coordinates[i].getY());
		}
		analysisResult.setDemArray(slopeArray);
	}

	/**
	 * 获取采样点所在瓦片行列号以及对应像素点坐标
	 *
	 *
	 * */
    public TilePixelEntity getPixelByTiled(LayerBaseInfo layerBaseInfo, String nativaBbox, int supportMaxLevel, double lon, double lat){
        int tileRow = 0;//行
        int tileCol = 0;//列
		int zoom = supportMaxLevel;//递归缩放级别标志位
        double x = Double.NaN;
        double y = Double.NaN;
        double tileX = Double.NaN;
        double tileY = Double.NaN;
        int x_pixel = 0;
        int y_pixel = 0;
        double MinX = Double.NaN;
        double MinY = Double.NaN;
        double MaxX = Double.NaN;
        double MaxY = Double.NaN;
        if (layerBaseInfo.getResourceSrs().equals("EPSG:4326")){
			MinX = -180.0d;
			MinY = -90.0d;
			MaxX = 180.0d;
			MaxY = 90.0d;
            x = (MaxX-MinX)/Math.pow(2,zoom+1);
            y = (MaxY-MinY)/Math.pow(2,zoom);
        }
        if (OgcEpsg.getOgcEpsgCodeType(layerBaseInfo.getResourceSrs()) == OgcEpsg.MERCATOR_CODE){
			MinX = GeometryUtil.mercator2Lonlat(GeometryUtil.MERCATOR_UNITS_NEGITAVE,GeometryUtil.MERCATOR_UNITS_NEGITAVE).getX();
			MinY = GeometryUtil.mercator2Lonlat(GeometryUtil.MERCATOR_UNITS_NEGITAVE,GeometryUtil.MERCATOR_UNITS_NEGITAVE).getY();
			MaxX = GeometryUtil.mercator2Lonlat(GeometryUtil.MERCATOR_UNITS,GeometryUtil.MERCATOR_UNITS).getX();
			MaxY = GeometryUtil.mercator2Lonlat(GeometryUtil.MERCATOR_UNITS,GeometryUtil.MERCATOR_UNITS).getY();
			x = (MaxX-MinX)/Math.pow(2,zoom);
			y = (MaxY-MinY)/Math.pow(2,zoom);
        }
		if (lon >= MinX && lon <= MaxX){
			tileCol = (int) Math.floor( (lon - MinX) /x);
			tileX = tileCol * x + MinX;
			if (tileX == lon && tileX != MinX) {
				tileCol = tileCol -1;
				tileX = tileX - x;
                x_pixel = (int) Math.round(Math.abs(lon - tileX)/x * layerBaseInfo.getTileWidth())-1;
			}else {
				//计算的像素点个数转换为二维坐标要-1
                x_pixel = (int) Math.round(Math.abs(lon - tileX) / x * layerBaseInfo.getTileWidth())-1;
				//为第0个像素点时，计算为-1，要处理为0
				if (x_pixel < 0){
					x_pixel = 0;
				}
            }
		}
		if (lat >= MinY && lat <= MaxY){
			tileRow = (int) (Math.floor( ( MaxY - lat) / y)) ;
			tileY =  MaxY - ( (tileRow)  * y );
			if (tileY == lat && tileY != MaxY) {
				tileRow = tileRow - 1;
				tileY = tileY + y;
                y_pixel = (int) Math.round(Math.abs(tileY - lat)/y * layerBaseInfo.getTileHeight())-1;
			}else {
                y_pixel = (int) Math.round(Math.abs(tileY - lat) / y * layerBaseInfo.getTileHeight())-1;
                if (y_pixel < 0){
                	y_pixel = 0;
				}
            }
		}
        TilePixelEntity tilePixelEntity = new TilePixelEntity();
        tilePixelEntity.setTileRow(tileRow);
        tilePixelEntity.setTileCol(tileCol);
        tilePixelEntity.setTileX(tileX);
        tilePixelEntity.setTileY(tileY);
        tilePixelEntity.setX_pixel(x_pixel);
        tilePixelEntity.setY_pixel(y_pixel);
        tilePixelEntity.setX_singlePixel(x/layerBaseInfo.getTileWidth());
        tilePixelEntity.setY_singlePixel(y/layerBaseInfo.getTileHeight());
		tilePixelEntity.setMinX(MinX);
		tilePixelEntity.setMaxX(MaxX);
		tilePixelEntity.setMinY(MinY);
		tilePixelEntity.setMaxY(MaxY);
        tilePixelEntity.setZoom(zoom);
        return tilePixelEntity;
    }


    public double tmsAnalysisDemByPoint(LayerBaseInfo layerBaseInfo, String nativaBbox, int supportMaxLevel, double lon, double lat){
        String fileUrl = null;//实例测试:77.5  9.85  4/22/8.tif 右上角数据
        int zoom = supportMaxLevel;//递归缩放级别标志位
        float[] demArray = new float[1];
        TilePixelEntity pixelByTiled = getPixelByTiled(layerBaseInfo, nativaBbox, supportMaxLevel, lon, lat);
        pixelByTiled.setTileRow((int)(Math.pow(2, pixelByTiled.getZoom()) - pixelByTiled.getTileRow() - 1));
        fileUrl = layerBaseInfo.getFilePath()+zoom+File.separator+pixelByTiled.getTileCol()+File.separator+pixelByTiled.getTileRow()+".tif";
        try {
            File file = new File(fileUrl);
            if (file.exists()) {
                BufferedImage read = ImageIO.read(file);
                try {
					read.getRaster().getPixel(pixelByTiled.getX_pixel(), pixelByTiled.getY_pixel(), demArray);
				}catch (RuntimeException e){
//                	log.error("tmsAnalays has error: Coordinate is not band");
				}
            }else {
                zoom--;
            }
        }catch (IOException e) {
            log.error("tmsAnalays has error : File not Found "+fileUrl );
        }
        if (zoom!=supportMaxLevel && zoom>= 0){
            return tmsAnalysisDemByPoint(layerBaseInfo,nativaBbox,zoom,lon,lat);
        }else {
            return demArray[0];
        }
    }

    public double tdbAnalysisDemByPoint(LayerBaseInfo layerBaseInfo, String nativeBbox, int supportMaxLevel, double lon, double lat){
        int zoom = supportMaxLevel;
        TilePixelEntity pixelByTiled = getPixelByTiled(layerBaseInfo, nativeBbox, zoom, lon, lat);
        //通过方法获取此瓦片的byte[]
        CustomDataLoadImpl loadTdbData = new CustomDataLoadImpl();
		int x = (int)Math.pow(2, zoom) - pixelByTiled.getTileRow() -1;
		int y = pixelByTiled.getTileCol();
        try {
			byte[] tdbData = loadTdbData.getElvationInfo(x, y, zoom, layerBaseInfo.getFilePath(), layerBaseInfo.getLayerName());
			if (tdbData == null && zoom > 0) {
				zoom--;
				return tdbAnalysisDemByPoint(layerBaseInfo, nativeBbox, zoom, lon, lat);
			} else {
				int n = 0;
				float[] dataArr = new float[layerBaseInfo.getTileWidth() * layerBaseInfo.getTileHeight()];
				if (tdbData.length != (layerBaseInfo.getTileWidth() * layerBaseInfo.getTileHeight() * 4)){
					//处理服务切图tiff的etdb数据解析后剥除tiff头问题
					BufferedImage read = null;
					try {
						read = ImageIO.read(new ByteArrayInputStream(tdbData));
						read.getSampleModel().getPixels(0,0,layerBaseInfo.getTileWidth(),layerBaseInfo.getTileHeight(),dataArr,read.getData().getDataBuffer());
					} catch (IOException e) {
						log.error("tdbAnalysisDemByPoint has error :" + e.getLocalizedMessage());
					}
				}else {
					//byte数组转float数组
					for (int i = 0; i < tdbData.length; i += 4) {
						byte[] demBuf = Arrays.copyOfRange(tdbData, i, (n + 1) * 4);
						float demValue = CommonUtil.bytes2float(demBuf);
						dataArr[n] = demValue;
						n++;
					}
				}
				//通过数组索引获取对应像素点的高程值
				int index = layerBaseInfo.getTileHeight() * pixelByTiled.getY_pixel() + pixelByTiled.getX_pixel();
//				//优化 查询单点不需要转换整个数组
//				byte[] demBuf = Arrays.copyOfRange(tdbData,index * 4,(index+1)*4);
//				float demValue = CommonUtil.bytes2float(demBuf);
				return dataArr[index];
			}
		}catch (NullPointerException e){
        	log.error("TDB AnalysisDemByPoint has an error: Point Tile is not exist!");
        	return 0;
		}
    }

    public double dealEspecialPixelPoint(LayerBaseInfo layerBaseInfo,String nativaBbox,TilePixelEntity pixelByTiled,double lon,double lat,boolean isSlope){
		double x_singlePixel = pixelByTiled.getX_singlePixel();
		double y_singlePixel = pixelByTiled.getY_singlePixel();
		double minX = pixelByTiled.getMinX();
		double maxX = pixelByTiled.getMaxX();
    	double minY = pixelByTiled.getMinY();
    	double maxY = pixelByTiled.getMaxY();
		double dx = 0;
		double dy = 0;
		if (lon == minX && lat == maxY){
		    //向下与向右的变化
			Coordinate[] coordinates = new Coordinate[3];
			coordinates[0] = new Coordinate(lon + x_singlePixel,lat);
			coordinates[1] = new Coordinate(lon,lat);
			coordinates[2] = new Coordinate(lon,lat - y_singlePixel);
			double[] coordinateTileDem = getCoordinateTileDemValues(layerBaseInfo, nativaBbox, coordinates);
			dx = (coordinateTileDem[1] - coordinateTileDem[0])/x_singlePixel;
			dy = (coordinateTileDem[2] - coordinateTileDem[1])/y_singlePixel;
		}else if (lon == minX && lat == minY){
		    //向上与向右的变化
			Coordinate[] coordinates = new Coordinate[3];
			coordinates[0] = new Coordinate(lon + x_singlePixel,lat);
			coordinates[1] = new Coordinate(lon,lat);
			coordinates[2] = new Coordinate(lon,lat + y_singlePixel);
			double[] coordinateTileDem = getCoordinateTileDemValues(layerBaseInfo, nativaBbox, coordinates);
			dx = (coordinateTileDem[1] - coordinateTileDem[0])/x_singlePixel;
			dy = (coordinateTileDem[1]-coordinateTileDem[2])/y_singlePixel;
		}else if (lon == maxX && lat == minY){
		    //向上与向左的变化
			Coordinate[] coordinates = new Coordinate[3];
			coordinates[0] = new Coordinate(lon - x_singlePixel,lat);
			coordinates[1] = new Coordinate(lon,lat);
			coordinates[2] = new Coordinate(lon,lat + y_singlePixel);
			double[] coordinateTileDem = getCoordinateTileDemValues(layerBaseInfo, nativaBbox, coordinates);
			dx = (coordinateTileDem[0] - coordinateTileDem[1])/x_singlePixel;
			dy = (coordinateTileDem[1]-coordinateTileDem[2])/y_singlePixel;
		}else if (lon == maxX && lat == maxY){
		    //向下与向左的变化
			Coordinate[] coordinates = new Coordinate[3];
			coordinates[0] = new Coordinate(lon - x_singlePixel,lat);
			coordinates[1] = new Coordinate(lon,lat);
			coordinates[2] = new Coordinate(lon,lat - y_singlePixel);
			double[] coordinateTileDem = getCoordinateTileDemValues(layerBaseInfo, nativaBbox, coordinates);
			dx = (coordinateTileDem[0] - coordinateTileDem[1])/x_singlePixel;
			dy = (coordinateTileDem[2]-coordinateTileDem[1])/y_singlePixel;
		}else if (lon == minX && lat != maxY && lat != minY){
            //向上与向下以及向右的变化
            Coordinate[] coordinates = new Coordinate[4];
            coordinates[0] = new Coordinate(lon + x_singlePixel,lat);
            coordinates[1] = new Coordinate(lon,lat);
            coordinates[2] = new Coordinate(lon,lat + y_singlePixel);
            coordinates[3] = new Coordinate(lon,lat - y_singlePixel);
            double[] coordinateTileDem = getCoordinateTileDemValues(layerBaseInfo, nativaBbox, coordinates);
            dx = (coordinateTileDem[1]-coordinateTileDem[0])/x_singlePixel;
            dy = (coordinateTileDem[2]-coordinateTileDem[3])/(2 * y_singlePixel);
		}else if (lon == maxX && lat != maxY && lat != minY){
            //向上与向下以及向左的变化
            Coordinate[] coordinates = new Coordinate[4];
            coordinates[0] = new Coordinate(lon - x_singlePixel,lat);
            coordinates[1] = new Coordinate(lon,lat);
            coordinates[2] = new Coordinate(lon,lat + y_singlePixel);
            coordinates[3] = new Coordinate(lon,lat - y_singlePixel);
            double[] coordinateTileDem = getCoordinateTileDemValues(layerBaseInfo, nativaBbox, coordinates);
            dx = (coordinateTileDem[0]-coordinateTileDem[1])/x_singlePixel;
            dy = (coordinateTileDem[2]-coordinateTileDem[3])/(2 * y_singlePixel);
		}else if (lat == minY && lon != maxX && lon != minX){
            //向上以及向左与向右的变化
            Coordinate[] coordinates = new Coordinate[4];
            coordinates[0] = new Coordinate(lon - x_singlePixel,lat);
            coordinates[1] = new Coordinate(lon,lat);
            coordinates[2] = new Coordinate(lon + x_singlePixel,lat);
            coordinates[3] = new Coordinate(lon,lat + y_singlePixel);
            double[] coordinateTileDem = getCoordinateTileDemValues(layerBaseInfo, nativaBbox, coordinates);
            dx = (coordinateTileDem[0]-coordinateTileDem[2])/(2 * x_singlePixel);
            dy = (coordinateTileDem[1]-coordinateTileDem[3])/y_singlePixel;
		}else if (lat == maxY && lon != maxX && lon != minX){
            //向下以及向左与向右的变化
            Coordinate[] coordinates = new Coordinate[4];
            coordinates[0] = new Coordinate(lon - x_singlePixel,lat);
            coordinates[1] = new Coordinate(lon,lat);
            coordinates[2] = new Coordinate(lon + x_singlePixel,lat);
            coordinates[3] = new Coordinate(lon,lat - y_singlePixel);
            double[] coordinateTileDem = getCoordinateTileDemValues(layerBaseInfo, nativaBbox, coordinates);
            dx = (coordinateTileDem[0]-coordinateTileDem[2])/(2 * x_singlePixel);
            dy = (coordinateTileDem[3]-coordinateTileDem[1])/y_singlePixel;
		}else {
		    return Double.NaN;
        }

        if (isSlope) {
            return Math.atan(Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2))) * (180 / GeometryUtil.PI);
        } else {
            if (dx == 0 && dy == 0) {
                return -1;
            }
            return Math.atan2(dy, dx) / GeometryUtil.OGC_DEGREE_TO_METERS;
        }
	}

    public double tmsAnalysisSlopeByPoint(LayerBaseInfo layerBaseInfo,String nativaBbox,int supportMaxLevel,double lon,double lat){
        TilePixelEntity pixelByTiled = getPixelByTiled(layerBaseInfo, nativaBbox, supportMaxLevel, lon, lat);
        double x_singlePixel = pixelByTiled.getX_singlePixel();
        double y_singlePixel = pixelByTiled.getY_singlePixel();
        if (lon == pixelByTiled.getMaxX() || lon == pixelByTiled.getMinX() || lat == pixelByTiled.getMinY() || lat == pixelByTiled.getMaxY()){
            return dealEspecialPixelPoint(layerBaseInfo,nativaBbox,pixelByTiled,lon,lat,true);
        }else {
            double[] lonArr = new double[3];
            double[] latArr = new double[3];
            float[] floatArray = new float[9];
            for (int i = -1; i < 2; i++) {
                lonArr[i + 1] = lon + (x_singlePixel * i);
                latArr[i + 1] = lat + (y_singlePixel * i);
            }
            int n = 0;
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    floatArray[n] = (float) tmsAnalysisDemByPoint(layerBaseInfo, nativaBbox, supportMaxLevel, lonArr[j], latArr[k]);
                    n++;
                }
            }
            return calcutePointSlope(floatArray, x_singlePixel, y_singlePixel);
        }
    }

    public double tdbAnalysisSlopeByPoint(LayerBaseInfo layerBaseInfo,String nativaBbox,int supportMaxLevel,double lon,double lat){
        TilePixelEntity pixelByTiled = getPixelByTiled(layerBaseInfo, nativaBbox, supportMaxLevel, lon, lat);
        double x_singlePixel = pixelByTiled.getX_singlePixel();
        double y_singlePixel = pixelByTiled.getY_singlePixel();
        if (lon == pixelByTiled.getMaxX() || lon == pixelByTiled.getMinX() || lat == pixelByTiled.getMinY() || lat == pixelByTiled.getMaxY()){
            return dealEspecialPixelPoint(layerBaseInfo,nativaBbox,pixelByTiled,lon,lat,true);
        }else {
            double[] lonArr = new double[3];
            double[] latArr = new double[3];
            float[] floatArray = new float[9];
            for (int i = -1; i < 2; i++) {
                lonArr[i + 1] = lon + (x_singlePixel * i);
                latArr[i + 1] = lat + (y_singlePixel * i);
            }
            int n = 0;
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    floatArray[n] = (float) tdbAnalysisDemByPoint(layerBaseInfo, nativaBbox, supportMaxLevel, lonArr[j], latArr[k]);
                    n++;
                }
            }
            return calcutePointSlope(floatArray, x_singlePixel, y_singlePixel);
        }
    }

    public double tmsAnalysisAspectByPoint(LayerBaseInfo layerBaseInfo,String nativaBbox,int supportMaxLevel,double lon,double lat){
        TilePixelEntity pixelByTiled = getPixelByTiled(layerBaseInfo, nativaBbox, supportMaxLevel, lon, lat);
        double x_singlePixel = pixelByTiled.getX_singlePixel();
        double y_singlePixel = pixelByTiled.getY_singlePixel();
        if (lon == pixelByTiled.getMaxX() || lon == pixelByTiled.getMinX() || lat == pixelByTiled.getMinY() || lat == pixelByTiled.getMaxY()){
            return dealEspecialPixelPoint(layerBaseInfo,nativaBbox,pixelByTiled,lon,lat,false);
        }else {
            double[] lonArr = new double[3];
            double[] latArr = new double[3];
            float[] floatArray = new float[9];
            for (int i = -1; i < 2; i++) {
                lonArr[i + 1] = lon + (x_singlePixel * i);
                latArr[i + 1] = lat + (y_singlePixel * i);
            }
            int n = 0;
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    floatArray[n] = (float) tmsAnalysisDemByPoint(layerBaseInfo, nativaBbox, supportMaxLevel, lonArr[j], latArr[k]);
                    n++;
                }
            }
            double aspect = calcutePointAspect(floatArray, x_singlePixel, y_singlePixel);
            // 如果用方位角表示
            if (aspect > 90.0) {
                aspect = 450.0d - aspect;
            } else {
                aspect = 90.0d - aspect;
            }
            // 不用方位角表示
            // if (aspect < 0) {
            // aspect+=360.0d;
            // }
            if (aspect == 360.0) {
                aspect = 0.0;
            }
            return aspect;
        }
    }

    public double tdbAnalysisAspectByPoint(LayerBaseInfo layerBaseInfo,String nativaBbox,int supportMaxLevel,double lon,double lat){
        TilePixelEntity pixelByTiled = getPixelByTiled(layerBaseInfo, nativaBbox, supportMaxLevel, lon, lat);
        double x_singlePixel = pixelByTiled.getX_singlePixel();
        double y_singlePixel = pixelByTiled.getY_singlePixel();
        if (lon == pixelByTiled.getMaxX() || lon == pixelByTiled.getMinX() || lat == pixelByTiled.getMinY() || lat == pixelByTiled.getMaxY()){
            return dealEspecialPixelPoint(layerBaseInfo,nativaBbox,pixelByTiled,lon,lat,false);
        }else {
            double[] lonArr = new double[3];
            double[] latArr = new double[3];
            float[] floatArray = new float[9];
            for (int i = -1; i < 2; i++) {
                lonArr[i + 1] = lon + (x_singlePixel * i);
                latArr[i + 1] = lat + (y_singlePixel * i);
            }
            int n = 0;
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    floatArray[n] = (float) tdbAnalysisDemByPoint(layerBaseInfo, nativaBbox, supportMaxLevel, lonArr[j], latArr[k]);
                    n++;
                }
            }
            double aspect = calcutePointAspect(floatArray, x_singlePixel, y_singlePixel);
            // 如果用方位角表示
            if (aspect > 90.0) {
                aspect = 450.0d - aspect;
            } else {
                aspect = 90.0d - aspect;
            }
            // 不用方位角表示
            // if (aspect < 0) {
            // aspect+=360.0d;
            // }
            if (aspect == 360.0) {
                aspect = 0.0;
            }
            return aspect;
        }
    }

	@Override
	public void getTileDemByLinePoint(LayerBaseInfo layerBaseInfo,String nativeBbox,String startPos,String endPos,double stride,AnalysisResult analysisResult) {
		Coordinate[] coordinates = GeometryUtil.calcuteCoordinates(startPos, endPos, stride);
		analysisResult.setCoordinates(coordinates);
		double[] demArray = getCoordinateTileDemValues(layerBaseInfo, nativeBbox, coordinates);
		analysisResult.setDemArray(demArray);
	}

	@Override
	public void getTileDemByLinePoint(LayerBaseInfo layerBaseInfo, String nativeBbox, String startPos, String endPos, int sampingNum, AnalysisResult analysisResult) {
		// 采样点数包括起点和终点坐标，如果采样一个点，则实际点数是3，如果采样5个点，则实际点数为7
		String[] startPointArray = startPos.split(",");
		String[] endPointArray = endPos.split(",");
		double startLon = Double.parseDouble(startPointArray[0]);
		double startLat = Double.parseDouble(startPointArray[1]);
		double endLon = Double.parseDouble(endPointArray[0]);
		double endLat = Double.parseDouble(endPointArray[1]);
		double rationX = (endLon - startLon) / (sampingNum + 1);
		double rationY = (endLat - startLat) / (sampingNum + 1);
		Coordinate[] coordinates = new Coordinate[sampingNum + 2];
		for (int i = 0; i < sampingNum + 2; i++) {
			coordinates[i] = new Coordinate(startLon + rationX * i, startLat + rationY * i);
		}
		analysisResult.setCoordinates(coordinates);
		double[] demArray = getCoordinateTileDemValues(layerBaseInfo, nativeBbox, coordinates);
		analysisResult.setDemArray(demArray);
	}

	@Override
	public void getTileDemBySampingNum(LayerBaseInfo layerBaseInfo, String nativeBbox, String leftUpperPos, String rightLowerPos,
									   int sampleXCount, int sampleYCount, AnalysisResult analysis) {
		Coordinate[] coordinates = getValidCoordinates(nativeBbox, leftUpperPos, rightLowerPos, sampleXCount,
				sampleYCount);
		analysis.setCoordinates(coordinates);
		double[] demArray = getCoordinateTileDemValues(layerBaseInfo,nativeBbox,coordinates);
		analysis.setDemArray(demArray);
		analysis.setImageWidth(sampleXCount + 2);
		analysis.setImageHeight(sampleYCount + 2);
	}

	@Override
	public void getTileDemBySampingRatio(LayerBaseInfo layerBaseInfo, String nativeBbox, String leftUpperPos, String rightLowerPos,
									   double strideX, double strideY, AnalysisResult analysis) {
		Coordinate[] coordinates = calculteCoordinates(nativeBbox, leftUpperPos, rightLowerPos, strideX, strideY,
				analysis);
		analysis.setCoordinates(coordinates);
		double[] demArray = getCoordinateTileDemValues(layerBaseInfo,nativeBbox,coordinates);
		analysis.setDemArray(demArray);
	}



	@Override
	public void getTileSlopeByLinePoint(LayerBaseInfo layerBaseInfo,String nativeBbox, String startPos,String endPos,int sampingNum,AnalysisResult analysisResult){
		String[] startPointArray = startPos.split(",");
		String[] endPointArray = endPos.split(",");
		double startLon = Double.parseDouble(startPointArray[0]);
		double startLat = Double.parseDouble(startPointArray[1]);
		double endLon = Double.parseDouble(endPointArray[0]);
		double endLat = Double.parseDouble(endPointArray[1]);
		double rationX = (endLon - startLon) / (sampingNum + 1);
		double rationY = (endLat - startLat) / (sampingNum + 1);
		Coordinate[] coordinates = new Coordinate[sampingNum + 2];
		for (int i = 0; i < sampingNum + 2; i++) {
			coordinates[i] = new Coordinate(startLon + rationX * i, startLat + rationY * i);
		}
		analysisResult.setCoordinates(coordinates);
		double[] slopeArray = getCoordinateTileSlopeValues(layerBaseInfo, nativeBbox, coordinates);
		analysisResult.setDemArray(slopeArray);
	}

	@Override
	public void getTileSlopeByLinePoint(LayerBaseInfo layerBaseInfo,String nativeBbox, String startPos,String endPos,double stride,AnalysisResult analysisResult){
		Coordinate[] coordinates = GeometryUtil.calcuteCoordinates(startPos, endPos, stride);
		analysisResult.setCoordinates(coordinates);
		double[] slopeArray = getCoordinateTileSlopeValues(layerBaseInfo, nativeBbox, coordinates);
		analysisResult.setDemArray(slopeArray);
	}

	@Override
	public void getTileSlopeBySampingNum(LayerBaseInfo layerBaseInfo,String nativeBbox,String leftUpperPos,String rightLowerPos,int sampleXCount,int sampleYCount,AnalysisResult analysisResult){
		Coordinate[] coordinates = getValidCoordinates(nativeBbox, leftUpperPos, rightLowerPos, sampleXCount,
				sampleYCount);
		analysisResult.setCoordinates(coordinates);
		double[] slopeArray = getCoordinateTileSlopeValues(layerBaseInfo, nativeBbox, coordinates);
		analysisResult.setDemArray(slopeArray);
		analysisResult.setImageWidth(sampleXCount + 2);
		analysisResult.setImageHeight(sampleYCount + 2);
	}

	@Override
	public void getTileSlopeBySampingRatio(LayerBaseInfo layerBaseInfo,String nativeBbox,String leftUpperPos,String rightLowerPos,double strideX,double strideY,AnalysisResult analysisResult){
		Coordinate[] coordinates = calculteCoordinates(nativeBbox, leftUpperPos, rightLowerPos, strideX, strideY,
				analysisResult);
		analysisResult.setCoordinates(coordinates);
		double[] slopeArray = getCoordinateTileSlopeValues(layerBaseInfo, nativeBbox, coordinates);
		analysisResult.setDemArray(slopeArray);
	}

	public double[] getCoordinateTileSlopeValues(LayerBaseInfo layerBaseInfo, String nativeBbox, Coordinate[] coordinates) {
		double[] slopeArray = new double[coordinates.length];
		int index = 0;
		double value = 0.0;
		if (layerBaseInfo.getFileType().equals("TMS")) {
			for (Coordinate coordinate2 : coordinates) {
				try {
					value = tmsAnalysisSlopeByPoint(layerBaseInfo, nativeBbox, layerBaseInfo.getSupportMaxLevel(), coordinate2.getX(), coordinate2.getY());
					slopeArray[index] = value;
					index++;
				} catch (Exception e) {
					log.error("MapLayerAnalysisImpl getCoordinateTileSlopeValues has error:" + e.getLocalizedMessage());
				}
			}
		}
		if (layerBaseInfo.getFileType().equals("TDB")){
			for (Coordinate coordinate2 : coordinates) {
				try {
					value = tdbAnalysisSlopeByPoint(layerBaseInfo, nativeBbox, layerBaseInfo.getSupportMaxLevel(), coordinate2.getX(), coordinate2.getY());
					slopeArray[index] = value;
					index++;
				} catch (Exception e) {
					log.error("MapLayerAnalysisImpl getCoordinateTileSlopeValues has error:" + e.getLocalizedMessage());
				}
			}
		}
		return slopeArray;
	}

	@Override
	public void getTileAspectByLinePoint(LayerBaseInfo layerBaseInfo,String nativeBbox, String startPos,String endPos,int sampingNum,AnalysisResult analysisResult){
		String[] startPointArray = startPos.split(",");
		String[] endPointArray = endPos.split(",");
		double startLon = Double.parseDouble(startPointArray[0]);
		double startLat = Double.parseDouble(startPointArray[1]);
		double endLon = Double.parseDouble(endPointArray[0]);
		double endLat = Double.parseDouble(endPointArray[1]);
		double rationX = (endLon - startLon) / (sampingNum + 1);
		double rationY = (endLat - startLat) / (sampingNum + 1);
		Coordinate[] coordinates = new Coordinate[sampingNum + 2];
		for (int i = 0; i < sampingNum + 2; i++) {
			coordinates[i] = new Coordinate(startLon + rationX * i, startLat + rationY * i);
		}
		analysisResult.setCoordinates(coordinates);
		double[] aspectArray = getCoordinateTileAspectValues(layerBaseInfo, nativeBbox, coordinates);
		analysisResult.setDemArray(aspectArray);
	}

	@Override
	public void getTileAspectByLinePoint(LayerBaseInfo layerBaseInfo,String nativeBbox,String startPos,String endPos,double stride,AnalysisResult analysisResult){
		Coordinate[] coordinates = GeometryUtil.calcuteCoordinates(startPos, endPos, stride);
		analysisResult.setCoordinates(coordinates);
		double[] aspectArray = getCoordinateTileAspectValues(layerBaseInfo, nativeBbox, coordinates);
		analysisResult.setDemArray(aspectArray);
	}

	@Override
	public void getTileAspectBySampingNum(LayerBaseInfo layerBaseInfo,String nativeBbox,String leftUpperPos,String rightLowerPos,int sampleXCount,int sampleYCount,AnalysisResult analysisResult){
		Coordinate[] coordinates = getValidCoordinates(nativeBbox, leftUpperPos, rightLowerPos, sampleXCount,
				sampleYCount);
		analysisResult.setCoordinates(coordinates);
		double[] aspectArray = getCoordinateTileAspectValues(layerBaseInfo, nativeBbox, coordinates);
		analysisResult.setDemArray(aspectArray);
		analysisResult.setImageWidth(sampleXCount + 2);
		analysisResult.setImageHeight(sampleYCount + 2);
	}

	@Override
	public void getTileAspectBySampingRatio(LayerBaseInfo layerBaseInfo,String nativeBbox,String leftUpperPos,String rightLowerPos,double strideX,double strideY,AnalysisResult analysisResult){
		Coordinate[] coordinates = calculteCoordinates(nativeBbox, leftUpperPos, rightLowerPos, strideX, strideY,
				analysisResult);
		analysisResult.setCoordinates(coordinates);
		double[] aspectArray = getCoordinateTileAspectValues(layerBaseInfo, nativeBbox, coordinates);
		analysisResult.setDemArray(aspectArray);
	}

	public double[] getCoordinateTileAspectValues(LayerBaseInfo layerBaseInfo, String nativeBbox, Coordinate[] coordinates) {
		double[] aspectArray = new double[coordinates.length];
		int index = 0;
		double value = 0.0;
		if (layerBaseInfo.getFileType().equals("TMS")) {
			for (Coordinate coordinate2 : coordinates) {
				try {
					value = tmsAnalysisAspectByPoint(layerBaseInfo, nativeBbox, layerBaseInfo.getSupportMaxLevel(), coordinate2.getX(), coordinate2.getY());
					aspectArray[index] = value;
					index++;
				} catch (Exception e) {
					log.error("MapLayerAnalysisImpl getCoordinateTileAspectValues has error:" + e.getLocalizedMessage());
				}
			}
		}
		if (layerBaseInfo.getFileType().equals("TDB")){
			for (Coordinate coordinate2 : coordinates) {
				try {
					value = tdbAnalysisAspectByPoint(layerBaseInfo, nativeBbox, layerBaseInfo.getSupportMaxLevel(), coordinate2.getX(), coordinate2.getY());
					aspectArray[index] = value;
					index++;
				} catch (Exception e) {
					log.error("MapLayerAnalysisImpl getCoordinateTileAspectValues has error:" + e.getLocalizedMessage());
				}
			}
		}
		return aspectArray;
	}


}
