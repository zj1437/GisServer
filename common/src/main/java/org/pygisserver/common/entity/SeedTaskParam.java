package org.pygisserver.common.entity;

import lombok.Data;

/**
 * @Description: 切图任务设置参数
 * @author: 
 * @date: 2021-04-01
 */
@Data
public class SeedTaskParam {

	/**
	 * 存储类型
	 */
	private int storeType;
	
	/**
	 * 操作类型 seed 切图  reseed 重新切图  
	 */
	private String type;
	
	/**
	 * 针对sqlite数据库
	 */
	private String storePath;
	
	/**
	 * 切图策略名称
	 */
	private String gridSetName;
	
	/**
	 * 影像格式
	 */
	private String rasterFormat;
	
	/**
	 * 高程格式
	 */
	private String demFormat;
	
	/**
	 * 矢量格式
	 */
	private String vectorFormat;
	
	/**
	 * 瓦片宽度
	 */
	private int tileWidth;
	
	/**
	 * 瓦片高度
	 */
	private int tileHeight;
	
	/**
	 * 最大切片级别
	 */
	private int defaultMaxZoomLevel;
	
	/**
	 * 最大任务数
	 */
	private int maxTaskNum;
	
	/**
	 * 线程数
	 */
	private int threadCount;
	
	/**
	 * 导出文件路径
	 */
	private String exportPath;
}
