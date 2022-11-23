package org.pygisserver.web.service;

import java.util.Map;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * @Description: 获取当前数据信息
 * @author: 
 * @date: 2019-07-17
 */
public interface ReadDataInfo {

	/**
	 * 获取bbox
	 * @return
	 */
	ReferencedEnvelope getBboxFromFile();
	
	/**
	 * 获取图片宽度
	 * @return
	 */
	int getImgWidth();
	
	/**
	 * 获取图片高度
	 * @return
	 */
	int getImgHeight();
	
	/**
	 * 获取元数据的crs
	 * @return
	 */
	CoordinateReferenceSystem getResourceSrs();
	
	/**
	 * 数据类型
	 * @return
	 */
	int getDataType();
	
	/**
	 * 获取特征信息总数
	 * @return
	 */
	int featuresSize();
	
	/**
	 * 瓦片数据扩展名
	 * @return
	 */
	String getTileExtension();
	
	int getMaxLevel(ReferencedEnvelope originalEnvelope);
	
	/**
	 * 
	 * @return
	 */
	Map<Object,Object> scaleMap();
	
	/**
	 * 返回服务标识符，只用于海图服务
	 * @return
	 */
	String getSeaChartIdentifier();
	
	/**
	 * 
	 * @return
	 */
	int getTileOriginFlag();
}
