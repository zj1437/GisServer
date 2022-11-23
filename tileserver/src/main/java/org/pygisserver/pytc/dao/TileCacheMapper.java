package org.pygisserver.pytc.dao;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.ibatis.annotations.Param;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.entity.PostGisConnectEntity;
import org.pygisserver.common.entity.SeedTaskInstance;
import org.pygisserver.common.entity.SeedTaskParam;
import org.pygisserver.common.entity.ServerProtocolInfo;
import org.pygisserver.common.entity.TileTaskEntity;
import org.pygisserver.common.pytc.entity.RequestQueryCondition;
import org.pygisserver.common.pytc.entity.TileResourceInfo;
import org.pygisserver.common.ui.entity.LayerGroup;
import org.springframework.stereotype.Repository;

@Repository(value="tileCacheMapper")
public interface TileCacheMapper {

	/**
	 * 更新切片图层信息
	 * @param tileLayerInfo
	 */
	void updateTileTaskEntity(List<TileTaskEntity> tileTaskEntity);
	
	/**
	 * 保存瓦片切图任务
	 * @param tileTaskEntity
	 */
	void saveTileTaskEntity(TileTaskEntity tileTaskEntity);
	
	/**
	 * 保存切片图层信息
	 * @param tileLayerInfo
	 */
	void clearWrongStatusTasks(String taskName);
	
	/**
	 * 根据任务名称获取瓦片任务信息集合
	 * @param tileLayerInfo
	 */
	List<TileTaskEntity> queryTileTaskEntity(List<String> taskNames);
	
	
	/**
	 * 根据任务名称获取瓦片任务信息集合
	 * @param tileLayerInfo
	 */
	List<TileTaskEntity> queryTileTaskEntityByServerId(List<Integer> taskIds);
	
	List<SeedTaskInstance> queryTmsTaskEntityByParam(String layerName);
	
	/**
	 * 查询条件
	 * @param condition
	 * @return
	 */
	List<TileTaskEntity> queryTileTaskEntityByParam(RequestQueryCondition condition);
	
	/**
	 * 查询切图任务列表中任务总数
	 * @return
	 */
	int queryTileTaskNum(RequestQueryCondition queryCondition);
	
	/**
	 * 删除TileTaskEntity
	 * @param tileLayerInfo
	 */
	void deleteTileTaskEntityById(List<String> taskNames);
	
	
	/**
	 * 更新TileTaskEntity状态
	 * @param tileLayerInfo
	 */
	void updateTileTaskEntityStatus(@Param("taskName") String taskName,
				                    @Param("taskStatus") int taskStatus);
	
	/**
	 * 获取系统版本信息
	 * @return 返回版本信息
	 */
	List<ServerProtocolInfo> queryServerProtocolInfo();
	
	/**
	 * 查询缓存配置信息
	 * @return
	 */
	SeedTaskParam queryTileCacheConfig();

	
	/**
	 * 
	 * @param zoomLevel
	 * @param tileRow
	 * @param tileCol
	 * @return
	 */
	TileResourceInfo queryTileImageInfo(@Param("table") String table,
									  	@Param("x") int tileCol,
									  	@Param("y") int tileRow,
									  	@Param("z") int zoomLevel);
	

	void saveTileResourceInfo(@Param("resourceInfo") TileResourceInfo tileResourceInfo,
							  @Param("tableName") String tableName);
	
	
	/**
	 * 更新任务状态
	 * @param layerInfo
	 * @param status
	 */
	void updateLayerStatus(@Param("layerName") String layerInfo,
						   @Param("status")int status);
	
	/**
	 * 获取图层名称
	 * @param ids
	 * @return
	 */
	List<String> queryTileLayerName(List<Integer> ids);
	
	/**
	 * 删除任务
	 * @param taskNames
	 */
	void deleteTileTaskInstance(List<String> taskNames);
	
	/**
	 * 删除资源表
	 * @param layerNames
	 */
	void deleteSourceTable(Set<String> layerNames);
	
	/**
	 * 
	 */
	void openSqliteWALMode();
	
	/**
	 * 
	 * @param newName 新名称
	 * @param oldName 旧名称
	 */
	void updateLayerStoreInfo(@Param("newName") String newName,@Param("oldName") String oldName);

	/**
	 * 根据数据库名称获取已经存在的表名称
	 * @param sourceName
	 * @return
	 */
	List<String> queryExistsTableNames(String sourceName);
	
	//重构切图任务代码开始
	SeedTaskParam queryGlobalSeedParam();
	
	/**
	 * 获取切图任务实例
	 * @param layerName
	 * @return
	 */
	SeedTaskInstance queryTaskInstance(String taskName);
	
	/**
	 * 获取切图任务实例
	 * @param layerName
	 * @return
	 */
	List<SeedTaskInstance> queryTaskInstancesByLayerName(String layername);
	
	/**
	 * 查询服务基础信息
	 * @param layerName
	 * @return
	 */
	List<LayerBaseInfo> queryLayerBaseInfo(RequestQueryCondition layerName);
	
	
	/**
	 * 保存切图任务信息
	 * @param taskInstance
	 */
	void saveTileTaskInstance(SeedTaskInstance taskInstance);
	
	void updateServerStatus(@Param("layerName") String layerName,
							@Param("status") int status);
	
	
	PostGisConnectEntity queryPostGisBySourceName(String dataSourceName);

	void updateTileTaskTiles(@Param("totalTiles") int totalTiles,
							 @Param("taskName") String taskName,
							 @Param("status") int status,
							 @Param("startSeedTime") Date startSeedTime);
	
	void updateTaskInstance(@Param("taskName") String taskName,
							@Param("startSeedTime") Date startSeedTime);

	int queryDataformatByServerid(@Param("serverid") int serverid);
}
