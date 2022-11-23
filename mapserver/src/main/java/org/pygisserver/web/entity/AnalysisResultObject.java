package org.pygisserver.web.entity;

import java.io.Serializable;

import lombok.Data;

@Data
public class AnalysisResultObject implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2531197262140588254L;
	/**
	 * 高程数据
	 */
	private double[] demArray;
	
	
	private Object geometry;
	
	private int sampleX;
	
	private int sampleY;
}
