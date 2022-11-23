package org.pygisserver.web.entity;

import lombok.Data;

@Data
public class BboxInfoEntity {
	/**
	 * 获取即将发布的服务经纬度信息
	 */
	private String bboxInfo;

	/**
	 * 即将发布的服务是否为高程
	 */
	private boolean demServer;

	/**
	 * 获取即将发布的服务文件类型，主要用于osgb和terrain文件
	 */
	private String extension;

	/**
	 * 获取即将发布的服务文件原数据范围
	 */
	private String resourceBbox;

	/**
	 * 获取即将发布的服务文件原数据投影
	 */
    private String resourceCrs;
	
	public BboxInfoEntity(String bboxInfo,boolean demServer,String extension){
		this.bboxInfo = bboxInfo;
		this.demServer = demServer;
		this.extension = extension;
	}
	
	public BboxInfoEntity(){
		
	}
}