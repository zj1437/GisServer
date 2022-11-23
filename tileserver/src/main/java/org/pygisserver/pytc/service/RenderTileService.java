package org.pygisserver.pytc.service;

import org.pygisserver.common.entity.SeedTaskInstance;

public interface RenderTileService {

	/**
	 * 手动切图
	 * @param tileTaskInstance
	 */
	void seedTaskByParam(SeedTaskInstance tileTaskInstance);
	
	/**
	 * 激活已经暂停的任务
	 * @param layer
	 */
	void activeSeedTask(String layer);
	
	/**
	 * 停止进行中的任务
	 * @param layer
	 */
	void stopSeedTask(String layer);
}
