package org.pygisserver.web.service;

import org.pygisserver.common.entity.AnalysisResult;
import org.pygisserver.web.entity.AnalysisTaskParam;

public interface MapLayerService {

	/**
	 * 根据采样数量计算线高程值
	 * @param layerName
	 * @param startPos
	 * @param endPos
	 * @param sampleNum
	 * @param format
	 * @param analysisType  分析类型
	 * @return
	 */
	AnalysisTaskParam getLineFromSampleNum(String layerName,String startPos, String endPos,int sampleNum,String format,int analysisType);

	/**
	 * 根据采样步幅计算线高程值
	 * @param layerName
	 * @param startPos
	 * @param endPos
	 * @param stride
	 * @param format
	 * @param analysisType  分析类型
	 * @return
	 */
	AnalysisTaskParam getLineFromStride(String layerName,String startPos, String endPos,double stride,String format,int analysisType);
	
	/**
	 * 根据采样数计算矩形高程值
	 * @param layerName
	 * @param leftUpperPos
	 * @param rightLowerPos
	 * @param sampleNumX
	 * @param sampleNumY
	 * @param format
	 * @param analysisType  分析类型
	 * @return
	 */
	AnalysisTaskParam getRectangleFromSampleNum(String layerName,String leftUpperPos, String rightLowerPos,int sampleNumX,int sampleNumY,String format,int analysisType);

	/**
	 * 矩形框获取高程数据
	 * @param layerName
	 * @param leftUpperPos
	 * @param rightLowerPos
	 * @param strideX
	 * @param strideY
	 * @param format
	 * @param analysisType  分析类型
	 * @return
	 */
	AnalysisTaskParam getRectangleFromStride(String layerName,String leftUpperPos, String rightLowerPos,double strideX,double strideY,String format,int analysisType);
	
	/**
	 * 停止分析任务
	 * @param taskId
	 */
	void stopAnalysisTask(int taskId);
	
	/**
	 * 获取结果
	 * @return
	 */
	AnalysisResult getAnalysisResult(int taskId);
}
