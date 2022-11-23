package org.pygisserver.web.entity;

import lombok.Data;

/**
 * @Description: 图层、数据存储信息对象
 * @author:
 * @date: 2018-11-02
 */
@Data
public class StoreLayerInfo {

	private String storeId;
	
	private int serverId;
	
	private String layerId;
	
	public StoreLayerInfo(String storeId, int serverId, String layerId) {
		super();
		this.storeId = storeId;
		this.serverId = serverId;
		this.layerId = layerId;
	}
}
