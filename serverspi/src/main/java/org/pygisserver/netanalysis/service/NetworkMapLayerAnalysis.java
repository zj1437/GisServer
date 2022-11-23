package org.pygisserver.netanalysis.service;

import java.util.List;

import org.pygisserver.common.entity.RoadPlanEntity;
import org.pygisserver.common.entity.RoadPlanParameter;
import org.pygisserver.spi.service.PySpiService;

/**
 * @Description: 网络地图分析结果 
 * @author: 刘洋 
 * @date: 2021-07-05
 */
public interface NetworkMapLayerAnalysis extends PySpiService {

	/**
	 * 直接返回最终的规划路线
	 * @param roadPlanParameter 路径规划参数
	 * @return 
	 */
	String roadPlan(RoadPlanParameter roadPlanParameter);
	
	/**
	 * 获取最短路径
	 * @param roadPlanParameter 路径规划参数
	 * @return
	 */
	List<RoadPlanEntity> roadsPlan(RoadPlanParameter roadPlanParameter);


	
	/**
	 * 地理编码
	 * @param address 待解析地址
	 * @return
	 */
    String geocoding(String address);
    
    /**
	 * 逆向地理编码
	 * @param lng 经度
	 * @param lat 纬度
	 * @return
	 */
    String reverseGeocoding(String lng,String lat);
}
