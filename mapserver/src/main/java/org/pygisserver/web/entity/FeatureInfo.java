package org.pygisserver.web.entity;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import lombok.Data;

@Data
public class FeatureInfo {

	/**
	 * 特征信息id
	 */
	private int featureId;
	
	/**
	 * 当前服务id
	 */
	private int serverId;

	/**
	 * 原始数据地理坐标系
	 */
	private String resourceCrsWkt;
	
	/**
	 * 原始数据投影坐标srs
	 */
	private String resourceSrs;
	
	/**
	 * 本地投影坐标 默认WGS84 EPSG:4326
	 */
	private String  nativeSrs;
	
	/**
	 * 投影策略(默认declare，也就是强制使用声明的投影策略)
	 */
	private String projectionPolicy;
	
	/**
	 * 元数据转换到本地投影下的经纬度
	 */
	private String nativeLatLon;
	
	/**
	 * 元数据的bbox
	 */
	private String resourceBbox;
	
	/**
	 * 最大级别
	 */
	private int zoomLevel;
	
	/**
	 * 矢量格式  0 是点 1是线 2是面
	 */
	private int dataType; 
	
	/**
	 * 特征数据总数
	 */
	private int totalFeatures;
	
	/**
	 * 瓦片宽度
	 */
	private int tileWidth;
	
	/**
	 * 瓦片高度
	 */
	private int tileHeight;
	
	/**
	 * 瓦片扩展名-png或jpeg等
	 */
	private String tileExtension;
	
	/**
	 * 海图参数设置json
	 */
	private String seaChartSettingJson;
	
	/**
	 * 海图服务标识符
	 */
	private String seaChartIdentifier;
	
	/**
	 * 海图比例尺
	 */
	private String seaChartScale;

	/**
	 * 瓦片的起始方向
	 */
	private int origin;
	
	/**
	 * 输入透明色
	 */
	private String inputTransParentColor;
	
	/**
	 * 输出透明色
	 */
	private String outPutTransParentColor;
}
