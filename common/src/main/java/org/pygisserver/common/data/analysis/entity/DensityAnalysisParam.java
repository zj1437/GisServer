package org.pygisserver.common.data.analysis.entity;

import lombok.Data;

@Data
public class DensityAnalysisParam {

	/**
	 * 图层名称
	 */
	private String layerName;
	
	/**
	 * 文件url
	 */
	private String fileUrl;
	
	/**
	 * 输出路径
	 */
	private String outPutPath;
	
	/**
	 * 核密度半径
	 */
	private double radiusPixels;
	
	/**
	 * 权重名称(默认NONE)
	 */
	private String weightName = "None";
	
	/**
	 * 生成的像元大小
	 */
	private double ceilPixel;
	
	/**
	 * 是否应用于该图层（自动发布 1发布 0 不发布）
	 */
	private int autoPublish;
}
