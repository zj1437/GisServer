package org.pygisserver.common.entity;

import lombok.Data;

/**
 * 海图属性参数
 * @Description: 
 * @author:  
 * @date: 2020-10-22
 */
@Data
public class SeaChartFeatureInfo {
	
	private String codeNo;
	
	private String ObjectName;
	
	private String nameAcronym;
	
	private String attributeA;

	private String attributeB;
	
	private String attributeC;

	private int isStandard;
	
	private int isBase;
}
