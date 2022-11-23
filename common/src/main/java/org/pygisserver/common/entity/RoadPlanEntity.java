package org.pygisserver.common.entity;

import lombok.Data;

/**
 * @Description: 路网规划实体类 
 * @author: 
 * @date: 2021-07-06
 */
@Data
public class RoadPlanEntity {

	private String roadName;
	
	private double cost;
	
	/**
	 * 路网geometry数据
	 */
	private Object geometry;
	
}
