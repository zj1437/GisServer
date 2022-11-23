package org.pygisserver.common.entity;

import java.util.Map;

import org.pygisserver.common.pytc.entity.TileResourceInfo;

import lombok.Data;

@Data
public class SaveTdbImageEntity {
	private TileResourceInfo tileResourceInfo;
	private TDBDataConfig config;
	private Map<String, String> tdbParams;
	
	public SaveTdbImageEntity(){}
	
	public SaveTdbImageEntity(TileResourceInfo tileResourceInfo, TDBDataConfig config, Map<String, String> tdbParams){
		this.tileResourceInfo = tileResourceInfo;
		this.config = config;
		this.tdbParams = tdbParams;
	}
}
