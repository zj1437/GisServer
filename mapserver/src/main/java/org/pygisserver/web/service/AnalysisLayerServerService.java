package org.pygisserver.web.service;

import java.util.List;

import org.pygisserver.web.entity.HeatmapAnalysisResult;

/**
 * @Description: 分析服务图层相关处理类
 * @author: 刘洋 
 * @date: 2022-05-27
 */
public interface AnalysisLayerServerService {

	/**
	 * 查询分析矢量图层 
	 * @return 返回矢量分析图层
	 */
	List<String> queryAnalysisVectorLayer();
	
	/**
	 * 矢量图层分析参数解析
	 * @param fileUrl 文件url
	 * @return 返回热力图分析结果
	 */
	HeatmapAnalysisResult analysisVectorLayer(String fileUrl);
}
