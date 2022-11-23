package org.pygisserver.common.pytc.entity;

import java.util.List;

import lombok.Data;

/**
 * @Description: 数据源类型 
 * @author:  
 * @date: 2019-12-09
 */
@Data
public class DataSourceEntity {

	private int id;
	
	private String sourceKey;

	private String layerName; 
	
	private String userName;
	
	private String password;
	
	private String driverClass;
	
	private String jdbcUrl;
	
	private List<TableSource> tableSources;
	
	public DataSourceEntity(String layerName,String sourceKey, String userName, String password, String driverClass, String jdbcUrl) {
		super();
		this.layerName = layerName;
		this.sourceKey = sourceKey;
		this.userName = userName;
		this.password = password;
		this.driverClass = driverClass;
		this.jdbcUrl = jdbcUrl;
	}
	
	public DataSourceEntity(String layerName,String sourceKey) {
		super();
		this.layerName = layerName;
		this.sourceKey = sourceKey;
	}
}
