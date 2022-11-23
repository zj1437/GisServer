package org.pygisserver.common.pytc.entity;

import java.io.Serializable;

import lombok.Data;

/**
 * 前台数据分页实体类
 * 主要用于分页数据显示
 * @author 
 * @since 2018-08-22
 */
@Data
public class PageBean implements Serializable {
	
	 /**
	 * 
	 */
	private static final long serialVersionUID = -2562707590476454195L;

	/**
	 * 当前页
	 */
    private int pageNum;
    
    /**
     * 每页显示量
     */
    private int pageSize;
    
    /**
     * 总页数
     */
    private int totalPage; 
    
    /**
     * 总数
     */
    private int totalCount;
    
    /**
     * 排序名称 
     */
    private String sidx;
    
    /**
     * 排序方式
     */
    private String sord;
}
