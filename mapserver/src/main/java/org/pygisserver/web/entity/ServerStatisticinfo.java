package org.pygisserver.web.entity;

import java.util.Date;

import lombok.Data;

/**
 * @Description: 服务统计信息 
 * @author:  
 * @date: 2018-11-22
 */
@Data
public class ServerStatisticinfo {

	private int id;

	private int serverId;
	
	/**
	 * 服务名称
	 */
	private String serverName;
	
	/**
	 * 服务类型0 是地图服务，  1是模型服务
	 */
	private String serverType;
	
	/**
	 * 服务描述
	 */
	private String description;
	
	
	private String ogcName;
	
	/**
	 * 访问量
	 */
	private int accessNum; 
	
	/**
	 * 服务状态
	 */
	 private int  serverStatus;  // 0暂停 1 运行  2异常
	
	/**
	 * 最大响应时间
	 */
	private double maxReponseTime;

	/**
	 * 最小响应时间
	 */
	private double minReponseTime;
	
	/**
	 * 平均响应时间
	 */
	private long averageTime;
	
	/**
	 * 最大并发量
	 */
	private int maxConcurrent;
	
	/**
	 * 统计时间
	 */
	private Date statsticTime;
}
