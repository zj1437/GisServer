package org.pygisserver.common.pytc.entity;

import java.io.Serializable;

import org.opengis.geometry.BoundingBox;

import lombok.Data;

/**
 * @Description: 瓦片请求信息 
 * @author: 
 * @date: 2019-12-09
 */
@Data
public class TileRequestInfo implements Cloneable,Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1865685718406823907L;

	private int serverId;

	private String layerName;
	
	private String style;
	
	private String mineType;
	
	private String gridSet;
	
	private int tileRow;
	
	private int tileCol;

	private int zoomLevel;
	
	private String request;
	
	private boolean isCreateThumbal = false;
	
	/**
	 * 是否透明
	 */
	private boolean transparent;
	
	private int tileWidth;
	
	private int tileHeight;
	
	/**
	 * wms请求时的bbox,暂时新增到这个类中
	 */
	private BoundingBox bbox;
	
	/**
	 * wms请求图片宽度
	 */
	private int imageWidth;
	
	/**
	 * wms请求图片高度
	 */
	private int imageHeight;
	
	/**
	 * 瓦片请求范围
	 */
	private int[] tileRequestRange;
	
	
	private String systemCachePath;
	
	public TileRequestInfo(String layerName, String style, String mineType, String gridSet, int tileRow, int tileCol,
			int zoomLevel) {
		super();
		this.layerName = layerName;
		this.style = style;
		this.mineType = mineType;
		this.gridSet = gridSet;
		this.tileRow = tileRow;
		this.tileCol = tileCol;
		this.zoomLevel = zoomLevel;
	}

	public TileRequestInfo() {
		super();
	}

	public TileRequestInfo(int tileRow, int tileCol, int zoomLevel) {
		super();
		this.tileRow = tileRow;
		this.tileCol = tileCol;
		this.zoomLevel = zoomLevel;
	}
	
	
	
	@Override
	public TileRequestInfo clone() throws CloneNotSupportedException {
		return (TileRequestInfo) super.clone();
	}

	public TileRequestInfo(String layerName, String gridSet,String mineType) {
		super();
		this.layerName = layerName;
		this.mineType = mineType;
		this.gridSet = gridSet;
	}

}
