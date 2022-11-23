package org.pygisserver.web.entity;


import org.locationtech.jts.geom.Coordinate;

import lombok.Data;

/*
* 基础几何实体
*
* */
@Data
public class BasicGeomEntity {

    /*
     * 点坐标
     * */
    private float x;
    private float y;
    /**
	 * 原始数据地理坐标系
	 */
	private int resourceCrsWkt;
	
	/**
	 * 原始数据投影坐标srs
	 */
	private int resourceSrs;
	
	/**
	 * 本地投影坐标 默认WGS84 EPSG:4326
	 */
	private String  nativeSrs;
	
	/**
	 * 地区半径
	 */
	private int  areaRadius;
	
	/**
	 * 椭球体的长半轴
	 */
	private float  longHalFaxis;

    
}
