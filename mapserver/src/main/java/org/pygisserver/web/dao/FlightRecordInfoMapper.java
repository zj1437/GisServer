package org.pygisserver.web.dao;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.pygisserver.web.entity.FlightRecordInfo;
import org.springframework.stereotype.Repository;

@Repository
public interface FlightRecordInfoMapper {

	/**
	 * 批量存储记录
	 * @param records
	 * @return boolean
	 */
	void saveAllFlightRecordInfos(List<FlightRecordInfo> records);
	
	/**
	 * 根据时间戳查询航班数据
	 * @param date
	 * @return
	 */
	List<FlightRecordInfo> queryDatasByTimestamp(@Param("startTime")Date startTime,
			@Param("endTime")Date endTime);
	
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
	List<FlightRecordInfo> queryDatasByBboxAndTimestamp(@Param("bboxInfo")String bboxInfo,
			@Param("date")Date date);
	
	/**
	 * 查询某个航班号飞行轨迹
	 * @param flightNum
	 * @return
	 */
	List<FlightRecordInfo> queryDatasByFlightNum(String flightNum);
}
