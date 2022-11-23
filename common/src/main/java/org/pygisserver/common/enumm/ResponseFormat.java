package org.pygisserver.common.enumm;

import org.apache.commons.lang3.StringUtils;

public enum ResponseFormat {

	PNG("png","image/png"),

	JPEG("jpeg","image/jpeg"),
	
	JPG("jpg","image/jpeg"),
	
	TIFF("tiff","image/tiff"),
	
	OSGB("osgb","image/osgb"),
	
	TERRAIN("terrain","image/terrain"),
	
	PBF("pbf","application/x-protobuf;type=mapbox-vector"),
	
	PROTOBUF("protobuf","application/x-protobuf;type=mapbox-vector"),
	
	GEOJSON("geojson","application/json;type=geojson"),
	
	TOPOJSON("topojson","application/json;type=topojson"),

	HEIGHTARRAY("heightarray","image/heightarray");
	
	private String prefix;
	
	private String format;
	
	ResponseFormat(String prefix,String format){
		this.prefix = prefix;
		this.format = format;
	}

	public static String getFormatByPrefix(String prefix) {
		if (StringUtils.isBlank(prefix)) {
			return null;
		}
		for (ResponseFormat responseFormat : ResponseFormat.values()) {
			if (prefix.equals(responseFormat.getPrefix())) {
				return responseFormat.getFormat();
			}
		}
		return ResponseFormat.JPEG.getFormat();
	}
	
	/**
	 * @return the prefix
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * @param prefix the prefix to set
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/**
	 * @return the format
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * @param format the format to set
	 */
	public void setFormat(String format) {
		this.format = format;
	}
	
}
