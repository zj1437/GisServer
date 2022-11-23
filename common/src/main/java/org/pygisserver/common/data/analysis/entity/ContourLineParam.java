package org.pygisserver.common.data.analysis.entity;

import lombok.Data;

@Data
public class ContourLineParam {

	private String layerName;
	
	private String fileUrl;
	
	/**
	 * 等高线间距
	 */
	private int interval;
	
	/**
	 *  起始高程值（如果不指定则默认0）
	 */
	private double baseConter;
	
	/**
	 * 结果是否抽稀
	 */
	private boolean isSimplify;
	
	/**
	 * 是否平滑处理
	 */
	private boolean isSmooth;
	
	/**
	 * 是否自动发布
	 */
	private boolean autoPublish;
	
	/**
	 * 输出路径
	 */
	private String outPutLocation;
}
