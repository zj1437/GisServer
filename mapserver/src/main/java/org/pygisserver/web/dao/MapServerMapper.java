package org.pygisserver.web.dao;

import org.apache.ibatis.annotations.Param;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.entity.PostGisConnectEntity;
import org.pygisserver.common.entity.ServerProtocolInfo;
import org.pygisserver.common.pytc.entity.RequestQueryCondition;
import org.pygisserver.common.ui.entity.LayerGroup;
import org.pygisserver.web.entity.*;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @Description: 地图服务mapper 
 * @author:  
 * @date: 2018-10-29
 */
@Repository
public interface MapServerMapper {

	
	List<String> queryExistsLayers();

	List<String> queryExistsRestLayers();

	/**
	 * 根据服务ids 获取服务名称
	 * @param ids
	 * @return
	 */
	List<String> queryLayerNamesByServerIds(List<Integer> ids);
	
	/**
	 * 获取系统版本信息
	 * @return 返回版本信息
	 */
	List<ServerProtocolInfo> queryServerProtocolInfo();
	
	/**
	 * 保存地图服务
	 * @param mapData
	 */
	void saveMapData(DataServerEntity mapData);

	/**
	 * 保存REST服务
	 * @param mapData
	 */
	void saveRestData(DataServerEntity mapData);

	/**
	 * 根据服务id获取图层组信息
	 * @param serverId
	 * @return
	 */
	List<LayerGroup> queryLayerGroupByServerId(int serverId);
	
	
	/**
	 * 根据服务名称获取图层组信息
	 * @param serverName
	 * @return
	 */
	List<LayerGroup> queryLayerGroupByServerName(String serverName);

	/**
	 * 批量保存服务数据
	 * @param mapDatas
	 */
	void saveMapServerDatas(List<DataServerEntity>  mapDatas);

	/**
	 * 批量保存rest服务数据
	 * @param mapDatas
	 */
	void saveRestServerDatas(List<DataServerEntity>  mapDatas);

	/**
	 * 保存对应地图数据的特征信息
	 * @param featureInfo
	 */
	void saveFeatureData(FeatureInfo featureInfo);
	
	/**
	 * 获取高程级别
	 * @param layer
	 * @return
	 */
	int queryZoomLevel(String layer);
	
	/**
	 * 存储缓存图层信息
	 */
	void saveTiledLayerInfo();
	
	/**
	 * 
	 */
	void saveStoreLayerInfo(StoreLayerInfo storeLayerInfo);
	
	
	/**
	 * 获取存储信息
	 * @param ids
	 * @return
	 */
	List<StoreLayerInfo> queryStoreInfo(List<Integer> ids);
	
	/**
	 * 查询服务数量
	 * @return
	 */
	int queryServerDataCount(RequestQueryCondition queryCondition);

	/**
	 * 查询rest服务数量
	 * @param queryCondition
	 * @return
	 */
	int queryRestDataCount(RequestQueryCondition queryCondition);

	/**
	 * 获取服务数据
	 * @return
	 */
	List<DataServerEntity> queryServerData(RequestQueryCondition queryCondition);

	/**
	 * 获取rest服务数据
	 * @param queryCondition
	 * @return
	 */
	List<DataServerEntity> queryRestData(RequestQueryCondition queryCondition);

	/**
	 * 根据服务id获取服务信息
	 * 
	 * @param serverId
	 * @return
	 */
	DataServerEntity queryServerInfoById(int serverId);

	/**
	 * 根据服务id获取rest服务信息
	 *
	 * @param serverId
	 * @return
	 */
	DataServerEntity queryRestInfoById(int serverId);
	
	/**
	 * 根据id信息获取服务数据
	 * @param ids
	 * @return
	 */
	List<DataServerEntity> queryServerDataByIds(List<Integer> ids);
	
	/**
	 * 获取服务信息通过服务名称
	 * @param serverId
	 * @return 返回服务信息
	 */
	DataServerEntity queryServerInfoByServerName(String layerName);


	/**
	 * 获取服务信息通过服务名称
	 * @param serverId
	 * @return 返回服务信息
	 */
	DataServerEntity queryRestInfoByServerName(String layerName);
	
	
	/**
	 * 获取切片服务数据
	 * @return
	 */
	List<DataServerEntity> queryTileServerData(RequestQueryCondition queryCondition);
	
	int querySeedTaskNum(RequestQueryCondition queryCondition);
	
	/**
	 * 编辑服务数据
	 * @param mapData
	 */
	void editServerData(DataServerEntity mapData);

	/**
	 * 编辑rest服务数据
	 * @param mapData
	 */
	void editRestData(DataServerEntity mapData);
	
	/**
	 * 编辑特征信息数据
	 * @param featureInfo
	 */
	void editFeatureInfo(FeatureInfo featureInfo);
	
	/**
	 * 编辑存储图层信息
	 * @param storeLayerInfo
	 */
	void editStorelayerInfo(StoreLayerInfo storeLayerInfo);
	
	/**
	 * 删除服务数量
	 */
	void deleteServerData(List<Integer> ids);

	/**
	 * 删除rest服务数量
	 * @param ids
	 */
	void deleteRestData(List<Integer> ids);

	/**
	 * 删除图层组信息
	 * @param ids
	 */
	void deleteLayerGroupData(List<Integer> ids);

	/**
	 * 删除图层组信息，通过图层名称
	 * @param layerName
	 */
	void deleteLayerGroupByLayerName(List<String> layerName);
	
	/**
	 * 修改服务状态（暂停或者启动）
	 * @param status
	 */
	void updateServerStatus(int serverId);

	/**
	 * 修改服务状态rest（暂停或者启动）
	 * @param status
	 */
	void updateRestStatus(int serverId);

	/**
	 * 修改服务状态（暂停或者启动）
	 * @param serverId
	 */
	void updateServerStatu(int serverId);
	
	/**
	 * 根据服务名称获取当前服务的bbox
	 * @param serverName
	 * @return
	 */
	String queryTileServerBbox(String serverName);
	
	/**
	 * 
	 * 根据图层名称获取当前该图层对应的图层组名称
	 * @return
	 */
	List<String> queryLayerGroups(List<String> layerNames);
	
	/**
	 * 修改服务切片状态
	 * @param status
	 */
	void updateServerTileStatus(@Param("status")int status,
								@Param("serverName")String serverName);
	
	/**
	 * 修改服务的状态
	 * @param status
	 * @param serverId
	 */
	void updateServerStatusByIds(@Param("status")int status,
								 @Param("list")Collection<Integer> serverIds);

	/**
	 * 修改rest服务的状态
	 * @param status
	 * @param serverId
	 */
	void updateRestStatusByIds(@Param("status")int status,
								 @Param("list")Collection<Integer> serverIds);
	
	/**
	 * 更新地图服务数据
	 * @param status
	 * @param serverId
	 */
	void updateMapServerData(List<DataServerEntity> mapServerData);


	/**
	 * 更新rest服务数据
	 * @param status
	 * @param serverId
	 */
	void updateRestServerData(List<DataServerEntity> mapServerData);

	
	/**
	 * 保存服务id
	 * @param styleInfo
	 */
	void saveStyleInfo(StyleInfo styleInfo);
	
	/**
	 * 编辑样式
	 * @param styleInfo
	 */
	void editStyleInfo(StyleInfo styleInfo);
	
	/**
	 * 保存或更新对应的数据
	 * @param rasterColor
	 */
	void saveOrUpdateColorModel(RasterColorMap rasterColor);
	
	/**
	 * 删除图层组特征数据信息
	 * @param serverId
	 */
	void deleteLayerGroupFeature(int serverId);
	
	
	void updateTileTaskInfo(@Param("oldlayer") String oldLayerName,
							@Param("newlayer") String newLayerName);
	
	void updateDynamicInfo(@Param("oldlayer") String oldLayerName,
						   @Param("newlayer") String newLayerName);
	
	void updateLayerStoreInfo(@Param("oldlayer") String oldLayerName,
							  @Param("newlayer") String newLayerName);
	
	void updateTileEntityInfo(@Param("oldlayer") String oldLayerName,
							  @Param("newlayer") String newLayerName);
	
	/**
	 * 
	 * @param colorMap
	 */
	void saveRasterInfo(RasterColorMap colorMap);
	
	/**
	 * 查询图层名称根据Token信息
	 * @param token
	 * @return
	 */
	String queryLayerNameByToken(String token);
	
	/**
	 * 获取服务状态
	 * @param ids 服务ids
	 * @return
	 */
	List<Integer> getServerStatusByIds(List<Integer> ids);
	
	/**
	 * 查询当前的数据是否存在
	 * @return
	 */
	List<Map<String,Object>> queryExistsServerName(List<String> serverName);
	
	/**
	 * 根据文件的url 获取服务id
	 * @param fileUrl
	 * @return
	 */
	int queryServerIdByFileUrl(String fileUrl);
	
	
	/**
	 * 保存图层组对应的图层信息
	 * @param layerGroups
	 */
	void saveLayerGroups(List<LayerGroup> layerGroups);

	/**
	 * 
	 * @param paramMap
	 * @return
	 */
	List<LayerGroup> queryLayerGroup(Map<String,Object> paramMap);

	/**
	 * 
	 * @param paramMap
	 */
	void editLayerGroup(@Param("oldLayerName") String oldLayerName,@Param("newLayerName") String newLayerName);
	
	/**
	 * 保存PostgisInfo
	 * @param connectEntity
	 */
	void savePostgisInfo(PostGisConnectEntity connectEntity);
	
	/** 
	 * 编辑PostgisInfo
	 * @param connectEntity
	 */
	void editPostGis(PostGisConnectEntity paramMap);
	
	/**
	 * 查询PostgisInfo
	 * @param id
	 */
	List<PostGisConnectEntity> queryPostGis(int id);
	
	/**
	 * 查询PostgisInfo
	 * @param sourceName
	 */
	PostGisConnectEntity queryPostGisBySourceName(String sourceName);
	
	/**
	 * 删除PostgisInfo
	 * @param ids
	 */
	void deletePostgisInfo(List<Integer> ids);
	
	/**
	 * 根据数据源名称获取已经发布的服务名称
	 * @return
	 */
	List<String> queryServerNameBySourceName(String sourceName);
	
	/**
	 * 根据数据源名称获取已经发布的服务名称
	 * @return
	 */
	List<String> queryPostGISNamesById(List<Integer> ids);
	
	/**
	 * 根据数据源id查询已经发布的服务id
	 * @return
	 */
	List<Integer> queryServerIdBySourceId(Integer sourceId);
	
	List<String> queryExistsSources();
	
	/**
	 * 保存posftgis与server的中间表信息
	 */
	void savePostgisServerInfo(PostgisServerInfo psInfo);
	
	String getSeaChartScaleByServerId(int serverId);
	
	List<String> queryLayerNamesByDataType(int dataType);

	List<Map<String, Integer>> queryGroupServerShow(@Param("ids") List<Integer> ids);

	/**
	 * 查询图层基础信息
	 * @param layerName
	 * @return
	 */
	List<LayerBaseInfo> queryLayerBaseInfo(RequestQueryCondition layerName);

	/**
	 * 当发布服务勾选生成金字塔，但此服务已有缓存文件时不再生成金字塔缓存，修改服务状态
	 *
	 * @param serverId serverId
	 */
	void updateStatusAndOverview(int serverId);
	
	/**
	 * 查询矢量服务图层
	 * @return
	 */
	List<String> queryVectorLayer();
	
	/**
	 * 更新路径数据
	 * @param queryPoints 查询点
	 * @param isUpdate 是否跟新
	 */
	void updateRoadCost(List<String> queryPoints);
	
	
	/**
	 * 还原路径数据
	 * @param queryPoints 查询点
	 * @param isUpdate 是否跟新
	 */
	void restoreRoadCost(List<String> queryPoints);

	/**
	 * 修改ImageMosaic的输入和输出透明色
	 *
	 * @param inputTransparentColor
	 * @param outputTransparentColor
	 * @param serverId
	 */
	void editTransparentColor(@Param("inputTransparentColor")String inputTransparentColor,
							  @Param("outputTransparentColor")String outputTransparentColor,
							  @Param("serverId")int serverId);
}
