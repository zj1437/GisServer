package org.pygisserver.common.entity;

import java.io.Serializable;

import lombok.Data;

/**
 * @Description: 服务的基本图层信息
 * @author:  
 * @date: 2020-08-26
 */
@Data
public class UserLogbackInfo implements Serializable{
	private static final long serialVersionUID = 1L;
	/**
	 * 单个日志文件大小，最大10M
	 */
	private int logFileSize;
	
	/**
	 * 日志文件最大保存时间
	 */
	private int logMaxSave;
	
	/**
	 * 用户自定义日志路径
	 */
	private String curLogPath;
	
	/**
	 * 每次更新logFileSize、logMaxSave、curLogPath信息后生成的最新配置数据，用于动态更新logback配置
	 */
	private byte[] logbackDetail;
}
