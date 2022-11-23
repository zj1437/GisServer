package org.pygisserver.common.enumm;

import org.apache.commons.lang3.StringUtils;

/**
 * @Description: 文件类型枚举类 
 * @author: 
 * @date: 2019-07-18
 */
public enum DataTypeEnum {
	
	VECTOR("VECTOR",0),
	
	SHAPE("Shape",0),
	
	PostGIS("PostGIS",0),
	
	RASTER("RASTER",1),
	
	TIFF("GeoTIFF",1),
	
	ERDASIMG("ERDASImg",1),
	
	WMS("WMS",3),
	
	GROUP("GROUP",4),
	
//	Texx("Texx",2),
	
	TILED("TILED",6),
	
	MBTILES("MBTiles",6),
	
	FlatGis("FlatGis",6),
	
	TDB("TDB",6),
	
	TMS("TMS",6),
	
//	ERROR("ERROR",3),
	
	REMOTE("wmsremote",5),
	
	ONLINE("ONLINE",7),

	REST("REST",8);

	
	private String fileType;
	
	/**
	 * 文件code,表示当前是矢量还是栅格、或者是自定义数据
	 * 0是矢量、1是栅格、2是自定义数据
	 */
	private int fileCode;
	
	/**
	 * @return the fileType
	 */
	public String getFileType() {
		return fileType;
	}

	/**
	 * @param fileType the fileType to set
	 */
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	/**
	 * @return the fileCode
	 */
	public int getFileCode() {
		return fileCode;
	}

	/**
	 * @param fileCode the fileCode to set
	 */
	public void setFileCode(int fileCode) {
		this.fileCode = fileCode;
	}

	DataTypeEnum(String type,int code) {
		this.fileCode = code;
		this.fileType = type;
	}
	
	public static int getFileCode(String type) {
		if (StringUtils.isBlank(type)) {
			return 00;
		}
		for (DataTypeEnum dataType : DataTypeEnum.values()) {
			if (dataType.fileType.equalsIgnoreCase(type)) {
				return dataType.fileCode;
			}
		}
		//返回默认的错误数据
		return 00;
	}
}
