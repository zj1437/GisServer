package org.pygisserver.common.entity;

import java.io.Serializable;

import lombok.Data;

/**
 * @Description: 分析结果 
 * @author: 
 * @date: 2021-06-21
 */
@Data
public class PostureEntity implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1394113582995671092L;


	private String fileName;
	
	
	private String path;
	
	public PostureEntity(String fileName,String path) {
		this.fileName = fileName;
		this.path = path;
	}
	public PostureEntity() {
	}
}
