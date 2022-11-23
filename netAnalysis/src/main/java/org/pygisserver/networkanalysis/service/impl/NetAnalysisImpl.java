package org.pygisserver.networkanalysis.service.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.RoadPlanEntity;
import org.pygisserver.common.entity.RoadPlanParameter;
import org.pygisserver.common.enumm.ErrorCode;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.netanalysis.service.NetworkMapLayerAnalysis;
import org.pygisserver.networkanalysis.mapper.NetWorkAnalysisMapper;
import org.pygisserver.networkanalysis.mapper.RoadPlanMapper;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NetAnalysisImpl implements NetworkMapLayerAnalysis {


	private Geometry geometry;

	@Autowired
	private RoadPlanMapper roadPlanMapper;
	
	@Autowired
	private NetWorkAnalysisMapper netWorkAnalysisMapper;
	
	@Override
	public String roadPlan(RoadPlanParameter roadPlanParameter) {
		try {
			if (roadPlanParameter.getPlanType() == 0) {
				return roadPlanMapper.queryShortestRoad(roadPlanParameter.getStartX(), roadPlanParameter.getStartY(),
						roadPlanParameter.getEndX(), roadPlanParameter.getEndY(), roadPlanParameter.getRationValue());
			}else {
				return roadPlanMapper.queryBestRoad(roadPlanParameter.getStartX(), roadPlanParameter.getStartY(),
						roadPlanParameter.getEndX(), roadPlanParameter.getEndY(), roadPlanParameter.getRationValue());
			}
		} catch (Exception e) {
			log.error("NetAnalysisImpl roadPlan has error {}",e.getLocalizedMessage());
			throw new BaseException(ErrorCode.RUNTIME_EXCEPTION.getErrorCode(),"路径规划失败",e.getCause());
		}
	}


	@Override
	public List<RoadPlanEntity> roadsPlan(RoadPlanParameter roadPlanParameter) {
		try {
			if (roadPlanParameter.getPlanType() == 0) {
				return roadPlanMapper.queryShortestRoads(roadPlanParameter.getStartX(), roadPlanParameter.getStartY(),
						roadPlanParameter.getEndX(), roadPlanParameter.getEndY(), roadPlanParameter.getRationValue());
			}else {
				return roadPlanMapper.queryBestRoads(roadPlanParameter.getStartX(), roadPlanParameter.getStartY(),
						roadPlanParameter.getEndX(), roadPlanParameter.getEndY(), roadPlanParameter.getRationValue());
			}
		} catch (Exception e) {
			log.error("NetAnalysisImpl roadPlan has error {}",e.getLocalizedMessage());
			throw new BaseException(ErrorCode.RUNTIME_EXCEPTION.getErrorCode(),"路径规划失败",e.getCause());
		}
	}


	
	@Override
	public String geocoding(String address) {
		try {
			String result = netWorkAnalysisMapper.geocoding(address);
			if(StringUtils.isBlank(result)) {
				return "抱歉，未能找到结果";
			}
			return result;
		} catch (Exception e) {
			log.error("NetAnalysisImpl geocoding has error {}",e.getLocalizedMessage());
			return "抱歉，未能找到结果";
		}
		
	}

	@Override
	public String reverseGeocoding(String lng,String lat) {
		Double longitude = 0.0;
		Double latitide = 0.0;
		try {
			longitude = Double.valueOf(lng);
			latitide = Double.valueOf(lat);
		} catch (Exception e) {
			return "参数错误";
		}
		if(longitude < -180 || longitude > 180 || latitide < -90 || latitide > 90) {
			return "坐标参数错误";
		}
		try {
			String address = netWorkAnalysisMapper.reverseGeocoding(longitude,latitide);
			if(StringUtils.isBlank(address)) {
				return "抱歉，未能找到结果";
			}
			return address;
		} catch (Exception e) {
			log.error("NetAnalysisImpl reverseGeocoding has error {}",e.getLocalizedMessage());
			return "抱歉，未能找到结果";
		}
	}


}
