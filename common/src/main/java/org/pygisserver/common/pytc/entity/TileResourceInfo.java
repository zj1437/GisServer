package org.pygisserver.common.pytc.entity;

import lombok.Data;

/**
 * @Description: 瓦片资源信息 
 * @author: 
 * @date: 2019-12-12
 */
@Data
public class TileResourceInfo {

	private byte[] byteResource;
	
	private long x;
	
	private long y;
	
	private long z;

	public TileResourceInfo(byte[] byteResource, long x, long y, long z) {
		super();
		this.byteResource = byteResource;
		this.x = x;
		this.y = y;
		this.z = z;
	}
}
