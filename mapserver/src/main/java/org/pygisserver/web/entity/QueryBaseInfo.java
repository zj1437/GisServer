package org.pygisserver.web.entity;

import lombok.Data;

/**
 * @Description: 查询基础信息
 * @author:
 * @date: 2019-01-26
 */
@Data
public class QueryBaseInfo {

	/**
	 * 查询关键词
	 */
	private String keyWords;
    
    /**
     * POI所属类型
     */
    private String type;
    
    /**
     * 检索半径（圆形区域或者矩形，线段的缓冲半径）
     */
    private double distance;
    
    /**
     * 查询数据范围(1分页查询，2查询所有)
     */
    private int scope;
    
    /**
     * 是否限制城市 1表示限制、2表示不限制
     */
    private int cityLimit;
	
	/**
     * 第几页
     */
    private int pageNum;
    
    /**
     * 页面大小(默认每次显示的条数)
     */
    private int pageSize;
    
    /**
     * 当前poi数量
     */
    private int count;
    
    /**
     * 关键字的长度，决定数据库使用哪种索引处理
     */
    private int keyWordsLength;
}
