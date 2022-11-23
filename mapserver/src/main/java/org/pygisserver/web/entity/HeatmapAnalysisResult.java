package org.pygisserver.web.entity;

import java.util.List;

import lombok.Data;

/**
 * @Description: 矢量解析默认参数
 * @author: 刘洋 
 * @date: 2022-05-27
 */
@Data
public class HeatmapAnalysisResult {

	private float defaultKernelRadius;
	
	private List<String> attrNames;
	
	private float ceilPiexl;
}
