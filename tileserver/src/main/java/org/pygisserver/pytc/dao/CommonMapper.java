package org.pygisserver.pytc.dao;

import java.util.List;
import java.util.Set;

import org.apache.ibatis.annotations.Param;
import org.pygisserver.common.entity.LayerMetaData;
import org.pygisserver.common.entity.ServerContact;
import org.pygisserver.common.pytc.entity.DataSourceEntity;
import org.pygisserver.common.pytc.entity.LayerStoreTypeInfo;
import org.pygisserver.common.pytc.entity.TableSource;
import org.pygisserver.common.pytc.entity.TileRequestInfo;
import org.springframework.stereotype.Repository;

/**
 * @Description: 通用信息查询
 * @author:
 * @date: 2019-12-09
 */
@Repository(value="commonMapper")
public interface CommonMapper {

	/**
	 * 获取当前的资源文件
	 * @return
	 */
	List<DataSourceEntity> queryDataSourceInfo();
	
	/**
	 * 查询图层对应的db数据
	 * @param layerName
	 * @return
	 */
	List<String> queryDataDbNameByLayer(String layerName);
	
	/**
	 * 删除表数据
	 * @param tableName
	 */
	void dropTableName(List<String> tableName);
	
	/**
	 * 清空表（只删除数据）
	 * @param tableName
	 */
	void flushTable(List<String> tableName);
	
	/**
	 * 获取资源id
	 * @param dbName
	 * @return
	 */
	int querySourceId(String dbName);
	
	/**
	 * 保存资源文件
	 * @param dataSource
	 */
	void saveDataSource(DataSourceEntity dataSource);
	
	/**
	 * 
	 * @param tableSource
	 */
	void savaTableSource(TableSource tableSource);
	
	/**
	 * 存储元数据信息
	 */
	void saveMetaInfo(LayerMetaData metaData);
	
	/**
	 * 创建数据库信息
	 * @param DBName 数据库名称
	 */
	void createDBSource(@Param("dbname") String dbname);
	
	/**
	 * 创建对应的数据表
	 */
	void createTableForDB(@Param("tableName") String tableName);
	
	/**
	 * 创建对应的数据表
	 */
	void createSqliteTableForDB(@Param("tableName") String tableName);
	
	/**
	 * 创建索引
	 */
	void createIndex(@Param("indexName") String indexName,@Param("tableName") String tableName);
	
	/**
	 * 删除数据库
	 * @param dataBaseNames
	 */
	void dropDataBase(List<String> dataBaseNames);
	
	/**
	 * 获取服务提供商
	 * @return
	 */
	ServerContact queryServerContact();
	
	/**
	 * 根据图层名称删除资源信息
	 * @param dbsource
	 */
	void deleteSourceTable(String dbsource);
	
	/**
	 * 获取当前图层存储信息
	 * @return
	 */
	List<LayerStoreTypeInfo> queryLayerStore(String layerName);
	
	/**
	 * 获取当前图层存储信息
	 * @return
	 */
	List<LayerStoreTypeInfo> queryLayerStoreByReq(TileRequestInfo tileInfo);
	
	/**
	 * 获取存储类型信息
	 * @param storeInfo
	 * @return
	 */
	void saveLayerStoreInfo(LayerStoreTypeInfo storeInfo);
	
	/**
	 * 修改数据库名称
	 * @param oldDbName 旧的数据库
	 * @param newDbName 新的数据库
	 */
	void updateDBName(@Param("oldDbName") String oldDbName,@Param("newDbName") String newDbName);
	
	/**
	 * 更新数据库中的数据源
	 */
	void updateDynamicSource(@Param("newLayer") String newLayerName,
							 @Param("newDbName") String dbName,
							 @Param("jdbcUrl") String jdbcName,
							 @Param("oldName") String oldName);
	
	/**
	 * 断开当前数据库的连接
	 * @param dbname
	 */
	void terminateBackend(@Param("dbName") String dbname);
	
	/**
	 * 删除当前图层
	 */
	void deleteLayerStore(Set<String> layerName);
	
	/**
	 * 删除当前图层
	 */
	void deleteLayerStoreByDatasource(String dataSource);
	
	/**
	 * 删除动态库表
	 */
	void deleteDynamicTableInfo(@Param("layerName") String layerName,@Param("sourceName") String sourceName);
	
	/**
	 * 删除切图任务表
	 */
	void deleteTaskEntityInfo(Integer serverId);
	
	/**
	 * 删除切图任务信息
	 */
	void deleteTileTaskInfo(@Param("serverId") Integer serverId,@Param("taskName") String taskName);
}
