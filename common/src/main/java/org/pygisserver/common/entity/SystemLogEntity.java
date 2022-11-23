package org.pygisserver.common.entity;

import java.util.Date;

import lombok.Data;

@Data
public class SystemLogEntity {
	/**
	 * 日志记录序列号
	 */
	private int logId;
	/**
	 * 日志级别，当前主要有info、warning、error三个级别
	 */
	private int logLevel;
	/**
	 * 日志内容
	 */
	private String logContent;
	
	/**
	 * 日志内容
	 */
	private String sysLogUser;
	/**
	 * 系统日志来源，如数据库、文件读写等
	 */
	private String logSrc;
	/**
	 * 当前系统日志来源的IP地址
	 */
	private String logSrcIP;
	/**
	 * 系统日志发生时间
	 */
	private Date logHappenedTime;
	
	public SystemLogEntity (){	}
	public SystemLogEntity (int logLevel,String logContent,String sysLogUser,String logSrc,String logSrcIP,Date logHappenedTime){
		this.logLevel = logLevel;
		this.logContent = logContent;
		this.logSrc = logSrc;
		this.logSrcIP = logSrcIP;
		this.logHappenedTime = logHappenedTime;
		this.sysLogUser = sysLogUser;
	}
}
