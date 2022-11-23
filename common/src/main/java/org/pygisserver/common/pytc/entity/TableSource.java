package org.pygisserver.common.pytc.entity;

import lombok.Data;

/**
 * @Description: 
 * @author:  
 * @date: 2020-05-22
 */
@Data
public class TableSource {

	private int id;
	
	private int sourceId;
	
	private String tableName;
	
	public TableSource(int sourceId,String tableName) {
		this.sourceId = sourceId;
		this.tableName = tableName;
	}
}
