package org.pygisserver.web.entity;

import lombok.Data;

/**
 * @Description: 栅格色带 
 * @author: 
 * @date: 2019-03-12
 */
@Data
public class RasterColorMap {

	private int id;
	
	private int colorId;
	
	private int serverId;
	
	private String colorName;
	
	private String colorUrl;

	public RasterColorMap(String colorName, String colorUrl) {
		super();
		this.colorName = colorName;
		this.colorUrl = colorUrl;
	}
	
}
