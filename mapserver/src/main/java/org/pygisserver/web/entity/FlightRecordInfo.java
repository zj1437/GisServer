package org.pygisserver.web.entity;

import java.util.Date;

import lombok.Data;

/**
 * @Description: 全球航班飞行记录信息 
 * @author:  
 * @date: 2020-5-7
 */
@Data
//@Alias("flightRecordInfo")
public class FlightRecordInfo {
	
	/**
	 * id
	 */
    private long id;
    
    /**
     *航班号
     */
    private String flightNumber;
    
    /**
     * 纬度
     */
    private double latitude;

    /**
     * 经度
     */
    private double longitude;
    
    /**
     * 位置信息
     */
    private String pointInfo;
	
    /**
     * 航班信息更新时间
     */
    private Date flightTimestamp;
    
    /**
     * 飞行高度，单位是英尺
     */
    private double flyingHeight;
    
    /**
     *飞机机型
     */
    private String flightModel;
    
    /**
     * 当前无法识别数据-1
     */
    private double noUseData;

    /**
     * 方位角
     */
    private double azimuth;
	
    /**
     * 管理类型，ADSB或其他
     */
    private String managermentType;
    
    /**
     * 飞机注册号
     */
    private String registerNumber;
    
    /**
     * 航班始发地
     */
    private String startingPlace;

    /**
     * 航班目的地
     */
    private String destinationPlace;
	
    /**
     * 该航班所属航空公司
     */
    private String airlineBelongs;
    
    /**
     * 当前无法识别数据-2
     */
    private double otherInfo;
    
    public String toString() {
    	StringBuilder sBuilder = new StringBuilder("[");
    	sBuilder.append("flightNumber:").append(flightNumber)
    	        .append(",latitude:").append(latitude)
    	        .append(",longitude:").append(longitude)
    	        .append(",pointInfo:").append(pointInfo)
    	        .append(",flightTimestamp:").append(flightTimestamp)
    	        .append(",flyingHeight:").append(flyingHeight)
    	        .append(",flightModel:").append(flightModel)
    	        .append(",noUseData:").append(noUseData)
    	        .append(",azimuth:").append(azimuth)
    	        .append(",managermentType:").append(managermentType)
    	        .append(",registerNumber:").append(registerNumber)
    	        .append(",startingPlace:").append(startingPlace)
    	        .append(",destinationPlace:").append(destinationPlace)
    	        .append(",airlineBelongs:").append(airlineBelongs)
    	        .append(",otherInfo:").append(otherInfo).append("]");
    	return sBuilder.toString();
    }
}
