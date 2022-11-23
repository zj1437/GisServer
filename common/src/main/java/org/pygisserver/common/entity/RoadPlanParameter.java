package org.pygisserver.common.entity;

import lombok.Data;

@Data
public class RoadPlanParameter {

	private double startX;
	
	private double startY;
	
	private double endX;
	
	private double endY;
	
	private double planType;
	
	//限宽
	private double rationValue;  
	
	//限高
	private double limitValue;
	
	//规避点的x
	private double obstaclePointX;
	
	//规避点y
	private double obstaclePointY;
}
