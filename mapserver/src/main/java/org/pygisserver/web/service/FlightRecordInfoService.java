package org.pygisserver.web.service;

import java.util.Date;
import java.util.List;

import org.pygisserver.web.entity.FlightRecordInfo;

public interface FlightRecordInfoService {

	/**
	 * 存储记录
	 * @param filePath
	 * @return void
	 */
	void saveFlightRecordInfos(String filePath);
	
	/**
	 * 根据时间段查询航班数据
	 * @param date
	 * @return
	 */
	List<FlightRecordInfo> queryDatasByTimestamp(Date startTime, Date endTime);
	
	/**
	 * 根据经纬度范围查询航班数据
	 * @param bboxInfo
	 * @return
	 */
	List<FlightRecordInfo> queryDatasByBbox(String bboxInfo);
	
	/**
	 * 根据经纬度范围和时间戳查询航班数据
	 * @param bboxInfo
	 * @param date
	 * @return
	 */
	List<FlightRecordInfo> queryDatasByBboxAndTimestamp(String bboxInfo, Date date);
	
	/**
	 * 查询某个航班号飞行轨迹
	 * @param flightNum
	 * @return
	 */
	List<FlightRecordInfo> queryDatasByFlightNum(String flightNum);
	
}
