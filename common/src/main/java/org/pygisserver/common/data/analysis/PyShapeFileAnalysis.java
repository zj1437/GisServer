package org.pygisserver.common.data.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;

import org.pygisserver.common.enumm.ErrorCode;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.util.GeometryUtil;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 矢量数据解析 
 * @author: 刘洋 
 * @date: 2022-05-16
 */
@Slf4j
public class PyShapeFileAnalysis {

	private String vectorFile;
	
	private SimpleFeatureSource simpleFeatureSource;
	
	public PyShapeFileAnalysis(String vectorFile) {
		this.vectorFile = vectorFile;
		simpleFeatureSource();
	}
	
	public PyShapeFileAnalysis(String vectorFile,String charSet) {
		this.vectorFile = vectorFile;
		simpleFeatureSource(charSet);
	}
	
	public SimpleFeatureSource simpleFeatureSource() {
		if (simpleFeatureSource != null) {
			return simpleFeatureSource;
		}
		File file = new File(vectorFile);
		try {
			ShapefileDataStore shapefileDataStore = new ShapefileDataStore(file.toURI().toURL());
			this.simpleFeatureSource = shapefileDataStore.getFeatureSource();
			return simpleFeatureSource;
		} catch (IOException e) {
			log.error("ShapFileAnalysis simpleFeatureSource has error {}",e.getLocalizedMessage());
			throw new BaseException(ErrorCode.RUNTIME_EXCEPTION.getErrorCode(),
					ExceptionsUtil.formatInfo(ExceptionsUtil.ANALYSIS_SHAPE_FILE, file.getName()));
		}
	}
	
	public SimpleFeatureSource simpleFeatureSource(String charSet) {
		if (simpleFeatureSource != null) {
			return simpleFeatureSource;
		}
		File file = new File(vectorFile);
		try {
			ShapefileDataStore shapefileDataStore = new ShapefileDataStore(file.toURI().toURL());
			shapefileDataStore.setCharset(Charset.forName(charSet));
			this.simpleFeatureSource = shapefileDataStore.getFeatureSource();
			return simpleFeatureSource;
		} catch (IOException e) {
			log.error("ShapFileAnalysis simpleFeatureSource has error {}",e.getLocalizedMessage());
			throw new BaseException(ErrorCode.RUNTIME_EXCEPTION.getErrorCode(), 
					ExceptionsUtil.formatInfo(ExceptionsUtil.ANALYSIS_SHAPE_FILE, file.getName()));
		}
	}
	
	public ReferencedEnvelope getEnvelope() {
		try {
			ReferencedEnvelope resourceCrs = simpleFeatureSource.getBounds();
			CoordinateReferenceSystem coordinateReferenceSystem = resourceCrs.getCoordinateReferenceSystem();
			Set<ReferenceIdentifier> identifiers = coordinateReferenceSystem.getIdentifiers();
			if (identifiers == null || identifiers.isEmpty()){
				try {
					//处理从ShapefileDataStore中获取不到投影导致图层组预览接口失败问题(实际为数据问题),为空默认设置为4326
					Integer epsgCode = CRS.lookupEpsgCode(coordinateReferenceSystem, false);
					String srs = null;
					if (epsgCode == null){
						srs = "EPSG:4326";
					}else {
						srs = "EPSG:" + epsgCode;
					}
					resourceCrs = GeometryUtil.transformCoordinateSystem(resourceCrs,srs);
				} catch (FactoryException e) {
					log.error("FeShapeFileAnalysis getEnvelope has error : " + e.getLocalizedMessage());
				}
			}
			return resourceCrs;
		} catch (IOException e) {
			log.error("ShapFileAnalysis getEnvelope has error {}",e.getLocalizedMessage());
			throw new BaseException(ErrorCode.RUNTIME_EXCEPTION.getErrorCode(),"获取bbox失败");
		}
	}
	
	/**
	 * 获取几何类型
	 * @return
	 */
	public GeometryType geometryType() {
		return simpleFeatureSource.getSchema().getGeometryDescriptor().getType();
	}
	
	/**
	 * 
	 * @return
	 */
	public List<AttributeDescriptor> getAttrbutesName(){
		return simpleFeatureSource.getSchema().getAttributeDescriptors();
	}
	
	/**
	 * 获取要素数量
	 * @return
	 */
	public int featureSizes() {
		try {
			return simpleFeatureSource.getFeatures().size();
		} catch (IOException e) {
			log.error("ShapFileAnalysis featureSizes has error {}",e.getLocalizedMessage());
			throw new BaseException(ErrorCode.RUNTIME_EXCEPTION.getErrorCode(),"获取矢量要素数量");
		}
	}
	
	public String getSrs() {
		try {
			return getSrs(simpleFeatureSource.getSchema().getCoordinateReferenceSystem());
		} catch (FactoryException e) {
			log.error("FeShapeFileAnalysis getSrs has error {}",e.getMessage());
		}
		return null;
	}
	
	
	/**
	 * 如果有EPSG就记录，没有可以不记录
	 * 
	 * @param resourceCrs
	 * @return String
	 * @throws FactoryException
	 */
	private String getSrs(CoordinateReferenceSystem resourceCrs) throws FactoryException  {
		// 如果proj中不自带srs，则查询获取
		Integer epsgCode = CRS.lookupEpsgCode(resourceCrs, false);
		if (epsgCode == null) {
			return "EPSG:4326";
		} else {
			return "EPSG:" + epsgCode;
		}
	}
	/**
	 * TODO 增加根据参数的过滤查询，自定义过滤查询对象，
	 * 将自定义对象转为geotools的query和fiter
	 * 来处理
	 */
	
}
