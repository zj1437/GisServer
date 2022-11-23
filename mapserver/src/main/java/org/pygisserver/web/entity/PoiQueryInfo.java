package org.pygisserver.web.entity;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * @Description: POI的基础信息 
 * @author:  
 * @date: 2019-01-26
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class PoiQueryInfo extends QueryBaseInfo {

    /**
     * 区域
     */
    private String region;
    
    /**
     * 点信息
     */
    private String point;

    /**
     * 直线
     */
    private String lineString;

    /**
     * 矩形
     */
    private String bounds;

    /**
     * 多边形
     */
    private String polygon;
    
    
    /**
     * 查询关键词
     */
    private List<String> keyWordList;
	

	public PoiQueryInfo(String region) {
		super();
		this.region = region;
	}
}
