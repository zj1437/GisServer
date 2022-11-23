package org.pygisserver.web.dao;

import org.pygisserver.common.entity.SeedTaskParam;
import org.pygisserver.common.entity.SeedTaskInstance;
import org.springframework.stereotype.Repository;

@Repository
public interface TileCacheMapper {

	SeedTaskParam queryGlobalSeedParam();
	
	/**
	 * 获取切图任务实例
	 * @param layerName
	 * @return
	 */
	SeedTaskInstance queryTaskInstance(String layerName);
	
	/**
	 * 保存切图全局配置信息
	 * @param seedParam
	 */
	void saveSeedTileConfig(SeedTaskParam seedParam);
	
	void updateExportPath(String exportPath);
}
