package org.pygisserver.web.entity;

import java.util.Date;

import lombok.Data;

/**
 * @Description: 查询条件信息
 * @author: 
 * @date: 2019-03-09
 */
@Data
public class QueryBaseCondition {

	/**
	 * 开始时间
	 */
	private Date startTime;
	
	/**
	 * 结束时间
	 */
	private Date endTime;
}
