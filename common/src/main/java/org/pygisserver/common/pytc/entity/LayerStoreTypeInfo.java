package org.pygisserver.common.pytc.entity;

import lombok.Data;

/**
 * @Description: 图层存储类型信息 
 * @author:  
 * @date: 2020-05-25
 */
@Data
public class LayerStoreTypeInfo {

	/**
	 * 数据资源
	 */
	private String dataSource;
	
	/**
	 * 图层名称
	 */
	private String layerName;
	
	/**
	 * 存储类型
	 */
	private int storeTypeId;
	
	/**
	 * 存储路径
	 */
	private String storePath;
	
	/**
	 * 切图格式
	 */
	private String tileFormat;
	
	/**
	 * 瓦片大小
	 */
	private int tileSize;
	
	/**
	 * 投影格式
	 */
	private String srs;
	
	public LayerStoreTypeInfo(String dataSource,String layerName,int storeTypeId,String tileFormat) {
		this.dataSource = dataSource;
		this.layerName = layerName;
		this.storeTypeId = storeTypeId;
		this.tileFormat = tileFormat;
	}
	
	public LayerStoreTypeInfo(String dataSource,String layerName,int storeTypeId,String storePath,String tileFormat) {
		this.dataSource = dataSource;
		this.layerName = layerName;
		this.storeTypeId = storeTypeId;
		this.storePath = storePath;
		this.tileFormat = tileFormat;
	}
	
}
