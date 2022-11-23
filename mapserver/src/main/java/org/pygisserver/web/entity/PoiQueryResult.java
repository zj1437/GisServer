package org.pygisserver.web.entity;

import lombok.Data;

/**
 * @Description: POI查询结果 
 * @author:
 * @date: 2019-01-26
 */
@Data
public class PoiQueryResult {


	private int id;
	
	/**
	 * poi名称
	 */
	private String name;
	
	/**
	 * poi唯一标识
	 */
	private String uuid;
	
	/**
	 * 纬度
	 */
	private double longitude;
	
	/**
	 * 纬度
	 */
	private double latitude;
	
	/**
	 * 距离当前点的位置
	 */
    private double distance;
    
    /**
     * 地址
     */
    private String address;
    
    /**
     * 电话
     */
    private String phone;
    
    /**
     * 分类类型
     */
    private String type;
    
    /**
     * 邮编
     */
    private String zipCode;
    
    /**
     * 区域编码
     */
    private String admincode;
}
