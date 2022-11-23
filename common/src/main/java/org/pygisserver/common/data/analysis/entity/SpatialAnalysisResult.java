package org.pygisserver.common.data.analysis.entity;

import lombok.Data;

@Data
public class SpatialAnalysisResult {
	
	/**
	 * 任务id
	 */
	private int taskId;
	
//	/**
//	 * 任务进度0—100
//	 */
//	private double progress;
	
	/**
	 * 任务结果
	 */
	private String result;
	
	/**
	 * 结果位置
	 */
	private String outPutLocation;
	
	
	/**
	 * 分析结果文件名称
	 */
	private String fileName;
}
