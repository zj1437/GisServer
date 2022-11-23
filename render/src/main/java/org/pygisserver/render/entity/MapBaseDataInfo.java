package org.pygisserver.render.entity;

import lombok.Data;

/**
 * @Description: 地图基础数据 
 * @author:  
 * @date: 2021-03-16
 */
@Data
public class MapBaseDataInfo {

	private String fileUrl;
	
	/**
	 * 图层名称
	 */
	private String layerName;
	
	/**
	 * 数据格式（比如  shp,tiff ）
	 */
	private String dataFormat;
	
	/**
	 * 数据类型(栅格、矢量、瓦片)
	 */
	private int dataType;
	
	/**
	 * 样式名称
	 */
	private String sldName;
	
	/**
	 * 样式文件
	 */
	private String sldFileUrl;
	
	/**
	 * 是否为图层组
	 */
	private boolean isLayerGroup;
}
