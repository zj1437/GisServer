package org.pygisserver.common.pytc.entity;

import java.io.Serializable;

import lombok.Data;

/**
 * @Description: 服务查询条件对象
 * @author:  
 * @date: 2018-10-29
 */
@Data
public class RequestQueryCondition implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4667411723851302414L;

	private String serverName;

	//适配web端需求把查询服务id放到这个实体内
	private int serverId;

	private String serverProtocol;
	
	private int serverType;
	
	private int serverStatus = -2;
	
	private String publishor;
	
	private int styleType;
	
	private String styleName;
	
	private int enabled = -1;
	
	private String protocolName;
	/**
	 * 用户名检索
	 */
	private String userName;
	/**
	 * 请求IP检索
	 */
	private String reqIP;
	/**
	 * 日志级别检索
	 */
	private int logLevel;
	/**
	 * 日志开始时间检索
	 */
	private String startTime;
	
	/**
	 * 日志开始时间检索
	 */
	private String logContent;
	/**
	 * 日志结束时间检索
	 */
	private String endTime;
	
	/**
	 * 分页信息
	 */
	private PageBean pageInfo;


	public RequestQueryCondition(String serverName, int serverType, int serverStatus, String publishor) {
		super();
		this.serverName = serverName;
		this.serverType = serverType;
		this.serverStatus = serverStatus;
		this.publishor = publishor;
	}
	
	public RequestQueryCondition() {
		
	}
}
