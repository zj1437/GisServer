package org.pygisserver.pytc.entity;

import java.util.concurrent.Future;

import lombok.Data;

/**
 * @Description: 已经提交的切图任务
 * @author:  
 * @date: 2021-04-02
 */
@Data
public class SubmittedSeedTask {

	private TileTask tileTask;
	
	private Future<TileTask> future;
	
	public SubmittedSeedTask(TileTask tileTask,Future<TileTask> future) {
		this.tileTask = tileTask;
		this.future = future;
	}
	
}
