package org.pygisserver.common.entity;

import lombok.Data;

/**
 * @Description: 响应实体
 * @author:  
 * @date: 2018-10-24
 */
@Data
public class ReponseEntity<T> {

	/**
     * 返回状态码
     */
	private int statusCode;

	/**
	 * 状态信息
	 */
	private String msg;
	
    /**
     * 返回的数据list
     */
    private T items;
    
    /**
     * 总数
     */
    private int totalNum;
    
    /**
     * 当前第几页
     */
    private int pageNo;
    
    /**
     * 每页显示数量
     */
    private int pageSize;

    /**
     * 成功状态信息
     * @param statusCode
     * @param msg
     * @param items
     */
	public ReponseEntity(int statusCode,String msg, T items) {
		super();
		this.statusCode = statusCode;
		this.msg = msg;
		this.items = items;
	}
	
	/**
	 * 错误状态的情况直接返回信息
	 * @param statusCode
	 * @param msg
	 */
	public ReponseEntity(int statusCode,String msg) {
		super();
		this.statusCode = statusCode;
		this.msg = msg;
	}

	/**
	 * 
	 * @param statusCode
	 * @param msg
	 * @param items
	 * @param totalNum
	 */
	public ReponseEntity(int statusCode, String msg, T items, int totalNum) {
		super();
		this.statusCode = statusCode;
		this.msg = msg;
		this.items = items;
		this.totalNum = totalNum;
	}

	public ReponseEntity(int statusCode, String msg, T items, int totalNum, int pageNo) {
		super();
		this.statusCode = statusCode;
		this.msg = msg;
		this.items = items;
		this.totalNum = totalNum;
		this.pageNo = pageNo;
	}
	
	
	
	public ReponseEntity(int statusCode, String msg, T items, int totalNum, int pageNo,int pageSize) {
		super();
		this.statusCode = statusCode;
		this.msg = msg;
		this.items = items;
		this.totalNum = totalNum;
		this.pageNo = pageNo;
		this.pageSize = pageSize;
	}
	
}
