package org.pygisserver.common.entity;

import lombok.Data;

@Data
public class FunctionPageEntity {
	

	/**
	 * 功能id
	 * 
	 */
	private int functionId;
	/**
	 * 功能name
	 * 
	 */
	private String functionName;
	/**
	 * 功能状态
	 * 0表示关闭，1表示开启
	 */
	private int functionState;
	/**
	 * 功能父id
	 * 
	 */
	private int parentFunctionId;
	/**
	 * 功能路径
	 * 
	 */
	private String functionRoute;
	/**
	 * 功能是否展开
	 * 
	 */
	private boolean functionOpenSubmenue;
	/**
	 * 功能排序
	 * 
	 */
	private int functionIndex;
	/**
	 * 功能别名
	 * 
	 */
	private String functionAlias;
	
}
