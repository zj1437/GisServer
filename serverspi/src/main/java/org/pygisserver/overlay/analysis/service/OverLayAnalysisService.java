package org.pygisserver.overlay.analysis.service;

import org.pygisserver.common.data.analysis.entity.SpatialAnalysisResult;

/**
 * @Description: 叠加分析接口类 
 * @author: 刘洋 
 * @date: 2022-05-16
 */
public interface OverLayAnalysisService {

	/**
	 * 相交分析
	 * @param inputFile 输入文件
	 * @param overLayFile 叠加文件
	 * @param outPutLocation 目标位置
	 * @return 返回分析结果
	 */
	public String insectionAnalysis(String inputFile,String overLayFile,String outPutLocation,SpatialAnalysisResult analysisResult);
	
	/**
	 * 聚合分析
	 * @param inputFile 输入文件
	 * @param overLayFile 叠加文件
	 * @param outPutLocation 目标位置
	 * @return 返回分析结果
	 */
	public String aggregateAnalysis(String inputFile,String overLayFile,String outPutLocation,String charSetName,String aggregateName,SpatialAnalysisResult analysisResult);
	
	/**
	 * 合并分析
	 * @param inputFile
	 * @param overLayFile
	 * @param outPutLocation
	 * @return 返回分析结果
	 */
	public String combineAnalysis(String inputFile,String overLayFile,String outPutLocation,SpatialAnalysisResult analysisResult);
	
	
	/**
	 * 凸包分析
	 * @param inputFile 输入图层
	 * @param outPutLocation 目标位置
	 * @return  返回输入点或先的最小凸多边形
	 */
	public String convexhullAnalysis(String inputFile,String outPutLocation,SpatialAnalysisResult analysisResult);
	
	
	/**
	 * 差异分析
	 * 主要处理，输入图层除去与  输入图层和叠加图层相交的部分
	 * @param inputFile  
	 * @param overLayFile
	 * @param outPutLocation
	 * @return
	 */
	public String diffenceAnalysis(String inputFile,String overLayFile,String outPutLocation,SpatialAnalysisResult analysisResult);

}
