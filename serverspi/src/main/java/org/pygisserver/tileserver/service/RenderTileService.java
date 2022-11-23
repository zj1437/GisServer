package org.pygisserver.tileserver.service;

import java.util.List;

import org.pygisserver.common.entity.SeedTaskInstance;
import org.pygisserver.common.entity.TileTaskEntity;
import org.pygisserver.common.entity.UserLogbackInfo;
import org.pygisserver.common.pytc.entity.RequestQueryCondition;

/**
 * @Description: dubbo接口
 * @author: 刘洋 
 * @date: 2021-04-09
 */
public interface RenderTileService {
	
	/**
	 * 手动切图
	 * @param tileTaskInstance
	 */
	void seedTaskByParam(SeedTaskInstance tileTaskInstance, int openOverView);
	
	/**
	 * 切图之前先判断是否存在
	 * @param tileTaskInstance
	 */
	boolean submitTaskIsSeeding(SeedTaskInstance tileTaskInstance,boolean isExportTask);
	
	/**
	 * 切图之前先判断是否存在
	 * @param tileTaskInstance
	 */
	boolean exportServerData(SeedTaskInstance tileTaskInstance);
	
	boolean isCanDownloadTmsFile(SeedTaskInstance taskInstance);
	
	/**
	 * 自动开始切图
	 * @param layerName
	 */
	void seedTaskByLayerName(String layerName, int serverId, String reqIP, int openOverView);
	
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
	
	void deleteTdbCacheInfo(String taskName,String serverName);
	
	/**
	 * 
	 * @param ids
	 */
	void stopSeedTaskByIds(List<Integer> ids);
	
	/**
	 * 查询切图任务列表信息
	 * @return
	 */
	List<TileTaskEntity> queryTileTaskInfo(RequestQueryCondition queryCondition);
	
	/**
	 * 查询切图任务列表中任务总数
	 * @return
	 */
	int queryTileTaskNum(RequestQueryCondition queryCondition);
	
	/**
	 * 删除正在进行中切图任务
	 * @param taskName
	 */
	void deleteTileTask(List<String> taskNames,boolean isDelExportData);
	
	/**
	 * 清理切图任务
	 * @param layerName
	 */
	void flushTileTask(String layerName,boolean flag,boolean isClearExportFile);
	
	/**
	 * 设置日志信息
	 * @param logInfo
	 */
	void resetLogbackInfo(UserLogbackInfo logInfo);
	
	/**
	 * 更新缓存信息
	 * @param layerName
	 */
	void updateCacheInfo(String layerName);
	
	void addWaitTileTask(String serverName);
}
