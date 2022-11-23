package org.pygisserver.web.entity;

import lombok.Data;

@Data
public class SystemConfigInfo {
	
	/**
	 * 表记录编号
	 */
	private long id;
	
	/**
	 * 系统用户名
	 */
	private String systemUsername;
	
	/**
	 * 系统用户密码
	 */
	private String systemPassword;

	/**
	 * 文件服务器
	 */
	private String fileServer;
	
	/**
	 * 文件上传路径
	 */
	private String fileUploadDir;
	
	/**
	 * 系统时间
	 */
	private String systemTime;
	
	/**
	 * 样式文件路径
	 */
	private String stylePath;
}
