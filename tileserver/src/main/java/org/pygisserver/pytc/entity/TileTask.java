package org.pygisserver.pytc.entity;

import org.pygisserver.pytc.thread.SeedTaskTileQueue;

import lombok.Data;

/**
 * @Description: 切图线程任务 ,主要记录当前线程的状态，线程的id，
 * 
 * @author:  
 * @date: 2021-04-02
 */
@Data
public class TileTask {

	 /**
	  * @Description: 切片线程状态 
	  * @author:  
	  * @date: 2021-04-02
	  */
	 public static enum TILESTATUS {
	        READY,  
	        RUNNING, 
	        DONE,    
	        DEAD
	 };
	
	/**
	 * 线程状态
	 */
	private TILESTATUS threadState;
	 
	/**
	 * 任务id
	 */
	private int taskId;
	
	/**
	 * 任务名称
	 */
	private String taskName;
	
	
	private String layerName;
	
	/**
	 * 总瓦片数
	 */
	private long totalSize;
	
	/**
	 * 每个任务对应的瓦片队列
	 */
	private SeedTaskTileQueue seedTaskTileQueue;
	
	/**
	 * 花费时间
	 */
	private long spentTime;
	
	/**
	 * 剩余时间
	 */
	private long timeRemaining;
	
	
	/**
	 * 已完成瓦片
	 */
	private long tilesDone;
	
	/**
	 * 记录开始时间
	 */
	private long startTime;
	
	/**
	 * 0 实时渲染  1 瓦片拼接
	 */
	private int tileRenderType;  
	
	/**
	 * 瓦片任务是否中断（默认false）
	 */
	private boolean terminate = false;
	
	/**
	 * 任务启动的线程数
	 */
	private int threadCount;
	
	/**
	 * 中间级别
	 */
	private int mediaLevel;
	
	/**
	 * 中间级别数量
	 */
	private int mediaLvelTileCount;
	
	
	public TileTask(SeedTaskTileQueue seedTaskTileQueue,String taskName,String layerName) {
		this.threadState = TILESTATUS.READY;
		this.seedTaskTileQueue = seedTaskTileQueue;
		this.taskName = taskName;
		this.layerName = layerName;
	}
}
