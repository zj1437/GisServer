package org.pygisserver.web.entity;

import lombok.Data;

/**
 * @Description: 图层、数据存储信息对象
 * @author: 
 * @date: 2018-11-02
 */
@Data
public class PostgisServerInfo {

	private int id;
	
	private int serverId;
	
	private int dbId;
	
	public PostgisServerInfo(int serverId, int dbId) {
		super();
		this.serverId = serverId;
		this.dbId = dbId;
	}
}
