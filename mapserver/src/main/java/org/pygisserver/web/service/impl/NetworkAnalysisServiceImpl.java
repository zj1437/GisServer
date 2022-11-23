package org.pygisserver.web.service.impl;

import java.util.List;

import org.pygisserver.web.dao.MapServerMapper;
import org.pygisserver.web.service.NetworkAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description:  网络分析服务数据处理
 * @author: 刘洋 
 * @date: 2022-06-06
 */
@Service
@Slf4j
public class NetworkAnalysisServiceImpl implements NetworkAnalysisService {

	@Autowired
	private MapServerMapper mapServerMapper;
	
	@Override
	public void updateRoadCost(List<String> queryPoints) {
		try {
			mapServerMapper.updateRoadCost(queryPoints);
		} catch (Exception e) {
			log.error("networkAnalysisServiceImpl updateRoadCost has error {}", e.getLocalizedMessage());
		}
	}

	@Override
	public void restoreRoadCost(List<String> queryPoints) {
		try {
			mapServerMapper.restoreRoadCost(queryPoints);
		} catch (Exception e) {
			log.error("networkAnalysisServiceImpl restoreRoadCost has error {}", e.getLocalizedMessage());
		}
	}

}
