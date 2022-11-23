package org.pygisserver.common.entity;

import java.io.Serializable;

import lombok.Data;

/**
 * @Description: 切片任务实体，每一个切片图层对应一个任务 
 * @author: 
 * @date: 2019-11-26
 */
@Data
public class TileTaskEntity implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5117645818068601260L;

	private int taskId;
	
	/**
	 * 任务状态 0 等待中  1 进行中 2暂停 
	 */
	private int taskStatus;
	
	/**
	 * 图层名称
	 */
	private String layerName;
	
	/**
	 * 任务名称
	 */
	private String taskName;
	
	//format(20/1000=2%)
	private String progress;
	
	/**
	 * 已经切片的数据
	 */
	private long tiledTiles;
	
	/**
	 * 总的切片数据
	 */
	private long totalTiles;
	
	/**
	 * 已经用时
	 */
	//format 3分20秒
	private String spentTimeStr;
	
	private long spentTime;

	/**
	 * 剩余时间
	 */
	private String timeRemainingStr;
	
	/**
	 * 任务开始时间
	 */
	private String taskStartTime;
	
	/**
	 * 任务结束时间
	 */
	private long taskEndTime;
	
	/**
	 * 记录暂停前的切图记录数
	 */
	private long suspendTileSize;
	
	/**
	 * 暂停时，已使用时间
	 */
	private long suspendSpentTime;
	
	/**
	 * 任务类型，4是导出任务，其他均为切图任务，默认值为-1
	 */
	private int taskType;
	
	/**
	 * 切图级别，默认格式为start-end
	 */
	private String levelRange;

	public TileTaskEntity(String taskName, String progress, String spentTimeStr, String timeRemainingStr) {
		super();
		this.taskName = taskName;
		this.progress = progress;
		this.spentTimeStr = spentTimeStr;
		this.timeRemainingStr = timeRemainingStr;
	}

	public TileTaskEntity() {
		super();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TileTaskEntity other = (TileTaskEntity) obj;
		if (progress == null) {
			if (other.progress != null)
				return false;
		} else if (!progress.equals(other.progress))
			return false;
		if (spentTimeStr == null) {
			if (other.spentTimeStr != null)
				return false;
		} else if (!spentTimeStr.equals(other.spentTimeStr))
			return false;
		if (taskEndTime != other.taskEndTime)
			return false;
		if (taskId != other.taskId)
			return false;
		if (taskName == null) {
			if (other.taskName != null)
				return false;
		} else if (!taskName.equals(other.taskName))
			return false;
		if (taskStartTime == null) {
			if (other.taskStartTime != null)
				return false;
		} else if (!taskStartTime.equals(other.taskStartTime))
			return false;
		if (taskStatus != other.taskStatus)
			return false;
		if (tiledTiles != other.tiledTiles)
			return false;
		if (timeRemainingStr == null) {
			if (other.timeRemainingStr != null)
				return false;
		} else if (!timeRemainingStr.equals(other.timeRemainingStr))
			return false;
		if (totalTiles != other.totalTiles)
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((progress == null) ? 0 : progress.hashCode());
		result = prime * result + ((spentTimeStr == null) ? 0 : spentTimeStr.hashCode());
		result = prime * result + (int) (taskEndTime ^ (taskEndTime >>> 32));
		result = prime * result + taskId;
		result = prime * result + ((taskName == null) ? 0 : taskName.hashCode());
		result = prime * result + ((taskStartTime == null) ? 0 : taskStartTime.hashCode());
		result = prime * result + taskStatus;
		result = prime * result + (int) (tiledTiles ^ (tiledTiles >>> 32));
		result = prime * result + ((timeRemainingStr == null) ? 0 : timeRemainingStr.hashCode());
		result = prime * result + (int) (totalTiles ^ (totalTiles >>> 32));
		return result;
	}
}
