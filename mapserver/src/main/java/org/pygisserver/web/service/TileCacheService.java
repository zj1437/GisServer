package org.pygisserver.web.service;

import java.util.List;

import org.pygisserver.common.entity.SeedTaskParam;
import org.pygisserver.common.pytc.entity.RequestQueryCondition;
import org.pygisserver.common.entity.SeedTaskInstance;
import org.pygisserver.web.entity.DataServerEntity;

public interface TileCacheService {
 
	String transformCordinateBbox(String layerName,int epsgCode);
	
	/**
	 * 获取切图任务信息
	 * @return
	 */
	List<DataServerEntity> querySeedTaskInfo(RequestQueryCondition queryCondition);
	
	int querySeedTaskNum(RequestQueryCondition queryCondition);
	
	
	/**
	 * 保存全局切图参数
	 * @param seedParam
	 */
	void saveSeedParam(SeedTaskParam seedParam);
	
	/**
	 * 获取全局切图参数
	 * @return
	 */
	SeedTaskParam queryGlobalSeedParam();
	
	void updateExportPath(String exportPath);
	
	/**
	 * 计算当前图层
	 * @param layer
	 * @param gridSetId
	 * @param format
	 * @param startZoom
	 * @param endZoom
	 * @return
	 */
//	double calcuteTileSize(String layer, String gridSetId,String format,int startZoom, int endZoom, int tileWidth,String reqIP);
	
	double calcuteTileSize(String layer, String gridSetId,String format,int startZoom, int endZoom, String reqIP);
	
	/**
	 * 查询切图任务实例
	 * @param taskName 任务名称
	 * @return
	 */
	SeedTaskInstance queryTaskInstance(String taskName);
	
}
