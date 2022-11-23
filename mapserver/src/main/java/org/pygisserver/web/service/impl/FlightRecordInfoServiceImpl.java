package org.pygisserver.web.service.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.util.DateUtil;
import org.pygisserver.web.dao.FlightRecordInfoMapper;
import org.pygisserver.web.entity.FlightRecordInfo;
import org.pygisserver.web.service.FlightRecordInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author 
 * 2020-5-8
 */
@Service
@Slf4j
public class FlightRecordInfoServiceImpl implements FlightRecordInfoService {
	private static final Integer[] STRING_INDEX = {0,5,8,9,10,11,12};
	private static final Integer[] DOUBLE_INDEX = {1,2,4,6,7,13};
	private static final Integer MAXIMUM_INSERT_NUMBER = 1500;

	@Autowired
	private FlightRecordInfoMapper flightRecordInfoMapper;

	/**
	 * 保存航班飞行信息
	 */
	@Override
	public void saveFlightRecordInfos(String filePath){
		boolean isChinaData = false;
		if (filePath.contains("/china/")) {
			isChinaData = true;
		}
		if (checkFileType(filePath)) {
			parseJsonFile(filePath, isChinaData);
		} else {
			parseJsonDir(filePath);
		}
	}
	
	@Override
	public List<FlightRecordInfo> queryDatasByTimestamp(Date startTime, Date endTime) {
		return flightRecordInfoMapper.queryDatasByTimestamp(startTime,endTime);
	}
	
	@Override
	public List<FlightRecordInfo> queryDatasByBbox(String bboxInfo) {
		return flightRecordInfoMapper.queryDatasByBbox(bboxInfo);
	}
	
	@Override
	public List<FlightRecordInfo> queryDatasByBboxAndTimestamp(String bboxInfo, Date date) {
		return flightRecordInfoMapper.queryDatasByBboxAndTimestamp(bboxInfo, date);
	}
	
	@Override
	public List<FlightRecordInfo> queryDatasByFlightNum(String flightNum) {
		return flightRecordInfoMapper.queryDatasByFlightNum(flightNum);
	}
	
	/**
	 * 解析用户选中的单个json文件
	 * @param file
	 * @return List<FlightRecordInfo>
	 */
	private void parseJsonFile(String file, Boolean isChinaData){
		File parseFile = new File(file);
		List<FlightRecordInfo> records = new ArrayList<FlightRecordInfo>();
		JSONObject jsonObjects = null;
		Iterator<Object> objects = null;
		String flightInfos = null;
		String jsonObject = null;
		String[] infos;
		String realTime = null;
		Date dateTime;
		int fileNum = 0;
		try {
			realTime = parseFile.getName().substring(0, parseFile.getName().length()-5);
			dateTime = DateUtil.convertTimestampToDate(realTime);
			flightInfos = FileUtils.readFileToString(parseFile);
			flightInfos = flightInfos.substring(1, flightInfos.indexOf("},{")+1);
			jsonObjects = JSON.parseObject(flightInfos);
			objects = jsonObjects.values().iterator();
			while (objects.hasNext()) {
				FlightRecordInfo flightRecordInfo = new FlightRecordInfo();
				jsonObject = objects.next().toString();
				jsonObject = jsonObject.substring(1, jsonObject.length()-1);
				infos = jsonObject.split(",");
				String info;
				for(int i = 0;i< infos.length;i++) {
					info = infos[i];
					buildRecords(flightRecordInfo, info, i, dateTime);
				}
				String point = "point(" + flightRecordInfo.getLongitude() + " " + flightRecordInfo.getLatitude() + ")";
				flightRecordInfo.setPointInfo(point);
				records.add(flightRecordInfo);
			}
			clearNullRecords(records);
			saveRecords(records);
			fileNum++;
		} catch (FileNotFoundException e) {
			log.error("FlightRecordInfoServiceImpl parseJsonFile has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_NOT_FOUND, parseFile.getName()));
	    } catch (IOException e) {
	    	log.error("FlightRecordInfoServiceImpl parseJsonFile has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, parseFile.getName()));
	    }
	}
	
	private void saveRecords(List<FlightRecordInfo> records) {
		List<FlightRecordInfo> commitRecords;
		int executeTimes;
		int index = 0;
		if (records.size() % MAXIMUM_INSERT_NUMBER == 0) {
			executeTimes = records.size() / MAXIMUM_INSERT_NUMBER;
		} else {
			executeTimes = records.size() / MAXIMUM_INSERT_NUMBER + 1;
		}
		//批量插入数据库
		for (int i = 1; i <= executeTimes; i++) {
			if (i < executeTimes){
				commitRecords = records.subList(index, i * MAXIMUM_INSERT_NUMBER);
                index += MAXIMUM_INSERT_NUMBER;
            } else {
            	commitRecords = records.subList(index, records.size());
            }
			flightRecordInfoMapper.saveAllFlightRecordInfos(commitRecords);
		}
	}
	
	/**
	 * 构造航班飞行记录的POJO类
	 * @param flightRecordInfo
	 * @param recordDetail
	 * @param index
	 * @param time
	 */
	private void buildRecords(FlightRecordInfo flightRecordInfo, String recordDetail, int index, Date time){
		List<Integer> stringIndex =Arrays.asList(STRING_INDEX);
		List<Integer> doubleIndex =Arrays.asList(DOUBLE_INDEX);
		String temValue = null;
		Double douValue = 0.0;
		if (checkValue(recordDetail)){
			if (stringIndex.contains(index)) {
				temValue = recordDetail.substring(1,recordDetail.length() -1);
			} else if(doubleIndex.contains(index)) {
				douValue = Double.valueOf(recordDetail);
			}
		}
		
		switch (index) {
		case 0:
			flightRecordInfo.setFlightNumber(temValue);
			break;
		case 1:
			flightRecordInfo.setLatitude(douValue);
			break;
		case 2:
			flightRecordInfo.setLongitude(douValue);
			break;
		case 3:
			flightRecordInfo.setFlightTimestamp(time);
			break;
		case 4:
			flightRecordInfo.setFlyingHeight(douValue * 0.3048);
			break;
		case 5:
			flightRecordInfo.setFlightModel(temValue);
			break;
		case 6:
			flightRecordInfo.setNoUseData(douValue);
			break;
		case 7:
			flightRecordInfo.setAzimuth(douValue);
			break;
		case 8:
			flightRecordInfo.setManagermentType(temValue);
			break;
		case 9:
			flightRecordInfo.setRegisterNumber(temValue);
			break;
		case 10:
			flightRecordInfo.setStartingPlace(temValue);
			break;
		case 11:
			flightRecordInfo.setDestinationPlace(temValue);
			break;
		case 12:
			flightRecordInfo.setAirlineBelongs(temValue);
			break;
		case 13:
			flightRecordInfo.setOtherInfo(douValue);
			break;
		}
	}
	
	private boolean checkValue(String recordDetail) {
		//StringUtils.isBlank(recordDetail)
		if(recordDetail.equals("null")) {
			return false;
		} else if (StringUtils.equals(recordDetail, "\"\"")) {
			return false;
		} else if (recordDetail.equalsIgnoreCase("N/A")) {
			return false;
		}
	    return true;
	}
	
	/**
	 * 解析用户选定目录中的json文件
	 * @param path
	 * @return
	 */
	private void parseJsonDir(String path){
		String[] files;
		String file;
		File filePath = new File(path);
		files = filePath.list();
		boolean isChinaData = false;
		for(int i = 0;i< files.length;i++) {
			file = files[i];
			File f = new File(path + File.separator + file);
			if(f.toString().endsWith("/china")) {
				isChinaData = true;
			}
			if (f.isDirectory()) {
				parseJsonDir(f.toString());
			} else {
				 if (checkFileType(f.toString())) {
					 parseJsonFile(f.toString(), isChinaData);
				 }
			}
		}
	}
	
	/**
	 * 清除数据中航班号为空的记录
	 * @param flightRecordInfos
	 */
	private void clearNullRecords(List<FlightRecordInfo> flightRecordInfos) {
		Iterator<FlightRecordInfo> iterator = flightRecordInfos.iterator();
		FlightRecordInfo record = null;
		while(iterator.hasNext()) {
			record = iterator.next();
			if (record.getFlightNumber() == null) {
				iterator.remove();
			}
		}
	}
	
	/**
	 * 检查文件类型，作为过滤json文件的判断依据
	 * @param path
	 * @return
	 */
	private boolean checkFileType(String path) {
		return path.endsWith(".json") || path.endsWith(".JSON");
	}

}
