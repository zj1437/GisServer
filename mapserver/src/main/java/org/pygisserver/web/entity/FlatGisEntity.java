package org.pygisserver.web.entity;

import lombok.Data;

/**
 * flatgis/tms 相关属性封装类
 * @author admin
 *
 */
@Data
public class FlatGisEntity {
	/*
	 * 投影格式  flatgis/tms
	 */
    private String srs;
    /*
	 * 经纬度范围  flatgis/tms
	 */
    private String boundingBox;
    /*
	 * 瓦片宽度  flatgis/tms
	 */
    private int width;
    /*
	 * 瓦片高度  flatgis/tms
	 */
    private int height;
    /*
	 * 瓦片类型  flatgis/tms
	 */
    private String mimeType;
    /*
	 * 瓦片扩展名  flatgis/tms
	 */
    private String extension;
    /*
	 * 瓦片排列类型  该属性只有flatgis有
	 */
    private String tileType;
    
}
