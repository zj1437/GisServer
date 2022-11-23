package org.pygisserver.common.entity;

import org.locationtech.jts.geom.Coordinate;

import lombok.Data;

/**
 * @Description: 分析结果 
 * @author: 
 * @date: 2021-06-21
 */
@Data
public class AnalysisResult implements Cloneable{

	/**
	 * 高程数据
	 */
	private double[] demArray;
	
	/**
	 * 点坐标
	 */
	private Coordinate[] coordinates;
	
	/**
	 * 图片高
	 */
	private int imageWidth;
	
	/**
	 * 图片高
	 */
	private int imageHeight;
	
	/**
	 * 分析状态
	 */
	private int analysisStatus;  //0 是开始分析   1 分析进行中  2  分析完成
	
	
	private int taskId;

	@Override
	public AnalysisResult clone() {

		try {
			return (AnalysisResult) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}


}
