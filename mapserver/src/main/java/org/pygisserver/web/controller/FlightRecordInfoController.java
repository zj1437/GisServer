package org.pygisserver.web.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.entity.ReponseEntity;
import org.pygisserver.common.entity.ReponseStatus;
import org.pygisserver.common.exception.ArgumentException;
import org.pygisserver.common.util.DateUtil;
import org.pygisserver.web.service.FlightRecordInfoService;
import org.pygisserver.web.entity.BboxInfo;
import org.pygisserver.web.entity.FlightRecordInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

@Api(value="航班信息处理接口",tags="航班信息处理接口")
@RestController
@RequestMapping("/flightInfo")
@Slf4j
public class FlightRecordInfoController {
	
	@Autowired
	private FlightRecordInfoService flightRecordInfoService;
	
	
	/**
	 * 保存航班信息
	 * @param filePath
	 * @return
	 */
	@ApiOperation(value = "保存航班信息",notes="保存一个航班信息文件或一个目录")
	@ApiImplicitParam(name="filePath",value="文件url",required=true,paramType="query")
	@PostMapping("/saveRecord")
	public ReponseEntity<String> saveFlightRecordInfo(@RequestParam("filePath") String filePath){
		String tempPath = filePath;
		if (StringUtils.isBlank(tempPath)) {
			return new ReponseEntity<String>(ReponseStatus.ERROR.getCode(),"路径不能为空！");
		}
		flightRecordInfoService.saveFlightRecordInfos(tempPath);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),
				                         ReponseStatus.SUCCESS.getResult(),
				                         "航班信息保存成功！");
	}
	
	/**
	 * 根据时间段查询航班信息
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	@ApiOperation(value = "根据时间段查询航班信息",notes="根据时间段查询航班信息")
	@ApiImplicitParams({
		@ApiImplicitParam(name="startTime",value="起始时间",required=true,paramType="query"),
		@ApiImplicitParam(name="endTime",value="结束时间",required=true,paramType="query")
	})
	@GetMapping("/queryDatasByTimestamp")
	public ReponseEntity<List<FlightRecordInfo>> queryDatasByTimestamp(@RequestParam("startTime") String startTime,
			@RequestParam("endTime") String endTime){
		List<FlightRecordInfo> result = null;
		Date start = null;
		Date end = null;
		if (StringUtils.isBlank(startTime) || StringUtils.isBlank(endTime)) {
			System.err.println("时间戳不能为空！");
		}
		start = DateUtil.convertTimestampToDate(startTime);
		end = DateUtil.convertTimestampToDate(endTime);
		result = flightRecordInfoService.queryDatasByTimestamp(start,end);
		return new ReponseEntity<List<FlightRecordInfo>>(ReponseStatus.SUCCESS.getCode(),
				                         ReponseStatus.SUCCESS.getResult(),
				                         result);
	}
	
	
	/**
	 * 根据经纬度范围查询航班信息
	 * @param bboxInfo
	 * @return
	 */
	@ApiOperation(value = "根据经纬度范围查询航班数据",notes="根据经纬度范围查询航班数据")
	@ApiImplicitParam(name="bboxInfo",value="bbox经纬度范围",required=true,paramType="body")
	@GetMapping("/queryDatasByBbox")
	public ReponseEntity<List<FlightRecordInfo>> queryDatasByBbox(@RequestBody BboxInfo bboxInfo){
		List<FlightRecordInfo> result = null;
		if (bboxInfo.getMaxLatitude() > 90 || bboxInfo.getMinLatitude() < -90 || 
				bboxInfo.getMaxLongitude() > 180 || bboxInfo.getMinLongitude() < -180) {
				System.err.println("经度或纬度超出范围！");
		}
		result = flightRecordInfoService.queryDatasByBbox(buildBboxInfo(bboxInfo));
		return new ReponseEntity<List<FlightRecordInfo>>(ReponseStatus.SUCCESS.getCode(),
				                         ReponseStatus.SUCCESS.getResult(),
				                         result);
	}
	
	
	/**
	 * 结合经纬度和时间查询航班信息
	 * @param bboxInfo
	 * @param timestamp
	 * @return
	 */
	@ApiOperation(value = "根据经纬度范围和时间戳查询航班数据",notes="根据经纬度范围和时间戳查询航班数据")
	@ApiImplicitParams({
		@ApiImplicitParam(name="minLatitude",value="最小纬度",required=true,paramType="query"),
		@ApiImplicitParam(name="maxLatitude",value="最大纬度",required=true,paramType="query"),
		@ApiImplicitParam(name="minLongitude",value="最小经度",required=true,paramType="query"),
		@ApiImplicitParam(name="maxLongitude",value="最大经度",required=true,paramType="query"),
		@ApiImplicitParam(name="timestamp",value="时间戳",required=true,paramType="query")
	})
	@GetMapping("/queryDatasByBboxAndTime")
	public ReponseEntity<List<FlightRecordInfo>> queryDatasByBboxAndTime(@RequestParam("timestamp") String timestamp, 
			@RequestParam("minLatitude") double minLatitude,
			@RequestParam("maxLatitude") double maxLatitude,
			@RequestParam("minLongitude") double minLongitude,
			@RequestParam("maxLongitude") double maxLongitude){		
		List<FlightRecordInfo> result = null;
		if (maxLatitude > 90 || minLatitude < -90 || 
				maxLongitude > 180 || minLongitude < -180) {
			throw new ArgumentException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.ARGGUMENT_ILLEGAL, "Bbox请求信息"));
		}
		if (StringUtils.isBlank(timestamp)) {
			throw new ArgumentException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.ARGGUMENT_NULL, timestamp));
		}
		BboxInfo bboxInfo = new BboxInfo(minLatitude,maxLatitude,minLongitude,maxLongitude);
		Date date = new Date(Long.valueOf(timestamp));
		SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdFormat.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
		String formattedTime = sdFormat.format(date);
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			date = format.parse(formattedTime);
		} catch (ParseException e) {
			log.error("DateUtil convertTimestampToDate has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_TRANSFOR, "时间戳转换为Date"));
		}
		result = flightRecordInfoService.queryDatasByBboxAndTimestamp(buildBboxInfo(bboxInfo), date);
		return new ReponseEntity<List<FlightRecordInfo>>(ReponseStatus.SUCCESS.getCode(),
				                         ReponseStatus.SUCCESS.getResult(),
				                         result);
	}
	
	/**
	 * 根据航班号查询航班飞行轨迹
	 * @param timestamp
	 * @return
	 */
	@ApiOperation(value = "根据航班号查询航班飞行轨迹",notes="根据航班号查询航班飞行轨迹")
	@ApiImplicitParam(name="flightNum",value="航班名称",required=true,paramType="query")
	@GetMapping("/queryDatasByFlightNum")
	public ReponseEntity<List<FlightRecordInfo>> queryDatasByFlightNum(@RequestParam("flightNum") String flightNum){
		List<FlightRecordInfo> result = null;
		if (StringUtils.isBlank(flightNum)) {
			throw new ArgumentException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.ARGGUMENT_NULL, "航班号信息"));
		}
		result = flightRecordInfoService.queryDatasByFlightNum(flightNum);
		return new ReponseEntity<List<FlightRecordInfo>>(ReponseStatus.SUCCESS.getCode(),
				                         ReponseStatus.SUCCESS.getResult(),
				                         result);
	}
	
	private String buildBboxInfo(BboxInfo bboxInfo) {
		double minLa = bboxInfo.getMinLatitude();
		double maxLa = bboxInfo.getMaxLatitude();
		double minLo = bboxInfo.getMinLongitude();
		double maxLo = bboxInfo.getMaxLongitude();
		StringBuilder sBuilder = new StringBuilder("linestring(");
		sBuilder.append(minLo).append(" ").append(minLa).append(",")
		        .append(minLo).append(" ").append(maxLa).append(",")
		        .append(maxLo).append(" ").append(maxLa).append(",")
		        .append(maxLo).append(" ").append(minLa).append(",")
		        .append(minLo).append(" ").append(minLa).append(")");
		return sBuilder.toString();
	}

}
