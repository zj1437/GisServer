package org.pygisserver.common.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * @Description: 切图任务实例，用来存储当前正在进行切图的任务参数信息
 * @author:  
 * @date: 2019-11-28
 */
@Data
public class SeedTaskInstance implements Serializable {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 4708948255220423358L;

	private int taskId;
	
	/**
	 * 任务名称（图层+投影+格式+大小）
	 */
	private String taskName;
	
	/**
	 * 图层名称
	 */
	private String layerName;
	/**
	 * 样式名称
	 */
	private String styleName;
	
	/**
	 * 存储类型 1 psql 2 sqlite 3 mongodb 4 tdb 5 tms
	 */
	private int storeType;
	
	/**
	 * 存储路径
	 */
	private String storePath;
	
	/**
	 * 投影格式
	 */
	private String srs;
	
	/**
	 * 瓦片类型（image/jpeg,image/png）
	 */
	private String tileFormat;
	
	/**
	 * 切图类型（补全/重新切）
	 */
	private String seedType;
	
	/**
	 * 最大线程数
	 */
	private int threadNum;
	
	/**
	 * 切图起始级别
	 */
	private int zoomStart;
	
	/**
	 * 切图终止级别
	 */
	private int zoomEnd;
	
	/**
	 * 最小经度
	 */
	private Double minX;
	
	/**
	 * 最小纬度
	 */
	private double minY;
	
	/**
	 * 最大经度
	 */
	private double maxX;
	
	/**
	 * 最大纬度
	 */
	private double maxY;
	
	
	/**
	 * 最大任务数
	 */
	private int maxTaskNum;
	
	/**
	 * 瓦片宽度
	 */
	private int tileWidth;
	
	/**
	 * 瓦片高度
	 */
	private int tileHeight;
	
	/**
	 * 开始时间
	 */
	private Date startTime;

	public SeedTaskInstance(int threadNum, int zoomStart, int zoomEnd, int metaTilingX, int metaTilingY, int gutter,
			int tileWidth, int tileHeight) {
		super();
		this.threadNum = threadNum;
		this.zoomStart = zoomStart;
		this.zoomEnd = zoomEnd;
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
	}

	public SeedTaskInstance() {
	}
}
