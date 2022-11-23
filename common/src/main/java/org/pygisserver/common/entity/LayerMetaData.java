package org.pygisserver.common.entity;

import lombok.Data;

@Data
public class LayerMetaData {

	private String layerName;
	
	private String format;
	
	private String srs;

	public LayerMetaData(String layerName, String format, String srs) {
		super();
		this.layerName = layerName;
		this.format = format;
		this.srs = srs;
	}
}
