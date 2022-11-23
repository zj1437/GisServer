package org.pygisserver.web.entity;

import java.util.Date;
import lombok.Data;

/**
 * @Description: 系统操作日志 
 * @author: 
 * @date: 2018-11-26
 */
@Data
public class SysOperationLog {

	private int id;

	/**
	 * 操作类型
	 */
	private int operateType;

	/**
	 * 日志内容
	 */
	private String content;

	/**
	 * 操作人
	 */
	private String operator;

	/**
	 * 操作Ip
	 */
	private String operatorIP;

	/**
	 * 操作时间
	 */
	private Date operateTime;
}
