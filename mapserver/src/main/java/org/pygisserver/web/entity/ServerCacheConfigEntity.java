package org.pygisserver.web.entity;

import lombok.Data;

@Data
public class ServerCacheConfigEntity {

	/**
	 * id
	 */
	private int cacheId;
	
	/**
	 * 内存最小配置
	 */
	private int minRam;

	/**
	 * 内存最大配置
	 */
	private int maxRam;
	
	/**
	 * 用户设定的内存配置
	 */
	private int actualRam;
	
	/**
	 * 磁盘最小配置
	 */
	private int minDisk;

	/**
	 * 磁盘最大配置
	 */
	private int maxDisk;
	
	/**
	 * 用户设定的磁盘配置
	 */
	private int actualDisk;
	
	/**
	 * 用户设定的磁盘路径
	 */
	private String cachePath;
	
	/**
	 * 内存缓存使用开关
	 */
	private int useRedisCache;
}
