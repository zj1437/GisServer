package org.pygisserver.common.entity;

import java.io.Serializable;

import lombok.Data;

/**
 * @Description:地图金字塔数据信息 
 * @author: 
 * @date: 2020-09-16
 */
@Data
public class MapPyramidInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private int id;
	
	private int serverId;
	
	private double resolution;
	
	private String fileUrl;
}
