package org.pygisserver.web.service;

import java.util.List;

public interface NetworkAnalysisService {

	/**
	 * 跟新数据库中的数据
	 * @param queryPoints
	 */
	void updateRoadCost(List<String> queryPoints);
	
	/**
	 * 还原原来的数据点
	 * @param queryPoints
	 */
	void restoreRoadCost(List<String> queryPoints);
}
